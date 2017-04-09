package com.oasisfeng.condom;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
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
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings("deprecation") @ParametersAreNonnullByDefault
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
		expected_receiver_flags.set(SDK_INT >= N ? CondomContext.FLAG_RECEIVER_EXCLUDE_BACKGROUND : Intent.FLAG_RECEIVER_REGISTERED_ONLY);
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
		expected_receiver_flags.set(SDK_INT >= N ? CondomContext.FLAG_RECEIVER_EXCLUDE_BACKGROUND : Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		with(intents_with_inc_stop, EXPECT_BASE_CALLED, allBroadcastApis(condom));
		with(intents_with_inc_stop, EXPECT_BASE_CALLED, allServiceApis(condom));
	}

	@SafeVarargs private static void with(final Intent[] intents, final Runnable expectation, final Consumer<Intent>... tests) {
		for (final Intent intent : intents)
			for (final Consumer<Intent> test : tests) {
				test.accept(intent);
				expectation.run();
			}
	}

	@Test public void testOutboundJudgeIncludingDryRun() {
//		final AtomicBoolean delegation_expected = new AtomicBoolean(), delegated = new AtomicBoolean();
		final TestContext context = new TestContext();
		final CondomContext condom = CondomContext.wrap(context, TAG).setOutboundJudge(new CondomContext.OutboundJudge() {
			@Override public boolean shouldAllow(final CondomContext.OutboundType type, final String target_pkg) {
				mNumOutboundJudgeCalled.incrementAndGet();
				return ! DISALLOWED_PACKAGE.equals(target_pkg);
			}
		});
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
		@CallSuper protected void check(final Intent intent, final boolean broadcast) { assertBaseNotCalled(); mBaseCalled.set(true); }

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
					return buildResolveInfo(DISALLOWED_PACKAGE, true);
				}

				@Override public List<ResolveInfo> queryIntentServices(final Intent intent, final int flags) {
					check(intent, false);
					final List<ResolveInfo> resolves = new ArrayList<>();
					resolves.add(buildResolveInfo(ALLOWED_PACKAGE, true));
					resolves.add(buildResolveInfo(DISALLOWED_PACKAGE, true));
					return resolves;
				}

				@Override public List<ResolveInfo> queryBroadcastReceivers(final Intent intent, final int flags) {
					check(intent, true);
					final List<ResolveInfo> resolves = new ArrayList<>();
					resolves.add(buildResolveInfo(ALLOWED_PACKAGE, false));
					resolves.add(buildResolveInfo(DISALLOWED_PACKAGE, false));
					return resolves;
				}

				private ResolveInfo buildResolveInfo(final String pkg, final boolean service_or_receiver) {
					final ResolveInfo resolve = new ResolveInfo() { @Override public String toString() { return "ResolveInfo{test}"; } };
					if (service_or_receiver) {
						resolve.serviceInfo = new ServiceInfo();
						resolve.serviceInfo.packageName = pkg;
						resolve.serviceInfo.applicationInfo = new ApplicationInfo();
						resolve.serviceInfo.applicationInfo.uid = android.os.Process.myUid();
					} else {
						resolve.activityInfo = new ActivityInfo();
						resolve.activityInfo.packageName = pkg;
						resolve.activityInfo.applicationInfo = new ApplicationInfo();
						resolve.activityInfo.applicationInfo.uid = android.os.Process.myUid();
					}
					return resolve;
				}
			};
		}

		void assertBaseCalled() { assertTrue(mBaseCalled.getAndSet(false)); }
		void assertBaseNotCalled() { assertFalse(mBaseCalled.get()); }

		TestContext() { super((InstrumentationRegistry.getTargetContext())); }

		private final AtomicBoolean mBaseCalled = new AtomicBoolean();
	}

	private interface Consumer<T> { void accept(T t); }
}
