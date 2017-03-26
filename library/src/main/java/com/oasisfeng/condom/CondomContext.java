/*
 * Copyright (C) 2014 Oasis Feng. All rights reserved.
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
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

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
public class CondomContext extends PseudoContextWrapper {

	/**
	 * This is the very first (probably only) API you need to wrap the naked {@link Context} under protection of <code>CondomContext</code>
	 *
	 * @param base	the original context used before <code>CondomContext</code> is introduced.
	 * @param tag	the optional tag to distinguish between multiple instances of <code>CondomContext</code> used parallel.
	 */
	public static @CheckReturnValue CondomContext wrap(final Context base, final @Nullable String tag) {
		if (base instanceof CondomContext) return (CondomContext) base;
		final Context app_context = base.getApplicationContext();
		if (app_context instanceof Application) {	// The application context is indeed an Application, this should be preserved semantically.
			final Application app = (Application) app_context;
			final CondomApplication condom_app = new CondomApplication(app);
			final CondomContext condom_context = new CondomContext(base, condom_app, tag);
			condom_app.attachBaseContext(base == app_context ? condom_context : new CondomContext(app, app, tag));
			return condom_context;
		} else return new CondomContext(base, base == app_context ? null : new CondomContext(app_context, app_context, tag), tag);
	}

	enum OutboundType { START_SERVICE, BIND_SERVICE, BROADCAST }

	interface OutboundJudge {
		/**
		 * Judge the outbound request by its explicit target package.
		 *
		 * @return whether this outbound request should be allowed.
		 */
		boolean judge(OutboundType type, String target_pkg);
	}

	/** Set to dry-run mode to inspect the outbound wake-up only, no outbound requests will be actually blocked. */
	public CondomContext setDryRun(final boolean dry_run) {
		if (dry_run == mDryRun) return this;
		mDryRun = dry_run;
		if (dry_run) Log.w(TAG, "Start dry-run mode, no outbound requests will be blocked actually, despite stated in log.");
		else Log.w(TAG, "Stop dry-run mode.");
		return this;
	}

	/** Set a custom judge for the explicit target package of outbound service and broadcast requests. */
	public CondomContext setOutboundJudge(final OutboundJudge judge) { mOutboundJudge = judge; return this; }

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

	@Override public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) {
		final int original_flags = adjustIntentFlags(service);
		if (shouldBlockExplicitRequest(OutboundType.BIND_SERVICE, service)) return false;
		final boolean result = super.bindService(service, conn, flags);
		service.setFlags(original_flags);
		return result;
	}

	@Override public ComponentName startService(final Intent service) {
		final int original_flags = adjustIntentFlags(service);
		if (shouldBlockExplicitRequest(OutboundType.START_SERVICE, service)) return null;
		final ComponentName result = super.startService(service);
		service.setFlags(original_flags);
		return result;
	}

	@Override public void sendBroadcast(final Intent intent) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendBroadcast(intent);
		intent.setFlags(original_flags);
	}

	@Override public void sendBroadcast(final Intent intent, final String receiverPermission) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendBroadcast(intent, receiverPermission);
		intent.setFlags(original_flags);
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendBroadcastAsUser(intent, user);
		intent.setFlags(original_flags);
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendBroadcastAsUser(intent, user, receiverPermission);
		intent.setFlags(original_flags);
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendOrderedBroadcast(intent, receiverPermission);
		intent.setFlags(original_flags);
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission, final BroadcastReceiver resultReceiver,
											   final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		intent.setFlags(original_flags);
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission, final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		intent.setFlags(original_flags);
	}

	@Override public void sendStickyBroadcast(final Intent intent) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendStickyBroadcast(intent);
		intent.setFlags(original_flags);
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendStickyBroadcastAsUser(final Intent intent, final UserHandle user) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendStickyBroadcastAsUser(intent, user);
		intent.setFlags(original_flags);
	}

	@Override public void sendStickyOrderedBroadcast(final Intent intent, final BroadcastReceiver resultReceiver, final Handler scheduler,
													 final int initialCode, final String initialData, final Bundle initialExtras) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		intent.setFlags(original_flags);
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendStickyOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) return;
		super.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		intent.setFlags(original_flags);
	}

	// TODO: Protect package queries (for service and receiver)
	@Override public PackageManager getPackageManager() {
		if (DEBUG) Log.d(TAG, "getPackageManager() is invoked", new Throwable());
		return super.getPackageManager();
	}

	// TODO: Protect outbound provider requests
	@Override public ContentResolver getContentResolver() {
		if (DEBUG) Log.d(TAG, "getContentResolver() is invoked", new Throwable());
		return super.getContentResolver();
	}

	@Override public Context getApplicationContext() { return mApplicationContext; }

	/* ********************************* */

	private int adjustIntentFlags(final Intent intent) {
		final int original_flags = intent.getFlags();
		if (mDryRun) return original_flags;
		if (mExcludeBackgroundPackages)
			intent.addFlags(SDK_INT >= N ? FLAG_RECEIVER_EXCLUDE_BACKGROUND : Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		if (SDK_INT >= HONEYCOMB_MR1 && mExcludeStoppedPackages)
			intent.setFlags((intent.getFlags() & ~ Intent.FLAG_INCLUDE_STOPPED_PACKAGES) | Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
		return original_flags;
	}

	private boolean shouldBlockExplicitRequest(final OutboundType type, final Intent intent) {
		if (mOutboundJudge == null) return false;
		final ComponentName component = intent.getComponent();
		final String target_pkg = component != null ? component.getPackageName() : intent.getPackage();
		if (target_pkg == null) return false;
		if (target_pkg.equals(getPackageName())) return false;		// Targeting this package itself actually, not an outbound service.
		if (! mOutboundJudge.judge(type, target_pkg)) {
			if (DEBUG) Log.w(TAG, "Blocked outbound " + type + ": " + intent);
			return ! mDryRun;
		} else return false;
	}

	private CondomContext(final Context base, final @Nullable Context app_context, final @Nullable String tag) {
		super(base);
		mApplicationContext = app_context != null ? app_context : this;
		DEBUG = (base.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		TAG = tag == null ? "Condom" : "Condom." + tag;
	}

	private boolean mDryRun;
	private OutboundJudge mOutboundJudge;
	private boolean mExcludeStoppedPackages = true;
	private boolean mExcludeBackgroundPackages = true;
	private final boolean DEBUG;
	private final Context mApplicationContext;

	/**
	 * If set, the broadcast will never go to manifest receivers in background (cached
	 * or not running) apps, regardless of whether that would be done by default.  By
	 * default they will receive broadcasts if the broadcast has specified an
	 * explicit component or package name.
	 */
	@RequiresApi(N) private static final int FLAG_RECEIVER_EXCLUDE_BACKGROUND = 0x00800000;

	private final String TAG;

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

		CondomApplication(final Application app) { mApplication = app; }
		@Override public void attachBaseContext(final Context base) { super.attachBaseContext(base); }

		private final Application mApplication;
	}
}
