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

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Assume;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import static android.content.Intent.FLAG_RECEIVER_REGISTERED_ONLY;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@ParametersAreNonnullByDefault
public class CondomContextBlockingTest {

	@Test public void testSelfTargeted() {
		final TestContext context = new TestContext();
		final CondomContext condom = CondomContext.wrap(context, TAG), dry_condom = CondomContext.wrap(context, TAG, new CondomOptions().setDryRun(true));

		// Self-targeting test
		final String self_pkg = condom.getPackageName();
		final Intent[] self_targeted_intents = new Intent[] {
				intent().setPackage(self_pkg),
				intent().setComponent(new ComponentName(self_pkg, "X"))
		};
		for (final Context context2test : new Context[] {condom, condom.getApplicationContext(), dry_condom, dry_condom.getApplicationContext()})
			with(self_targeted_intents, allBroadcastAndServiceApis(context2test), context.EXPECT_BASE_CALLED, context.expectFlags(0));
	}

	@Test public void testPreventNone() {
		final TestContext context = new TestContext();
		final CondomOptions options = new CondomOptions().preventServiceInBackgroundPackages(false).preventBroadcastToBackgroundPackages(false);
		final CondomContext condom = CondomContext.wrap(context, TAG, options), dry_condom = CondomContext.wrap(context, TAG, options.setDryRun(true));
		//noinspection deprecation, intentional test for deprecated method
		condom.preventWakingUpStoppedPackages(false);
		//noinspection deprecation
		dry_condom.preventWakingUpStoppedPackages(false);

		for (final Context context2test : new Context[] {condom, condom.getApplicationContext(), dry_condom, dry_condom.getApplicationContext()})
			with(ALL_SORT_OF_INTENTS, allBroadcastAndServiceApis(context2test), context.EXPECT_BASE_CALLED, context.expectFlags(0));
	}

	@Test public void testPreventWakingUpStoppedPackages_IncludingDryRun() {
		final Intent[] intents_with_inc_stop = ALL_SORT_OF_INTENTS.clone();
		for (int i = 0; i < intents_with_inc_stop.length; i++)
			intents_with_inc_stop[i] = new Intent(intents_with_inc_stop[i]).addFlags(FLAG_INCLUDE_STOPPED_PACKAGES);
		final TestContext context = new TestContext();
		final CondomOptions options = new CondomOptions().preventBroadcastToBackgroundPackages(false).preventServiceInBackgroundPackages(false);
		final CondomContext condom = CondomContext.wrap(context, TAG, options), dry_condom = CondomContext.wrap(context, TAG, options.setDryRun(true));

		for (final Context context2test : new Context[] {condom, condom.getApplicationContext()})
			with(intents_with_inc_stop, allBroadcastAndServiceApis(context2test), context.EXPECT_BASE_CALLED, context.expectFlags(FLAG_EXCLUDE_STOPPED_PACKAGES));
		for (final Context context2test : new Context[] {dry_condom, dry_condom.getApplicationContext()})
			with(intents_with_inc_stop, allBroadcastAndServiceApis(context2test), context.EXPECT_BASE_CALLED, context.expectFlags(FLAG_INCLUDE_STOPPED_PACKAGES));
	}

	@Test public void testPreventBroadcastToBackgroundPackages() {
		final TestContext context = new TestContext();
		final CondomOptions options = new CondomOptions().preventBroadcastToBackgroundPackages(true);
		final CondomContext condom = CondomContext.wrap(context, TAG, options), dry_condom = CondomContext.wrap(context, TAG, options.setDryRun(true));
		final int extra_flag = SDK_INT >= N ? CondomCore.FLAG_RECEIVER_EXCLUDE_BACKGROUND : FLAG_RECEIVER_REGISTERED_ONLY;
		for (final Context context2test : new Context[] {condom, condom.getApplicationContext()})
			with(ALL_SORT_OF_INTENTS, allBroadcastApis(context2test), context.EXPECT_BASE_CALLED, context.expectFlags(FLAG_EXCLUDE_STOPPED_PACKAGES | extra_flag));
		for (final Context context2test : new Context[] {dry_condom, dry_condom.getApplicationContext()})
			with(ALL_SORT_OF_INTENTS, allBroadcastApis(context2test), context.EXPECT_BASE_CALLED, context.expectFlags(0));
	}

