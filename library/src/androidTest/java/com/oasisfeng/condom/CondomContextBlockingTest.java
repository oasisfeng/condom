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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.annotation.CallSuper;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.N;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@ParametersAreNonnullByDefault
public class CondomContextBlockingTest {

	@Test public void testDelegationAndExclusionFlagsIncludingDryRun() {
		final AtomicInteger expected_flags_added = new AtomicInteger(), expected_receiver_flags = new AtomicInteger(), unexpected_flags = new AtomicInteger();
		final TestContext context = new TestContext() {
			protected void check(final Intent intent, final boolean broadcast) {
				super.check(intent, broadcast);
				final int all_flags = expected_flags_added.get() | expected_receiver_flags.get() | unexpected_flags.get();
				assertEquals(INTENT_FLAGS, intent.getFlags() & ~ all_flags);        // Assert original flags intact
				assertEquals(expected_flags_added.get() | (broadcast ? expected_receiver_flags.get() : 0), (intent.getFlags() & all_flags));
			}
		};
		final Runnable EXPECT_BASE_CALLED = new Runnable() { @Override public void run() { context.assertBaseCalled(); } };
		final CondomContext condom = CondomContext.wrap(context, TAG);

		// Dry-run test
		condom.setDryRun(true);
		expected_flags_added.set(0);		// Flags should be intact
		with(ALL_SORT_OF_INTENTS, EXPECT_BASE_CALLED, allBroadcastApis(condom));
		with(ALL_SORT_OF_INTENTS, EXPECT_BASE_CALLED, allServiceApis(condom));
		condom.setDryRun(false);

		// Self-targeting test
		expected_flags_added.set(0);		// Flags should be intact
		final String self_pkg = condom.getPackageName();
		final Intent[] self_targeted_intents = new Intent[] {
				intent().setPackage(self_pkg),
				intent().setComponent(new ComponentName(self_pkg, "X"))
		};
		with(self_targeted_intents, EXPECT_BASE_CALLED, allBroadcastApis(condom));
		with(self_targeted_intents, EXPECT_BASE_CALLED, allServiceApis(condom));

		// Allow all (prevent none)
		expected_flags_added.set(0);		// Flags should be intact
		condom.preventBroadcastToBackgroundPackages(false).preventServiceInBackgroundPackages(false).preventWakingUpStoppedPackages(false);
		with(ALL_SORT_OF_INTENTS, EXPECT_BASE_CALLED, allBroadcastApis(condom));
		with(ALL_SORT_OF_INTENTS, EXPECT_BASE_CALLED, allServiceApis(condom));

		// Prevent broadcast to background packages
		condom.preventBroadcastToBackgroundPackages(true)  .preventServiceInBackgroundPackages(false).preventWakingUpStoppedPackages(false);
		expected_receiver_flags.set(SDK_INT >= N ? CondomCore.FLAG_RECEIVER_EXCLUDE_BACKGROUND : Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		with(ALL_SORT_OF_INTENTS, EXPECT_BASE_CALLED, allBroadcastApis(condom));
		expected_receiver_flags.set(0);

		// Prevent waking-up stopped packages.
		condom.preventWakingUpStoppedPackages(true)  .preventBroadcastToBackgroundPackages(false).preventServiceInBackgroundPackages(false);
		expected_flags_added.set(FLAG_EXCLUDE_STOPPED_PACKAGES);
		unexpected_flags.set(FLAG_INCLUDE_STOPPED_PACKAGES);
		final Intent[] intents_with_inc_stop = ALL_SORT_OF_INTENTS.clone();
		for (int i = 0; i < intents_with_inc_stop.length; i++)
			intents_with_inc_stop[i] = new Intent(intents_with_inc_stop[i]).addFlags(FLAG_INCLUDE_STOPPED_PACKAGES);
		with(intents_with_inc_stop, EXPECT_BASE_CALLED, allBroadcastApis(condom));
		with(intents_with_inc_stop, EXPECT_BASE_CALLED, allServiceApis(condom));

		// Normal test
		condom.preventWakingUpStoppedPackages(true).preventBroadcastToBackgroundPackages(true).preventServiceInBackgroundPackages(false);
		expected_flags_added.set(FLAG_EXCLUDE_STOPPED_PACKAGES);
		unexpected_flags.set(FLAG_INCLUDE_STOPPED_PACKAGES);
		expected_receiver_flags.set(SDK_INT >= N ? CondomCore.FLAG_RECEIVER_EXCLUDE_BACKGROUND : Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		with(intents_with_inc_stop, EXPECT_BASE_CALLED, allBroadcastApis(condom));
		with(intents_with_inc_stop, EXPECT_BASE_CALLED, allServiceApis(condom));
	}

	@Test public void testPreventServiceInBackgroundPackages() {
		final TestContext context = new TestContext();
		context.mTestingBackgroundUid = true;
		final CondomContext condom = CondomContext.wrap(context, TAG);
		final PackageManager pm = condom.getPackageManager();
		// Prevent service in background packages
		condom.preventServiceInBackgroundPackages(true)	 .preventBroadcastToBackgroundPackages(false).preventWakingUpStoppedPackages(false);
		assertEquals(3, pm.queryIntentServices(intent(), 0).size());
		context.assertBaseCalled();
		assertEquals("non.bg.service", pm.resolveService(intent(), 0).serviceInfo.packageName);
		context.assertBaseCalled();

		condom.preventServiceInBackgroundPackages(false).preventBroadcastToBackgroundPackages(false).preventWakingUpStoppedPackages(false);
		assertEquals(4, condom.getPackageManager().queryIntentServices(intent(), 0).size());
		context.assertBaseCalled();
		assertEquals(7777777, pm.resolveService(intent(), 0).serviceInfo.applicationInfo.uid);
		context.assertBaseCalled();
	}

	@Test public void testContentProvider() {
		final TestContext context = new TestContext();
		// Outbound judge
		CondomContext condom = CondomContext.wrap(context, TAG, new CondomOptions().setOutboundJudge(new OutboundJudge() { @Override public boolean shouldAllow(final OutboundType type, final String target_pkg) {
			final String settings_pkg = InstrumentationRegistry.getTargetContext().getPackageManager().resolveContentProvider(Settings.System.CONTENT_URI.getAuthority(), 0).packageName;
			return ! settings_pkg.equals(target_pkg);
		}}));
		assertNull(condom.getPackageManager().resolveContentProvider(Settings.AUTHORITY, 0));
		try {
			condom.getContentResolver().call(Settings.System.CONTENT_URI, "test", null, null);
			fail("Provider not blocked by outbound judge.");
		} catch (final IllegalArgumentException ignored) {}
		//noinspection ConstantConditions
		condom = CondomContext.wrap(context, TAG);

		// Regular provider access
		final String actual_android_id = Settings.System.getString(condom.getContentResolver(), Settings.System.ANDROID_ID);
		final String expected_android_id = Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);
		assertNotNull(actual_android_id);
		assertEquals(expected_android_id, actual_android_id);

		// Prevent stopped package
		context.mTestingStoppedProvider = true;
		assertNull(condom.getPackageManager().resolveContentProvider(Settings.AUTHORITY, 0));
		try {
			condom.getContentResolver().call(Settings.System.CONTENT_URI, "test", null, null);
			fail("Stopped provider not blocked.");
		} catch (final IllegalArgumentException ignored) {}
		context.mTestingStoppedProvider = false;
	}

	@SafeVarargs private static void with(final Intent[] intents, final Runnable expectation, final Consumer<Intent>... tests) {
		for (final Intent intent : intents)
			for (final Consumer<Intent> test : tests) {
				test.accept(intent);
				expectation.run();
			}
	}

	@Test public void testOutboundJudgeIncludingDryRun() {
		final TestContext context = new TestContext();
		final CondomContext condom = CondomContext.wrap(context, TAG, new CondomOptions().setOutboundJudge(new OutboundJudge() {
			@Override public boolean shouldAllow(final OutboundType type, final String target_pkg) {
				mNumOutboundJudgeCalled.incrementAndGet();
				return ! DISALLOWED_PACKAGE.equals(target_pkg);
			}
		}));
		final PackageManager pm = condom.getPackageManager();

		final Runnable EXPECT_OUTBOUND_JUDGE_REFUSAL = new Runnable() { @Override public void run() {
			context.assertBaseNotCalled();
			assertOutboundJudgeCalled(1);
		}};
		with(DISALLOWED_INTENTS, EXPECT_OUTBOUND_JUDGE_REFUSAL, allBroadcastApis(condom));

		assertNull(pm.resolveService(intent().setPackage(DISALLOWED_PACKAGE), 0));
		context.assertBaseNotCalled();
		assertOutboundJudgeCalled(1);

		assertEquals(1, pm.queryIntentServices(intent(), 0).size());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(2);

		assertEquals(1, pm.queryBroadcastReceivers(intent(), 0).size());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(2);

		final Runnable EXPECT_OUTBOUND_JUDGE_PASS = new Runnable() { @Override public void run() {
			context.assertBaseCalled();
			assertOutboundJudgeCalled(1);
		}};
		with(ALLOWED_INTENTS, EXPECT_OUTBOUND_JUDGE_PASS, allBroadcastApis(condom));
		condom.sendBroadcast(intent());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(0);

		// Dry-run test
		condom.setDryRun(true);
		with(DISALLOWED_INTENTS, EXPECT_OUTBOUND_JUDGE_PASS, allBroadcastApis(condom));
		with(ALLOWED_INTENTS, EXPECT_OUTBOUND_JUDGE_PASS, allBroadcastApis(condom));
		condom.sendBroadcast(intent());
		context.assertBaseCalled();
		assertOutboundJudgeCalled(0);
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

	private static Consumer<Intent>[] allBroadcastApis(final CondomContext condom) {
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
		return tests.toArray(new Consumer[tests.size()]);
	}

	@SuppressWarnings("unchecked") private static Consumer<Intent>[] allServiceApis(final CondomContext condom) {
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
		@CallSuper void check(final Intent intent, final boolean broadcast) { assertBaseNotCalled(); mBaseCalled.set(true); }

		@Override public ComponentName startService(final Intent intent) { check(intent, false); return null; }
		@Override public boolean bindService(final Intent intent, final ServiceConnection c, final int f) { check(intent, false); return false; }
		@Override public void sendBroadcast(final Intent intent) { check(intent, true); }
		@Override public void sendBroadcast(final Intent intent, final String p) { check(intent, true); }
		@Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user) { check(intent, true); }
		@Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission) { check(intent, true); }
		@Override public void sendStickyBroadcast(final Intent intent) { check(intent, true); }
		@Override public void sendStickyBroadcastAsUser(final Intent intent, final UserHandle u) { check(intent, true); }
		@Override public void sendOrderedBroadcast(final Intent intent, final String p) { check(intent, true); }
		@Override public void sendOrderedBroadcast(final Intent intent, final String p, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent, true); }
		@Override public void sendOrderedBroadcastAsUser(final Intent intent, final UserHandle u, final String p, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent, true); }
		@Override public void sendStickyOrderedBroadcast(final Intent intent, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent, true); }
		@Override public void sendStickyOrderedBroadcastAsUser(final Intent intent, final UserHandle u, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent, true); }

		@Override public PackageManager getPackageManager() {
			return new PackageManagerWrapper(InstrumentationRegistry.getTargetContext().getPackageManager()) {

				@Override public ResolveInfo resolveService(final Intent intent, final int flags) {
					check(intent, false);
					return buildResolveInfo(DISALLOWED_PACKAGE, true, 7777777);
				}

				@Override public List<ResolveInfo> queryIntentServices(final Intent intent, final int flags) {
					check(intent, false);
					final List<ResolveInfo> resolves = new ArrayList<>();
					if (mTestingBackgroundUid) {
						final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
						final List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(32);
						if (services != null) for (final ActivityManager.RunningServiceInfo service : services) {
							if (service.uid == android.os.Process.myUid()) continue;
							resolves.add(buildResolveInfo("bg.service", true, 999999999));	// Simulate a background UID.
							resolves.add(buildResolveInfo("non.bg.service", true, service.uid));
							break;
						}
					}
					resolves.add(buildResolveInfo(ALLOWED_PACKAGE, true, android.os.Process.myUid()));
					resolves.add(buildResolveInfo(DISALLOWED_PACKAGE, true, android.os.Process.myUid()));
					return resolves;
				}

				@Override public List<ResolveInfo> queryBroadcastReceivers(final Intent intent, final int flags) {
					check(intent, true);
					final List<ResolveInfo> resolves = new ArrayList<>();
					resolves.add(buildResolveInfo(ALLOWED_PACKAGE, false, android.os.Process.myUid()));
					resolves.add(buildResolveInfo(DISALLOWED_PACKAGE, false, android.os.Process.myUid()));
					return resolves;
				}

				@Override public ProviderInfo resolveContentProvider(final String name, final int flags) {
					final ProviderInfo info = super.resolveContentProvider(name, flags);
					if (mTestingStoppedProvider) info.applicationInfo.flags |= ApplicationInfo.FLAG_STOPPED;
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

		void assertBaseCalled() { assertTrue(mBaseCalled.getAndSet(false)); }
		void assertBaseNotCalled() { assertFalse(mBaseCalled.get()); }

		TestContext() { super((InstrumentationRegistry.getTargetContext())); }

		boolean mTestingBackgroundUid;
		boolean mTestingStoppedProvider;
		private final AtomicBoolean mBaseCalled = new AtomicBoolean();
	}

	private interface Consumer<T> { void accept(T t); }
}
