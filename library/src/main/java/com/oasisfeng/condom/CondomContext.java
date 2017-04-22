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

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.support.annotation.CheckResult;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.oasisfeng.condom.util.Lazy;

import java.util.Iterator;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;

/**
 * The condom-style {@link ContextWrapper} to prevent unwanted behaviors going through.
 *
 * Created by Oasis on 2017/3/25.
 */
@ParametersAreNonnullByDefault @Keep
public class CondomContext extends ContextWrapper {

	public static @CheckResult CondomContext wrap(final Context base, final @Nullable String tag) {
		return wrap(base, tag, new CondomOptions());
	}

	/**
	 * This is the very first (probably only) API you need to wrap the naked {@link Context} under protection of <code>CondomContext</code>
	 *
	 * @param base	the original context used before <code>CondomContext</code> is introduced.
	 * @param tag	the optional tag to distinguish between multiple instances of <code>CondomContext</code> used parallel.
	 */
	public static @CheckResult CondomContext wrap(final Context base, final @Nullable String tag, final CondomOptions options) {
		if (base instanceof CondomContext) return (CondomContext) base;
		final Context app_context = base.getApplicationContext();
		final CondomCore condom = new CondomCore(base, options);
		if (app_context instanceof Application) {	// The application context is indeed an Application, this should be preserved semantically.
			final Application app = (Application) app_context;
			final CondomApplication condom_app = new CondomApplication(condom, app, tag);
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
	@Deprecated public CondomContext preventServiceInBackgroundPackages(final boolean prevent_or_not) { mCondom.mExcludeBackgroundServices = prevent_or_not; return this; }

	/* ****** Hooked Context APIs ****** */

	@Override public boolean bindService(final Intent intent, final ServiceConnection conn, final int flags) {
		final boolean result = mCondom.proceed(OutboundType.BIND_SERVICE, intent, Boolean.FALSE, new CondomCore.WrappedValueProcedure<Boolean>() {
			@Override public Boolean proceed(final Intent intent) {
				return CondomContext.super.bindService(intent, conn, flags);
			}
		});
		final String target_pkg;
		if (result && (target_pkg = CondomCore.getTargetPackage(intent)) != null && ! getPackageName().equals(target_pkg))		// 3rd-party service
			mCondom.log(TAG, CondomCore.CondomEvent.BIND_PASS, target_pkg, intent.toString());
		return result;
	}

	@Override public ComponentName startService(final Intent intent) {
		final ComponentName component = mCondom.proceed(OutboundType.START_SERVICE, intent, null, new CondomCore.WrappedValueProcedure<ComponentName>() {
			@Override public ComponentName proceed(final Intent intent) {
				return CondomContext.super.startService(intent);
			}
		});
		if (component != null && ! getPackageName().equals(component.getPackageName()))		// 3rd-party service
			mCondom.log(TAG, CondomCore.CondomEvent.START_PASS, component.getPackageName(), intent.toString());
		return component;
	}

	@Override public void sendBroadcast(final Intent intent) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendBroadcast(intent);
		}});
	}

	@Override public void sendBroadcast(final Intent intent, final String receiverPermission) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendBroadcast(intent, receiverPermission);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendBroadcastAsUser(intent, user);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendBroadcastAsUser(intent, user, receiverPermission);
		}});
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendOrderedBroadcast(intent, receiverPermission);
		}});
	}

	@Override public void sendOrderedBroadcast(final Intent intent, final String receiverPermission, final BroadcastReceiver resultReceiver,
											   final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission, final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}});
	}

	@Override public void sendStickyBroadcast(final Intent intent) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendStickyBroadcast(intent);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendStickyBroadcastAsUser(final Intent intent, final UserHandle user) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendStickyBroadcastAsUser(intent, user);
		}});
	}

	@Override public void sendStickyOrderedBroadcast(final Intent intent, final BroadcastReceiver resultReceiver, final Handler scheduler,
													 final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}});
	}

	@RequiresApi(JELLY_BEAN_MR1) @Override public void sendStickyOrderedBroadcastAsUser(final Intent intent, final UserHandle user, final BroadcastReceiver resultReceiver, final Handler scheduler, final int initialCode, final String initialData, final Bundle initialExtras) {
		mCondom.proceedBroadcast(intent, new CondomCore.WrappedProcedure() { @Override public void run(final Intent intent) {
			CondomContext.super.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}});
	}

	@Override public ContentResolver getContentResolver() { return mContentResolver.get(); }
	@Override public PackageManager getPackageManager() { return mPackageManager.get(); }
	@Override public Context getApplicationContext() { return mApplicationContext; }
	@Override public Context getBaseContext() {
		mCondom.logConcern(TAG, "getBaseContext");
		return mBaseContext.get();
	}

	/* ********************************* */

	private CondomContext(final CondomCore condom, final @Nullable Context app_context, final @Nullable String tag) {
		super(condom.mBase);
		final Context base = condom.mBase;
		mCondom = condom;
		mApplicationContext = app_context != null ? app_context : this;
		mBaseContext = new Lazy<Context>() { @Override protected Context create() {
			return new PseudoContextImpl(CondomContext.this);
		}};
		mPackageManager = new Lazy<PackageManager>() { @Override protected PackageManager create() {
			return new CondomPackageManager(base.getPackageManager());
		}};
		mContentResolver = new Lazy<ContentResolver>() { @Override protected ContentResolver create() {
			return new CondomContentResolver(base, base.getContentResolver());
		}};
		TAG = CondomCore.buildTag("Condom", "Condom.", tag);
	}

	CondomCore mCondom;
	private final Context mApplicationContext;
	private final Lazy<Context> mBaseContext;
	private final Lazy<PackageManager> mPackageManager;
	private final Lazy<ContentResolver> mContentResolver;
	final String TAG;

	/* ****** Internal branch functionality ****** */

	class CondomPackageManager extends PackageManagerWrapper {

		@Override public List<ResolveInfo> queryBroadcastReceivers(final Intent intent, final int flags) {
			return mCondom.proceedQuery(OutboundType.QUERY_RECEIVERS, intent, new CondomCore.WrappedValueProcedure<List<ResolveInfo>>() { @Override public List<ResolveInfo> proceed(final Intent intent) {
				return CondomPackageManager.super.queryBroadcastReceivers(intent, flags);
			}});
		}

		@Override public List<ResolveInfo> queryIntentServices(final Intent intent, final int flags) {
			return mCondom.proceedQuery(OutboundType.QUERY_SERVICES, intent, new CondomCore.WrappedValueProcedure<List<ResolveInfo>>() { @Override public List<ResolveInfo> proceed(final Intent intent) {
				final List<ResolveInfo> result = CondomPackageManager.super.queryIntentServices(intent, flags);
				if (! mCondom.mExcludeBackgroundServices || result.isEmpty()) return result;

				final int my_uid = Process.myUid(); CondomCore.BackgroundUidFilter bg_uid_filter = null;
				final Iterator<ResolveInfo> result_iterator = result.iterator();
				while (result_iterator.hasNext()) {
					final ResolveInfo candidate = result_iterator.next();
					final ApplicationInfo app_info = candidate.serviceInfo.applicationInfo;
					if (app_info.uid == my_uid) continue;
					if (bg_uid_filter == null) bg_uid_filter = mCondom.new BackgroundUidFilter();
					if (! bg_uid_filter.isUidNotBackground(app_info.uid)) {
						if (! mCondom.mDryRun) result_iterator.remove();
						mCondom.log(TAG, CondomCore.CondomEvent.FILTER_BG_SERVICE, app_info.packageName, intent.toString());
					}
				}
				return result;
			}});
		}

		@Override public ResolveInfo resolveService(final Intent intent, final int flags) {
			// Intent flags could only filter background receivers, we have to deal with services by ourselves.
			return mCondom.proceed(OutboundType.QUERY_SERVICES, intent, null, new CondomCore.WrappedValueProcedure<ResolveInfo>() { @Override public ResolveInfo proceed(final Intent intent) {
				if (! mCondom.mExcludeBackgroundServices) return CondomPackageManager.super.resolveService(intent, flags);

				final List<ResolveInfo> candidates = CondomPackageManager.super.queryIntentServices(intent, flags);
				final ResolveInfo resolve = mCondom.getFirstNonBackground(intent, candidates, TAG);
				if (mCondom.mDryRun) return candidates == null || candidates.isEmpty() ? null : candidates.get(0);
				return resolve;
			}});
		}

		@Override public ProviderInfo resolveContentProvider(final String name, final int flags) {
			final ProviderInfo provider = super.resolveContentProvider(name, flags);
			if (! mCondom.shouldAllowProvider(provider) && ! mCondom.mDryRun) return null;
			return provider;
		}

		@Override public List<PackageInfo> getInstalledPackages(final int flags) {
			mCondom.logConcern(TAG, "PackageManager.getInstalledPackages");
			return super.getInstalledPackages(flags);
		}

		@Override public List<ApplicationInfo> getInstalledApplications(final int flags) {
			mCondom.logConcern(TAG, "PackageManager.getInstalledApplications");
			return super.getInstalledApplications(flags);
		}

		CondomPackageManager(final PackageManager base) { super(base); }
	}

	private class CondomContentResolver extends ContentResolverWrapper {

		@Override public IContentProvider acquireUnstableProvider(final Context c, final String name) {
			if (! shouldAllowProvider(c, name) && ! mCondom.mDryRun) return null;
			return super.acquireUnstableProvider(c, name);
		}

		@Override public IContentProvider acquireProvider(final Context c, final String name) {
			if (! shouldAllowProvider(c, name) && ! mCondom.mDryRun) return null;
			return super.acquireProvider(c, name);
		}

		private boolean shouldAllowProvider(final Context c, final String name) {
			return CondomContext.this.mCondom.shouldAllowProvider(c.getPackageManager().resolveContentProvider(name, 0));
		}

		CondomContentResolver(final Context context, final ContentResolver base) { super(context, base); }
	}

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

		// The actual context returned may not be semantically consistent. We'll keep an eye for it in the wild.
		@Override public Context getBaseContext() {
			mCondom.logConcern(TAG, "Application.getBaseContext");
			return super.getBaseContext();
		}

		@Override public void attachBaseContext(final Context base) { super.attachBaseContext(base); }

		CondomApplication(final CondomCore condom, final Application app, final @Nullable String tag) {
			mCondom = condom;
			mApplication = app;
			TAG = CondomCore.buildTag("CondomApp", "CondomApp.", tag);
		}

		private final CondomCore mCondom;
		private final Application mApplication;
		private final String TAG;
	}

	// This should act as what ContextImpl stands for in the naked Context structure.
	private static class PseudoContextImpl extends PseudoContextWrapper {
		public PseudoContextImpl(final CondomContext condom) { super(condom); }
	}
}
