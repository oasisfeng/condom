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
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import androidx.annotation.CheckResult;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.Size;

import com.oasisfeng.condom.util.Lazy;

import java.util.concurrent.Executor;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.Q;

/**
 * The condom-style {@link ContextWrapper} to prevent unwanted behaviors going through.
 *
 * Created by Oasis on 2017/3/25.
 */
@Keep
public class CondomContext extends ContextWrapper {

	public static @CheckResult CondomContext wrap(final Context base, final @Nullable @Size(max=13) String tag) {
		return wrap(base, tag, new CondomOptions());
	}

	/**
	 * This is the very first (probably only) API you need to wrap the naked {@link Context} under protection of <code>CondomContext</code>
	 *
	 * @param base	the original context used before <code>CondomContext</code> is introduced.
	 * @param tag	the optional tag to distinguish between multiple instances of <code>CondomContext</code> used parallel.
	 */
	public static @CheckResult CondomContext wrap(final Context base, final @Nullable @Size(max=13) String tag, final CondomOptions options) {
		if (base instanceof CondomContext) {
			final CondomContext condom = ((CondomContext) base);
			Log.w("Condom", "The wrapped context is already a CondomContext (tag: " + condom.TAG + "), tag and options specified here will be ignore.");
			return condom;
		}
		final Context app_context = base.getApplicationContext();
		final CondomCore condom = new CondomCore(base, options, CondomCore.buildLogTag("Condom", "Condom.", tag));
		if (app_context instanceof Application) {	// The application context is indeed an Application, this should be preserved semantically.
			final Application app = (Application) app_context;
			final CondomApplication condom_app = new CondomApplication(condom, app, tag);	// TODO: Application instance should be unique across CondomContext.
			final CondomContext condom_context = new CondomContext(condom, condom_app, tag);
			condom_app.attachBaseContext(base == app_context ? condom_context : new CondomContext(condom, app, tag));
			return condom_context;
		} else return new CondomContext(condom, base == app_context ? null : new CondomContext(condom, app_context, tag), tag);
	}

	/** @deprecated Use {@link CondomOptions} instead */
	public CondomContext setDryRun(final boolean dry_run) {
		if (dry_run == mCondom.mDryRun) return this;
		mCondom.mDryRun = dry_run;
		if (dry_run) Log.w(TAG, "Start dry-run mode, no outbound requests will be blocked actually, despite later stated in log.");
		else Log.w(TAG, "Stop dry-run mode.");
		return this;
	}

	/** @deprecated Use {@link CondomOptions} instead */
	@Deprecated public CondomContext preventWakingUpStoppedPackages(final boolean prevent_or_not) { mCondom.mExcludeStoppedPackages = prevent_or_not; return this; }

	/** @deprecated Use {@link CondomOptions} instead */
	@Deprecated public CondomContext preventBroadcastToBackgroundPackages(final boolean prevent_or_not) { mCondom.mExcludeBackgroundReceivers = prevent_or_not; return this; }

	/** @deprecated Use {@link CondomOptions} instead */
	@Deprecated public CondomContext preventServiceInBackgroundPackages(final boolean prevent_or_not) { if (SDK_INT < O) mCondom.mExcludeBackgroundServices = prevent_or_not; return this; }

	/* ****** Hooked Context APIs ****** */

	@Override public boolean bindService(final Intent intent, final ServiceConnection conn, final int flags) {
		return doBindService(intent, () -> CondomContext.super.bindService(intent, conn, flags));
	}

	@RequiresApi(Q) @Override public boolean bindService(final Intent intent, final int flags, final Executor executor, final ServiceConnection conn) {
		return doBindService(intent, () -> CondomContext.super.bindService(intent, flags, executor, conn));
	}

	@RequiresApi(Q) @Override public boolean bindIsolatedService(final Intent intent, final int flags, final String instanceName, final Executor executor, final ServiceConnection conn) {
		return doBindService(intent, () -> CondomContext.super.bindIsolatedService(intent, flags, instanceName, executor, conn));
	}

