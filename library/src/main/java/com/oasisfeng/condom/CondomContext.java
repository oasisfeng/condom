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
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Keep;
import android.support.annotation.RequiresApi;
import android.util.Log;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.N;

/**
 * The condom-style {@link ContextWrapper} to prevent unwanted behaviors going through.
 *
 * Created by Oasis on 2017/3/25.
 */
@ParametersAreNonnullByDefault @Keep
public class CondomContext extends ContextWrapper {

	/** This is the very first (probably only) API you need to wrap the naked {@link Context} in the protection of <code>CondomContext</code> */
	public static @CheckReturnValue CondomContext wrap(final Context base) {
		if (base instanceof CondomContext) return (CondomContext) base;
		final Context app_context = base.getApplicationContext();
		if (app_context instanceof Application)		// The application context is indeed an Application, this should be preserved semantically.
			return new CondomContext(base, new CondomApplication((Application) app_context));
		else return new CondomContext(base, new CondomContext(app_context, app_context));
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
	public CondomContext setDryRun(final boolean dry_run) { mDryRun = dry_run; return this; }

	/** Set a custom judge for the explicit target package of outbound service and broadcast requests. */
	public CondomContext setOutboundJudge(final OutboundJudge judge) { mOutboundJudge = judge; return this; }

	/**
	 * Allow outbound service request to wake-up force-stopped packages. (default: Disallow, not recommended to change)
	 *
	 * <p>If a package is force-stopped by user, it usually mean that app did not work as expected (most probably due to repeated crashes).
	 * Waking-up those packages usually leads to bad user experience, even the worse, the infamous annoying "APP STOPPED" dialog.
	 */
	public CondomContext setAllowWakingUpStoppedPackages(final boolean allow_or_not) { mAllowWakingUpStoppedPackages = allow_or_not; return this; }

	/**
	 * Allow implicit broadcast to be delivered to manifest receivers in background (cached or not running) apps. (default: Disallow)
	 *
	 * <p>This restriction is always enforced by Android O, and it works similarly by only targeting registered receivers on previous Android versions.
	 */
	public CondomContext setAllowBroadcastToBackgroundPackages(final boolean allow_or_not) { mAllowBroadcastToBackgroundPackages = allow_or_not; return this; }

	/* ****** Hooked Context APIs ****** */

	@Override public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) {
		final int original_flags = adjustIntentFlags(service);
		if (shouldBlockExplicitRequest(OutboundType.BIND_SERVICE, service)) {
			if (! mDryRun) return false;
			Log.w(TAG, "[DRY-RUN] Suppose to block outbound explicit service binding: " + service);
		}
		final boolean result = super.bindService(service, conn, flags);
		service.setFlags(original_flags);
		return result;
	}

	@Override public ComponentName startService(final Intent service) {
		final int original_flags = adjustIntentFlags(service);
		if (shouldBlockExplicitRequest(OutboundType.START_SERVICE, service)) {
			if (! mDryRun) return null;
			Log.w(TAG, "[DRY-RUN] Suppose to block outbound explicit service starting: " + service);
		}
		final ComponentName result = super.startService(service);
		service.setFlags(original_flags);
		return result;
	}

	@Override public void sendBroadcast(final Intent intent) {
		final int original_flags = adjustIntentFlags(intent);
		if (shouldBlockExplicitRequest(OutboundType.BROADCAST, intent)) {
			if (! mDryRun) return;
			Log.w(TAG, "[DRY-RUN] Suppose to block outbound explicit broadcast: " + intent);
		}
		super.sendBroadcast(intent);
		intent.setFlags(original_flags);
	}

	@Override public Context getApplicationContext() { return mApplicationContext; }

	// TODO: Protect package queries (for service and receiver)
	@Override public PackageManager getPackageManager() {
		if (mDebug) Log.d(TAG, "getPackageManager() is invoked", new Throwable());
		return super.getPackageManager();
	}

	// TODO: Protect outbound provider requests
	@Override public ContentResolver getContentResolver() {
		if (mDebug) Log.d(TAG, "getContentResolver() is invoked", new Throwable());
		return super.getContentResolver();
	}

	/* ********************************* */

	private int adjustIntentFlags(final Intent intent) {
		final int original_flags = intent.getFlags();
		if (mDryRun) return original_flags;
		if (! mAllowBroadcastToBackgroundPackages)
			intent.addFlags(SDK_INT >= N ? FLAG_RECEIVER_EXCLUDE_BACKGROUND : Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		if (SDK_INT >= HONEYCOMB_MR1 && ! mAllowWakingUpStoppedPackages)
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
			if (mDebug) Log.w(TAG, "Blocked outbound " + type + ": " + intent);
			return true;
		} else return false;
	}

	private CondomContext(final Context base, final Context app_context) {
		super(base);
		mApplicationContext = app_context;
		mDebug = (base.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
	}

	private boolean mDryRun;
	private OutboundJudge mOutboundJudge;
	private boolean mAllowWakingUpStoppedPackages;
	private boolean mAllowBroadcastToBackgroundPackages;
	private final boolean mDebug;
	private final Context mApplicationContext;

	/**
	 * If set, the broadcast will never go to manifest receivers in background (cached
	 * or not running) apps, regardless of whether that would be done by default.  By
	 * default they will receive broadcasts if the broadcast has specified an
	 * explicit component or package name.
	 */
	@RequiresApi(N) private static final int FLAG_RECEIVER_EXCLUDE_BACKGROUND = 0x00800000;

	private static final String TAG = "Project.Condom";

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

		CondomApplication(final Application app) {
			mApplication = app;
			attachBaseContext(new CondomContext(app, app));
		}

		private final Application mApplication;
	}
}
