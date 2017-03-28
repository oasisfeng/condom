package com.oasisfeng.condom;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
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
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@SuppressWarnings("deprecation") @ParametersAreNonnullByDefault
public class CondomContextBlockingTest {

	@Test public void testDelegationAndExclusionFlagsIncludingDryRun() {
		final AtomicInteger expected_flags = new AtomicInteger(), unexpected_flags = new AtomicInteger();
		final Context context = new ContextWrapper(InstrumentationRegistry.getTargetContext()) {
			@Override public ComponentName startService(final Intent intent) { check(intent); return null; }
			@Override public boolean bindService(final Intent intent, final ServiceConnection c, final int f) { check(intent); return false; }
			@Override public void sendBroadcast(final Intent intent) { check(intent); }
			@Override public void sendBroadcast(final Intent intent, final String p) { check(intent); }
			@Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user) { check(intent); }
			@Override public void sendBroadcastAsUser(final Intent intent, final UserHandle user, final String receiverPermission) { check(intent); }
			@Override public void sendStickyBroadcast(final Intent intent) { check(intent); }
			@Override public void sendStickyBroadcastAsUser(final Intent intent, final UserHandle u) { check(intent); }
			@Override public void sendOrderedBroadcast(final Intent intent, final String p) { check(intent); }
			@Override public void sendOrderedBroadcast(final Intent intent, final String p, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent); }
			@Override public void sendOrderedBroadcastAsUser(final Intent intent, final UserHandle u, final String p, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent); }
			@Override public void sendStickyOrderedBroadcast(final Intent intent, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent); }
			@Override public void sendStickyOrderedBroadcastAsUser(final Intent intent, final UserHandle u, final BroadcastReceiver r, final Handler s, final int c, final String d, final Bundle e) { check(intent); }
			private void check(final Intent intent) {
				mDelegated.set(true);
				final int all_flags = expected_flags.get() | unexpected_flags.get();
				assertEquals(INTENT_FLAGS, intent.getFlags() & ~ all_flags);		// Assert original flags intact
				assertEquals(expected_flags.get(), (intent.getFlags() & all_flags));
			}
		};
		final CondomContext condom = CondomContext.wrap(context, TAG);

		// Dry-run test
		expected_flags.set(0);		// Flags should be intact
		condom.setDryRun(true);
		condom.startService(intent());
		assertBaseCalled();
		condom.bindService(intent(), SERVICE_CONNECTION, 0);
		assertBaseCalled();
		condom.sendBroadcast(intent());		// Test just one of the broadcast-related APIs, since they all share the same logic.
		assertBaseCalled();
		condom.setDryRun(false);

		// Self-targeting test
		expected_flags.set(0);		// Flags should be intact
		final String self_pkg = context.getPackageName();
		condom.startService(intent().setPackage(self_pkg));
		assertBaseCalled();
		condom.bindService(intent().setPackage(self_pkg), SERVICE_CONNECTION, 0);
		assertBaseCalled();
		condom.sendBroadcast(intent().setPackage(self_pkg));
		assertBaseCalled();
		final ComponentName local_comp = new ComponentName(self_pkg, "A");
		condom.startService(intent().setComponent(local_comp));
		assertBaseCalled();
		condom.bindService(intent().setComponent(local_comp), SERVICE_CONNECTION, 0);
		assertBaseCalled();
		condom.sendBroadcast(intent().setComponent(local_comp));
		assertBaseCalled();

		// Allow broadcast to background packages and waking-up stopped packages.
		condom.preventWakingUpStoppedPackages(false);
		condom.preventBroadcastToBackgroundPackages(false);
		condom.sendBroadcast(intent());
		assertBaseCalled();
		// Prevent broadcast to background packages
		condom.preventBroadcastToBackgroundPackages(true);
		expected_flags.set(SDK_INT >= N ? CondomContext.FLAG_RECEIVER_EXCLUDE_BACKGROUND : Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		condom.sendBroadcast(intent());
		condom.preventWakingUpStoppedPackages(true);
		// Prevent waking-up stopped packages.
		condom.preventWakingUpStoppedPackages(true);
		if (SDK_INT >= HONEYCOMB_MR1) {
			expected_flags.set(expected_flags.get() | Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
			unexpected_flags.set(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		}

		// Normal test
		condom.startService(intent());
		assertBaseCalled();
		condom.bindService(intent(), SERVICE_CONNECTION, 0);
		assertBaseCalled();

		condom.sendBroadcast(intent());
		assertBaseCalled();
		condom.sendBroadcast(intent(), null);
		assertBaseCalled();
		condom.sendStickyBroadcast(intent());
		assertBaseCalled();
		condom.sendOrderedBroadcast(intent(), null);
		assertBaseCalled();
		condom.sendOrderedBroadcast(intent(), null, null, null, 0, null, null);
		assertBaseCalled();
		if (SDK_INT >= JELLY_BEAN_MR1) {
			condom.sendBroadcastAsUser(intent(), USER);
			assertBaseCalled();
			condom.sendBroadcastAsUser(intent(), USER, null);
			assertBaseCalled();
			condom.sendStickyBroadcastAsUser(intent(), USER);
			assertBaseCalled();
			condom.sendOrderedBroadcastAsUser(intent(), USER, null, null, null, 0, null, null);
			assertBaseCalled();
			condom.sendStickyOrderedBroadcast(intent(), null, null, 0, null, null);
			assertBaseCalled();
			condom.sendStickyOrderedBroadcastAsUser(intent(), USER,null, null, 0, null, null);
			assertBaseCalled();
		}
	}

	private void assertBaseCalled() {
		assertTrue(mDelegated.get());
		mDelegated.set(false);
	}

	@Test public void testOutboundJudgeIncludingDryRun() {
		final AtomicBoolean delegation_expected = new AtomicBoolean(false);
		final Context context = new ContextWrapper(InstrumentationRegistry.getTargetContext()) {
			@Override public void sendBroadcast(final Intent intent) {
				if (! delegation_expected.get()) fail("Not blocked as expected");
				else mDelegated.set(true);
			}

			@Override public PackageManager getPackageManager() {
				return new PackageManagerWrapper(InstrumentationRegistry.getTargetContext().getPackageManager()) {
					@Override public ResolveInfo resolveService(final Intent intent, final int flags) {
						return buildResolveInfo(DISALLOWED_PACKAGE, true);
					}

					@Override public List<ResolveInfo> queryIntentServices(final Intent intent, final int flags) {
						final List<ResolveInfo> resolves = new ArrayList<>();
						resolves.add(buildResolveInfo(ALLOWED_PACKAGE, true));
						resolves.add(buildResolveInfo(DISALLOWED_PACKAGE, true));
						return resolves;
					}

					@Override public List<ResolveInfo> queryBroadcastReceivers(final Intent intent, final int flags) {
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
						} else {
							resolve.activityInfo = new ActivityInfo();
							resolve.activityInfo.packageName = pkg;
						}
						return resolve;
					}
				};
			}
		};
		final CondomContext condom = CondomContext.wrap(context, TAG).setOutboundJudge(new CondomContext.OutboundJudge() {
			@Override public boolean shouldAllow(final CondomContext.OutboundType type, final String target_pkg) {
				return ! DISALLOWED_PACKAGE.equals(target_pkg);
			}
		});
		final PackageManager pm = condom.getPackageManager();
		condom.sendBroadcast(intent().setPackage(DISALLOWED_PACKAGE));
		condom.sendBroadcast(intent().setComponent(new ComponentName(DISALLOWED_PACKAGE, "A")));
		assertNull(pm.resolveService(intent().setPackage(DISALLOWED_PACKAGE), 0));
		assertEquals(1, pm.queryIntentServices(intent(), 0).size());
		assertEquals(1, pm.queryBroadcastReceivers(intent(), 0).size());

		delegation_expected.set(true);
		condom.sendBroadcast(new Intent("com.example.TEST").setPackage(ALLOWED_PACKAGE));
		assertTrue(mDelegated.get());
		condom.sendBroadcast(new Intent("com.example.TEST").setComponent(new ComponentName(ALLOWED_PACKAGE, "A")));
		assertTrue(mDelegated.get());
		condom.sendBroadcast(new Intent("com.example.TEST"));
		assertTrue(mDelegated.get());

		// Dry-run test
		condom.setDryRun(true);
		delegation_expected.set(true);
		condom.sendBroadcast(new Intent("com.example.TEST").setPackage(DISALLOWED_PACKAGE));
		condom.sendBroadcast(new Intent("com.example.TEST").setComponent(new ComponentName(DISALLOWED_PACKAGE, "A")));
		condom.sendBroadcast(new Intent("com.example.TEST").setPackage(ALLOWED_PACKAGE));
		condom.sendBroadcast(new Intent("com.example.TEST").setComponent(new ComponentName(ALLOWED_PACKAGE, "A")));
		condom.sendBroadcast(new Intent("com.example.TEST"));
	}

	@Before public void prepare() {
		mDelegated.set(false);
	}

	private static Intent intent() { return new Intent("com.example.TEST").addFlags(INTENT_FLAGS); }

	private final AtomicBoolean mDelegated = new AtomicBoolean();

	private static final UserHandle USER = SDK_INT >= JELLY_BEAN_MR1 ? android.os.Process.myUserHandle() : null;
	private static final int INTENT_FLAGS = Intent.FLAG_DEBUG_LOG_RESOLUTION | Intent.FLAG_FROM_BACKGROUND;
	private static final ServiceConnection SERVICE_CONNECTION = new ServiceConnection() {
		@Override public void onServiceConnected(final ComponentName name, final IBinder service) {}
		@Override public void onServiceDisconnected(final ComponentName name) {}
	};
	private static final String DISALLOWED_PACKAGE = "a.b.c";
	private static final String ALLOWED_PACKAGE = "x.y.z";
	private static final String TAG = "Test";
}
