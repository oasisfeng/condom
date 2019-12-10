/*
 * Copyright (C) 2017 Oasis Feng. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oasisfeng.condom;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static android.content.pm.PackageManager.GET_PROVIDERS;
import static android.content.pm.PackageManager.GET_RECEIVERS;
import static android.content.pm.PackageManager.GET_SERVICES;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.Q;

/**
 * Process-level condom
 *
 * Created by Oasis on 2017/4/17.
 */
@Keep
public class CondomProcess {

	/**
	 * Install the condom protection for current process if it is not the default process.
	 *
	 * <p>This method must be called in {@link Application#onCreate()} to eliminate potential leakage.
	 */
	public static void installExceptDefaultProcess(final Application app) {
		installExceptDefaultProcess(app, new CondomOptions());
	}

	/**
	 * Install the condom protection for current process if it is not the default process.
	 *
	 * <p>This method must be called in {@link Application#onCreate()} to eliminate potential leakage.
	 */
	public static void installExceptDefaultProcess(final Application app, final CondomOptions options) {
		validateCondomOptions(options);
		final String current_process_name = getProcessName(app);
		if (current_process_name == null) return;
		final String default_process_name = app.getApplicationInfo().processName;
		if (! current_process_name.equals(default_process_name)) install(app, current_process_name, options);
	}

