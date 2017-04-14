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
import android.content.Context;
import android.content.ContextWrapper;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;

public class CondomContextBasicSemanticTest {

	@Test public void testApplicationAsApplicationContextOfBaseContext() throws Exception {
		final Context context = InstrumentationRegistry.getTargetContext();
		final Context app_context = context.getApplicationContext();
		assertTrue(app_context instanceof Application);
		final CondomContext condom_context = CondomContext.wrap(context, TAG);
		final Context condom_app_context = condom_context.getApplicationContext();
		assertTrue(condom_app_context instanceof Application);
		assertNotSame(app_context, condom_app_context);
		assertTrue(((Application) condom_app_context).getBaseContext() instanceof CondomContext);
	}

	@Test public void testNonApplicationAsApplicationContextOfBaseContext() throws Exception {
		final Context context = InstrumentationRegistry.getTargetContext();
		final ContextWrapper context_wo_app = new ContextWrapper(context) {
			@Override public Context getApplicationContext() {
				return new ContextWrapper(context);
			}
		};
		final CondomContext condom_context = CondomContext.wrap(context_wo_app, TAG);
		final Context condom_app_context = condom_context.getApplicationContext();
		assertFalse(condom_app_context instanceof Application);
		assertTrue(condom_app_context instanceof CondomContext);
	}

	@Test public void testApplicationContextAsBaseContext() throws Exception {
		final Context context = InstrumentationRegistry.getTargetContext();
		final Context app_context = context.getApplicationContext();
		assertTrue(app_context instanceof Application);
		final CondomContext condom_context = CondomContext.wrap(app_context, TAG);
		final Context condom_app_context = condom_context.getApplicationContext();
		assertTrue(condom_app_context instanceof Application);
		assertEquals(condom_context, ((Application) condom_app_context).getBaseContext());
		assertTrue(((Application) condom_app_context).getBaseContext() instanceof CondomContext);
	}

	@Test public void testNonApplicationRootContextAsBaseContext() throws Exception {
		final Context context = InstrumentationRegistry.getTargetContext();
		final ContextWrapper context_wo_app = new ContextWrapper(context) {
			@Override public Context getApplicationContext() { return this; }
		};
		final CondomContext condom_context = CondomContext.wrap(context_wo_app, TAG);
		final Context condom_app_context = condom_context.getApplicationContext();
		assertFalse(condom_app_context instanceof Application);
		assertEquals(condom_context, condom_app_context);
	}

	private static final String TAG = "Test.Semantic";
}
