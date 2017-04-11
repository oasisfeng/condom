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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Miscellaneous test cases
 *
 * Created by Oasis on 2017/4/10.
 */
public class MiscTest {

	@Test public void testEventLog() throws IOException {
		readNewEvents(CondomContext.CondomEvent.CONCERN);

		condom.getBaseContext();
		Object[] data = readLastEvent(CondomContext.CondomEvent.CONCERN);
		assertEquals("getBaseContext", data[0]);
		assertCallerMatch(data);

		((Application) condom.getApplicationContext()).getBaseContext();
		data = readLastEvent(CondomContext.CondomEvent.CONCERN);
		assertEquals("Application.getBaseContext", data[0]);
		assertCallerMatch(data);

		condom.getPackageManager().getInstalledApplications(0);
		data = readLastEvent(CondomContext.CondomEvent.CONCERN);
		assertEquals("PackageManager.getInstalledApplications", data[0]);
		assertCallerMatch(data);

		condom.getPackageManager().getInstalledPackages(0);
		data = readLastEvent(CondomContext.CondomEvent.CONCERN);
		assertEquals("PackageManager.getInstalledPackages", data[0]);
		assertCallerMatch(data);

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

		final List<ResolveInfo> result = condom.getPackageManager().queryIntentServices(intent.setPackage(null).setComponent(null), 0);
		assertEquals(1, result.size());		// 1 left: non.bg.service
		final List<EventLog.Event> events = readNewEvents(CondomContext.CondomEvent.FILTER_BG_SERVICE);
		assertEquals(2, events.size());		// 2 filtered: bg.service.*
		data = (Object[]) events.get(0).getData();
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals(TAG, data[1]);
		assertEquals("bg.service.1", data[2]);
		final String expected_intent = new Intent(intent).addFlags(SDK_INT >= HONEYCOMB_MR1 ? Intent.FLAG_EXCLUDE_STOPPED_PACKAGES : 0).toString();	// Flags altered
		assertEquals(expected_intent, data[3]);
		data = (Object[]) events.get(1).getData();
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals(TAG, data[1]);
		assertEquals("bg.service.2", data[2]);
		assertEquals(expected_intent, data[3]);

		final ResolveInfo resolve = condom.getPackageManager().resolveService(intent, 0);
		assertEquals("non.bg.service", resolve.serviceInfo.applicationInfo.packageName);
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

	private static void assertCallerMatch(final Object[] data) {
		final String string = data[1].toString();
		assertTrue(string, string.startsWith(MiscTest.class.getName() + ".testEventLog:"));
	}

	private static Object[] readLastEvent(final CondomContext.CondomEvent type) throws IOException {
		final List<EventLog.Event> events = readNewEvents(type);
		assertEquals(1, events.size());
		return (Object[]) events.get(0).getData();
	}

	private static List<EventLog.Event> readNewEvents(final CondomContext.CondomEvent type) throws IOException {
		final List<EventLog.Event> events = new ArrayList<>();
		EventLog.readEvents(new int[] { "Condom".hashCode() + type.ordinal() }, events);
		if (events.isEmpty()) return Collections.emptyList();
		final EventLog.Event last = events.get(events.size() - 1);
		if (mLastEventTime == 0) {
			mLastEventTime = last.getTimeNanos();
			return events;
		}
		final Iterator<EventLog.Event> iterator = events.iterator();
		while (iterator.hasNext()) {
			final EventLog.Event event = iterator.next();
			if (event.getTimeNanos() > mLastEventTime) {
				mLastEventTime = events.get(events.size() - 1).getTimeNanos();	// Advance to the latest one, since they all are returned.
				break;
			} else iterator.remove();
		}
		return events;
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
					final ResolveInfo resolve = new ResolveInfo() { @Override public String toString() { return "ResolveInfo{test}"; } };
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
