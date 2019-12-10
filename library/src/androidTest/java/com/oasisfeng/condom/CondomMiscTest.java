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
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import android.util.DisplayMetrics;
import android.util.EventLog;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.O;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Miscellaneous test cases
 *
 * Created by Oasis on 2017/4/10.
 */
public class CondomMiscTest {

	@Test public void testHiddenApi() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		final Object uid = PackageManager.class.getMethod("getUidForSharedUser", String.class).invoke(condom.getPackageManager(),"android.uid.system");
		assertEquals(1000, (int) uid);
		// This hidden API is used by some 3rd-party libraries, as reported in issue #9 on GitHub.
		if (SDK_INT >= LOLLIPOP) PackageManager.class.getMethod("getUserBadgeForDensity", UserHandle.class, int.class)
				.invoke(condom.getPackageManager(), Process.myUserHandle(), DisplayMetrics.DENSITY_DEFAULT);
	}

	@Test public void testEventLog() throws IOException {
		readNewEvents(CondomCore.CondomEvent.CONCERN);

		condom.getBaseContext();
		Object[] data = readLastEvent(CondomCore.CondomEvent.CONCERN);
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals(condom.TAG, data[1]);
		assertEquals("getBaseContext", data[2]);
		assertCallerMatch(data);

		((Application) condom.getApplicationContext()).getBaseContext();
		data = readLastEvent(CondomCore.CondomEvent.CONCERN);
		assertEquals("Application.getBaseContext", data[2]);
		assertCallerMatch(data);

		condom.getPackageManager().getInstalledApplications(0);
		data = readLastEvent(CondomCore.CondomEvent.CONCERN);
		assertEquals("PackageManager.getInstalledApplications", data[2]);
		assertCallerMatch(data);

		condom.getPackageManager().getInstalledPackages(0);
		data = readLastEvent(CondomCore.CondomEvent.CONCERN);
		assertEquals("PackageManager.getInstalledPackages", data[2]);
		assertCallerMatch(data);

		final Intent intent = new Intent().setPackage("a.b.c");
		condom.bindService(intent, SERVICE_CONNECTION, 0);
		data = readLastEvent(CondomCore.CondomEvent.BIND_PASS);
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals("Condom." + TAG, data[1]);
		assertEquals(intent.getPackage(), data[2]);
		assertEquals(intent.toString(), data[3]);

		condom.startService(intent);
		data = readLastEvent(CondomCore.CondomEvent.START_PASS);
		assertEquals(condom.getPackageName(), data[0]);
		assertEquals("Condom." + TAG, data[1]);
		assertEquals(intent.getPackage(), data[2]);
		assertEquals(intent.toString(), data[3]);

		if (SDK_INT < O) {
			final List<ResolveInfo> result = condom.getPackageManager().queryIntentServices(intent.setPackage(null).setComponent(null), 0);
			assertEquals(1, result.size());		// 1 left: non.bg.service
			final List<EventLog.Event> events = readNewEvents(CondomCore.CondomEvent.FILTER_BG_SERVICE);
			assertEquals(2, events.size());		// 2 filtered: bg.service.*
			data = (Object[]) events.get(0).getData();
			assertEquals(condom.getPackageName(), data[0]);
			assertEquals("Condom." + TAG, data[1]);
			assertEquals("bg.service.1", data[2]);
			final String expected_intent = new Intent(intent).toString();	// Flags altered
			assertEquals(expected_intent, data[3]);
			data = (Object[]) events.get(1).getData();
			assertEquals(condom.getPackageName(), data[0]);
			assertEquals("Condom." + TAG, data[1]);
			assertEquals("bg.service.2", data[2]);
			assertEquals(expected_intent, data[3]);

			final ResolveInfo resolve = condom.getPackageManager().resolveService(intent, 0);
			assertEquals("non.bg.service", resolve.serviceInfo.applicationInfo.packageName);
			data = readLastEvent(CondomCore.CondomEvent.FILTER_BG_SERVICE);
			assertEquals(condom.getPackageName(), data[0]);
			assertEquals("Condom." + TAG, data[1]);
			assertEquals("bg.service.1", data[2]);
			assertEquals(expected_intent, data[3]);
		}

		final CondomContext condom_wo_tag = CondomContext.wrap(new ContextWrapper(ApplicationProvider.getApplicationContext()) {
			@Override public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) { return true; }
			@Override public ComponentName startService(final Intent service) {
				return service.getComponent() != null ? service.getComponent() : new ComponentName(service.getPackage(), "A");
			}
		}, null);

		final ComponentName component = new ComponentName("x.y.z", "O");
		intent.setPackage(null).setComponent(component);
		condom_wo_tag.bindService(intent, SERVICE_CONNECTION, 0);
		data = readLastEvent(CondomCore.CondomEvent.BIND_PASS);
		assertEquals(condom_wo_tag.getPackageName(), data[0]);
		assertEquals("Condom", data[1]);
		assertEquals(intent.getComponent().getPackageName(), data[2]);
		assertEquals(intent.toString(), data[3]);

		condom_wo_tag.startService(intent);
		data = readLastEvent(CondomCore.CondomEvent.START_PASS);
		assertEquals(condom_wo_tag.getPackageName(), data[0]);
		assertEquals("Condom", data[1]);
		assertEquals(intent.getComponent().getPackageName(), data[2]);
		assertEquals(intent.toString(), data[3]);
	}

	private static void assertCallerMatch(final Object[] data) {
		final String string = data[3].toString();
		assertTrue(string, string.startsWith(CondomMiscTest.class.getName() + ".testEventLog:"));
	}

	private static Object[] readLastEvent(final CondomCore.CondomEvent type) throws IOException {
		final List<EventLog.Event> events = readNewEvents(type);
		assertEquals(1, events.size());
		return (Object[]) events.get(0).getData();
	}

	private static List<EventLog.Event> readNewEvents(final CondomCore.CondomEvent type) throws IOException {
		final List<EventLog.Event> events = new ArrayList<>();
		EventLog.readEvents(new int[] { EVENT_TAG_MARK, "Condom".hashCode() + type.ordinal() }, events);
		if (events.isEmpty()) return Collections.emptyList();
		for (int i = events.size() - 1; i >= 0; i --) {
			final EventLog.Event event = events.get(i);
			if (event.getTag() == EVENT_TAG_MARK) {
				EventLog.writeEvent(EVENT_TAG_MARK);
				return events.subList(i + 1, events.size());
			}
		}
		EventLog.writeEvent(EVENT_TAG_MARK);
		return events;
	}

	private static final ServiceConnection SERVICE_CONNECTION = new ServiceConnection() {
		@Override public void onServiceConnected(final ComponentName name, final IBinder service) {}
		@Override public void onServiceDisconnected(final ComponentName name) {}
	};


	private final CondomContext condom = CondomContext.wrap(new ContextWrapper(ApplicationProvider.getApplicationContext()) {
		@Override public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) { return true; }
		@Override public ComponentName startService(final Intent service) {
			return service.getComponent() != null ? service.getComponent() : new ComponentName(service.getPackage(), "A");
		}

		@Override public PackageManager getPackageManager() {
			return new PackageManagerWrapper(super.getPackageManager()) {
				@Override public List<ResolveInfo> queryIntentServices(final Intent intent, final int flags) {
					final List<ResolveInfo> resolves = new ArrayList<>();
					final String ime = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
					if (ime != null) try {
						final String ime_pkg = ComponentName.unflattenFromString(ime).getPackageName();
						final int uid = getPackageManager().getPackageUid(ime_pkg, 0);
						resolves.add(buildResolveInfo("bg.service.1", 999999999));		// Simulate a background UID.
						resolves.add(buildResolveInfo("non.bg.service", uid));
						resolves.add(buildResolveInfo("bg.service.2", 88888888));
					} catch (final NameNotFoundException ignored) {}	// Should hardly happen
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

	private static final int EVENT_TAG_MARK = "Condom".hashCode() + 999;
	private static final String TAG = "Test";
}
