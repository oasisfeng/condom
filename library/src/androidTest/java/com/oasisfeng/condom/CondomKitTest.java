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
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.test.InstrumentationRegistry;
import android.telephony.TelephonyManager;

import com.oasisfeng.condom.kit.NullDeviceIdKit;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_SECURE_SETTINGS;
import static android.Manifest.permission.WRITE_SETTINGS;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test cases for {@link CondomKit}
 *
 * Created by Oasis on 2017/7/22.
 */
public class CondomKitTest {

	@Test public void testBasicKit() throws ReflectiveOperationException, NameNotFoundException {
		final ActivityManager am = createActivityManager(context);
		final CondomOptions option = new CondomOptions().addKit(new CondomKit() { @Override public void onRegister(final CondomKitRegistry registry) {
			registry.registerSystemService(Context.ACTIVITY_SERVICE, new SystemServiceSupplier() { @Override public Object getSystemService(final Context context, final String name) {
				return am;
			}});
			registry.addPermissionSpoof(WRITE_SETTINGS);
			registry.addPermissionSpoof(ACCESS_COARSE_LOCATION);
		}});
		final CondomContext condom = CondomContext.wrap(new ContextWrapper(context), "KitTest", option);

		assertEquals(am, condom.getSystemService(Context.ACTIVITY_SERVICE));
		assertEquals(am, condom.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE));
		Assert.assertNotNull(condom.getSystemService(Context.NOTIFICATION_SERVICE));	// Service not registered in kit

		assertPermission(condom, WRITE_SETTINGS, true);
		assertPermission(condom.getApplicationContext(), WRITE_SETTINGS, true);
		assertPermission(condom, ACCESS_COARSE_LOCATION, true);
		assertPermission(condom.getApplicationContext(), ACCESS_COARSE_LOCATION, true);
		assertPermission(condom, WRITE_SECURE_SETTINGS, false);					// Permission not registered to spoof in kit
		assertPermission(condom.getApplicationContext(), WRITE_SECURE_SETTINGS, false);
	}

	@Test @SuppressLint("HardwareIds") public void testNullDeviceIdKit() throws NameNotFoundException {
		final CondomContext condom = CondomContext.wrap(new ContextWrapper(context), "NullDeviceId",
				new CondomOptions().addKit(new NullDeviceIdKit()));
		final TelephonyManager tm = (TelephonyManager) condom.getSystemService(Context.TELEPHONY_SERVICE);
		assertTrue(condom.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE).getClass().getName().startsWith(NullDeviceIdKit.class.getName()));

		assertPermission(condom, READ_PHONE_STATE, true);

		assertNull(tm.getDeviceId());
		if (SDK_INT >= M) assertNull(tm.getDeviceId(0));
		assertNull(tm.getImei());
		assertNull(tm.getImei(0));
		if (SDK_INT >= O) assertNull(tm.getMeid());
		if (SDK_INT >= O) assertNull(tm.getMeid(0));
		assertNull(tm.getSimSerialNumber());
		assertNull(tm.getLine1Number());
		assertNull(tm.getSubscriberId());
	}

	private static void assertPermission(final Context context, final String permission, final boolean granted) throws NameNotFoundException {
		final int state = granted ? PERMISSION_GRANTED : PERMISSION_DENIED;
		assertEquals(state, context.checkPermission(permission, Process.myPid(), Process.myUid()));
		if (SDK_INT >= M) assertEquals(state, context.checkSelfPermission(permission));

		final PackageInfo pkg_info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
		if (! granted) return;
		assertNotNull(pkg_info.requestedPermissions);
		for (int i = 0; i < pkg_info.requestedPermissions.length; i++)
			if (permission.equals(pkg_info.requestedPermissions[i])) {
				assertEquals("Not granted: " + permission, PackageInfo.REQUESTED_PERMISSION_GRANTED, pkg_info.requestedPermissionsFlags[i]);
				return;
			}
		fail(permission + " is not in requested permissions: " + Arrays.deepToString(pkg_info.requestedPermissions));
	}

	private static ActivityManager createActivityManager(final Context context) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		final Constructor<ActivityManager> am_constructor = ActivityManager.class.getDeclaredConstructor(Context.class, Handler.class);
		am_constructor.setAccessible(true);
		return am_constructor.newInstance(context, new Handler(Looper.getMainLooper()));
	}

	private final Context context = InstrumentationRegistry.getTargetContext();
}
