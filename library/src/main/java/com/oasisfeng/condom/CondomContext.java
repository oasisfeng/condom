/*
 * Copyright (C) 2017 Oasis Feng. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oasis designates this
 * particular file as subject to the "Classpath" exception as provided
 * in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.oasisfeng.condom;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.N;

/**
 * The condom-style {@link ContextWrapper} to prevent unwanted behaviors going through.
 *
 * Created by Oasis on 2017/3/25.
 */
@ParametersAreNonnullByDefault @Keep
public class CondomContext extends ContextWrapper {

	/**
	 * This is the very first (probably only) API you need to wrap the naked {@link Context} under protection of <code>CondomContext</code>
	 *
	 * @param base	the original context used before <code>CondomContext</code> is introduced.
	 * @param tag	the optional tag to distinguish between multiple instances of <code>CondomContext</code> used parallel.
	 */
	public static @CheckReturnValue CondomContext wrap(final Context base, final @Nullable String tag) {
		if (base instanceof CondomContext) return (CondomContext) base;
		final Context app_context = base.getApplicationContext();
		final boolean debuggable = (base.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		if (app_context instanceof Application) {	// The application context is indeed an Application, this should be preserved semantically.
			final Application app = (Application) app_context;
			final CondomApplication condom_app = new CondomApplication(app, tag, debuggable);
			final CondomContext condom_context = new CondomContext(base, condom_app, tag, debuggable);
			condom_app.attachBaseContext(base == app_context ? condom_context : new CondomContext(app, app, tag, debuggable));
			return condom_context;
		} else return new CondomContext(base, base == app_context ? null : new CondomContext(app_context, app_context, tag, debuggable), tag, debuggable);
	}

	enum OutboundType { START_SERVICE, BIND_SERVICE, BROADCAST, QUERY_SERVICES, QUERY_RECEIVERS }

	public interface OutboundJudge {
		/**
		 * Judge the outbound request by its explicit target package.
		 *
		 * @return whether this outbound request should be allowed, or whether the query result entry should be included in the returned collection.
		 *         Disallowed service request will simply fail and broadcast will be dropped.
		 */
		boolean shouldAllow(OutboundType type, String target_pkg);
	}

	/** Set a custom judge for the explicit target package of outbound service and broadcast requests. */
	public CondomContext setOutboundJudge(final OutboundJudge judge) { mOutboundJudge = judge; return this; }

	/** Set to dry-run mode to inspect the outbound wake-up only, no outbound requests will be actually blocked. */
	public CondomContext setDryRun(final boolean dry_run) {
		if (dry_run == mDryRun) return this;
		mDryRun = dry_run;
		if (dry_run) Log.w(TAG, "Start dry-run mode, no outbound requests will be blocked actually, despite stated in log.");
		else Log.w(TAG, "Stop dry-run mode.");
		return this;
	}

	/**
	 * Prevent outbound service request from waking-up force-stopped packages. (default: true, not recommended to change)
	 *
	 * <p>If a package is force-stopped by user, it usually mean that app did not work as expected (most probably due to repeated crashes).
	 * Waking-up those packages usually leads to bad user experience, even the worse, the infamous annoying "APP STOPPED" dialog.
	 */
	public CondomContext preventWakingUpStoppedPackages(final boolean prevent_or_not) { mExcludeStoppedPackages = prevent_or_not; return this; }

	/**
	 * Prevent broadcast to be delivered to manifest receivers in background (cached or not running) apps. (default: true)
	 *
	 * <p>This restriction is supported natively since Android O, and it works similarly by only targeting registered receivers on previous Android versions.
	 */
	public CondomContext preventBroadcastToBackgroundPackages(final boolean prevent_or_not) { mExcludeBackgroundPackages = prevent_or_not; return this; }

	/* ****** Hooked Context APIs ****** */

	@Override public boolean bindService(final Intent intent, final ServiceConnection conn, final int flags) {
		return proceed(OutboundType.BIND_SERVICE, intent, Boolean.FALSE, new WrappedValueProcedure<Boolean>() { @Override public Boolean proceed(final Intent intent) {
			return CondomContext.super.bindService(intent, conn, flags);
		}});
	}

	@Override public ComponentName startService(final Intent intent) {
		return proceed(OutboundType.START_SERVICE, intent, null, new WrappedValueProcedure<ComponentName>() { @Override public ComponentName proceed(final Intent intent) {
			return CondomContext.super.startService(intent);
		}});
	}

	@Override public void sendBroadcast(final Intent intent) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendBroadcast(intent);
		}});
	}

	@Override public void sendBroadcast(final Intent intent, final String receiverPermission) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendBroadcast(intent, receiverPermission);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendBroadcastAsUser(intent, user);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendBroadcastAsUser(intent, user, receiverPermission);
		}});
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendOrderedBroadcast(intent, receiverPermission);
		}});
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission, final BroadcastReceiver resultReceiver,
											   final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission, final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}});
	}

	@Override public void sendStickyBroadcast(final Intent intent) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendStickyBroadcast(intent);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendStickyBroadcastAsUser(final Intent intent, final UserHandle user) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendStickyBroadcastAsUser(intent, user);
		}});
	}

	@Override public void sendStickyOrderedBroadcast(final Intent intent, final BroadcastReceiver resultReceiver, final Handler scheduler,
													 final int initialCode, final String initialData, final Bundle initialExtras) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendStickyOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		proceed(OutboundType.BROADCAST, intent, new WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}});
	}

	// TODO: Protect outbound provider requests
	@Override public ContentResolver getContentResolver() {
		if (DEBUG) Log.d(TAG, "getContentResolver() is invoked", new Throwable());
		return super.getContentResolver();
	}

	@Override public PackageManager getPackageManager() { return mPackageManager; }
	@Override public Context getApplicationContext() { return mApplicationContext; }
	@Override public Context getBaseContext() {
		if (DEBUG) Log.d(TAG, "getBaseContext() is invoked", new Throwable());
		return mBaseContext;
	}

	/* ********************************* */

	private interface WrappedValueProcedure<R> {
		R proceed(Intent intent);
	}

	private static abstract class WrappedProcedure implements WrappedValueProcedure<Void> {
		abstract void run(Intent intent);
		@Override public Void proceed(final Intent intent) { run(intent); return null; }
	}

	@SuppressWarnings("ResultOfMethodCallIgnored") private void proceed(final OutboundType type, final Intent intent, final WrappedProcedure procedure) {
		proceed(type, intent, null, procedure);
	}

	private @CheckReturnValue <T> T proceed(final OutboundType type, final Intent intent, final @Nullable T negative_value, final WrappedValueProcedure<T> procedure) {
		final ComponentName component = intent.getComponent();
		final String target_pkg = component != null ? component.getPackageName() : intent.getPackage();
		if (getPackageName().equals(target_pkg)) return procedure.proceed(intent);		// Self-targeting request is allowed unconditionally
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockRequestTarget(type, target_pkg)) {
			if (DEBUG) Log.w(TAG, "Blocked outbound " + type + ": " + intent);
			return negative_value;
		}
		try {
			return procedure.proceed(intent);
		} finally {
			intent.setFlags(original_flags);
		}
	}

	private @CheckReturnValue List<ResolveInfo> proceedQuery(final OutboundType type, final Intent intent, final WrappedValueProcedure<List<ResolveInfo>> procedure) {
		return proceed(type, intent, Collections.<ResolveInfo>emptyList(), new WrappedValueProcedure<List<ResolveInfo>>() { @Override public List<ResolveInfo> proceed(final Intent intent) {
			final List<ResolveInfo> candidates = procedure.proceed(intent);
			final Iterator<ResolveInfo> iterator = candidates.iterator();
			while (iterator.hasNext()) {
				final ResolveInfo candidate = iterator.next();
				final String pkg = type == OutboundType.QUERY_SERVICES ? candidate.serviceInfo.packageName
						: (type == OutboundType.QUERY_RECEIVERS ? candidate.activityInfo.packageName : null);
				if (shouldBlockRequestTarget(type, pkg)) {
					iterator.remove();		// TODO: Not safe to assume the list returned from PackageManager is modifiable.
					Log.w(TAG, "Filtered " + pkg + " from " + type + ": " + intent);
				}
			}
			return candidates;
		}});
	}

	private int adjustIntentFlags(final Intent intent) {
		final int original_flags = intent.getFlags();
		if (mDryRun) return original_flags;
		if (mExcludeBackgroundPackages)
			intent.addFlags(SDK_INT >= N ? FLAG_RECEIVER_EXCLUDE_BACKGROUND : Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		if (SDK_INT >= HONEYCOMB_MR1 && mExcludeStoppedPackages)
			intent.setFlags((intent.getFlags() & ~ Intent.FLAG_INCLUDE_STOPPED_PACKAGES) | Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
		return original_flags;
	}

	private boolean shouldBlockRequestTarget(final OutboundType type, final @Nullable String target_pkg) {
		return target_pkg != null && mOutboundJudge != null && ! mOutboundJudge.shouldAllow(type, target_pkg) && ! mDryRun;
	}

	private CondomContext(final Context base, final @Nullable Context app_context, final @Nullable String tag, final boolean debuggable) {
		super(base);
		mApplicationContext = app_context != null ? app_context : this;
		mPackageManager = new CondomPackageManager(base.getPackageManager());
		TAG = tag == null ? "Condom" : "Condom." + tag;
		DEBUG = debuggable;
	}

	private boolean mDryRun;
	private OutboundJudge mOutboundJudge;
	private boolean mExcludeStoppedPackages = true;
	private boolean mExcludeBackgroundPackages = true;
	private final Context mApplicationContext;
	private final Context mBaseContext = new PseudoContextImpl(this);
	private final PackageManager mPackageManager;
	private final String TAG;
	private final boolean DEBUG;

	/**
	 * If set, the broadcast will never go to manifest receivers in background (cached
	 * or not running) apps, regardless of whether that would be done by default.  By
	 * default they will receive broadcasts if the broadcast has specified an
	 * explicit component or package name.
	 *
	 * @since API level 24 (Android N)
	 */
	@VisibleForTesting static final int FLAG_RECEIVER_EXCLUDE_BACKGROUND = 0x00800000;

	/* ****** Internal branch functionality ****** */

	private class CondomPackageManager extends PackageManagerWrapper {

		@Override public List<ResolveInfo> queryBroadcastReceivers(final Intent intent, final int flags) {
			return proceedQuery(OutboundType.QUERY_RECEIVERS, intent, new WrappedValueProcedure<List<ResolveInfo>>() { @Override public List<ResolveInfo> proceed(final Intent intent) {
				return CondomPackageManager.super.queryBroadcastReceivers(intent, flags);
			}});
		}

		@Override public List<ResolveInfo> queryIntentServices(final Intent intent, final int flags) {
			return proceedQuery(OutboundType.QUERY_SERVICES, intent, new WrappedValueProcedure<List<ResolveInfo>>() { @Override public List<ResolveInfo> proceed(final Intent intent) {
				return CondomPackageManager.super.queryIntentServices(intent, flags);
			}});
		}

		@Override public ResolveInfo resolveService(final Intent intent, final int flags) {
			return proceed(OutboundType.QUERY_SERVICES, intent, null, new WrappedValueProcedure<ResolveInfo>() { @Override public ResolveInfo proceed(final Intent intent) {
				return CondomPackageManager.super.resolveService(intent, flags);
			}});
		}

		CondomPackageManager(final PackageManager base) { super(base); }
	}

	private static class CondomApplication extends Application {

		@Override public void registerComponentCallbacks(final ComponentCallbacks callback) {
			if (SDK_INT >= ICE_CREAM_SANDWICH) mApplication.registerComponentCallbacks(callback);
		}
		@Override public void unregisterComponentCallbacks(final ComponentCallbacks callback) {
			if (SDK_INT >= ICE_CREAM_SANDWICH) mApplication.unregisterComponentCallbacks(callback);
		}
		@Override public void registerActivityLifecycleCallbacks(final ActivityLifecycleCallbacks callback) {
			if (SDK_INT >= ICE_CREAM_SANDWICH) mApplication.registerActivityLifecycleCallbacks(callback);
		}
		@Override public void unregisterActivityLifecycleCallbacks(final ActivityLifecycleCallbacks callback) {
			if (SDK_INT >= ICE_CREAM_SANDWICH) mApplication.unregisterActivityLifecycleCallbacks(callback);
		}
		@Override public void registerOnProvideAssistDataListener(final OnProvideAssistDataListener callback) {
			if (SDK_INT >= JELLY_BEAN_MR2) mApplication.registerOnProvideAssistDataListener(callback);
		}
		@Override public void unregisterOnProvideAssistDataListener(final OnProvideAssistDataListener callback) {
			if (SDK_INT >= JELLY_BEAN_MR2) mApplication.unregisterOnProvideAssistDataListener(callback);
		}

		// The actual context returned may not be semantically consistent. We'll keep an eye for it in the wild.
		@Override public Context getBaseContext() {
			if (DEBUG) Log.w(TAG, "Application.getBaseContext() is invoked", new Throwable());
			return super.getBaseContext();
		}

		@Override public void attachBaseContext(final Context base) { super.attachBaseContext(base); }

		CondomApplication(final Application app, final @Nullable String tag, final boolean debuggable) {
			mApplication = app;
			TAG = tag == null ? "CondomApp" : "CondomApp." + tag;
			DEBUG = debuggable;
		}

		private final Application mApplication;
		private final boolean DEBUG;
		private final String TAG;
	}

	// This should act as what ContextImpl stands for in the naked Context structure.
	private static class PseudoContextImpl extends PseudoContextWrapper {
		public PseudoContextImpl(final CondomContext condom) { super(condom); }
	}
}