	@Test public void testPreventServiceInBackgroundPackages() {
		Assume.assumeTrue(SDK_INT < O);

		final TestContext context = new TestContext();
		context.mTestingBackgroundUid = true;
		final CondomOptions options = new CondomOptions().preventServiceInBackgroundPackages(true).preventBroadcastToBackgroundPackages(false);
		final CondomContext condom = CondomContext.wrap(context, TAG, options), dry_condom = CondomContext.wrap(context, TAG, options.setDryRun(true));
		for (final Context context2test : new Context[] {condom, condom.getApplicationContext()}) {
			final List<ResolveInfo> services = context2test.getPackageManager().queryIntentServices(intent(), 0);
			assertEquals(3, services.size());
			context.assertBaseCalled();
			final ResolveInfo resolve = context2test.getPackageManager().resolveService(intent(), 0);
			assertEquals("non.bg.service", resolve.serviceInfo.packageName);
			context.assertBaseCalled();
		}
		for (final Context context2test : new Context[] {dry_condom, dry_condom.getApplicationContext()}) {
			final List<ResolveInfo> services = context2test.getPackageManager().queryIntentServices(intent(), 0);
			assertEquals(4, services.size());
			context.assertBaseCalled();
			assertEquals(7777777, context2test.getPackageManager().resolveService(intent(), 0).serviceInfo.applicationInfo.uid);
			context.assertBaseCalled();
		}
	}

	@Test public void testContentProviderOutboundJudge() {
		final TestContext context = new TestContext();
		final CondomOptions options = new CondomOptions().setOutboundJudge((type, intent, target_pkg) -> {
			final String settings_pkg = ApplicationProvider.getApplicationContext().getPackageManager().resolveContentProvider(Settings.System.CONTENT_URI.getAuthority(), 0).packageName;
			return ! settings_pkg.equals(target_pkg);
		});
		final CondomContext condom = CondomContext.wrap(context, TAG, options), dry_condom = CondomContext.wrap(context, TAG, options.setDryRun(true));

		for (final Context context2test : new Context[] {condom, condom.getApplicationContext()}) {
			assertNull(context2test.getPackageManager().resolveContentProvider(Settings.AUTHORITY, 0));
			assertNull(context2test.getContentResolver().acquireContentProviderClient(Settings.System.CONTENT_URI));
		}
		for (final Context context2test : new Context[] {dry_condom, dry_condom.getApplicationContext()}) {
			assertNotNull(context2test.getPackageManager().resolveContentProvider(Settings.AUTHORITY, 0));
			assertNotNull(context2test.getContentResolver().acquireContentProviderClient(Settings.System.CONTENT_URI));
		}
	}

