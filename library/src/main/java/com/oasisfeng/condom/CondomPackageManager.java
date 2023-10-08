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

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Condom wrapper of {@link PackageManager}
 *
 * Created by Oasis on 2018/1/6.
 */
class CondomPackageManager extends PackageManagerWrapper {

	@Override public @NonNull List<ResolveInfo> queryBroadcastReceivers(final Intent intent, final int flags) {
		return mCondom.proceedQuery(OutboundType.QUERY_RECEIVERS, intent, new CondomCore.WrappedValueProcedure<List<ResolveInfo>>() {
			@Override public List<ResolveInfo> proceed() {
				return CondomPackageManager.super.queryBroadcastReceivers(intent, flags);
			}
		}, CondomCore.RECEIVER_PACKAGE_GETTER);
	}

	@Override public @NonNull List<ResolveInfo> queryIntentServices(final Intent intent, final int flags) {
		final int original_intent_flags = intent.getFlags();
		return mCondom.proceedQuery(OutboundType.QUERY_SERVICES, intent, new CondomCore.WrappedValueProcedure<List<ResolveInfo>>() {
			@Override public List<ResolveInfo> proceed() {
				final List<ResolveInfo> result = CondomPackageManager.super.queryIntentServices(intent, flags);
				mCondom.filterCandidates(OutboundType.QUERY_SERVICES, intent.setFlags(original_intent_flags), result, TAG, true);
				return result;
			}
		}, CondomCore.SERVICE_PACKAGE_GETTER);
	}

	@Override public ResolveInfo resolveService(final Intent intent, final int flags) {
		final int original_intent_flags = intent.getFlags();
		// Intent flags could only filter background receivers, we have to deal with services by ourselves.
		return mCondom.proceed(OutboundType.QUERY_SERVICES, intent, null, new CondomCore.WrappedValueProcedure<ResolveInfo>() {
			@Override public ResolveInfo proceed() {
				if (! mCondom.mExcludeBackgroundServices && mCondom.mOutboundJudge == null)
					return CondomPackageManager.super.resolveService(intent, flags);    // Shortcut for pass-through

				final List<ResolveInfo> candidates = CondomPackageManager.super.queryIntentServices(intent, flags);
				final Intent original_intent = intent.setFlags(original_intent_flags);    // Restore the intent flags early before getFirstMatch().
				return mCondom.filterCandidates(OutboundType.QUERY_SERVICES, original_intent, candidates, TAG, false);
			}
		});
	}

	@Override public ProviderInfo resolveContentProvider(final String name, final int flags) {
		final ProviderInfo provider = super.resolveContentProvider(name, flags);
		if (! mCondom.shouldAllowProvider(provider)) return null;
		return provider;
	}

	@Override public @NonNull List<PackageInfo> getInstalledPackages(final int flags) {
		mCondom.logConcern(TAG, "PackageManager.getInstalledPackages");
		return super.getInstalledPackages(flags);
	}

	@Override public @NonNull List<ApplicationInfo> getInstalledApplications(final int flags) {
		mCondom.logConcern(TAG, "PackageManager.getInstalledApplications");
		return super.getInstalledApplications(flags);
	}

	@Override public @NonNull ApplicationInfo getApplicationInfo(final String pkg, final int flags) throws NameNotFoundException {
		return mCondom.proceed(OutboundType.GET_APPLICATION_INFO, pkg, null, new CondomCore.WrappedValueProcedureThrows<ApplicationInfo, NameNotFoundException>() {
			@Override public ApplicationInfo proceed() throws NameNotFoundException {
				return CondomPackageManager.super.getApplicationInfo(pkg, flags);
			}
		});
	}

	@Override public PackageInfo getPackageInfo(final String pkg, final int flags) throws NameNotFoundException {
		final PackageInfo info = mCondom.proceed(OutboundType.GET_PACKAGE_INFO, pkg, null, new CondomCore.WrappedValueProcedureThrows<PackageInfo, NameNotFoundException>() {
			@Override public PackageInfo proceed() throws NameNotFoundException {
				return CondomPackageManager.super.getPackageInfo(pkg, flags);
			}
		});
		if (info == null) throw new NameNotFoundException(pkg);
		if ((flags & PackageManager.GET_PERMISSIONS) != 0 && ! mCondom.getSpoofPermissions().isEmpty() && mCondom.getPackageName().equals(pkg)) {
			final List<String> requested_permissions = info.requestedPermissions == null ? new ArrayList<String>()
					: new ArrayList<>(Arrays.asList(info.requestedPermissions));
			final List<String> missing_permissions = new ArrayList<>(mCondom.getSpoofPermissions());
			missing_permissions.removeAll(requested_permissions);
			if (! missing_permissions.isEmpty()) {
				requested_permissions.addAll(missing_permissions);
				info.requestedPermissions = requested_permissions.toArray(new String[requested_permissions.size()]);
			}    // Even if all permissions to spoof are already requested, the permission granted state still requires amending.

			if (SDK_INT >= JELLY_BEAN) {
				final int[] req_permissions_flags = info.requestedPermissionsFlags == null ? new int[requested_permissions.size()]
						: Arrays.copyOf(info.requestedPermissionsFlags, requested_permissions.size());
				for (int i = 0; i < info.requestedPermissions.length; i++)
					if (mCondom.shouldSpoofPermission(info.requestedPermissions[i]))
						req_permissions_flags[i] = PackageInfo.REQUESTED_PERMISSION_GRANTED;
				info.requestedPermissionsFlags = req_permissions_flags;
			}
		}
		return info;
	}

	@Nullable @Override public String[] getPackagesForUid(final int uid) {
		final List<String> result = mCondom.proceedQuery(OutboundType.QUERY_PACKAGES, null, new CondomCore.WrappedValueProcedure<List<String>>() {
			@Override public @Nullable List<String> proceed() {
				final String[] result = CondomPackageManager.super.getPackagesForUid(uid);
				return result != null ? Arrays.asList(result) : null;
			}
		}, IDENTITY_FUNCTION);
		return result != null && ! result.isEmpty() ? result.toArray(new String[0]) : null;
	}

	@Override public int checkPermission(final String permName, final String pkgName) {
		return mCondom.proceed(OutboundType.CHECK_PERMISSION, pkgName, PERMISSION_DENIED, new CondomCore.WrappedValueProcedure<Integer>() {
			@Override public Integer proceed() {
				return CondomPackageManager.super.checkPermission(permName, pkgName);
			}
		});
	}

	@RequiresApi(LOLLIPOP) public @NonNull PackageInstaller getPackageInstaller() {
		throw new UnsupportedOperationException("PackageManager.getPackageInstaller() is not yet supported by Project Condom. " +
				"If it causes trouble, please file an issue on GitHub.");
	}

	CondomPackageManager(final CondomCore condom, final PackageManager base, final String tag) {
		super(base);
		mCondom = condom;
		TAG = tag;
	}

	private final CondomCore mCondom;
	private final String TAG;
	private static final CondomCore.Function<String,String> IDENTITY_FUNCTION = new CondomCore.Function<String, String>() {
		@Override public String apply(final String s) { return s; }
	};
}
