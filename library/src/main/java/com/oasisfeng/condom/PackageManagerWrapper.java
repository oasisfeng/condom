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

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.KITKAT_WATCH;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;
import static android.os.Build.VERSION_CODES.S;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.FeatureInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Delegation wrapper of {@link PackageManager}
 *
 * Created by Oasis on 2017/3/27.
 */
@Keep @SuppressWarnings("LocalCanBeFinal")
public class PackageManagerWrapper extends PackageManager {

	@SuppressWarnings("deprecation") public PackageManagerWrapper(PackageManager base) { mBase = base; }

	@Override public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
		return mBase.getPackageInfo(packageName, flags);
	}

	@Override public String[] currentToCanonicalPackageNames(String[] names) {
		return mBase.currentToCanonicalPackageNames(names);
	}

	@Override public String[] canonicalToCurrentPackageNames(String[] names) {
		return mBase.canonicalToCurrentPackageNames(names);
	}

	@Override public Intent getLaunchIntentForPackage(String packageName) {
		return mBase.getLaunchIntentForPackage(packageName);
	}

	@RequiresApi(LOLLIPOP) @Override public Intent getLeanbackLaunchIntentForPackage(String packageName) {
		return mBase.getLeanbackLaunchIntentForPackage(packageName);
	}

	@Override public int[] getPackageGids(String packageName) throws NameNotFoundException {
		return mBase.getPackageGids(packageName);
	}

	@RequiresApi(N) @Override public int[] getPackageGids(String packageName, int flags) throws NameNotFoundException {
		return mBase.getPackageGids(packageName, flags);
	}

	@RequiresApi(N) @Override public int getPackageUid(String packageName, int flags) throws NameNotFoundException {
		return mBase.getPackageUid(packageName, flags);
	}

	@Override public PermissionInfo getPermissionInfo(String name, int flags) throws NameNotFoundException {
		return mBase.getPermissionInfo(name, flags);
	}

	@Override public @NonNull List<PermissionInfo> queryPermissionsByGroup(@Nullable String group, int flags) throws NameNotFoundException {
		return mBase.queryPermissionsByGroup(group, flags);
	}

	@Override public @NonNull PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws NameNotFoundException {
		return mBase.getPermissionGroupInfo(name, flags);
	}

	@Override public @NonNull List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
		return mBase.getAllPermissionGroups(flags);
	}

	@Override public @NonNull ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
		return mBase.getApplicationInfo(packageName, flags);
	}

	@Override public @NonNull ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getActivityInfo(component, flags);
	}

	@Override public @NonNull ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getReceiverInfo(component, flags);
	}

	@Override public @NonNull ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getServiceInfo(component, flags);
	}

	@Override public @NonNull ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getProviderInfo(component, flags);
	}

	@Override public @NonNull List<PackageInfo> getInstalledPackages(int flags) {
		return mBase.getInstalledPackages(flags);
	}

	@RequiresApi(JELLY_BEAN_MR2) @Override public @NonNull List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
		return mBase.getPackagesHoldingPermissions(permissions, flags);
	}

	@Override public int checkPermission(String permName, String pkgName) {
		return mBase.checkPermission(permName, pkgName);
	}

	@RequiresApi(M) @Override public boolean isPermissionRevokedByPolicy(String permName, String pkgName) {
		return mBase.isPermissionRevokedByPolicy(permName, pkgName);
	}

	@Override public boolean addPermission(PermissionInfo info) {
		return mBase.addPermission(info);
	}
	@Override public boolean addPermissionAsync(PermissionInfo info) {
		return mBase.addPermissionAsync(info);
	}
	@Override public int checkSignatures(String pkg1, String pkg2) {
		return mBase.checkSignatures(pkg1, pkg2);
	}
	@Override public int checkSignatures(int uid1, int uid2) {
		return mBase.checkSignatures(uid1, uid2);
	}
	@Override public @NonNull List<ApplicationInfo> getInstalledApplications(int flags) {
		return mBase.getInstalledApplications(flags);
	}
	@Override @Nullable public String[] getPackagesForUid(int uid) {
		return mBase.getPackagesForUid(uid);
	}
	@Override @Nullable public String getNameForUid(int uid) {
		return mBase.getNameForUid(uid);
	}
	@Override public @NonNull FeatureInfo[] getSystemAvailableFeatures() {
		return mBase.getSystemAvailableFeatures();
	}
	@Override public void removePermission(String name) {
		mBase.removePermission(name);
	}

	@RequiresApi(O) @Override public boolean canRequestPackageInstalls() {
		return mBase.canRequestPackageInstalls();
	}
	@RequiresApi(O) @Override public void clearInstantAppCookie() {
		mBase.clearInstantAppCookie();
	}
	@RequiresApi(O) @Override public ChangedPackages getChangedPackages(int sequenceNumber) {
		return mBase.getChangedPackages(sequenceNumber);
	}
	@RequiresApi(O) @Override public @NonNull byte[] getInstantAppCookie() {
		return mBase.getInstantAppCookie();
	}
	@RequiresApi(O) @Override public int getInstantAppCookieMaxBytes() {
		return mBase.getInstantAppCookieMaxBytes();
	}
	@RequiresApi(O) @Override public PackageInfo getPackageInfo(VersionedPackage versionedPackage, int flags) throws NameNotFoundException {
		return mBase.getPackageInfo(versionedPackage, flags);
	}
	@RequiresApi(O) @Override public @NonNull List<SharedLibraryInfo> getSharedLibraries(int flags) {
		return mBase.getSharedLibraries(flags);
	}
	@Override public String[] getSystemSharedLibraryNames() {
		return mBase.getSystemSharedLibraryNames();
	}
	@RequiresApi(O) @Override public boolean isInstantApp() {
		return mBase.isInstantApp();
	}
	@RequiresApi(O) @Override public boolean isInstantApp(String packageName) {
		return mBase.isInstantApp(packageName);
	}
	@RequiresApi(O) @Override public void setApplicationCategoryHint(String packageName, int categoryHint) {
		mBase.setApplicationCategoryHint(packageName, categoryHint);
	}
	@RequiresApi(O) @Override public void updateInstantAppCookie(@Nullable byte[] cookie) {
		mBase.updateInstantAppCookie(cookie);
	}

	@RequiresApi(P) @Override public boolean isPackageSuspended() {
		return mBase.isPackageSuspended();
	}
	@RequiresApi(P) @Override public Bundle getSuspendedPackageAppExtras() {
		return mBase.getSuspendedPackageAppExtras();
	}

	@Override public boolean hasSystemFeature(String name) {
		return mBase.hasSystemFeature(name);
	}

	@RequiresApi(N) @Override public boolean hasSystemFeature(String name, int version) {
		return mBase.hasSystemFeature(name, version);
	}

	@Override public ResolveInfo resolveActivity(Intent intent, int flags) {
		return mBase.resolveActivity(intent, flags);
	}

	@Override public @NonNull List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
		return mBase.queryIntentActivities(intent, flags);
	}

	@Override public @NonNull List<ResolveInfo> queryIntentActivityOptions(@Nullable ComponentName caller, @Nullable Intent[] specifics, Intent intent, int flags) {
		return mBase.queryIntentActivityOptions(caller, specifics, intent, flags);
	}

	@Override public @NonNull List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
		return mBase.queryBroadcastReceivers(intent, flags);
	}

	@Override public ResolveInfo resolveService(Intent intent, int flags) {
		return mBase.resolveService(intent, flags);
	}

	@Override public @NonNull List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
		return mBase.queryIntentServices(intent, flags);
	}

	@RequiresApi(KITKAT) @Override public @NonNull List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
		return mBase.queryIntentContentProviders(intent, flags);
	}

	@Override public ProviderInfo resolveContentProvider(String name, int flags) {
		return mBase.resolveContentProvider(name, flags);
	}

	@Override public @NonNull List<ProviderInfo> queryContentProviders(@Nullable String processName, int uid, int flags) {
		return mBase.queryContentProviders(processName, uid, flags);
	}

	@Override public @NonNull InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) throws NameNotFoundException {
		return mBase.getInstrumentationInfo(className, flags);
	}

	@Override public @NonNull List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
		return mBase.queryInstrumentation(targetPackage, flags);
	}

	@Override public Drawable getDrawable(String packageName, int resid, @Nullable ApplicationInfo appInfo) {
		return mBase.getDrawable(packageName, resid, appInfo);
	}

	@Override public @NonNull Drawable getActivityIcon(ComponentName activityName) throws NameNotFoundException {
		return mBase.getActivityIcon(activityName);
	}

	@Override public @NonNull Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
		return mBase.getActivityIcon(intent);
	}

	@RequiresApi(KITKAT_WATCH) @Override public Drawable getActivityBanner(ComponentName activityName) throws NameNotFoundException {
		return mBase.getActivityBanner(activityName);
	}

	@RequiresApi(KITKAT_WATCH) @Override public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
		return mBase.getActivityBanner(intent);
	}

	@Override public @NonNull Drawable getDefaultActivityIcon() {
		return mBase.getDefaultActivityIcon();
	}

	@Override public @NonNull Drawable getApplicationIcon(ApplicationInfo info) {
		return mBase.getApplicationIcon(info);
	}

	@Override public @NonNull Drawable getApplicationIcon(String packageName) throws NameNotFoundException {
		return mBase.getApplicationIcon(packageName);
	}

	@RequiresApi(KITKAT_WATCH) @Override public Drawable getApplicationBanner(ApplicationInfo info) {
		return mBase.getApplicationBanner(info);
	}

	@RequiresApi(KITKAT_WATCH) @Override public Drawable getApplicationBanner(String packageName) throws NameNotFoundException {
		return mBase.getApplicationBanner(packageName);
	}

	@Override public Drawable getActivityLogo(ComponentName activityName) throws NameNotFoundException {
		return mBase.getActivityLogo(activityName);
	}

	@Override public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
		return mBase.getActivityLogo(intent);
	}

	@Override public Drawable getApplicationLogo(ApplicationInfo info) {
		return mBase.getApplicationLogo(info);
	}

	@Override public Drawable getApplicationLogo(String packageName) throws NameNotFoundException {
		return mBase.getApplicationLogo(packageName);
	}

	@RequiresApi(api = LOLLIPOP) @Override public @NonNull Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
		return mBase.getUserBadgedIcon(icon, user);
	}

	@RequiresApi(api = LOLLIPOP) @Override public @NonNull Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, @Nullable Rect badgeLocation, int badgeDensity) {
		return mBase.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
	}

	@RequiresApi(api = LOLLIPOP) @Override public @NonNull CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
		return mBase.getUserBadgedLabel(label, user);
	}

	@Override public CharSequence getText(String packageName, int resid, @Nullable ApplicationInfo appInfo) {
		return mBase.getText(packageName, resid, appInfo);
	}

	@Override public XmlResourceParser getXml(String packageName, int resid, @Nullable ApplicationInfo appInfo) {
		return mBase.getXml(packageName, resid, appInfo);
	}

	@Override public @NonNull CharSequence getApplicationLabel(ApplicationInfo info) {
		return mBase.getApplicationLabel(info);
	}

	@Override public @NonNull Resources getResourcesForActivity(ComponentName activityName) throws NameNotFoundException {
		return mBase.getResourcesForActivity(activityName);
	}

	@Override public @NonNull Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException {
		return mBase.getResourcesForApplication(app);
	}

	@Override public @NonNull Resources getResourcesForApplication(String appPackageName) throws NameNotFoundException {
		return mBase.getResourcesForApplication(appPackageName);
	}

	@Override public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) {
		return mBase.getPackageArchiveInfo(archiveFilePath, flags);
	}

	@Override public void verifyPendingInstall(int id, int verificationCode) {
		mBase.verifyPendingInstall(id, verificationCode);
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
		mBase.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
	}

	@Override public void setInstallerPackageName(String targetPackage, @Nullable String installerPackageName) {
		mBase.setInstallerPackageName(targetPackage, installerPackageName);
	}

	@Override public String getInstallerPackageName(String packageName) {
		return mBase.getInstallerPackageName(packageName);
	}

	@Override public void addPackageToPreferred(String packageName) {
		mBase.addPackageToPreferred(packageName);
	}

	@Override public void removePackageFromPreferred(String packageName) {
		mBase.removePackageFromPreferred(packageName);
	}

	@Override public @NonNull List<PackageInfo> getPreferredPackages(int flags) {
		return mBase.getPreferredPackages(flags);
	}

	@Override public void addPreferredActivity(IntentFilter filter, int match, @Nullable ComponentName[] set, ComponentName activity) {
		mBase.addPreferredActivity(filter, match, set, activity);
	}

	@Override public void clearPackagePreferredActivities(String packageName) {
		mBase.clearPackagePreferredActivities(packageName);
	}

	@Override public int getPreferredActivities(@NonNull List<IntentFilter> outFilters, @NonNull List<ComponentName> outActivities, @Nullable String packageName) {
		return mBase.getPreferredActivities(outFilters, outActivities, packageName);
	}

	@Override public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {
		mBase.setComponentEnabledSetting(componentName, newState, flags);
	}

	@Override public int getComponentEnabledSetting(ComponentName componentName) {
		return mBase.getComponentEnabledSetting(componentName);
	}

	@Override public void setApplicationEnabledSetting(String packageName, int newState, int flags) {
		mBase.setApplicationEnabledSetting(packageName, newState, flags);
	}

	@Override public int getApplicationEnabledSetting(String packageName) {
		return mBase.getApplicationEnabledSetting(packageName);
	}

	@Override public boolean isSafeMode() {
		return mBase.isSafeMode();
	}

	@RequiresApi(LOLLIPOP) @Override @NonNull public PackageInstaller getPackageInstaller() {
		return mBase.getPackageInstaller();
	}

	@RequiresApi(P) @Override public boolean hasSigningCertificate(String packageName, byte[] certificate, int type) {
		return mBase.hasSigningCertificate(packageName, certificate, type);
	}

	@RequiresApi(Q) public boolean addWhitelistedRestrictedPermission(String packageName, String permission, int whitelistFlags) {
		return mBase.addWhitelistedRestrictedPermission(packageName, permission, whitelistFlags);
	}
	@RequiresApi(Q) public @NonNull Set<String> getWhitelistedRestrictedPermissions(String packageName, int whitelistFlag) {
		return mBase.getWhitelistedRestrictedPermissions(packageName, whitelistFlag);
	}
	@RequiresApi(Q) public boolean removeWhitelistedRestrictedPermission(String packageName, String permission, int whitelistFlags) {
		return mBase.removeWhitelistedRestrictedPermission(packageName, permission, whitelistFlags);
	}
	@RequiresApi(Q) public @NonNull List<ModuleInfo> getInstalledModules(int flags) {
		return mBase.getInstalledModules(flags);
	}
	@RequiresApi(Q) public @NonNull ModuleInfo getModuleInfo(String packageName, int flags) throws NameNotFoundException {
		return mBase.getModuleInfo(packageName, flags);
	}
	@RequiresApi(Q) public boolean getSyntheticAppDetailsActivityEnabled(String packageName) {
		return mBase.getSyntheticAppDetailsActivityEnabled(packageName);
	}
	@RequiresApi(Q) public boolean isPackageSuspended(String packageName) throws NameNotFoundException {
		return mBase.isPackageSuspended(packageName);
	}
	@RequiresApi(Q) public boolean isDeviceUpgrading() { return mBase.isDeviceUpgrading(); }

	@RequiresApi(R) @Override public @NonNull CharSequence getBackgroundPermissionOptionLabel() {
		return mBase.getBackgroundPermissionOptionLabel();
	}
	@RequiresApi(R) @Override public @NonNull InstallSourceInfo getInstallSourceInfo(@NonNull final String packageName) throws NameNotFoundException {
		return mBase.getInstallSourceInfo(packageName);
	}
	@RequiresApi(R) @Override public @NonNull Set<String> getMimeGroup(@NonNull final String mimeGroup) {
		return mBase.getMimeGroup(mimeGroup);
	}
	@RequiresApi(R) @Override public boolean isAutoRevokeWhitelisted() {
		return mBase.isAutoRevokeWhitelisted();
	}
	@RequiresApi(R) @Override public boolean isAutoRevokeWhitelisted(@NonNull final String packageName) {
		return mBase.isAutoRevokeWhitelisted(packageName);
	}
	@RequiresApi(R) @Override public boolean isDefaultApplicationIcon(@NonNull final Drawable drawable) {
		return mBase.isDefaultApplicationIcon(drawable);
	}
	@RequiresApi(R) @Override public boolean setAutoRevokeWhitelisted(@NonNull final String packageName, final boolean whitelisted) {
		return mBase.setAutoRevokeWhitelisted(packageName, whitelisted);
	}
	@RequiresApi(R) @Override public void setMimeGroup(@NonNull final String mimeGroup, @NonNull final Set<String> mimeTypes) {
		mBase.setMimeGroup(mimeGroup, mimeTypes);
	}

	@RequiresApi(S) @Override public void getGroupOfPlatformPermission(@NonNull final String permissionName, @NonNull final Executor executor, @NonNull final Consumer<String> callback) {
		mBase.getGroupOfPlatformPermission(permissionName, executor, callback);
	}
	@RequiresApi(S) @Override public void getPlatformPermissionsForGroup(@NonNull final String permissionGroupName, @NonNull final Executor executor, @NonNull final Consumer<List<String>> callback) {
		mBase.getPlatformPermissionsForGroup(permissionGroupName, executor, callback);
	}
	@RequiresApi(S) @Override public @NonNull Property getProperty(@NonNull final String propertyName, @NonNull final ComponentName component) throws NameNotFoundException {
		return mBase.getProperty(propertyName, component);
	}
	@RequiresApi(S) @Override public @NonNull Property getProperty(@NonNull final String propertyName, @NonNull final String packageName) throws NameNotFoundException {
		return mBase.getProperty(propertyName, packageName);
	}
	@RequiresApi(S) @Override public @NonNull Resources getResourcesForApplication(@NonNull final ApplicationInfo app, @Nullable final Configuration configuration) throws NameNotFoundException {
		return mBase.getResourcesForApplication(app, configuration);
	}
	@RequiresApi(S) @Override public int getTargetSdkVersion(@NonNull final String packageName) throws NameNotFoundException {
		return mBase.getTargetSdkVersion(packageName);
	}
	@RequiresApi(S) @Override public @NonNull List<Property> queryActivityProperty(@NonNull final String propertyName) {
		return mBase.queryActivityProperty(propertyName);
	}
	@RequiresApi(S) @Override public @NonNull List<Property> queryApplicationProperty(@NonNull final String propertyName) {
		return mBase.queryApplicationProperty(propertyName);
	}
	@RequiresApi(S) @Override public @NonNull List<Property> queryProviderProperty(@NonNull final String propertyName) {
		return mBase.queryProviderProperty(propertyName);
	}
	@RequiresApi(S) @Override public @NonNull List<Property> queryReceiverProperty(@NonNull final String propertyName) {
		return mBase.queryReceiverProperty(propertyName);
	}
	@RequiresApi(S) @Override public @NonNull List<Property> queryServiceProperty(@NonNull final String propertyName) {
		return mBase.queryServiceProperty(propertyName);
	}
	@RequiresApi(S) @Override public void requestChecksums(@NonNull final String packageName, final boolean includeSplits, final int required, @NonNull final List<Certificate> trustedInstallers, @NonNull final OnChecksumsReadyListener onChecksumsReadyListener) throws CertificateEncodingException, NameNotFoundException {
		mBase.requestChecksums(packageName, includeSplits, required, trustedInstallers, onChecksumsReadyListener);
	}

	@RequiresApi(TIRAMISU) @Override public boolean canPackageQuery(@NonNull final String sourcePackageName, @NonNull final String targetPackageName) throws NameNotFoundException {
		return mBase.canPackageQuery(sourcePackageName, targetPackageName);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull ActivityInfo getActivityInfo(@NonNull final ComponentName component, @NonNull final ComponentInfoFlags flags) throws NameNotFoundException {
		return mBase.getActivityInfo(component, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull ApplicationInfo getApplicationInfo(@NonNull final String packageName, @NonNull final ApplicationInfoFlags flags) throws NameNotFoundException {
		return mBase.getApplicationInfo(packageName, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<ApplicationInfo> getInstalledApplications(@NonNull final ApplicationInfoFlags flags) {
		return mBase.getInstalledApplications(flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<PackageInfo> getInstalledPackages(@NonNull final PackageInfoFlags flags) {
		return mBase.getInstalledPackages(flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull IntentSender getLaunchIntentSenderForPackage(@NonNull final String packageName) {
		return mBase.getLaunchIntentSenderForPackage(packageName);
	}
	@RequiresApi(TIRAMISU) @Override public @Nullable PackageInfo getPackageArchiveInfo(@NonNull final String archiveFilePath, @NonNull final PackageInfoFlags flags) {
		return mBase.getPackageArchiveInfo(archiveFilePath, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @Nullable int[] getPackageGids(@NonNull final String packageName, @NonNull final PackageInfoFlags flags) throws NameNotFoundException {
		return mBase.getPackageGids(packageName, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull PackageInfo getPackageInfo(@NonNull final VersionedPackage versionedPackage, @NonNull final PackageInfoFlags flags) throws NameNotFoundException {
		return mBase.getPackageInfo(versionedPackage, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull PackageInfo getPackageInfo(@NonNull final String packageName, @NonNull final PackageInfoFlags flags) throws NameNotFoundException {
		return mBase.getPackageInfo(packageName, flags);
	}
	@RequiresApi(TIRAMISU) @Override public int getPackageUid(@NonNull final String packageName, @NonNull final PackageInfoFlags flags) throws NameNotFoundException {
		return mBase.getPackageUid(packageName, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<PackageInfo> getPackagesHoldingPermissions(@NonNull final String[] permissions, @NonNull final PackageInfoFlags flags) {
		return mBase.getPackagesHoldingPermissions(permissions, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull ProviderInfo getProviderInfo(@NonNull final ComponentName component, @NonNull final ComponentInfoFlags flags) throws NameNotFoundException {
		return mBase.getProviderInfo(component, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull ActivityInfo getReceiverInfo(@NonNull final ComponentName component, @NonNull final ComponentInfoFlags flags) throws NameNotFoundException {
		return mBase.getReceiverInfo(component, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull ServiceInfo getServiceInfo(@NonNull final ComponentName component, @NonNull final ComponentInfoFlags flags) throws NameNotFoundException {
		return mBase.getServiceInfo(component, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<SharedLibraryInfo> getSharedLibraries(@NonNull final PackageInfoFlags flags) {
		return mBase.getSharedLibraries(flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<ResolveInfo> queryBroadcastReceivers(@NonNull final Intent intent, @NonNull final ResolveInfoFlags flags) {
		return mBase.queryBroadcastReceivers(intent, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<ProviderInfo> queryContentProviders(@Nullable final String processName, final int uid, @NonNull final ComponentInfoFlags flags) {
		return mBase.queryContentProviders(processName, uid, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<ResolveInfo> queryIntentActivities(@NonNull final Intent intent, @NonNull final ResolveInfoFlags flags) {
		return mBase.queryIntentActivities(intent, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<ResolveInfo> queryIntentActivityOptions(@Nullable final ComponentName caller, @Nullable final List<Intent> specifics, @NonNull final Intent intent, @NonNull final ResolveInfoFlags flags) {
		return mBase.queryIntentActivityOptions(caller, specifics, intent, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<ResolveInfo> queryIntentContentProviders(@NonNull final Intent intent, @NonNull final ResolveInfoFlags flags) {
		return mBase.queryIntentContentProviders(intent, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @NonNull List<ResolveInfo> queryIntentServices(@NonNull final Intent intent, @NonNull final ResolveInfoFlags flags) {
		return mBase.queryIntentServices(intent, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @Nullable ResolveInfo resolveActivity(@NonNull final Intent intent, @NonNull final ResolveInfoFlags flags) {
		return mBase.resolveActivity(intent, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @Nullable ProviderInfo resolveContentProvider(@NonNull final String authority, @NonNull final ComponentInfoFlags flags) {
		return mBase.resolveContentProvider(authority, flags);
	}
	@RequiresApi(TIRAMISU) @Override public @Nullable ResolveInfo resolveService(@NonNull final Intent intent, @NonNull final ResolveInfoFlags flags) {
		return mBase.resolveService(intent, flags);
	}
	@RequiresApi(TIRAMISU) @Override public void setComponentEnabledSettings(@NonNull final List<ComponentEnabledSetting> settings) {
		mBase.setComponentEnabledSettings(settings);
	}

	@RequiresApi(UPSIDE_DOWN_CAKE) @Override public @NonNull boolean[] canPackageQuery(@NonNull final String sourcePackageName, @NonNull final String[] targetPackageNames) throws NameNotFoundException {
		return mBase.canPackageQuery(sourcePackageName, targetPackageNames);
	}
	@RequiresApi(UPSIDE_DOWN_CAKE) @Override public void relinquishUpdateOwnership(@NonNull final String targetPackage) {
		mBase.relinquishUpdateOwnership(targetPackage);
	}

	final private PackageManager mBase;
}
