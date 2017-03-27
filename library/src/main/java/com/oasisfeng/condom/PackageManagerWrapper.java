package com.oasisfeng.condom;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import java.util.List;

import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.KITKAT_WATCH;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;

/**
 * Delegation wrapper of {@link PackageManager}
 *
 * Created by Oasis on 2017/3/27.
 */
class PackageManagerWrapper extends PackageManager {

	public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
		return mBase.getPackageInfo(packageName, flags);
	}

	public String[] currentToCanonicalPackageNames(String[] names) {
		return mBase.currentToCanonicalPackageNames(names);
	}

	public String[] canonicalToCurrentPackageNames(String[] names) {
		return mBase.canonicalToCurrentPackageNames(names);
	}

	public Intent getLaunchIntentForPackage(String packageName) {
		return mBase.getLaunchIntentForPackage(packageName);
	}

	@RequiresApi(LOLLIPOP) public Intent getLeanbackLaunchIntentForPackage(String packageName) {
		return mBase.getLeanbackLaunchIntentForPackage(packageName);
	}

	public int[] getPackageGids(String packageName) throws NameNotFoundException {
		return mBase.getPackageGids(packageName);
	}

	@RequiresApi(N) public int[] getPackageGids(String packageName, int flags) throws NameNotFoundException {
		return mBase.getPackageGids(packageName, flags);
	}

	@RequiresApi(N) public int getPackageUid(String packageName, int flags) throws NameNotFoundException {
		return mBase.getPackageUid(packageName, flags);
	}

	public PermissionInfo getPermissionInfo(String name, int flags) throws NameNotFoundException {
		return mBase.getPermissionInfo(name, flags);
	}

	public List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws NameNotFoundException {
		return mBase.queryPermissionsByGroup(group, flags);
	}

	public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws NameNotFoundException {
		return mBase.getPermissionGroupInfo(name, flags);
	}

	public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
		return mBase.getAllPermissionGroups(flags);
	}

	public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
		return mBase.getApplicationInfo(packageName, flags);
	}

	public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getActivityInfo(component, flags);
	}

	public ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getReceiverInfo(component, flags);
	}

	public ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getServiceInfo(component, flags);
	}

	public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException {
		return mBase.getProviderInfo(component, flags);
	}

	public List<PackageInfo> getInstalledPackages(int flags) {
		return mBase.getInstalledPackages(flags);
	}

	@RequiresApi(JELLY_BEAN_MR2) public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
		return mBase.getPackagesHoldingPermissions(permissions, flags);
	}

	public int checkPermission(String permName, String pkgName) {
		return mBase.checkPermission(permName, pkgName);
	}

	@RequiresApi(M) public boolean isPermissionRevokedByPolicy(@NonNull String permName, @NonNull String pkgName) {
		return mBase.isPermissionRevokedByPolicy(permName, pkgName);
	}

	public boolean addPermission(PermissionInfo info) {
		return mBase.addPermission(info);
	}

	public boolean addPermissionAsync(PermissionInfo info) {
		return mBase.addPermissionAsync(info);
	}

	public void removePermission(String name) {
		mBase.removePermission(name);
	}

	public int checkSignatures(String pkg1, String pkg2) {
		return mBase.checkSignatures(pkg1, pkg2);
	}

	public int checkSignatures(int uid1, int uid2) {
		return mBase.checkSignatures(uid1, uid2);
	}

	@Nullable public String[] getPackagesForUid(int uid) {
		return mBase.getPackagesForUid(uid);
	}

	@Nullable public String getNameForUid(int uid) {
		return mBase.getNameForUid(uid);
	}

	public List<ApplicationInfo> getInstalledApplications(int flags) {
		return mBase.getInstalledApplications(flags);
	}

	public String[] getSystemSharedLibraryNames() {
		return mBase.getSystemSharedLibraryNames();
	}

	public FeatureInfo[] getSystemAvailableFeatures() {
		return mBase.getSystemAvailableFeatures();
	}

	public boolean hasSystemFeature(String name) {
		return mBase.hasSystemFeature(name);
	}

	@RequiresApi(N) public boolean hasSystemFeature(String name, int version) {
		return mBase.hasSystemFeature(name, version);
	}

	public ResolveInfo resolveActivity(Intent intent, int flags) {
		return mBase.resolveActivity(intent, flags);
	}

	public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
		return mBase.queryIntentActivities(intent, flags);
	}

	public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags) {
		return mBase.queryIntentActivityOptions(caller, specifics, intent, flags);
	}

	public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
		return mBase.queryBroadcastReceivers(intent, flags);
	}

	public ResolveInfo resolveService(Intent intent, int flags) {
		return mBase.resolveService(intent, flags);
	}

	public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
		return mBase.queryIntentServices(intent, flags);
	}

	@RequiresApi(KITKAT) public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
		return mBase.queryIntentContentProviders(intent, flags);
	}

	public ProviderInfo resolveContentProvider(String name, int flags) {
		return mBase.resolveContentProvider(name, flags);
	}

	public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
		return mBase.queryContentProviders(processName, uid, flags);
	}

	public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) throws NameNotFoundException {
		return mBase.getInstrumentationInfo(className, flags);
	}

	public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
		return mBase.queryInstrumentation(targetPackage, flags);
	}

	public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
		return mBase.getDrawable(packageName, resid, appInfo);
	}

	public Drawable getActivityIcon(ComponentName activityName) throws NameNotFoundException {
		return mBase.getActivityIcon(activityName);
	}

	public Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
		return mBase.getActivityIcon(intent);
	}

	@RequiresApi(KITKAT_WATCH) public Drawable getActivityBanner(ComponentName activityName) throws NameNotFoundException {
		return mBase.getActivityBanner(activityName);
	}

	@RequiresApi(KITKAT_WATCH) public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
		return mBase.getActivityBanner(intent);
	}

	public Drawable getDefaultActivityIcon() {
		return mBase.getDefaultActivityIcon();
	}

	public Drawable getApplicationIcon(ApplicationInfo info) {
		return mBase.getApplicationIcon(info);
	}

	public Drawable getApplicationIcon(String packageName) throws NameNotFoundException {
		return mBase.getApplicationIcon(packageName);
	}

	@RequiresApi(KITKAT_WATCH) public Drawable getApplicationBanner(ApplicationInfo info) {
		return mBase.getApplicationBanner(info);
	}

	@RequiresApi(KITKAT_WATCH) public Drawable getApplicationBanner(String packageName) throws NameNotFoundException {
		return mBase.getApplicationBanner(packageName);
	}

	public Drawable getActivityLogo(ComponentName activityName) throws NameNotFoundException {
		return mBase.getActivityLogo(activityName);
	}

	public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
		return mBase.getActivityLogo(intent);
	}

	public Drawable getApplicationLogo(ApplicationInfo info) {
		return mBase.getApplicationLogo(info);
	}

	public Drawable getApplicationLogo(String packageName) throws NameNotFoundException {
		return mBase.getApplicationLogo(packageName);
	}

	@RequiresApi(api = LOLLIPOP) public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
		return mBase.getUserBadgedIcon(icon, user);
	}

	@RequiresApi(api = LOLLIPOP) public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity) {
		return mBase.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
	}

	@RequiresApi(api = LOLLIPOP) public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
		return mBase.getUserBadgedLabel(label, user);
	}

	public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo) {
		return mBase.getText(packageName, resid, appInfo);
	}

	public XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) {
		return mBase.getXml(packageName, resid, appInfo);
	}

	public CharSequence getApplicationLabel(ApplicationInfo info) {
		return mBase.getApplicationLabel(info);
	}

	public Resources getResourcesForActivity(ComponentName activityName) throws NameNotFoundException {
		return mBase.getResourcesForActivity(activityName);
	}

	public Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException {
		return mBase.getResourcesForApplication(app);
	}

	public Resources getResourcesForApplication(String appPackageName) throws NameNotFoundException {
		return mBase.getResourcesForApplication(appPackageName);
	}

	public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) {
		return mBase.getPackageArchiveInfo(archiveFilePath, flags);
	}

	@RequiresApi(ICE_CREAM_SANDWICH) public void verifyPendingInstall(int id, int verificationCode) {
		mBase.verifyPendingInstall(id, verificationCode);
	}

	@RequiresApi(JELLY_BEAN_MR1) public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
		mBase.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
	}

	@RequiresApi(HONEYCOMB) public void setInstallerPackageName(String targetPackage, String installerPackageName) {
		mBase.setInstallerPackageName(targetPackage, installerPackageName);
	}

	public String getInstallerPackageName(String packageName) {
		return mBase.getInstallerPackageName(packageName);
	}

	public void addPackageToPreferred(String packageName) {
		mBase.addPackageToPreferred(packageName);
	}

	public void removePackageFromPreferred(String packageName) {
		mBase.removePackageFromPreferred(packageName);
	}

	public List<PackageInfo> getPreferredPackages(int flags) {
		return mBase.getPreferredPackages(flags);
	}

	public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {
		mBase.addPreferredActivity(filter, match, set, activity);
	}

	public void clearPackagePreferredActivities(String packageName) {
		mBase.clearPackagePreferredActivities(packageName);
	}

	public int getPreferredActivities(@NonNull List<IntentFilter> outFilters, @NonNull List<ComponentName> outActivities, String packageName) {
		return mBase.getPreferredActivities(outFilters, outActivities, packageName);
	}

	public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {
		mBase.setComponentEnabledSetting(componentName, newState, flags);
	}

	public int getComponentEnabledSetting(ComponentName componentName) {
		return mBase.getComponentEnabledSetting(componentName);
	}

	public void setApplicationEnabledSetting(String packageName, int newState, int flags) {
		mBase.setApplicationEnabledSetting(packageName, newState, flags);
	}

	public int getApplicationEnabledSetting(String packageName) {
		return mBase.getApplicationEnabledSetting(packageName);
	}

	public boolean isSafeMode() {
		return mBase.isSafeMode();
	}

	@RequiresApi(api = LOLLIPOP) @NonNull public PackageInstaller getPackageInstaller() {
		return mBase.getPackageInstaller();
	}

	PackageManagerWrapper(PackageManager base) { mBase = base; }

	PackageManager mBase;
}
