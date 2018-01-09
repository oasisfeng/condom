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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.CheckResult;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.EventLog;
import android.util.Log;

import com.oasisfeng.condom.util.Lazy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND;
import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;

/**
 * The shared functionality for condom wrappers.
 *
 * Created by Oasis on 2017/4/21.
 */
@Keep @RestrictTo(RestrictTo.Scope.LIBRARY) @SuppressWarnings("TypeParameterHidesVisibleType")
class CondomCore {

	interface WrappedValueProcedure<R> extends WrappedValueProcedureThrows<R, RuntimeException> {}

	interface WrappedValueProcedureThrows<R, T extends Throwable> { @Nullable R proceed() throws T; }

	static abstract class WrappedProcedure implements WrappedValueProcedure<Boolean> {
		abstract void run();
		@Override public Boolean proceed() { run(); return null; }
	}

	ContentResolver getContentResolver() { return mContentResolver.get(); }
	PackageManager getPackageManager() { return mPackageManager.get(); }
	String getPackageName() { return mBase.getPackageName(); }

	void proceedBroadcast(final Context context, final Intent intent, final WrappedValueProcedure<Boolean> procedure,
						  final @Nullable BroadcastReceiver resultReceiver) {
		if (proceed(OutboundType.BROADCAST, intent, Boolean.FALSE, procedure) == Boolean.FALSE && resultReceiver != null)
			resultReceiver.onReceive(new ReceiverRestrictedContext(context), intent);
	}

	@CheckResult <R, T extends Throwable> R proceed(final OutboundType type, final @Nullable Intent intent, final @Nullable R negative_value,
													final WrappedValueProcedureThrows<R, T> procedure) throws T {
		final String target_pkg = intent != null ? getTargetPackage(intent) : null;
		if (target_pkg != null) {
			if (mBase.getPackageName().equals(target_pkg)) return procedure.proceed();	// Self-targeting request is allowed unconditionally

			if (shouldBlockRequestTarget(type, intent, target_pkg)) return negative_value;
		}
		final int original_flags = intent != null ? adjustIntentFlags(type, intent) : 0;
		try {
			return procedure.proceed();
		} finally {
			if (intent != null) intent.setFlags(original_flags);
		}
	}

	@CheckResult <R, T extends Throwable> R proceed(final OutboundType type, final String target_pkg, final @Nullable R negative_value,
													final WrappedValueProcedureThrows<R, T> procedure) throws T {
		if (mBase.getPackageName().equals(target_pkg)) return procedure.proceed();	// Self-targeting request is allowed unconditionally
		if (shouldBlockRequestTarget(type, null, target_pkg)) return negative_value;
		return procedure.proceed();
	}

	@CheckResult <T, E extends Throwable> List<T> proceedQuery(final OutboundType type, final @Nullable Intent intent,
															   final WrappedValueProcedureThrows<List<T>, E> procedure, final Function<T, String> pkg_getter) throws E {
		return proceed(type, intent, Collections.<T>emptyList(), new WrappedValueProcedureThrows<List<T>, E>() { @Override public List<T> proceed() throws E {
			final List<T> candidates = procedure.proceed();

			if (candidates != null && mOutboundJudge != null && (intent == null || getTargetPackage(intent) == null)) {	// Package-targeted intent is already filtered by OutboundJudge in proceed().
				final Iterator<T> iterator = candidates.iterator();
				while (iterator.hasNext()) {
					final T candidate = iterator.next();
					final String pkg = pkg_getter.apply(candidate);
					if (pkg != null && shouldBlockRequestTarget(type, intent, pkg))		// Dry-run is checked inside shouldBlockRequestTarget()
						iterator.remove();		// TODO: Not safe to assume the list returned from PackageManager is modifiable.
				}
			}
			return candidates;
		}});
	}
	interface Function<T, R> { R apply(T t); }

	static String getTargetPackage(final Intent intent) {
		final ComponentName component = intent.getComponent();
		return component != null ? component.getPackageName() : intent.getPackage();
	}

	private boolean shouldBlockRequestTarget(final OutboundType type, final @Nullable Intent intent, final String target_pkg) {
		// Dry-run must be checked at the latest to ensure outbound judge is always called.
		return mOutboundJudge != null && ! mOutboundJudge.shouldAllow(type, intent, target_pkg) && ! mDryRun;
	}

