/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.content.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.RequiresApi;
import android.util.AndroidException;

import java.util.List;
import java.util.Set;

import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O_MR1;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;

/** Stub class for real PackageManager, only contains APIs (including hidden) for regular app (without privileges). */
public abstract class PackageManager {

	public static class NameNotFoundException extends AndroidException {
		public NameNotFoundException() {}
		public NameNotFoundException(final String name) { super(name); }
	}

	public static final int GET_ACTIVITIES              = 0x00000001;
	public static final int GET_RECEIVERS               = 0x00000002;
	public static final int GET_SERVICES                = 0x00000004;
	public static final int GET_PROVIDERS               = 0x00000008;
	public static final int GET_DISABLED_COMPONENTS     = 0x00000200;
	public static final int GET_PERMISSIONS             = 0x00001000;
	public static final int GET_UNINSTALLED_PACKAGES    = 0x00002000;
	public static final int GET_DISABLED_UNTIL_USED_COMPONENTS = 0x00008000;
	public static final int MATCH_ALL					= 0x00020000;
	public static final int PERMISSION_GRANTED = 0;
	public static final int PERMISSION_DENIED = -1;

	public abstract PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException;
	public abstract PackageInfo getPackageInfo(final VersionedPackage versionedPackage, final int flags) throws NameNotFoundException;
	/** @hide */// @RequiresPermission(Manifest.permission.INTERACT_ACROSS_USERS)
	public abstract PackageInfo getPackageInfoAsUser(String packageName, int flags, int userId) throws NameNotFoundException;
	public abstract String[] currentToCanonicalPackageNames(String[] names);
	public abstract String[] canonicalToCurrentPackageNames(String[] names);
	public abstract Intent getLaunchIntentForPackage(String packageName);
	public abstract Intent getLeanbackLaunchIntentForPackage(String packageName);
	public abstract int[] getPackageGids(String packageName) throws NameNotFoundException;
	public abstract int[] getPackageGids(String packageName, int flags) throws NameNotFoundException;
	public abstract int getPackageUid(String packageName, int flags) throws NameNotFoundException;
	/** @hide */
	public abstract int getPackageUidAsUser(String packageName, int userId) throws NameNotFoundException;
	/** @hide */
	public abstract int getPackageUidAsUser(String packageName, int flags, int userId) throws NameNotFoundException;
	public abstract PermissionInfo getPermissionInfo(String name, int flags) throws NameNotFoundException;
	public abstract List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws NameNotFoundException;
	public abstract PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws NameNotFoundException;
	public abstract List<PermissionGroupInfo> getAllPermissionGroups(int flags);
	public abstract ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException;
	/** @hide */
	public abstract ApplicationInfo getApplicationInfoAsUser(String packageName, int flags, int userId) throws NameNotFoundException;
	public abstract ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException;
	public abstract ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException;
	public abstract ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException;
	public abstract ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException;
	public abstract List<PackageInfo> getInstalledPackages(int flags);
	public abstract List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags);
	/** @hide */ //@SystemApi
	public abstract List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId);
	public abstract int checkPermission(String permName, String pkgName);
	public abstract boolean isPermissionRevokedByPolicy(String permName, String pkgName);
	/** @hide */
	public abstract String getPermissionControllerPackageName();
	public abstract boolean addPermission(PermissionInfo info);
	public abstract boolean addPermissionAsync(PermissionInfo info);
	public abstract void removePermission(String name);
	/** @hide */
	public abstract boolean shouldShowRequestPermissionRationale(String permission);
	/** @hide */
	public Intent buildRequestPermissionsIntent(String[] permissions) { throw new UnsupportedOperationException(); }
	public abstract int checkSignatures(String pkg1, String pkg2);
	public abstract int checkSignatures(int uid1, int uid2);
	public abstract String[] getPackagesForUid(int uid);
	public abstract String getNameForUid(int uid);
	/** @hide */
	@RequiresApi(O_MR1) public abstract String[] getNamesForUids(int[] uids);
	/** @hide */
	public abstract int getUidForSharedUser(String sharedUserName) throws NameNotFoundException;
	public abstract List<ApplicationInfo> getInstalledApplications(int flags);
	public abstract boolean isInstantApp();
	public abstract boolean isInstantApp(final String packageName);
	public abstract int getInstantAppCookieMaxBytes();
	public abstract byte[] getInstantAppCookie();
	public abstract void clearInstantAppCookie();
	public abstract void updateInstantAppCookie(final byte[] cookie);
	public abstract String[] getSystemSharedLibraryNames();
	public abstract List<SharedLibraryInfo> getSharedLibraries(final int flags);
	public abstract ChangedPackages getChangedPackages(final int sequenceNumber);
	/** @hide */
	public abstract String getServicesSystemSharedLibraryPackageName();
	/** @hide */
	public abstract String getSharedSystemSharedLibraryPackageName();
	public abstract FeatureInfo[] getSystemAvailableFeatures();
	public abstract boolean hasSystemFeature(String name);
	public abstract boolean hasSystemFeature(String name, int version);
	public abstract ResolveInfo resolveActivity(Intent intent, int flags);
	/** @hide */
	public abstract ResolveInfo resolveActivityAsUser(Intent intent, int flags, int userId);
	public abstract List<ResolveInfo> queryIntentActivities(Intent intent, int flags);
	/** @hide */
	public abstract List<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int flags, int userId);
	public abstract List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags);
	public abstract List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags);
	/** @hide */ //@SystemApi
	public List<ResolveInfo> queryBroadcastReceiversAsUser(Intent intent, int flags, UserHandle userHandle) { throw new UnsupportedOperationException(); }
	/** @hide */
	public abstract List<ResolveInfo> queryBroadcastReceiversAsUser(Intent intent, int flags, int userId);
	/** @hide */ @Deprecated
	public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags, int userId) { throw new UnsupportedOperationException(); }
	public abstract ResolveInfo resolveService(Intent intent, int flags);
	/** @hide */
	@RequiresApi(P) public abstract ResolveInfo resolveServiceAsUser(Intent intent, int flags, int userId);
	public abstract List<ResolveInfo> queryIntentServices(Intent intent, int flags);
	/** @hide */
	public abstract List<ResolveInfo> queryIntentServicesAsUser(Intent intent, int flags, int userId);
	/** @hide */
	public abstract List<ResolveInfo> queryIntentContentProvidersAsUser(Intent intent, int flags, int userId);
	public abstract List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags);
	public abstract ProviderInfo resolveContentProvider(String name, int flags);
	/** @hide */
	public abstract ProviderInfo resolveContentProviderAsUser(String name, int flags, int userId);
	public abstract List<ProviderInfo> queryContentProviders( String processName, int uid, int flags);
	public abstract InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) throws NameNotFoundException;
	public abstract List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags);
	public abstract Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo);
	public abstract Drawable getActivityIcon(ComponentName activityName) throws NameNotFoundException;
	public abstract Drawable getActivityIcon(Intent intent) throws NameNotFoundException;
	public abstract Drawable getActivityBanner(ComponentName activityName) throws NameNotFoundException;
	public abstract Drawable getActivityBanner(Intent intent)
			throws NameNotFoundException;
	public abstract Drawable getDefaultActivityIcon();
	public abstract Drawable getApplicationIcon(ApplicationInfo info);
	public abstract Drawable getApplicationIcon(String packageName)
			throws NameNotFoundException;
	public abstract Drawable getApplicationBanner(ApplicationInfo info);
	public abstract Drawable getApplicationBanner(String packageName)
			throws NameNotFoundException;
	public abstract Drawable getActivityLogo(ComponentName activityName)
			throws NameNotFoundException;
	public abstract Drawable getActivityLogo(Intent intent)
			throws NameNotFoundException;
	public abstract Drawable getApplicationLogo(ApplicationInfo info);
	public abstract Drawable getApplicationLogo(String packageName)
			throws NameNotFoundException;
	/** @hide Only on N ~ N_MR1 */
	@RequiresApi(N) public abstract Drawable getManagedUserBadgedDrawable(Drawable drawable, Rect badgeLocation, int badgeDensity);
	public abstract Drawable getUserBadgedIcon(Drawable icon, UserHandle user);
	public abstract Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity);
	/** @hide */
	public abstract Drawable getUserBadgeForDensity(UserHandle user, int density);
	/** @hide */
	public abstract Drawable getUserBadgeForDensityNoBackground(UserHandle user, int density);
	public abstract CharSequence getUserBadgedLabel(CharSequence label, UserHandle user);
	public abstract CharSequence getText(String packageName, int resid, ApplicationInfo appInfo);
	public abstract XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo);
	public abstract CharSequence getApplicationLabel(ApplicationInfo info);
	public abstract Resources getResourcesForActivity(ComponentName activityName) throws NameNotFoundException;
	public abstract Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException;
	public abstract Resources getResourcesForApplication(String appPackageName) throws NameNotFoundException;
	public abstract Resources getResourcesForApplicationAsUser(String appPackageName, int userId) throws NameNotFoundException;
	public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) { throw new UnsupportedOperationException(); }
	public abstract void verifyPendingInstall(int id, int verificationCode);
	public abstract void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay);
	/** @hide */
	public abstract List<IntentFilter> getAllIntentFilters(String packageName);
	public abstract void setInstallerPackageName(String targetPackage, String installerPackageName);
	public abstract String getInstallerPackageName(String packageName);
	/** @hide */
	public abstract void getPackageSizeInfoAsUser(String packageName, int userId, IPackageStatsObserver observer);
	/** @hide */
	public void getPackageSizeInfo(String packageName, IPackageStatsObserver observer) { throw new UnsupportedOperationException(); }
	@Deprecated
	public abstract void addPackageToPreferred(String packageName);
	@Deprecated
	public abstract void removePackageFromPreferred(String packageName);
	public abstract List<PackageInfo> getPreferredPackages(int flags);
	@Deprecated
	public abstract void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity);
	public abstract void clearPackagePreferredActivities(String packageName);
	public abstract int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName);
	/** @hide */
	public abstract ComponentName getHomeActivities(List<ResolveInfo> outActivities);
	public abstract void setComponentEnabledSetting(ComponentName componentName, int newState, int flags);
	public abstract int getComponentEnabledSetting(ComponentName componentName);
	public abstract void setApplicationEnabledSetting(String packageName, int newState, int flags);
	public abstract int getApplicationEnabledSetting(String packageName);
	/** @hide */
	public abstract void flushPackageRestrictionsAsUser(int userId);
	public abstract boolean isSafeMode();
	public abstract void setApplicationCategoryHint(final String packageName, final int categoryHint);
	public abstract boolean canRequestPackageInstalls();
	/** @hide */
	public abstract KeySet getKeySetByAlias(String packageName, String alias);
	/** @hide */
	public abstract KeySet getSigningKeySet(String packageName);
	/** @hide */
	public abstract boolean isSignedBy(String packageName, KeySet ks);
	/** @hide */
	public abstract boolean isSignedByExactly(String packageName, KeySet ks);
	@RequiresApi(P) public boolean isPackageSuspended() { throw new UnsupportedOperationException("isPackageSuspended not implemented"); }
	@RequiresApi(P) public Bundle getSuspendedPackageAppExtras() { throw new UnsupportedOperationException("getSuspendedPackageAppExtras not implemented"); }
	/** @hide */
	public abstract boolean isPackageSuspendedForUser(String packageName, int userId);

