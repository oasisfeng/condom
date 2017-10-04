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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;

import com.oasisfeng.condom.kit.NullDeviceIdKit;
import com.oasisfeng.condom.simulation.TestApplication;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.ParametersAreNonnullByDefault;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

@ParametersAreNonnullByDefault
public class CondomProcessTest {

	@Test public void testBindService() {
		runInSeparateProcess(new TestService.Procedure() { @Override public void run(final Context context) {
			final Intent intent = new Intent(context, TestService.class);
			final ServiceConnection connection = new ServiceConnection() {
				@Override public void onServiceConnected(final ComponentName name, final IBinder service) {}
				@Override public void onServiceDisconnected(final ComponentName name) {}
			};
			assertTrue("Test service is not properly setup.", context.bindService(intent, connection, Context.BIND_AUTO_CREATE));
			context.unbindService(connection);

			installCondomProcess(context, new CondomOptions().setOutboundJudge(sBlockAllJudge));

			withFakeSelfPackageName(new Runnable() { @Override public void run() {
				assertFalse(context.bindService(intent, connection, Context.BIND_AUTO_CREATE));
			}});	// Block by outbound judge
			context.unbindService(connection);
		}});
		// TODO: More cases
	}

	@Test public void testStartService() {
		runInSeparateProcess(new TestService.Procedure() { @Override public void run(final Context context) {
			final Intent intent = new Intent(context, TestService.class);
			assertNotNull("Test service is not properly setup.", context.startService(intent));
			assertTrue(context.stopService(intent));

			installCondomProcess(context, new CondomOptions().setOutboundJudge(sBlockAllJudge));

			withFakeSelfPackageName(new Runnable() { @Override public void run() {
				assertNull(context.startService(intent));
			}});	// Block by outbound judge
			assertTrue(context.stopService(intent));
		}});
		// TODO: More cases
	}

	private static final String ACTION_TEST = "TEST";

	@Test public void testBroadcast() {
		runInSeparateProcess(new TestService.Procedure() { @Override public void run(final Context context) {
			testOrderedBroadcast(context, new Intent(ACTION_TEST).setPackage(context.getPackageName()), true);

			installCondomProcess(context, new CondomOptions().setOutboundJudge(sBlockAllJudge));

			context.sendBroadcast(new Intent(ACTION_TEST).setPackage(context.getPackageName()));	// Ensure no exception

			testOrderedBroadcast(context, new Intent(ACTION_TEST), true);
			testOrderedBroadcast(context, new Intent(ACTION_TEST).setPackage(context.getPackageName()), true);	// Self targeted should always be allowed.
			withFakeSelfPackageName(new Runnable() { @Override public void run() {
				testOrderedBroadcast(context, new Intent(ACTION_TEST).setPackage(context.getPackageName()), false);
			}});
		}});
	}

	private static void testOrderedBroadcast(final Context context, final Intent intent, final boolean expected_pass) {
		final BroadcastReceiver responder = new BroadcastReceiver() { @Override public void onReceive(final Context context, final Intent intent) {
			setResultCode(Activity.RESULT_OK);
		}};
		if (intent.getAction() != null) context.registerReceiver(responder, new IntentFilter(intent.getAction()));

		try {
			final SettableFuture<Integer> future = new SettableFuture<>();
			context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() { @Override public void onReceive(final Context context, final Intent intent) {
				future.set(getResultCode());
			}}, null, Activity.RESULT_CANCELED, null, null);
			final int result = waitForCompletion(future);
			assertEquals(expected_pass ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
		} finally {
			if (intent.getAction() != null) context.unregisterReceiver(responder);
		}
	}

