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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;

import com.oasisfeng.condom.ext.PackageManagerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The options for condom initialization.
 *
 * Created by Oasis on 2017/4/22.
 */
@Keep
public class CondomOptions {

	/**
	 * Prevent broadcast to be delivered to manifest receivers in background (cached or not running) apps. (default: true)
	 *
	 * <p>This restriction is supported natively since Android O, and it works similarly by only targeting registered receivers on previous Android versions.
	 */
	public CondomOptions preventBroadcastToBackgroundPackages(final boolean prevent_or_not) { mExcludeBackgroundReceivers = prevent_or_not; return this; }

	/**
	 * Prevent service in background (cached or not running) apps to be discovered via {@link PackageManager#queryIntentServices(Intent, int)}
	 * or {@link PackageManager#resolveService(Intent, int)}. (default: true)
	 *
	 * <p>This restriction is supported natively since Android O, and it works similarly by dropping candidate services in background packages.
	 */
	public CondomOptions preventServiceInBackgroundPackages(final boolean prevent_or_not) { mExcludeBackgroundServices = prevent_or_not; return this; }

	/** Set a custom judge for the explicit target package of outbound service and broadcast requests. */
	public CondomOptions setOutboundJudge(final OutboundJudge judge) { mOutboundJudge = judge; return this; }

	public CondomOptions setPackageManagerFactory(final PackageManagerFactory factory) { mPackageManagerFactory = factory; return this; }

	/** Set to dry-run mode to inspect the outbound wake-up only, no outbound requests will be actually blocked. */
	public CondomOptions setDryRun(final boolean dry_run) { mDryRun = dry_run; return this; }

	public CondomOptions addKit(final CondomKit kit) {
		if (mKits == null) mKits = new ArrayList<>();
		mKits.add(kit);
		return this;
	}

	boolean mDryRun;
	@Nullable OutboundJudge mOutboundJudge;
	@Nullable PackageManagerFactory mPackageManagerFactory;
	boolean mExcludeBackgroundReceivers = true;
	boolean mExcludeBackgroundServices = true;
	@Nullable List<CondomKit> mKits;
}
