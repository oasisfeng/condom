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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.support.annotation.Keep;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.view.Display;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;

/**
 * A package-private copy of {@link ContextWrapper}, to prevent potential leakage caused by {@link ContextWrapper#getBaseContext()}",
 * followed by "instanceof ContextWrapper" check on {@link CondomContext}.
 *
 * Created by Oasis on 2017/3/26.
 */
@Keep @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PseudoContextWrapper extends Context {

	@Override public AssetManager getAssets() {
		return mBase.getAssets();
	}

	@Override public Resources getResources() {
		return mBase.getResources();
	}

	@Override public PackageManager getPackageManager() {
		return mBase.getPackageManager();
	}

	@Override public ContentResolver getContentResolver() {
		return mBase.getContentResolver();
	}

	@Override public Looper getMainLooper() {
		return mBase.getMainLooper();
	}

	@Override public Context getApplicationContext() {
		return mBase.getApplicationContext();
	}

	@Override public void setTheme(int resid) {
		mBase.setTheme(resid);
	}

	@Override public Resources.Theme getTheme() {
		return mBase.getTheme();
	}

	@Override public ClassLoader getClassLoader() {
		return mBase.getClassLoader();
	}

	@Override public String getPackageName() {
		return mBase.getPackageName();
	}

	@Override public ApplicationInfo getApplicationInfo() {
		return mBase.getApplicationInfo();
	}

	@Override public String getPackageResourcePath() {
		return mBase.getPackageResourcePath();
	}

	@Override public String getPackageCodePath() {
		return mBase.getPackageCodePath();
	}

	@Override public SharedPreferences getSharedPreferences(String name, int mode) {
		return mBase.getSharedPreferences(name, mode);
	}

	@RequiresApi(N) @Override public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
		return mBase.moveSharedPreferencesFrom(sourceContext, name);
	}

	@RequiresApi(N) @Override public boolean deleteSharedPreferences(String name) {
		return mBase.deleteSharedPreferences(name);
	}

	@Override public FileInputStream openFileInput(String name) throws FileNotFoundException {
		return mBase.openFileInput(name);
	}

	@Override public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
		return mBase.openFileOutput(name, mode);
	}

	@Override public boolean deleteFile(String name) {
		return mBase.deleteFile(name);
	}

	@Override public File getFileStreamPath(String name) {
		return mBase.getFileStreamPath(name);
	}

	@Override public String[] fileList() {
		return mBase.fileList();
	}

	@RequiresApi(N) @Override public File getDataDir() {
		return mBase.getDataDir();
	}

	@Override public File getFilesDir() {
		return mBase.getFilesDir();
	}

	@RequiresApi(LOLLIPOP) @Override public File getNoBackupFilesDir() {
		return mBase.getNoBackupFilesDir();
	}

	@Override public File getExternalFilesDir(String type) {
		return mBase.getExternalFilesDir(type);
	}

	@RequiresApi(KITKAT) @Override public File[] getExternalFilesDirs(String type) {
		return mBase.getExternalFilesDirs(type);
	}

	@RequiresApi(HONEYCOMB) @Override public File getObbDir() {
		return mBase.getObbDir();
	}

	@RequiresApi(KITKAT) @Override public File[] getObbDirs() {
		return mBase.getObbDirs();
	}

	@Override public File getCacheDir() {
		return mBase.getCacheDir();
	}

	@RequiresApi(LOLLIPOP) @Override public File getCodeCacheDir() {
		return mBase.getCodeCacheDir();
	}

	@Override public File getExternalCacheDir() {
		return mBase.getExternalCacheDir();
	}

	@RequiresApi(KITKAT) @Override public File[] getExternalCacheDirs() {
		return mBase.getExternalCacheDirs();
	}

	@RequiresApi(LOLLIPOP) @Override public File[] getExternalMediaDirs() {
		return mBase.getExternalMediaDirs();
	}

	@Override public File getDir(String name, int mode) {
		return mBase.getDir(name, mode);
	}

	@Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
		return mBase.openOrCreateDatabase(name, mode, factory);
	}

	@RequiresApi(HONEYCOMB) @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory,
			DatabaseErrorHandler errorHandler) {
		return mBase.openOrCreateDatabase(name, mode, factory, errorHandler);
	}

	@RequiresApi(N) @Override public boolean moveDatabaseFrom(Context sourceContext, String name) {
		return mBase.moveDatabaseFrom(sourceContext, name);
	}

	@Override public boolean deleteDatabase(String name) {
		return mBase.deleteDatabase(name);
	}

	@Override public File getDatabasePath(String name) {
		return mBase.getDatabasePath(name);
	}

	@Override public String[] databaseList() {
		return mBase.databaseList();
	}

	@Override @Deprecated public Drawable getWallpaper() {
		return mBase.getWallpaper();
	}

	@Override @Deprecated public Drawable peekWallpaper() {
		return mBase.peekWallpaper();
	}

	@Override @Deprecated public int getWallpaperDesiredMinimumWidth() {
		return mBase.getWallpaperDesiredMinimumWidth();
	}

	@Override @Deprecated public int getWallpaperDesiredMinimumHeight() {
		return mBase.getWallpaperDesiredMinimumHeight();
	}

	@Override @Deprecated public void setWallpaper(Bitmap bitmap) throws IOException {
		mBase.setWallpaper(bitmap);
	}

	@Override @Deprecated public void setWallpaper(InputStream data) throws IOException {
		mBase.setWallpaper(data);
	}

	@Override @Deprecated public void clearWallpaper() throws IOException {
		mBase.clearWallpaper();
	}

	@Override public void startActivity(Intent intent) {
		mBase.startActivity(intent);
	}

	@RequiresApi(JELLY_BEAN) @Override public void startActivity(Intent intent, Bundle options) {
		mBase.startActivity(intent, options);
	}

	@RequiresApi(HONEYCOMB) @Override public void startActivities(Intent[] intents) {
		mBase.startActivities(intents);
	}

	@RequiresApi(JELLY_BEAN) @Override public void startActivities(Intent[] intents, Bundle options) {
		mBase.startActivities(intents, options);
	}

	@Override public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
			throws IntentSender.SendIntentException {
		mBase.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags);
	}

	@RequiresApi(JELLY_BEAN) @Override public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues,
			int extraFlags, Bundle options) throws IntentSender.SendIntentException {
		mBase.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options);
	}

	@Override public void sendBroadcast(Intent intent) {
		mBase.sendBroadcast(intent);
	}

	@Override public void sendBroadcast(Intent intent, String receiverPermission) {
		mBase.sendBroadcast(intent, receiverPermission);
	}

	@Override public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
		mBase.sendOrderedBroadcast(intent, receiverPermission);
	}

	@Override public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler,
			int initialCode, String initialData, Bundle initialExtras) {
		mBase.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override public void sendBroadcastAsUser(Intent intent, UserHandle user) {
		mBase.sendBroadcastAsUser(intent, user);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override
	public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
		mBase.sendBroadcastAsUser(intent, user, receiverPermission);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override
	public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver,
										   Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		mBase.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	@Override @SuppressLint("MissingPermission") public void sendStickyBroadcast(Intent intent) {
		mBase.sendStickyBroadcast(intent);
	}

	@Override @SuppressLint("MissingPermission") public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver,
			Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		mBase.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	@Override @SuppressLint("MissingPermission") public void removeStickyBroadcast(Intent intent) {
		mBase.removeStickyBroadcast(intent);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
		mBase.sendStickyBroadcastAsUser(intent, user);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override public void sendStickyOrderedBroadcastAsUser(Intent intent,
			UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		mBase.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	@RequiresApi(JELLY_BEAN_MR1) @SuppressLint("MissingPermission") @Override @Deprecated public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
		mBase.removeStickyBroadcastAsUser(intent, user);
	}

	@Override public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
		return mBase.registerReceiver(receiver, filter);
	}

	@RequiresApi(O) @Override public Intent registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter, final int flags) {
		return mBase.registerReceiver(receiver, filter, flags);
	}

	@Override public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
		return mBase.registerReceiver(receiver, filter, broadcastPermission, scheduler);
	}

	@RequiresApi(O) @Override public Intent registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter, final String broadcastPermission, final Handler scheduler, final int flags) {
		return mBase.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags);
	}

	@Override public void unregisterReceiver(BroadcastReceiver receiver) {
		mBase.unregisterReceiver(receiver);
	}

	@Override public ComponentName startService(Intent service) {
		return mBase.startService(service);
	}

	@RequiresApi(O) @Override public ComponentName startForegroundService(final Intent service) {
		return mBase.startForegroundService(service);
	}

	@Override public boolean stopService(Intent name) {
		return mBase.stopService(name);
	}

	@Override public boolean bindService(Intent service, ServiceConnection conn, int flags) {
		return mBase.bindService(service, conn, flags);
	}

	@Override public void unbindService(ServiceConnection conn) {
		mBase.unbindService(conn);
	}

	@Override public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
		return mBase.startInstrumentation(className, profileFile, arguments);
	}

	@Override public Object getSystemService(String name) {
		return mBase.getSystemService(name);
	}

	@RequiresApi(M) @Override public String getSystemServiceName(Class<?> serviceClass) {
		return mBase.getSystemServiceName(serviceClass);
	}

	@Override public int checkPermission(String permission, int pid, int uid) {
		return mBase.checkPermission(permission, pid, uid);
	}

	@Override public int checkCallingPermission(String permission) {
		return mBase.checkCallingPermission(permission);
	}

	@Override public int checkCallingOrSelfPermission(String permission) {
		return mBase.checkCallingOrSelfPermission(permission);
	}

	@RequiresApi(M) @Override public int checkSelfPermission(String permission) {
		return mBase.checkSelfPermission(permission);
	}

	@Override public void enforcePermission(String permission, int pid, int uid, String message) {
		mBase.enforcePermission(permission, pid, uid, message);
	}

	@Override public void enforceCallingPermission(String permission, String message) {
		mBase.enforceCallingPermission(permission, message);
	}

	@Override public void enforceCallingOrSelfPermission( String permission, String message) {
		mBase.enforceCallingOrSelfPermission(permission, message);
	}

	@Override public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
		mBase.grantUriPermission(toPackage, uri, modeFlags);
	}

	@Override public void revokeUriPermission(Uri uri, int modeFlags) {
		mBase.revokeUriPermission(uri, modeFlags);
	}

	@RequiresApi(O) @Override public void revokeUriPermission(final String toPackage, final Uri uri, final int modeFlags) {
		mBase.revokeUriPermission(toPackage, uri, modeFlags);
	}

	@Override public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
		return mBase.checkUriPermission(uri, pid, uid, modeFlags);
	}

	@Override public int checkCallingUriPermission(Uri uri, int modeFlags) {
		return mBase.checkCallingUriPermission(uri, modeFlags);
	}

	@Override public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
		return mBase.checkCallingOrSelfUriPermission(uri, modeFlags);
	}

	@Override public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags) {
		return mBase.checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags);
	}

	@Override public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
		mBase.enforceUriPermission(uri, pid, uid, modeFlags, message);
	}

	@Override public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
		mBase.enforceCallingUriPermission(uri, modeFlags, message);
	}

	@Override public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
		mBase.enforceCallingOrSelfUriPermission(uri, modeFlags, message);
	}

	@Override public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags, String message) {
		mBase.enforceUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags, message);
	}

	@Override public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
		return mBase.createPackageContext(packageName, flags);
	}

	@RequiresApi(O) @Override public Context createContextForSplit(final String splitName) throws PackageManager.NameNotFoundException {
		return mBase.createContextForSplit(splitName);
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public Context createConfigurationContext(Configuration overrideConfiguration) {
		return mBase.createConfigurationContext(overrideConfiguration);
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public Context createDisplayContext(Display display) {
		return mBase.createDisplayContext(display);
	}

	@Override public boolean isRestricted() {
		return mBase.isRestricted();
	}

	@RequiresApi(N) @Override public Context createDeviceProtectedStorageContext() {
		return mBase.createDeviceProtectedStorageContext();
	}

	@RequiresApi(N) @Override public boolean isDeviceProtectedStorage() {
		return mBase.isDeviceProtectedStorage();
	}

	public PseudoContextWrapper(Context base) {
		mBase = base;
	}

	final Context mBase;
}