	/**
	 * Install the condom protection for current process if its process name matches. This method should be called in {@link Application#onCreate()}.
	 *
	 * @param process_names list of processes where Condom process should NOT be installed, in the form exactly as defined
	 *                      by <code>"android:process"</code> attribute of components in <code>AndroidManifest.xml</code>.
	 *                      <b>BEWARE: Default process must be explicitly listed here if it is expected to be excluded.</b>
	 */
	public static void installExcept(final Application app, final CondomOptions options, final String... process_names) {
		if (process_names.length == 0) throw new IllegalArgumentException("At lease one process name must be specified");
		validateCondomOptions(options);
		final String current_process_name = getProcessName(app);
		if (current_process_name == null) return;
		for (final String process_name : process_names)
			if (! current_process_name.equals(getFullProcessName(app, process_name))) {
				install(app, current_process_name, options);
				return;
			}

		if ((app.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) validateProcessNames(app, process_names);
	}

	/**
	 * Install the condom protection for current process. This method should generally never be called in the main process of app.
	 *
	 * <p>It is suggested to use {@link android.content.ContentProvider ContentProvider} with "android:process", to install it in specified process.
	 */
	public static void installInCurrentProcess(final Application app, final String tag, final CondomOptions options) {
		install(app, tag, options);
	}

	private static void validateCondomOptions(final CondomOptions options) {
		if (options.mKits != null && ! options.mKits.isEmpty())
			throw new IllegalArgumentException("CondomKit is not yet compatible with CondomProcess. " +
				"If you really need this, please submit a feature request on GitHub issue tracker, with the use case.");
	}

	private static void validateProcessNames(final Application app, final String[] process_names) {
		final Thread thread = new Thread(() -> doValidateProcessNames(app, process_names));
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	private static void doValidateProcessNames(final Application app, final String[] process_names) {
		try {
			final PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(),
					GET_ACTIVITIES | GET_SERVICES | GET_RECEIVERS | GET_PROVIDERS);
			final Set<String> defined_process_names = new HashSet<>();
			if (info.activities != null) for (final ActivityInfo activity : info.activities) defined_process_names.add(activity.processName);
			if (info.services != null) for (final ServiceInfo service : info.services) defined_process_names.add(service.processName);
			if (info.receivers != null) for (final ActivityInfo receiver : info.receivers) defined_process_names.add(receiver.processName);
			if (info.providers != null) for (final ProviderInfo provider : info.providers) defined_process_names.add(provider.processName);
			for (final String process_name : process_names)
				if (! defined_process_names.contains(getFullProcessName(app, process_name)))
					throw new IllegalArgumentException("Process name \"" + process_name + "\" is not used by any component in AndroidManifest.xml");
		} catch (final PackageManager.NameNotFoundException ignored) {}		// Should never happen
	}

	private static String getFullProcessName(final Context context, final String process_name) {
		return process_name.length() > 0 && process_name.charAt(0) == ':' ? context.getPackageName() + process_name : process_name;
	}

	private static @Nullable String getProcessName(final Context context) {
		final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		if (am == null) return null;
		final List<ActivityManager.RunningAppProcessInfo> processes;
		try {
			processes = am.getRunningAppProcesses();
		} catch (final SecurityException e) { return null; }	// Isolated process not allowed to call getRunningAppProcesses
		final int pid = Process.myPid();
		if (processes != null) for (final ActivityManager.RunningAppProcessInfo process : processes)
			if (process.pid == pid) return process.processName;
		Log.e(TAG, "Error querying the name of current process.");
		return null;
	}

	private static void install(final Application app, final String process_name_or_tag, final CondomOptions options) {
		final int pos_colon = process_name_or_tag.indexOf(':');
		final String tag = pos_colon > 0 ? process_name_or_tag.substring(pos_colon + 1) : process_name_or_tag;
		FULL_TAG = "Condom:" + tag;
		TAG = CondomCore.asLogTag(FULL_TAG);

		final CondomCore condom = new CondomCore(app, options, TAG);
		try {
			installCondomProcessActivityManager(condom);
			installCondomProcessPackageManager(condom);
			Log.d(TAG, "Global condom is installed in current process");
		} catch (final Exception e) {
			condom.logConcern(TAG_INCOMPATIBILITY, e.getMessage());
			Log.e(TAG, "Error installing global condom in current process", e);
		}
	}

	@SuppressLint("PrivateApi") private static void installCondomProcessActivityManager(final CondomCore condom)
			throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		final Class<?> ActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
		Field ActivityManagerNative_gDefault = null;
		if (SDK_INT < O) try {
			ActivityManagerNative_gDefault = ActivityManagerNative.getDeclaredField("gDefault");
		} catch (final NoSuchFieldException ignored) {}		// ActivityManagerNative.gDefault is no longer available on Android O.
		if (ActivityManagerNative_gDefault == null) { //noinspection JavaReflectionMemberAccess
			ActivityManagerNative_gDefault = ActivityManager.class.getDeclaredField("IActivityManagerSingleton");
		}
		ActivityManagerNative_gDefault.setAccessible(true);
		final Class<?> Singleton = Class.forName("android.util.Singleton");
		@SuppressLint("DiscouragedPrivateApi") final Method Singleton_get = Singleton.getDeclaredMethod("get");
		Singleton_get.setAccessible(true);
		final Field Singleton_mInstance = Singleton.getDeclaredField("mInstance");
		Singleton_mInstance.setAccessible(true);
		final Class<?> IActivityManager = Class.forName("android.app.IActivityManager");

		final Object/* Singleton */singleton = ActivityManagerNative_gDefault.get(null);
		if (singleton == null) throw new IllegalStateException("ActivityManagerNative.gDefault is null");
		final Object/* IActivityManager */am = Singleton_get.invoke(singleton);
		if (am == null) throw new IllegalStateException("ActivityManagerNative.gDefault.get() returns null");

		final InvocationHandler handler;
		if (Proxy.isProxyClass(am.getClass()) && (handler = Proxy.getInvocationHandler(am)) instanceof CondomProcessActivityManager) {
			Log.w(TAG, "CondomActivityManager was already installed in this process.");
			((CondomProcessActivityManager) handler).mCondom = condom;
		} else {
			final Object condom_am = Proxy.newProxyInstance(condom.mBase.getClassLoader(), new Class[] { IActivityManager },
					new CondomProcessActivityManager(condom, am));
			Singleton_mInstance.set(singleton, condom_am);
		}
	}

	@SuppressLint("PrivateApi") private static void installCondomProcessPackageManager(final CondomCore condom)
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		final Class<?> ActivityThread = Class.forName("android.app.ActivityThread");
		final Field ActivityThread_sPackageManager = ActivityThread.getDeclaredField("sPackageManager");
		ActivityThread_sPackageManager.setAccessible(true);
		final Class<?> IPackageManager = Class.forName("android.content.pm.IPackageManager");

