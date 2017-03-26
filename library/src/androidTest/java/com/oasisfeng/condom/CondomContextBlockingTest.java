package com.oasisfeng.condom;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.N;
import static junit.framework.Assert.assertEquals;
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
		condom.setDryRun(true);
		condom.startService(intent());
		assertBaseCalled();
		condom.bindService(intent(), SERVICE_CONNECTION, 0);
		assertBaseCalled();
		condom.sendBroadcast(intent());		// Test just one of the broadcast-related APIs, since they all share the same logic.
		assertBaseCalled();
		condom.setDryRun(false);

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
		};
		final CondomContext condom = CondomContext.wrap(context, TAG).setOutboundJudge(new CondomContext.OutboundJudge() {
			@Override public boolean shouldAllow(final CondomContext.OutboundType type, final String target_pkg) {
				return ! "a.b.c".equals(target_pkg);
			}
		});
		condom.sendBroadcast(new Intent("com.example.TEST").setPackage("a.b.c"));
		condom.sendBroadcast(new Intent("com.example.TEST").setComponent(new ComponentName("a.b.c", "A")));
		delegation_expected.set(true);
		condom.sendBroadcast(new Intent("com.example.TEST"));
		assertTrue(mDelegated.get());

		// Dry-run test
		condom.setDryRun(true);
		delegation_expected.set(true);
		condom.sendBroadcast(new Intent("com.example.TEST").setPackage("a.b.c"));
		condom.sendBroadcast(new Intent("com.example.TEST").setComponent(new ComponentName("a.b.c", "A")));
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

	private static final String TAG = "Test";
}
