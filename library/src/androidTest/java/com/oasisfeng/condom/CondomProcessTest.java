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

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;

import com.oasisfeng.condom.kit.NullDeviceIdKit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@ParametersAreNonnullByDefault
public class CondomProcessTest {

	@Test public void testBindService() {
		sCondomProcessPackageManager.mCondom.mOutboundJudge = mBlockAllJudge;
		final Intent intent = new Intent().setPackage("a.b.c");

		context().bindService(intent, SERVICE_CONNECTION, Context.BIND_AUTO_CREATE);
		assertOutboundJudgeCalled(1);
		assertNotNull(mIntent);
		// TODO: More cases
	}

	@Test public void testStartService() {
		sCondomProcessPackageManager.mCondom.mOutboundJudge = mBlockAllJudge;
		final Intent intent = new Intent().setPackage("a.b.c");

		context().startService(intent);
		assertOutboundJudgeCalled(1);
		assertNotNull(mIntent);
		// TODO: More cases
	}

	@Test public void testBroadcast() {
		sCondomProcessPackageManager.mCondom.mOutboundJudge = mBlockAllJudge;
		final Intent intent = new Intent();

		mIntent = null;
		context().sendBroadcast(intent);
		assertOutboundJudgeCalled(0);
		assertNull(mIntent);

		mIntent = null;
		context().sendBroadcast(new Intent(intent.setPackage("a.b.c")));
		assertOutboundJudgeCalled(1);
		assertNotNull(mIntent);
		assertTrue(mIntent.filterEquals(intent));
	}

	@Test public void testQuery() {
		final Context context = context();
		final Intent service_intent = new Intent("android.view.InputMethod").addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES/* For consistency */);
		final ResolveInfo service = context.getPackageManager().resolveService(service_intent, 0);
		assertNotNull(service);
		final List<ResolveInfo> services = context.getPackageManager().queryIntentServices(service_intent, 0);
		assertNotNull(services);
		assertFalse(services.isEmpty());

		final List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(new Intent(Intent.ACTION_BOOT_COMPLETED), 0);
		assertNotNull(receivers);
		assertFalse(receivers.isEmpty());

		sCondomProcessPackageManager.mCondom.mOutboundJudge = mBlockAllJudge;

		assertNull(context.getPackageManager().resolveService(service_intent, 0));
		assertOutboundJudgeCalled(services.size());		// Outbound judge should have been called for each candidates.
		List<ResolveInfo> result = context.getPackageManager().queryIntentServices(service_intent, 0);
		assertTrue(result.isEmpty());
		assertOutboundJudgeCalled(services.size());

		service_intent.setPackage(service.serviceInfo.packageName);
		assertNull(context.getPackageManager().resolveService(service_intent, 0));
		assertOutboundJudgeCalled(1);		// Only once for explicitly targeted intent
		result = context.getPackageManager().queryIntentServices(service_intent, 0);
		assertTrue(result.isEmpty());
		assertOutboundJudgeCalled(1);

		service_intent.setPackage(null);
	}

	@Test public void testProvider() {
		final ContentResolver resolver = context().getContentResolver();
		// Regular provider access
		final String android_id = Settings.System.getString(resolver, Settings.System.ANDROID_ID);
		assertNotNull(android_id);
		final ContentProviderClient client = resolver.acquireContentProviderClient(Settings.AUTHORITY);
		assertNotNull(client);
		client.release();

		sCondomProcessPackageManager.mCondom.mOutboundJudge = mBlockAllJudge;

		assertNull(resolver.acquireContentProviderClient("downloads"));
	}

	@Before public void reset() {
		sCondomProcessPackageManager.mCondom.mOutboundJudge = null;
		mIntent = null;
	}

	@BeforeClass public static void checkInstallation() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException {
		try {
			CondomProcess.installExcept(((Application) InstrumentationRegistry.getTargetContext().getApplicationContext()),
					new CondomOptions().addKit(new NullDeviceIdKit()), "");
			fail("CondomKit is incompatible with CondomProcess");
		} catch (final IllegalArgumentException ignored) {}

		// Install in default process intentionally, since test cases cannot run in secondary process.
		CondomProcess.installExcept(((Application) InstrumentationRegistry.getTargetContext().getApplicationContext()), new CondomOptions(), "");

		// Check IActivityManager proxy
		@SuppressLint("PrivateApi") final Object am_proxy = Class.forName("android.app.ActivityManagerNative").getMethod("getDefault").invoke(null);
		assertTrue(Proxy.isProxyClass(am_proxy.getClass()));
		sCondomProcessActivityManager = (CondomProcess.CondomProcessActivityManager) Proxy.getInvocationHandler(am_proxy);
		assertEquals(CondomProcess.CondomProcessActivityManager.class, sCondomProcessActivityManager.getClass());

		// Check IPackageManager proxy
		final PackageManager pm = context().getPackageManager();
		assertEquals("android.app.ApplicationPackageManager", pm.getClass().getName());
		final Field ApplicationPackageManager_mPm = pm.getClass().getDeclaredField("mPM");
		ApplicationPackageManager_mPm.setAccessible(true);
		final Object pm_proxy = ApplicationPackageManager_mPm.get(pm);
		assertTrue(Proxy.isProxyClass(pm_proxy.getClass()));
		sCondomProcessPackageManager = (CondomProcess.CondomProcessPackageManager) Proxy.getInvocationHandler(pm_proxy);
		assertEquals(CondomProcess.CondomProcessPackageManager.class, sCondomProcessPackageManager.getClass());
	}

	private void assertOutboundJudgeCalled(final int count) { assertEquals(count, mNumOutboundJudgeCalled.getAndSet(0)); }

	// The sContext returned by getTargetContext() is actually never accessible except in test cases.
	private static Context context() { return InstrumentationRegistry.getTargetContext().getApplicationContext(); }

	private final AtomicInteger mNumOutboundJudgeCalled = new AtomicInteger();
	private Intent mIntent;
	private final OutboundJudge mBlockAllJudge = new OutboundJudge() { @Override public boolean shouldAllow(final OutboundType type, final @Nullable Intent intent, final String target_pkg) {
		mIntent = intent;
		mNumOutboundJudgeCalled.incrementAndGet();
		return false;
	}};

	private static CondomProcess.CondomProcessActivityManager sCondomProcessActivityManager;
	private static CondomProcess.CondomProcessPackageManager sCondomProcessPackageManager;
	private static final ServiceConnection SERVICE_CONNECTION = new ServiceConnection() {
		@Override public void onServiceConnected(final ComponentName name, final IBinder service) {}
		@Override public void onServiceDisconnected(final ComponentName name) {}
	};

	private static final String TAG = CondomProcessTest.class.getSimpleName();
}