	private boolean doBindService(final Intent intent, final CondomCore.WrappedValueProcedure<Boolean> procedure) {
		final boolean result = mCondom.proceed(OutboundType.BIND_SERVICE, intent, Boolean.FALSE, procedure);
		if (result) mCondom.logIfOutboundPass(TAG, intent, CondomCore.getTargetPackage(intent), CondomCore.CondomEvent.BIND_PASS);
		return result;
	}

	@Override public ComponentName startService(final Intent intent) {
		final ComponentName component = mCondom.proceed(OutboundType.START_SERVICE, intent, null, () ->
				CondomContext.super.startService(intent));
		if (component != null) mCondom.logIfOutboundPass(TAG, intent, component.getPackageName(), CondomCore.CondomEvent.START_PASS);
		return component;
	}

	@Override public void sendBroadcast(final Intent intent) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendBroadcast(intent);
		}}, null);
	}

	@Override public void sendBroadcast(final Intent intent, final String receiverPermission) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendBroadcast(intent, receiverPermission);
		}}, null);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendBroadcastAsUser(intent, user);
		}}, null);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override
	public void sendBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendBroadcastAsUser(intent, user, receiverPermission);
		}}, null);
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendOrderedBroadcast(intent, receiverPermission);
		}}, null);
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission, final BroadcastReceiver resultReceiver,
											   final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}}, resultReceiver);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override
	public void sendOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission,
			final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}}, resultReceiver);
	}

	@Override @SuppressLint("MissingPermission") public void sendStickyBroadcast(final Intent intent) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendStickyBroadcast(intent);
		}}, null);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override public void sendStickyBroadcastAsUser(final Intent intent, final UserHandle user) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendStickyBroadcastAsUser(intent, user);
		}}, null);
	}

	@Override @SuppressLint("MissingPermission") public void sendStickyOrderedBroadcast(final Intent intent, final BroadcastReceiver resultReceiver,
			final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}}, resultReceiver);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override
	public void sendStickyOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			CondomContext.super.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}}, resultReceiver);
	}

	@Override public Object getSystemService(final String name) {
		final Object service = mCondom.getSystemService(name);
		return service != null ? service : super.getSystemService(name);
	}

	@RequiresApi(M) @Override public int checkSelfPermission(final String permission) {
		return mCondom.shouldSpoofPermission(permission) ? PERMISSION_GRANTED : super.checkSelfPermission(permission);
	}

	@Override public int checkPermission(final String permission, final int pid, final int uid) {
		return pid == Process.myPid() && uid == Process.myUid() && mCondom.shouldSpoofPermission(permission) ? PERMISSION_GRANTED
				: super.checkPermission(permission, pid, uid);
	}

	@Override public ContentResolver getContentResolver() { return mCondom.getContentResolver(); }
	@Override public PackageManager getPackageManager() { return mCondom.getPackageManager(); }
	@Override public Context getApplicationContext() { return mApplicationContext; }
	@Override public Context getBaseContext() {
		mCondom.logConcern(TAG, "getBaseContext");
		return mBaseContext.get();
	}

	/* ********************************* */

	private CondomContext(final CondomCore condom, final @Nullable Context app_context, final @Nullable @Size(max=16) String tag) {
		super(condom.mBase);
		mCondom = condom;
		mApplicationContext = app_context != null ? app_context : this;
		mBaseContext = new Lazy<Context>() { @Override protected Context create() {
			return new PseudoContextImpl(CondomContext.this);
		}};
		TAG = CondomCore.buildLogTag("Condom", "Condom.", tag);
	}

	CondomCore mCondom;
	private final Context mApplicationContext;
	private final Lazy<Context> mBaseContext;
	final String TAG;

	/* ****** Internal branch functionality ****** */

	// This should act as what ContextImpl stands for in the naked Context structure.
	private static class PseudoContextImpl extends PseudoContextWrapper {
		public PseudoContextImpl(final CondomContext condom) { super(condom); }
	}
}
