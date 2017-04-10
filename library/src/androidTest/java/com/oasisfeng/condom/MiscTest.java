package com.oasisfeng.condom;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.util.EventLog;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Miscellaneous test cases
 *
 * Created by Oasis on 2017/4/10.
 */
public class MiscTest {

	@Test public void testEventLog() throws IOException {
		condom.getBaseContext();
		Object[] data = readLastEvent(CondomContext.CondomEvent.CONCERN);
		assertEquals("getBaseContext", data[0]);
		assertTrue(data[1].toString(), data[1].toString().startsWith(MiscTest.class.getName() + ".testEventLog:"));

		((Application) condom.getApplicationContext()).getBaseContext();
		data = readLastEvent(CondomContext.CondomEvent.CONCERN);
		assertEquals("Application.getBaseContext", data[0]);
		assertTrue(data[1].toString(), data[1].toString().startsWith(MiscTest.class.getName() + ".testEventLog:"));

		condom.getContentResolver();
		data = readLastEvent(CondomContext.CondomEvent.CONCERN);
		assertEquals("getContentResolver", data[0]);
		assertTrue(data[1].toString(), data[1].toString().startsWith(MiscTest.class.getName() + ".testEventLog:"));

		final Intent intent = new Intent().setPackage("a.b.c");
		condom.bindService(intent, SERVICE_CONNECTION, 0);
		data = readLastEvent(CondomContext.CondomEvent.BIND_PASS);
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals(TAG, data[1]);
		assertEquals(intent.getPackage(), data[2]);
		assertEquals(intent.toString(), data[3]);

		condom.startService(intent);
		data = readLastEvent(CondomContext.CondomEvent.START_PASS);
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals(TAG, data[1]);
		assertEquals(intent.getPackage(), data[2]);
		assertEquals(intent.toString(), data[3]);

		condom.getPackageManager().queryIntentServices(intent.setPackage(null).setComponent(null), 0);
		data = readNextEvent(CondomContext.CondomEvent.FILTER_BG_SERVICE, false);
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals(TAG, data[1]);
		assertEquals("bg.service.1", data[2]);
		final String expected_intent = new Intent(intent).addFlags(SDK_INT >= HONEYCOMB_MR1 ? Intent.FLAG_EXCLUDE_STOPPED_PACKAGES : 0).toString();	// Flags altered
		assertEquals(expected_intent, data[3]);
		data = readNextEvent(CondomContext.CondomEvent.FILTER_BG_SERVICE, true);
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals(TAG, data[1]);
		assertEquals("bg.service.2", data[2]);
		assertEquals(expected_intent, data[3]);

		condom.getPackageManager().resolveService(intent, 0);
		data = readLastEvent(CondomContext.CondomEvent.FILTER_BG_SERVICE);
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals(TAG, data[1]);
		assertEquals("bg.service.1", data[2]);
		assertEquals(expected_intent, data[3]);

		final CondomContext condom_wo_tag = CondomContext.wrap(new ContextWrapper(InstrumentationRegistry.getTargetContext()) {
			@Override public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) { return true; }
			@Override public ComponentName startService(final Intent service) {
				return service.getComponent() != null ? service.getComponent() : new ComponentName(service.getPackage(), "A");
			}
		}, null);

		final ComponentName component = new ComponentName("x.y.z", "O");
		intent.setPackage(null).setComponent(component);
		condom_wo_tag.bindService(intent, SERVICE_CONNECTION, 0);
		data = readLastEvent(CondomContext.CondomEvent.BIND_PASS);
		assertEquals(condom_wo_tag.getPackageName(), data[0]);
		assertEquals("", data[1]);
		assertEquals(intent.getComponent().getPackageName(), data[2]);
		assertEquals(intent.toString(), data[3]);

		condom_wo_tag.startService(intent);
		data = readLastEvent(CondomContext.CondomEvent.START_PASS);
		assertEquals(condom_wo_tag.getPackageName(), data[0]);
		assertEquals("", data[1]);
		assertEquals(intent.getComponent().getPackageName(), data[2]);
		assertEquals(intent.toString(), data[3]);
	}

	private static Object[] readLastEvent(final CondomContext.CondomEvent type) throws IOException {
		return readNextEvent(type, true);
	}

	private static Object[] readNextEvent(final CondomContext.CondomEvent type, final boolean assert_no_more) throws IOException {
		final List<EventLog.Event> events = new ArrayList<>();
		EventLog.readEvents(new int[] { "Condom".hashCode() + type.ordinal() }, events);
		assertFalse(events.isEmpty());
		final EventLog.Event last = events.get(events.size() - 1);
		if (mLastEventTime == 0) {
			mLastEventTime = last.getTimeNanos();
			return (Object[]) last.getData();
		}
		for (final EventLog.Event event : events)
			if (event.getTimeNanos() > mLastEventTime) {
				if (assert_no_more && last.getTimeNanos() > event.getTimeNanos()) fail("Still more events");
				mLastEventTime = event.getTimeNanos();
				return (Object[]) event.getData();
			}
		fail("No more event");
		return new Object[0];
	}

	private static long mLastEventTime;

	private static final ServiceConnection SERVICE_CONNECTION = new ServiceConnection() {
		@Override public void onServiceConnected(final ComponentName name, final IBinder service) {}
		@Override public void onServiceDisconnected(final ComponentName name) {}
	};


	private final CondomContext condom = CondomContext.wrap(new ContextWrapper(InstrumentationRegistry.getTargetContext()) {
		@Override public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) { return true; }
		@Override public ComponentName startService(final Intent service) {
			return service.getComponent() != null ? service.getComponent() : new ComponentName(service.getPackage(), "A");
		}

		@Override public PackageManager getPackageManager() {
			return new PackageManagerWrapper(super.getPackageManager()) {
				@Override public List<ResolveInfo> queryIntentServices(final Intent intent, final int flags) {
					final List<ResolveInfo> resolves = new ArrayList<>();
					final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
					final List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(32);
					if (services != null) for (final ActivityManager.RunningServiceInfo service : services) {
						if (service.uid == android.os.Process.myUid()) continue;
						resolves.add(buildResolveInfo("bg.service.1", 999999999));		// Simulate a background UID.
						resolves.add(buildResolveInfo("non.bg.service", service.uid));
						resolves.add(buildResolveInfo("bg.service.2", 88888888));
						break;
					}
					return resolves;
				}

				private ResolveInfo buildResolveInfo(final String pkg, final int uid) {
					final ResolveInfo resolve = new ResolveInfo();
					resolve.serviceInfo = new ServiceInfo();
					resolve.serviceInfo.packageName = pkg;
					resolve.serviceInfo.applicationInfo = new ApplicationInfo();
					resolve.serviceInfo.applicationInfo.packageName = pkg;
					resolve.serviceInfo.applicationInfo.uid = uid;
					return resolve;
				}
			};
		}
	}, TAG);

	private static final String TAG = "Test";
}