		final Object pm = ActivityThread_sPackageManager.get(null);
		final InvocationHandler handler;
		if (Proxy.isProxyClass(pm.getClass()) && (handler = Proxy.getInvocationHandler(pm)) instanceof CondomProcessPackageManager) {
			Log.w(TAG, "CondomPackageManager was already installed in this process.");
			((CondomProcessPackageManager) handler).mCondom = condom;
		} else {
			final Object condom_pm = Proxy.newProxyInstance(condom.mBase.getClassLoader(), new Class[] { IPackageManager },
					new CondomProcessPackageManager(condom, pm));
			ActivityThread_sPackageManager.set(null, condom_pm);
		}
	}

	private CondomProcess() {}

	private static final String TAG_INCOMPATIBILITY = "Incompatibility";

	/* ==================== */

	@VisibleForTesting static class CondomProcessActivityManager extends CondomSystemService {

		private Object proceed(final Object proxy, final Method method, final Object[] args) throws Throwable {
			final String method_name = method.getName(); final Intent intent; final int result;
			switch (method_name) {
			case "broadcastIntent":	// int broadcastIntent(IApplicationThread caller, Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle map, String/[23+] String[] requiredPermissions, [18+ int appOp], [23+ Bundle options], boolean serialized, boolean sticky, [16+ int userId]);
				result = mCondom.proceed(OutboundType.BROADCAST, (Intent) args[1], Integer.MIN_VALUE,
						() -> (Integer) CondomProcessActivityManager.super.invoke(proxy, method, args));
				final Object result_receiver = args[3];
				if (result != Integer.MIN_VALUE) return result;
				if (result_receiver == null) return 0/* ActivityManager.BROADCAST_SUCCESS */;
				// Invoke the result receiver as if the ordered broadcast has been sent to no one, if the broadcast is blocked by condom.
				final Method IIntentReceiver_performReceive = result_receiver.getClass().getMethod("performReceive", SDK_INT >= JELLY_BEAN_MR1
				// void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, [17+ int sendingUser])
						? new Class[] { Intent.class, int.class, String.class, Bundle.class, boolean.class, boolean.class, int.class}
						: new Class[] { Intent.class, int.class, String.class, Bundle.class, boolean.class, boolean.class });
				IIntentReceiver_performReceive.invoke(result_receiver, SDK_INT >= JELLY_BEAN_MR1
						? new Object[] { args[1], args[4], args[5], args[6], args[args.length - 3], args[args.length - 2], args[args.length - 1] }
						: new Object[] { args[1], args[4], args[5], args[6], args[8], args[9] });
				return 0/* ActivityManager.BROADCAST_SUCCESS */;
			case "bindService":				// Android P-
			case "bindIsolatedService":		// Android Q+
				intent = (Intent) args[2];
				result = mCondom.proceed(OutboundType.BIND_SERVICE, intent, 0,
						() -> (Integer) CondomProcessActivityManager.super.invoke(proxy, method, args));	// Result: 0 - no match, >0 - succeed, <0 - SecurityException.
				if (result > 0) mCondom.logIfOutboundPass(FULL_TAG, intent, CondomCore.getTargetPackage(intent), CondomCore.CondomEvent.BIND_PASS);
				return result;
			case "startService":
				intent = (Intent) args[1];
				final ComponentName component = mCondom.proceed(OutboundType.START_SERVICE, intent, null,
						() -> (ComponentName) CondomProcessActivityManager.super.invoke(proxy, method, args));
				if (component != null) mCondom.logIfOutboundPass(FULL_TAG, intent, component.getPackageName(), CondomCore.CondomEvent.START_PASS);
				return component;
			case "getContentProvider":		// (ApplicationThread, [Q+ String opPackageName], String authority, int userId, boolean stable)
				final String name = (String) args[SDK_INT >= Q ? 2 : 1];
				if (! mCondom.shouldAllowProvider(mCondom.mBase, name, PackageManager.MATCH_ALL))	// MATCH_ALL as special hint to ask the hooked IPackageManager.resolveContentProvider() to bypass.
					return null;	// Actually blocked by IPackageManager.resolveContentProvider() which is called in shouldAllowProvider() above.
				break;
			}
			return super.invoke(proxy, method, args);
		}

		@Override public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			try {
				return proceed(proxy, method, args);
			} catch (final Exception e) {
				if (DEBUG) Log.e(TAG, "Error proceeding " + method, e);
			}
			return super.invoke(proxy, method, args);
		}

		CondomProcessActivityManager(final CondomCore condom, final Object am) { super (am, "IActivityManager.", condom.DEBUG); mCondom = condom; }

		private CondomCore mCondom;
	}

	@VisibleForTesting static class CondomProcessPackageManager extends CondomSystemService {

		private Object proceed(final Object proxy, final Method method, final Object[] args) throws Throwable {
			final String method_name = method.getName();
			OutboundType outbound_type = null;
			switch (method_name) {
			case "queryIntentServices":
				outbound_type = OutboundType.QUERY_SERVICES;
				if (IPackageManager_queryIntentServices == null) IPackageManager_queryIntentServices = method;
				if (args[0] == DUMMY_INTENT) return null;	// Short-circuit for capturing IPackageManager_queryIntentServices.
			case "queryIntentReceivers":
				if (outbound_type == null) outbound_type = OutboundType.QUERY_RECEIVERS;

				final Object result = super.invoke(proxy, method, args);

				final List<ResolveInfo> list = mCondom.proceedQuery(outbound_type, (Intent) args[0], () -> asList(result),
						outbound_type == OutboundType.QUERY_SERVICES ? CondomCore.SERVICE_PACKAGE_GETTER : CondomCore.RECEIVER_PACKAGE_GETTER);	// Both "queryIntentServices" and "queryIntentReceivers" reach here.
				if (list.isEmpty()) asList(result).clear();	// In case Collections.emptyList() is returned due to targeted query being rejected by outbound judge.
				return result;

			case "resolveService":
				// Intent flags could only filter background receivers, we have to deal with services by ourselves.
				final Intent intent = (Intent) args[0];
				final int original_intent_flags = intent.getFlags();
				return mCondom.proceed(OutboundType.QUERY_SERVICES, intent, null, () -> {
					if (! mCondom.mExcludeBackgroundServices && mCondom.mOutboundJudge == null)
						return (ResolveInfo) CondomProcessPackageManager.super.invoke(proxy, method, args);

					if (IPackageManager_queryIntentServices == null) {
						mCondom.mBase.getPackageManager().queryIntentServices(DUMMY_INTENT, 0);
						if (IPackageManager_queryIntentServices == null)
							throw new IllegalStateException("Failed to capture IPackageManager.queryIntentServices()");
					}
					final List<ResolveInfo> candidates = asList(CondomProcessPackageManager.super.invoke(proxy, IPackageManager_queryIntentServices, args));
					return mCondom.filterCandidates(OutboundType.QUERY_SERVICES, intent.setFlags(original_intent_flags), candidates, FULL_TAG, false);
				});

			case "resolveContentProvider":
				final ProviderInfo provider = (ProviderInfo) super.invoke(proxy, method, args);
				final int flags = (int) args[1];
				if ((flags & PackageManager.MATCH_ALL) != 0) return provider;	// MATCH_ALL will be used by the hooked IActivityManager.getContentProvider().
				return mCondom.shouldAllowProvider(provider) ? provider : null;
			case "getInstalledApplications":
			case "getInstalledPackages":
				mCondom.logConcern(FULL_TAG, "IPackageManager." + method_name);
				break;
			}
			return super.invoke(proxy, method, args);
		}

		final Intent DUMMY_INTENT = new Intent();

		@SuppressWarnings("unchecked")
		private <T> List<T> asList(final Object list) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
			if (list instanceof List) return (List<T>) list;
			final Class<?> clazz = list.getClass();
			if (! "android.content.pm.ParceledListSlice".equals(clazz.getName()))
				throw new IllegalArgumentException("Neither List nor ParceledListSlice: " + clazz);
			if (ParceledListSlice_getList == null) ParceledListSlice_getList = clazz.getMethod("getList");
			return (List<T>) ParceledListSlice_getList.invoke(list);
		}

		@Override public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			try {
				return proceed(proxy, method, args);
			} catch (final Exception e) {
				if (DEBUG) Log.e(TAG, "Error proceeding " + method, e);
			}
			return super.invoke(proxy, method, args);
		}

		CondomProcessPackageManager(final CondomCore condom, final Object pm) { super(pm, "IPackageManager.", condom.DEBUG); mCondom = condom; }
		@VisibleForTesting CondomCore mCondom;
		private Method IPackageManager_queryIntentServices;
		private Method ParceledListSlice_getList;
	}

	private static class CondomSystemService implements InvocationHandler {

		@Override public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			if (DEBUG) Log.d(TAG, mServiceTag + method.getName() + (args == null ? "" : Arrays.deepToString(args)));
			try {
				return method.invoke(mService, args);
			} catch (final InvocationTargetException e) {
				throw e.getTargetException();
			}
		}

		CondomSystemService(final Object am, final String tag, final boolean debuggable) { mService = am; mServiceTag = tag; DEBUG = debuggable; }

		private final Object mService;
		private final String mServiceTag;
		final boolean DEBUG;
	}

	private static String FULL_TAG = "CondomProcess";	// Both will be replaced by compound tag in install().
	private static String TAG = "CondomProcess";
}