	@Test public void testContentProvider() {
		final TestContext context = new TestContext();
		final CondomContext condom = CondomContext.wrap(context, TAG), dry_condom = CondomContext.wrap(context, TAG, new CondomOptions().setDryRun(true));

		// Regular provider access
		final String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		assertNotNull(android_id);
		for (final Context context2test : new Context[] {condom, condom.getApplicationContext(), dry_condom, dry_condom.getApplicationContext()}) {
			final String condom_android_id = Settings.Secure.getString(context2test.getContentResolver(), Settings.Secure.ANDROID_ID);
			assertEquals(android_id, condom_android_id);
		}

		context.mTestingStoppedProvider = true;
		for (final Context context2test : new Context[] {condom, condom.getApplicationContext()}) {
			// Prevent stopped packages,
			assertNull(context2test.getPackageManager().resolveContentProvider(TEST_AUTHORITY, 0));
			assertNull(context2test.getContentResolver().acquireContentProviderClient(TEST_CONTENT_URI));
			// Providers in system package should not be blocked.
			assertNotNull(context2test.getPackageManager().resolveContentProvider(Settings.AUTHORITY, 0));
			assertNotNull(context2test.getContentResolver().acquireContentProviderClient(Settings.System.CONTENT_URI));
		}
		for (final Context context2test : new Context[] {dry_condom, dry_condom.getApplicationContext()}) {
			assertNotNull(context2test.getPackageManager().resolveContentProvider(TEST_AUTHORITY, 0));
			assertNotNull(context2test.getContentResolver().acquireContentProviderClient(TEST_CONTENT_URI));
		}

		context.mTestingStoppedProvider = false;
	}
	private static final String TEST_AUTHORITY = "com.oasisfeng.condom.test";
	private static final Uri TEST_CONTENT_URI = Uri.parse("content://" + TEST_AUTHORITY + "/");

	public static class TestProvider extends ContentProvider {
		@Override public boolean onCreate() { return true; }
		@Nullable @Override public Cursor query(@NonNull final Uri uri, @Nullable final String[] strings, @Nullable final String s, @Nullable final String[] strings1, @Nullable final String s1) { return null; }
		@Nullable @Override public String getType(@NonNull final Uri uri) { return null; }
		@Nullable @Override public Uri insert(@NonNull final Uri uri, @Nullable final ContentValues contentValues) { return null; }
		@Override public int delete(@NonNull final Uri uri, @Nullable final String s, @Nullable final String[] strings) { return 0; }
		@Override public int update(@NonNull final Uri uri, @Nullable final ContentValues contentValues, @Nullable final String s, @Nullable final String[] strings) { return 0; }
	}

	@Test public void testOutboundJudge() {
		final TestContext context = new TestContext();
		final CondomOptions options = new CondomOptions().setOutboundJudge(new OutboundJudge() {
			@Override public boolean shouldAllow(final OutboundType type, final @Nullable Intent intent, final String target_pkg) {
				mNumOutboundJudgeCalled.incrementAndGet();
				return ! DISALLOWED_PACKAGE.equals(target_pkg);
			}
		});
		final CondomContext condom = CondomContext.wrap(context, TAG, options), dry_condom = CondomContext.wrap(context, TAG, options.setDryRun(true));

		final Runnable EXPECT_OUTBOUND_JUDGE_REFUSAL = new Runnable() { @Override public void run() {
			context.assertBaseNotCalled();
			assertOutboundJudgeCalled(1);
		}};
		final Runnable EXPECT_OUTBOUND_JUDGE_PASS = new Runnable() { @Override public void run() {
			context.assertBaseCalled();
			assertOutboundJudgeCalled(1);
		}};
		for (final Context context2test : new Context[] {condom, condom.getApplicationContext()})
			with(DISALLOWED_INTENTS, allBroadcastApis(context2test), EXPECT_OUTBOUND_JUDGE_REFUSAL);
		for (final Context context2test : new Context[] {dry_condom, dry_condom.getApplicationContext()})
			with(DISALLOWED_INTENTS, allBroadcastApis(context2test), EXPECT_OUTBOUND_JUDGE_PASS);
		for (final Context context2test : new Context[] {condom, condom.getApplicationContext(), dry_condom, dry_condom.getApplicationContext()})
			with(ALLOWED_INTENTS, allBroadcastApis(context2test), EXPECT_OUTBOUND_JUDGE_PASS);

		final PackageManager pm = condom.getPackageManager(), dry_pm = dry_condom.getPackageManager();
		assertNull(pm.resolveService(intent().setPackage(DISALLOWED_PACKAGE), 0));
		context.assertBaseNotCalled();
		assertOutboundJudgeCalled(1);
		assertNotNull(dry_pm.resolveService(intent().setPackage(DISALLOWED_PACKAGE), 0));
		context.assertBaseCalled();
		assertOutboundJudgeCalled(1);

		assertEquals(1, pm.queryIntentServices(intent(), 0).size());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(2);
		assertEquals(2, dry_pm.queryIntentServices(intent(), 0).size());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(2);

		assertEquals(1, pm.queryBroadcastReceivers(intent(), 0).size());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(2);
		assertEquals(2, dry_pm.queryBroadcastReceivers(intent(), 0).size());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(2);

		condom.sendBroadcast(intent());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(0);
		dry_condom.sendBroadcast(intent());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(0);
	}