//	/** @hide */
//	public abstract @Nullable VolumeInfo getPackageCurrentVolume(ApplicationInfo app);
//	/** @hide */
//	public abstract List<VolumeInfo> getPackageCandidateVolumes(ApplicationInfo app);
//	/** @hide */
//	public abstract int movePrimaryStorage(VolumeInfo vol);
//	/** @hide */
//	public abstract @Nullable VolumeInfo getPrimaryStorageCurrentVolume();
//	/** @hide */
//	public abstract List<VolumeInfo> getPrimaryStorageCandidateVolumes();
//	/** @hide */
//	public abstract VerifierDeviceIdentity getVerifierDeviceIdentity();
	/** @hide */
	public abstract boolean isUpgrade();
	public abstract PackageInstaller getPackageInstaller();
	/** @hide */
	public abstract Drawable loadItemIcon(PackageItemInfo itemInfo, ApplicationInfo appInfo);
	/** @hide */
	public abstract Drawable loadUnbadgedItemIcon(PackageItemInfo itemInfo, ApplicationInfo appInfo);
	/** @hide */
	public abstract boolean isPackageAvailable(String packageName);
	@RequiresApi(P) public boolean hasSigningCertificate(String packageName, byte[] certificate, int type) { throw new UnsupportedOperationException(); }
	@RequiresApi(P) public boolean hasSigningCertificate(int uid, byte[] certificate, int type) { throw new UnsupportedOperationException(); }
	/** @hide */ @RequiresApi(P) public String getSystemTextClassifierPackageName() { throw new UnsupportedOperationException(); }
	/** @hide */ @RequiresApi(P) public boolean isPackageStateProtected(String packageName, int userId) { throw new UnsupportedOperationException(); }

	@RequiresApi(Q) public boolean addWhitelistedRestrictedPermission(String packageName, String permission, int whitelistFlags) { throw new UnsupportedOperationException(); }
	@RequiresApi(Q) public Set<String> getWhitelistedRestrictedPermissions(String packageName, int whitelistFlag) { throw new UnsupportedOperationException(); }
	@RequiresApi(Q) public boolean removeWhitelistedRestrictedPermission(String packageName, String permission, int whitelistFlags) { throw new UnsupportedOperationException(); }
	@RequiresApi(Q) public List<ModuleInfo> getInstalledModules(int flags) { throw new UnsupportedOperationException(); }
	@RequiresApi(Q) public ModuleInfo getModuleInfo(String packageName, int flags) throws NameNotFoundException { throw new UnsupportedOperationException(); }
	@RequiresApi(Q) public boolean getSyntheticAppDetailsActivityEnabled(String packageName) { throw new UnsupportedOperationException(); }
	@RequiresApi(Q) public boolean isPackageSuspended(String packageName) throws NameNotFoundException { throw new UnsupportedOperationException(); }
	@RequiresApi(Q) public boolean isDeviceUpgrading() { return false; }
}
