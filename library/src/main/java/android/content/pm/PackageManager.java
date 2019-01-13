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

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.annotation.RequiresApi;
import android.support.annotation.RequiresPermission;
import android.util.AndroidException;

import java.util.List;

import static android.os.Build.VERSION_CODES.O_MR1;
import static android.os.Build.VERSION_CODES.P;

/** Stub class for real PackageManager, only contains APIs (including hidden) for regular app (without privileges). */
public abstract class PackageManager {

	public static class NameNotFoundException extends AndroidException {
		public NameNotFoundException() {}
		public NameNotFoundException(final String name) { super(name); }
	}

	/** @hide */ //@SystemApi
	public interface OnPermissionsChangedListener {
		void onPermissionsChanged(int uid);
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
	/** @hide */ //@SystemApi
	public abstract void grantRuntimePermission(String packageName, String permissionName, UserHandle user);
	/** @hide */ //@SystemApi
	public abstract void revokeRuntimePermission(String packageName, String permissionName, UserHandle user);
	/** @hide */ //@SystemApi
	public abstract int getPermissionFlags(String permissionName, String packageName, UserHandle user);
	/** @hide */ //@SystemApi
	public abstract void updatePermissionFlags(String permissionName, String packageName, int flagMask, int flagValues, UserHandle user);
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
//	/** @hide */ @RequiresPermission(Manifest.permission.ACCESS_EPHEMERAL_APPS)
//	public abstract List<EphemeralApplicationInfo> getEphemeralApplications();
	/** @hide */
	public abstract Drawable getEphemeralApplicationIcon(String packageName);
	/** @hide */
	public abstract boolean isEphemeralApplication();
	/** @hide */
	public abstract int getEphemeralCookieMaxSizeBytes();
	/** @hide */
	public abstract byte[] getEphemeralCookie();
	/** @hide */
	public abstract boolean setEphemeralCookie( byte[] cookie);
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
	/** @hide */
	public abstract Drawable getManagedUserBadgedDrawable(Drawable drawable, Rect badgeLocation, int badgeDensity);
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
//	/** @hide */ @Deprecated
//	public abstract void installPackage(Uri packageURI, IPackageInstallObserver observer, int flags, String installerPackageName);
//	/** @hide */ @Deprecated
//	public abstract void installPackage(Uri packageURI, PackageInstallObserver observer, int flags, String installerPackageName);
	/** @hide */
	public abstract int installExistingPackage(String packageName) throws NameNotFoundException;
	/** @hide */ //@RequiresPermission(anyOf = { Manifest.permission.INSTALL_PACKAGES, Manifest.permission.INTERACT_ACROSS_USERS_FULL })
	public abstract int installExistingPackageAsUser(String packageName, int userId) throws NameNotFoundException;
	public abstract void verifyPendingInstall(int id, int verificationCode);
	public abstract void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay);
	/** @hide */ //@SystemApi
	public abstract void verifyIntentFilter(int verificationId, int verificationCode, List<String> failedDomains);
	/** @hide */
	public abstract int getIntentVerificationStatusAsUser(String packageName, int userId);
	/** @hide */
	public abstract boolean updateIntentVerificationStatusAsUser(String packageName, int status, int userId);
//	/** @hide */
//	public abstract List<IntentFilterVerificationInfo> getIntentFilterVerifications(String packageName);
	/** @hide */
	public abstract List<IntentFilter> getAllIntentFilters(String packageName);
	/** @hide */ //@TestApi
	public abstract String getDefaultBrowserPackageNameAsUser(int userId);
	/** @hide */
	public abstract boolean setDefaultBrowserPackageNameAsUser(String packageName, int userId);
	public abstract void setInstallerPackageName(String targetPackage, String installerPackageName);
	public abstract String getInstallerPackageName(String packageName);
	/** @hide */
	public abstract void deletePackage(String packageName, IPackageDeleteObserver observer, int flags);
	/** @hide */ @RequiresPermission(anyOf = { Manifest.permission.DELETE_PACKAGES/*, Manifest.permission.INTERACT_ACROSS_USERS_FULL */})
	public abstract void deletePackageAsUser(String packageName, IPackageDeleteObserver observer, int flags, int userId);
	/** @hide */
	public abstract void clearApplicationUserData(String packageName, IPackageDataObserver observer);
	/** @hide */
	public abstract void deleteApplicationCacheFiles(String packageName, IPackageDataObserver observer);
	/** @hide */
	public abstract void deleteApplicationCacheFilesAsUser(String packageName, int userId, IPackageDataObserver observer);
	/** @hide */
	public void freeStorageAndNotify(long freeStorageSize, IPackageDataObserver observer) { throw new UnsupportedOperationException(); }
	/** @hide */
	public abstract void freeStorageAndNotify(String volumeUuid, long freeStorageSize, IPackageDataObserver observer);
	/** @hide */
	public void freeStorage(long freeStorageSize, IntentSender pi) { throw new UnsupportedOperationException(); }
	/** @hide */
	public abstract void freeStorage(String volumeUuid, long freeStorageSize, IntentSender pi);
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
	/** @hide */
	public void addPreferredActivityAsUser(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
		throw new RuntimeException("Not implemented. Must override in a subclass.");
	}
	/** @hide */ @Deprecated
	public abstract void replacePreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity);
	/** @hide */ @Deprecated
	public void replacePreferredActivityAsUser(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
		throw new RuntimeException("Not implemented. Must override in a subclass.");
	}
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
	/** @hide */
	public abstract boolean setApplicationHiddenSettingAsUser(String packageName, boolean hidden, UserHandle userHandle);
	/** @hide */
	public abstract boolean getApplicationHiddenSettingAsUser(String packageName, UserHandle userHandle);
	public abstract boolean isSafeMode();
	public abstract void setApplicationCategoryHint(final String packageName, final int categoryHint);
	public abstract boolean canRequestPackageInstalls();
	/** @hide */ //@SystemApi @RequiresPermission(Manifest.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS)
	public abstract void addOnPermissionsChangeListener(OnPermissionsChangedListener listener);
	/** @hide */ //@SystemApi
	public abstract void removeOnPermissionsChangeListener(OnPermissionsChangedListener listener);
	/** @hide */
	public abstract KeySet getKeySetByAlias(String packageName, String alias);
	/** @hide */
	public abstract KeySet getSigningKeySet(String packageName);
	/** @hide */
	public abstract boolean isSignedBy(String packageName, KeySet ks);
	/** @hide */
	public abstract boolean isSignedByExactly(String packageName, KeySet ks);
	/** @hide */
	public abstract String[] setPackagesSuspendedAsUser(String[] packageNames, boolean suspended, int userId);
	@RequiresApi(P) public boolean isPackageSuspended() { throw new UnsupportedOperationException("isPackageSuspended not implemented"); }
	@RequiresApi(P) public Bundle getSuspendedPackageAppExtras() { throw new UnsupportedOperationException("getSuspendedPackageAppExtras not implemented"); }
	/** @hide */
	public abstract boolean isPackageSuspendedForUser(String packageName, int userId);
	/** @hide */
	public static boolean isMoveStatusFinished(int status) { throw new UnsupportedOperationException(); }
	/** @hide */
	public static abstract class MoveCallback {
		public void onCreated(int moveId, Bundle extras) {}
		public abstract void onStatusChanged(int moveId, int status, long estMillis);
	}
	/** @hide */
	public abstract int getMoveStatus(int moveId);
	/** @hide */
	public abstract void registerMoveCallback(MoveCallback callback, Handler handler);
	/** @hide */
	public abstract void unregisterMoveCallback(MoveCallback callback);