	private static void with(final Intent[] intents, final Consumer<Intent>[] tests, final Runnable... expectations) {
		for (final Intent intent : intents)
			for (final Consumer<Intent> test : tests) {
				test.accept(intent);
				for (final Runnable expectation : expectations) expectation.run();
			}
	}

	private static Intent intent() { return new Intent("com.example.TEST").addFlags(INTENT_FLAGS); }

	private static final UserHandle USER = SDK_INT >= JELLY_BEAN_MR1 ? android.os.Process.myUserHandle() : null;
	private static final int INTENT_FLAGS = Intent.FLAG_DEBUG_LOG_RESOLUTION | Intent.FLAG_FROM_BACKGROUND;	// Just random flags to verify flags preservation.
	private static final ServiceConnection SERVICE_CONNECTION = new ServiceConnection() {
		@Override public void onServiceConnected(final ComponentName name, final IBinder service) {}
		@Override public void onServiceDisconnected(final ComponentName name) {}
	};
	private static final String DISALLOWED_PACKAGE = "a.b.c";
	private static final String ALLOWED_PACKAGE = "x.y.z";
	private static final ComponentName DISALLOWED_COMPONENT = new ComponentName(DISALLOWED_PACKAGE, "A");
	private static final ComponentName ALLOWED_COMPONENT = new ComponentName(ALLOWED_PACKAGE, "A");
	private static final int FLAG_EXCLUDE_STOPPED_PACKAGES = SDK_INT >= HONEYCOMB_MR1 ? Intent.FLAG_EXCLUDE_STOPPED_PACKAGES : 0;
	private static final int FLAG_INCLUDE_STOPPED_PACKAGES = SDK_INT >= HONEYCOMB_MR1 ? Intent.FLAG_INCLUDE_STOPPED_PACKAGES : 0;

	private static final Intent[] ALL_SORT_OF_INTENTS = new Intent[] {
			intent(),
			intent().setPackage(ALLOWED_PACKAGE),
			intent().setPackage(DISALLOWED_PACKAGE),
			intent().setComponent(ALLOWED_COMPONENT),
			intent().setComponent(DISALLOWED_COMPONENT),
	};

	private static final Intent[] ALLOWED_INTENTS = new Intent[] {
			intent().setPackage(ALLOWED_PACKAGE),
			intent().setComponent(ALLOWED_COMPONENT),
	};

	private static final Intent[] DISALLOWED_INTENTS = new Intent[] {
			intent().setPackage(DISALLOWED_PACKAGE),
			intent().setComponent(DISALLOWED_COMPONENT),
	};

	private static Consumer<Intent>[] allBroadcastAndServiceApis(final Context condom) {
		final Consumer<Intent>[] broadcast_apis = allBroadcastApis(condom);
		final Consumer<Intent>[] service_apis = allServiceApis(condom);
		final Consumer<Intent>[] all = Arrays.copyOf(broadcast_apis, broadcast_apis.length + service_apis.length);
		System.arraycopy(service_apis, 0, all, broadcast_apis.length, service_apis.length);
		return all;
	}