	@SuppressLint("WrongConstant") private int adjustIntentFlags(final OutboundType type, final Intent intent) {
		final int original_flags = intent.getFlags();
		if (mDryRun) return original_flags;
		if (mExcludeBackgroundReceivers && (type == OutboundType.BROADCAST || type == OutboundType.QUERY_RECEIVERS))
			intent.addFlags(SDK_INT >= N ? FLAG_RECEIVER_EXCLUDE_BACKGROUND : Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		if (SDK_INT >= HONEYCOMB_MR1 && mExcludeStoppedPackages)
			intent.setFlags((intent.getFlags() & ~ Intent.FLAG_INCLUDE_STOPPED_PACKAGES) | Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
		return original_flags;
	}

	@Nullable ResolveInfo filterCandidates(final OutboundType type, final Intent original_intent, final @Nullable List<ResolveInfo> candidates,
										   final String tag, final boolean remove) {
		if (candidates == null || candidates.isEmpty()) return null;

		final int my_uid = Process.myUid();
		BackgroundUidFilter bg_uid_filter = null;
		ResolveInfo match = null;
		for (final Iterator<ResolveInfo> iterator = candidates.iterator(); iterator.hasNext(); match = null) {
			final ResolveInfo candidate = iterator.next();
			final ApplicationInfo app_info = candidate.serviceInfo.applicationInfo;
			final int uid = app_info.uid;
			if (uid == my_uid) match = candidate;        // Self UID is always allowed
			else if (mOutboundJudge == null || mOutboundJudge.shouldAllow(type, original_intent, app_info.packageName)) {
				if (mExcludeBackgroundServices) {
					if (bg_uid_filter == null) bg_uid_filter = new BackgroundUidFilter();
					if (bg_uid_filter.isUidNotBackground(uid)) match = candidate;
				} else match = candidate;
			}

			if (match == null) log(tag, CondomEvent.FILTER_BG_SERVICE, app_info.packageName, original_intent.toString());
			if (mDryRun) return candidate;        // Always touch nothing and return the first candidate in dry-run mode.
			if (remove) {
				if (match == null) iterator.remove();
			} else if (match != null) return match;
		}
		return null;
	}

	boolean shouldAllowProvider(final @Nullable ProviderInfo provider) {
		if (provider == null) return true;		// We know nothing about the provider, better allow than block.
		if (mBase.getPackageName().equals(provider.packageName)) return true;
		if (shouldBlockRequestTarget(OutboundType.CONTENT, null, provider.packageName)) return false;
		if (Settings.AUTHORITY.equals(provider.authority)) return true;	// Always allow access to system settings, to avoid rare cases in the wild that the provider info of Settings provider is inaccurate.
		//noinspection SimplifiableIfStatement
		if (SDK_INT >= HONEYCOMB_MR1 && mExcludeStoppedPackages
				&& (provider.applicationInfo.flags & (FLAG_SYSTEM | ApplicationInfo.FLAG_STOPPED)) == ApplicationInfo.FLAG_STOPPED) return mDryRun;
		return true;
	}

	boolean shouldAllowProvider(final Context context, final String name, final int flags) {
		return shouldAllowProvider(context.getPackageManager().resolveContentProvider(name, flags));
	}

	Object getSystemService(final String name) {
		if (mKitManager != null) {
			final CondomKit.SystemServiceSupplier supplier = mKitManager.mSystemServiceSuppliers.get(name);
			if (supplier != null) {
				final Object service = supplier.getSystemService(mBase, name);
				if (service != null) return service;
			}
		}
		return null;
	}

	boolean shouldSpoofPermission(final String permission) {
		return mKitManager != null && mKitManager.mSpoofPermissions.contains(permission);
	}

	Set<String> getSpoofPermissions() {
		return mKitManager != null ? mKitManager.mSpoofPermissions : Collections.<String>emptySet();
	}

	enum CondomEvent { CONCERN, BIND_PASS, START_PASS, FILTER_BG_SERVICE }

	private void log(final String tag, final CondomEvent event, final String... args) {
		final Object[] event_args = new Object[2 + args.length];
		event_args[0] = mBase.getPackageName(); event_args[1] = tag;	// Package name and tag are shared parameters for all events.
		System.arraycopy(args, 0, event_args, 2, args.length);
		EventLog.writeEvent(EVENT_TAG + event.ordinal(), event_args);
		if (DEBUG) Log.d(asLogTag(tag), event.name() + " " + Arrays.toString(args));
	}

	void logConcern(final String tag, final String label) {
		EventLog.writeEvent(EVENT_TAG + CondomEvent.CONCERN.ordinal(), mBase.getPackageName(), tag, label, getCaller());
		if (DEBUG) Log.w(asLogTag(tag), label + " is invoked", new Throwable());
	}

	void logIfOutboundPass(final String tag, final Intent intent, final @Nullable String target_pkg, final CondomEvent event) {
		if (target_pkg != null && ! mBase.getPackageName().equals(target_pkg))
			log(tag, event, target_pkg, intent.toString());
	}

	private static String getCaller() {
		final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		if (stack.length <= 5) return "<bottom>";
		final StackTraceElement caller = stack[5];
		return caller.getClassName() + "." + caller.getMethodName() + ":" + caller.getLineNumber();
	}

	static String buildLogTag(final String default_tag, final String prefix, final @Nullable String tag) {
		return tag == null || tag.isEmpty() ? default_tag : asLogTag(prefix + tag);
	}

	static String asLogTag(final String tag) {	// Logging tag can be at most 23 characters.
		return tag.length() > 23 ? tag.substring(0, 23) : tag;
	}

	CondomCore(final Context base, final CondomOptions options, final String tag) {
		mBase = base;
		DEBUG = (base.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		mExcludeBackgroundReceivers = options.mExcludeBackgroundReceivers;
		mExcludeBackgroundServices = SDK_INT < O && options.mExcludeBackgroundServices;
		mOutboundJudge = options.mOutboundJudge;
		mDryRun = options.mDryRun;

		mPackageManager = new Lazy<PackageManager>() { @Override protected PackageManager create() {
			return new CondomPackageManager(CondomCore.this, base.getPackageManager(), tag);
		}};
		mContentResolver = new Lazy<ContentResolver>() { @Override protected ContentResolver create() {
			return new CondomContentResolver(CondomCore.this, base, base.getContentResolver());
		}};

		final List<CondomKit> kits = options.mKits == null ? null : new ArrayList<>(options.mKits);
		if (kits != null && ! kits.isEmpty()) {
			mKitManager = new CondomKitManager();
			for (final CondomKit kit : kits)
				kit.onRegister(mKitManager);
		} else mKitManager = null;

		if (mDryRun) Log.w(tag, "Start dry-run mode, no outbound requests will be blocked actually, despite later stated in log.");
	}

	final Context mBase;	// The real Context
	final boolean DEBUG;

	boolean mDryRun;
	@VisibleForTesting @Nullable OutboundJudge mOutboundJudge;
	boolean mExcludeStoppedPackages = true;
	boolean mExcludeBackgroundReceivers;
	boolean mExcludeBackgroundServices;
	private final Lazy<PackageManager> mPackageManager;
	private final Lazy<ContentResolver> mContentResolver;
	private final @Nullable CondomKitManager mKitManager;

	static final Function<ResolveInfo,String> SERVICE_PACKAGE_GETTER = new Function<ResolveInfo, String>() {
		@Override public String apply(final ResolveInfo resolve) {
			return resolve.serviceInfo.packageName;
		}
	};
	static final Function<ResolveInfo,String> RECEIVER_PACKAGE_GETTER = new Function<ResolveInfo, String>() {
		@Override public String apply(final ResolveInfo resolve) {
			return resolve.activityInfo.packageName;
		}
	};

	private static final int EVENT_TAG = "Condom".hashCode();

	/** Mirror of the hidden Intent.FLAG_RECEIVER_EXCLUDE_BACKGROUND, since API level 24 (Android N) */
	@RequiresApi(N) @VisibleForTesting static final int FLAG_RECEIVER_EXCLUDE_BACKGROUND = 0x00800000;

	static class CondomKitManager implements CondomKit.CondomKitRegistry {

		@Override public void addPermissionSpoof(final String permission) {
			mSpoofPermissions.add(permission);
		}

		@Override public void registerSystemService(final String name, final CondomKit.SystemServiceSupplier supplier) {
			mSystemServiceSuppliers.put(name, supplier);
		}

		final Map<String, CondomKit.SystemServiceSupplier> mSystemServiceSuppliers = new HashMap<>();
		final Set<String> mSpoofPermissions = new HashSet<>();
	}

	class BackgroundUidFilter {

		boolean isUidNotBackground(final int uid) {
			if (running_processes != null) {
				for (final ActivityManager.RunningAppProcessInfo running_process : running_processes)
					if (running_process.pid != 0 && running_process.importance < IMPORTANCE_BACKGROUND && running_process.uid == uid)
						return true;	// Same UID does not guarantee same process. This is spared intentionally.
			} else if (running_services != null) {
				for (final ActivityManager.RunningServiceInfo running_service : running_services)
					if (running_service.pid != 0 && running_service.uid == uid)	// Same UID does not guarantee same process. This is spared intentionally.
						return true;	// Only running process is qualified, although getRunningServices() may not include all running app processes.
			}
			return false;	// Fallback: Always treat as background app, since app with same UID will not reach here.
		}

		BackgroundUidFilter() {
			final ActivityManager am = (ActivityManager) mBase.getSystemService(ACTIVITY_SERVICE);
			if (am == null) {
				running_services = null;
				running_processes = null;
			} else if (SDK_INT >= LOLLIPOP_MR1) {		// getRunningAppProcesses() is limited on Android 5.1+.
				running_services = am.getRunningServices(64);	// Too many services are never healthy, thus ignored intentionally.
				running_processes = null;
			} else {
				running_services = null;
				running_processes = am.getRunningAppProcesses();
			}
		}

		private final @Nullable List<ActivityManager.RunningServiceInfo> running_services;
		private final @Nullable List<ActivityManager.RunningAppProcessInfo> running_processes;
	}

	class ReceiverRestrictedContext extends ContextWrapper {

		ReceiverRestrictedContext(final Context base) {
			super(base);
		}

		@Override public Intent registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter) {
			return registerReceiver(receiver, filter, null, null);
		}

		@Override public Intent registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter,
												 final @Nullable String broadcastPermission, final @Nullable Handler scheduler) {
			if (receiver == null) {
				// Allow retrieving current sticky broadcast; this is safe since we
				// aren't actually registering a receiver.
				return super.registerReceiver(null, filter, broadcastPermission, scheduler);
			} else {
				throw new ReceiverCallNotAllowedException(
						"BroadcastReceiver components are not allowed to register to receive intents");
			}
		}

		@Override public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) {
			throw new ReceiverCallNotAllowedException(
					"BroadcastReceiver components are not allowed to bind to services");
		}
	}
}