//	/** @hide */
//	public abstract int movePackage(String packageName, VolumeInfo vol);
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
	public abstract void addCrossProfileIntentFilter(IntentFilter filter, int sourceUserId, int targetUserId, int flags);
	/** @hide */
	public abstract void clearCrossProfileIntentFilters(int sourceUserId);
	/** @hide */
	public abstract Drawable loadItemIcon(PackageItemInfo itemInfo, ApplicationInfo appInfo);
	/** @hide */
	public abstract Drawable loadUnbadgedItemIcon(PackageItemInfo itemInfo, ApplicationInfo appInfo);
	/** @hide */
	public abstract boolean isPackageAvailable(String packageName);
	/** @hide */
	public static String installStatusToString(int status, String msg) { throw new UnsupportedOperationException(); }
	/** @hide */
	public static String installStatusToString(int status) { throw new UnsupportedOperationException(); }
	/** @hide */
	public static int installStatusToPublicStatus(int status) { throw new UnsupportedOperationException(); }
	/** @hide */
	public static String deleteStatusToString(int status, String msg) { throw new UnsupportedOperationException(); }
	/** @hide */
	public static String deleteStatusToString(int status) { throw new UnsupportedOperationException(); }
	/** @hide */
	public static int deleteStatusToPublicStatus(int status) { throw new UnsupportedOperationException(); }
	/** @hide */
	public static String permissionFlagToString(int flag) { throw new UnsupportedOperationException(); }
	@RequiresApi(P) public boolean hasSigningCertificate(String packageName, byte[] certificate, int type) { throw new UnsupportedOperationException(); }
	@RequiresApi(P) public boolean hasSigningCertificate(int uid, byte[] certificate, int type) { throw new UnsupportedOperationException(); }
	/** @hide */ @RequiresApi(P) public String getSystemTextClassifierPackageName() { throw new UnsupportedOperationException(); }
	/** @hide */ @RequiresApi(P) public boolean isPackageStateProtected(String packageName, int userId) { throw new UnsupportedOperationException(); }
}