	private static Consumer<Intent>[] allBroadcastApis(final Context condom) {
		final List<Consumer<Intent>> tests = new ArrayList<>();
		tests.add(new Consumer<Intent>() { @Override public void accept(final Intent intent) { condom.sendBroadcast(intent); }});
		tests.add(new Consumer<Intent>() { @Override public void accept(final Intent intent) { condom.sendBroadcast(intent, permission.DUMP); }});
		tests.add(new Consumer<Intent>() { @Override public void accept(final Intent intent) { condom.sendOrderedBroadcast(intent, permission.DUMP); }});
		tests.add(new Consumer<Intent>() { @Override public void accept(final Intent intent) { condom.sendOrderedBroadcast(intent, permission.DUMP, null, null, 0, null, null); }});
		tests.add(new Consumer<Intent>() { @Override public void accept(final Intent intent) { condom.sendStickyBroadcast(intent); }});
		tests.add(new Consumer<Intent>() { @Override public void accept(final Intent intent) { condom.sendStickyOrderedBroadcast(intent, null, null, 0, null, null); }});
		if (SDK_INT >= JELLY_BEAN_MR1) {
			tests.add(new Consumer<Intent>() { @TargetApi(JELLY_BEAN_MR1) @Override public void accept(final Intent intent) { condom.sendBroadcastAsUser(intent, USER); }});
			tests.add(new Consumer<Intent>() { @TargetApi(JELLY_BEAN_MR1) @Override public void accept(final Intent intent) { condom.sendBroadcastAsUser(intent, USER, null); }});
			tests.add(new Consumer<Intent>() { @TargetApi(JELLY_BEAN_MR1) @Override public void accept(final Intent intent) { condom.sendStickyBroadcastAsUser(intent, USER); }});
			tests.add(new Consumer<Intent>() { @TargetApi(JELLY_BEAN_MR1) @Override public void accept(final Intent intent) { condom.sendOrderedBroadcastAsUser(intent, USER, null, null, null, 0, null, null); }});
			tests.add(new Consumer<Intent>() { @TargetApi(JELLY_BEAN_MR1) @Override public void accept(final Intent intent) { condom.sendStickyOrderedBroadcastAsUser(intent, USER,null, null, 0, null, null); }});
		}
		tests.add(new Consumer<Intent>() { @Override public void accept(final Intent intent) { condom.getPackageManager().queryBroadcastReceivers(intent, 0); }});

		//noinspection unchecked
		return tests.toArray(new Consumer[0]);
	}

	@SuppressWarnings("unchecked") private static Consumer<Intent>[] allServiceApis(final Context condom) {
		return new Consumer[] {
				new Consumer<Intent>() { @Override public void accept(final Intent intent) {
					condom.startService(intent);
				}}, new Consumer<Intent>() { @Override public void accept(final Intent intent) {
					condom.bindService(intent, SERVICE_CONNECTION, 0);
				}}
		};
	}

	private void assertOutboundJudgeCalled(final int count) { assertEquals(count, mNumOutboundJudgeCalled.getAndSet(0)); }

	private final AtomicInteger mNumOutboundJudgeCalled = new AtomicInteger();
	private static final String TAG = "Test";


	private class TestContext extends ContextWrapper {

		@CallSuper void check(final Intent intent) {
			assertBaseNotCalled();
			mBaseCalled = true;
			mIntentFlags = intent.getFlags();
		}

