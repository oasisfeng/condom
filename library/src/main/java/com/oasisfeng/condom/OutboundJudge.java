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

/**
 * The callback for outbound request filtering.
 *
 * Created by Oasis on 2017/4/21.
 */
@Keep
public interface OutboundJudge {
	/**
	 * Judge the outbound request or query by intent and its target package, which may or may not be explicit in intent.
	 *
	 * <p>For query requests (including {@link PackageManager#resolveService(Intent, int)}, {@link PackageManager#queryIntentServices(Intent, int)}
	 * and {@link PackageManager#queryBroadcastReceivers(Intent, int)}), this will be called for each candidate,
	 * before additional filtering (e.g. {@link CondomOptions#preventServiceInBackgroundPackages(boolean)}) is applied.
	 *
	 * <p>Note: Implicit broadcast will never go through this.
	 *
	 * @param type the type of outbound request or query being judged
	 * @param intent the intent of current request or query, or null if unavailable (e.g. content provider access).
	 * @param target_package the target package of current request or candidate package of current query, may or may not be explicit in intent.
	 * @return whether this outbound request should be allowed, or whether the query result entry should be included in the returned collection.
	 *         Disallowed service request will simply fail while disallowed broadcast target will be skipped.
	 */
	boolean shouldAllow(OutboundType type, final @Nullable Intent intent, String target_package);
}