	@Test public void testQuery() {
		runInSeparateProcess(new TestService.Procedure() { @Override public void run(final Context context) {
			installCondomProcess(context, new CondomOptions());		// Must be installed before the first call to Context.getPackageManager().

			final Intent service_intent = new Intent("android.view.InputMethod").addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES/* For consistency */);
			final ResolveInfo service = context.getPackageManager().resolveService(service_intent, 0);
			Assume.assumeNotNull(service);

			final List<ResolveInfo> services = context.getPackageManager().queryIntentServices(service_intent, 0);
			assertNotNull(services);
			assertFalse(services.isEmpty());

			final List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(new Intent(Intent.ACTION_BOOT_COMPLETED), 0);
			assertNotNull(receivers);
			assertFalse(receivers.isEmpty());

			installCondomProcess(context, new CondomOptions().setOutboundJudge(sBlockAllJudge));

			assertNull(context.getPackageManager().resolveService(service_intent, 0));
			List<ResolveInfo> result = context.getPackageManager().queryIntentServices(service_intent, 0);
			assertTrue(result.isEmpty());

			service_intent.setPackage(service.serviceInfo.packageName);
			assertNull(context.getPackageManager().resolveService(service_intent, 0));
			result = context.getPackageManager().queryIntentServices(service_intent, 0);
			assertTrue(result.isEmpty());

			service_intent.setPackage(null);
		}});
	}

	@Test public void testProvider() {
		runInSeparateProcess(new TestService.Procedure() { @Override public void run(final Context context) {
			final ContentResolver resolver = context.getContentResolver();
			// Regular provider access
			final String android_id = Settings.Secure.getString(resolver, Settings.System.ANDROID_ID);
			assertNotNull(android_id);
			final ContentProviderClient client = resolver.acquireContentProviderClient(Settings.AUTHORITY);
			assertNotNull(client);
			client.release();

			installCondomProcess(context, new CondomOptions().setOutboundJudge(sBlockAllJudge));

			assertNull(resolver.acquireContentProviderClient("downloads"));
		}});
	}

	@Test public void testCondomKitSetup() {
		runInSeparateProcess(new TestService.Procedure() { @Override public void run(final Context context) {
			try {
				installCondomProcess(context, new CondomOptions().addKit(new NullDeviceIdKit()));
				fail("CondomKit is incompatible with CondomProcess");
			} catch (final IllegalArgumentException ignored) {}
		}});
	}

	@Before public void prepare() throws InterruptedException {		// Ensure the separate process is always cleanly started.
		final Context context = context();
		//noinspection ConstantConditions
		((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).killBackgroundProcesses(context().getPackageName());
		if (! context.bindService(new Intent(context, TestService.class), mServiceConnection, Context.BIND_AUTO_CREATE))
			throw new IllegalStateException("TestService is not properly setup");
		while (mServiceBinder == null) Thread.sleep(20);
	}

	@After public void release() {
		context().unbindService(mServiceConnection);
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override public void onServiceConnected(final ComponentName name, final IBinder service) {
			mServiceBinder = service;
		}

		@Override public void onServiceDisconnected(final ComponentName name) {
			mServiceBinder = null;
		}
	};

	private void runInSeparateProcess(final TestService.Procedure procedure) {
		//noinspection ConstantConditions
		TestService.invokeService(mServiceBinder, procedure);
	}

	private static void installCondomProcess(final Context context, final CondomOptions options) {
		CondomProcess.installExceptDefaultProcess((Application) context.getApplicationContext(), options);
	}

	private static void withFakeSelfPackageName(final Runnable runnable) {
		TestApplication.sEnablePackageNameFake = true;
		runnable.run();
		TestApplication.sEnablePackageNameFake = false;
	}

	private static <T> T waitForCompletion(final SettableFuture<T> future) {
		try {
			return future.get(3, TimeUnit.SECONDS);
		} catch (final InterruptedException | TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

	private static Context context() { return InstrumentationRegistry.getTargetContext(); }

	private static final OutboundJudge sBlockAllJudge = new OutboundJudge() { @Override public boolean shouldAllow(final OutboundType type, final @Nullable Intent intent, final String target_pkg) {
		return false;
	}};

	private volatile IBinder mServiceBinder;

	final static class SettableFuture<T> implements Future<T> {

		@Override public boolean cancel(final boolean mayInterruptIfRunning) {
			return false;
		}

		@Override public boolean isCancelled() {
			return false;
		}

		@Override public boolean isDone() {
			return latch.getCount() == 0;
		}

		@Override public T get() throws InterruptedException {
			latch.await();
			return value;
		}

		@Override public T get(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
			if (latch.await(timeout, unit)) return value;
			else throw new TimeoutException();
		}

		void set(final T result) {
			value = result;
			latch.countDown();
		}

		private T value;
		private final CountDownLatch latch = new CountDownLatch(1);
	}
}