		@Override public ComponentName startService(final Intent intent) { check(intent); return null; }
		@Override public boolean bindService(final Intent intent, final ServiceConnection c, final int f) { check(intent); return false; }
		@Override public void sendBroadcast(final Intent intent) { check(intent); }
		@Override public void sendBroadcast(final Intent intent, final String p) { check(intent); }
		@Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user) { check(intent); }
		@Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission) { check(intent); }
		@SuppressWarnings("deprecation") @Override public void sendStickyBroadcast(final Intent intent) { check(intent); }
		@SuppressWarnings("deprecation") @Override public void sendStickyBroadcastAsUser(final Intent intent, final UserHandle u) { check(intent); }
		@Override public void sendOrderedBroadcast(final Intent intent, final String p) { check(intent); }
		@Override public void sendOrderedBroadcast(final Intent intent, final String p, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent); }
		@Override public void sendOrderedBroadcastAsUser(final Intent intent, final UserHandle u, final String p, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent); }
		@SuppressWarnings("deprecation") @Override public void sendStickyOrderedBroadcast(final Intent intent, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent); }
		@SuppressWarnings("deprecation") @Override public void sendStickyOrderedBroadcastAsUser(final Intent intent, final UserHandle u, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent); }

		@Override public PackageManager getPackageManager() {
			return new PackageManagerWrapper(ApplicationProvider.getApplicationContext().getPackageManager()) {

				@Override public ResolveInfo resolveService(final Intent intent, final int flags) {
					check(intent);
					return buildResolveInfo(DISALLOWED_PACKAGE, true, 7777777);	// Must be consistent with the first entry from queryIntentServices().
				}

				@Override public List<ResolveInfo> queryIntentServices(final Intent intent, final int flags) {
					check(intent);
					final List<ResolveInfo> resolves = new ArrayList<>();
					if (mTestingBackgroundUid) {
						final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
						Assume.assumeTrue(am != null);
						final List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(32);
						if (services != null) for (final ActivityManager.RunningServiceInfo service : services) {
							if (service.pid == 0 || service.uid == android.os.Process.myUid()) continue;
							resolves.add(buildResolveInfo(DISALLOWED_PACKAGE, true, 7777777));	// Simulate a background UID.
							resolves.add(buildResolveInfo("non.bg.service", true, service.uid));
							break;
						}
					}
					resolves.add(buildResolveInfo(ALLOWED_PACKAGE, true, android.os.Process.myUid()));
					resolves.add(buildResolveInfo(DISALLOWED_PACKAGE, true, android.os.Process.myUid()));
					return resolves;
				}

				@Override public List<ResolveInfo> queryBroadcastReceivers(final Intent intent, final int flags) {
					check(intent);
					final List<ResolveInfo> resolves = new ArrayList<>();
					resolves.add(buildResolveInfo(ALLOWED_PACKAGE, false, android.os.Process.myUid()));
					resolves.add(buildResolveInfo(DISALLOWED_PACKAGE, false, android.os.Process.myUid()));
					return resolves;
				}

				@Override public ProviderInfo resolveContentProvider(final String name, final int flags) {
					final ProviderInfo info = super.resolveContentProvider(name, flags);
					if (info != null && mTestingStoppedProvider) {
						if (getPackageName().equals(info.packageName)) info.packageName += ".dummy";	// To simulate a package other than current one.
						info.applicationInfo.flags |= ApplicationInfo.FLAG_STOPPED;
					}
					return info;
				}

				private ResolveInfo buildResolveInfo(final String pkg, final boolean service_or_receiver, final int uid) {
					final ResolveInfo r = new ResolveInfo() { @Override public String toString() { return "ResolveInfo{test}"; } };
					final ComponentInfo info = service_or_receiver ? (r.serviceInfo = new ServiceInfo()) : (r.activityInfo = new ActivityInfo());
					info.packageName = pkg;
					info.applicationInfo = new ApplicationInfo();
					info.applicationInfo.packageName = pkg;
					info.applicationInfo.uid = uid;
					return r;
				}
			};
		}

		@Override public Context getApplicationContext() { return this; }

		void assertBaseCalled() { assertTrue(mBaseCalled); mBaseCalled = false; }
		void assertBaseNotCalled() { assertFalse(mBaseCalled); }

		Runnable expectFlags(final int flags) {
			return new Runnable() { @Override public void run() {
				assertEquals(flags | INTENT_FLAGS, mIntentFlags);
			}};
		}

		TestContext() { super(ApplicationProvider.getApplicationContext()); }

		boolean mTestingBackgroundUid;
		boolean mTestingStoppedProvider;
		private int mIntentFlags;
		private boolean mBaseCalled;

		final Runnable EXPECT_BASE_CALLED = new Runnable() { @Override public void run() { assertBaseCalled(); } };
	}

	private interface Consumer<T> { void accept(T t); }
}
