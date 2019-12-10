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
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.Size;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.M;

/**
 * Application wrapper for {@link CondomContext#getApplicationContext()}
 *
 * Created by Oasis on 2018/1/6.
 */
class CondomApplication extends Application {

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

	// The actual context returned may not be semantically consistent. Let's keep an eye for it in the wild.
	@Override public Context getBaseContext() {
		mCondom.logConcern(TAG, "Application.getBaseContext");
		return super.getBaseContext();
	}

	@Override protected void attachBaseContext(final Context base) { super.attachBaseContext(base); }

	/* ****** Hooked Context APIs ****** */

	@Override public boolean bindService(final Intent intent, final ServiceConnection conn, final int flags) {
		final boolean result = mCondom.proceed(OutboundType.BIND_SERVICE, intent, Boolean.FALSE, new CondomCore.WrappedValueProcedure<Boolean>() { @Override public Boolean proceed() {
			return mApplication.bindService(intent, conn, flags);
		}});
		if (result) mCondom.logIfOutboundPass(TAG, intent, CondomCore.getTargetPackage(intent), CondomCore.CondomEvent.BIND_PASS);
		return result;
	}

	@Override public ComponentName startService(final Intent intent) {
		final ComponentName component = mCondom.proceed(OutboundType.START_SERVICE, intent, null, new CondomCore.WrappedValueProcedure<ComponentName>() { @Override public ComponentName proceed() {
			return mApplication.startService(intent);
		}});
		if (component != null) mCondom.logIfOutboundPass(TAG, intent, component.getPackageName(), CondomCore.CondomEvent.START_PASS);
		return component;
	}

	@Override public void sendBroadcast(final Intent intent) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendBroadcast(intent);
		}}, null);
	}

	@Override public void sendBroadcast(final Intent intent, final String receiverPermission) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendBroadcast(intent, receiverPermission);
		}}, null);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendBroadcastAsUser(intent, user);
		}}, null);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override
	public void sendBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendBroadcastAsUser(intent, user, receiverPermission);
		}}, null);
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendOrderedBroadcast(intent, receiverPermission);
		}}, null);
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission, final BroadcastReceiver resultReceiver,
											   final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}}, resultReceiver);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override
	public void sendOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission,
										   final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}}, resultReceiver);
	}

	@Override @SuppressLint("MissingPermission") public void sendStickyBroadcast(final Intent intent) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendStickyBroadcast(intent);
		}}, null);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override public void sendStickyBroadcastAsUser(final Intent intent, final UserHandle user) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendStickyBroadcastAsUser(intent, user);
		}}, null);
	}

	@Override @SuppressLint("MissingPermission") public void sendStickyOrderedBroadcast(final Intent intent, final BroadcastReceiver resultReceiver,
																						final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}}, resultReceiver);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override
	public void sendStickyOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(this, intent, new CondomCore.WrappedProcedure() { @Override public void run() {
			mApplication.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode, initialData, initialExtras);
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
	@Override public Context getApplicationContext() { return this; }

	CondomApplication(final CondomCore condom, final Application app, final @Nullable @Size(max = 13) String tag) {
		mCondom = condom;
		mApplication = app;
		TAG = CondomCore.buildLogTag("CondomApp", "CondomApp.", tag);
	}

	private final CondomCore mCondom;
	private final Application mApplication;
	private final String TAG;
}
