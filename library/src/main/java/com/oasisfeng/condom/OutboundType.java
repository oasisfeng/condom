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

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import androidx.annotation.Keep;

/**
 * The type of outbound request
 *
 * Created by Oasis on 2017/4/21.
 */
@Keep
public enum OutboundType {
	/** @see Context#startService(Intent) */
	START_SERVICE,
	/** @see Context#bindService(Intent, ServiceConnection, int) */
	BIND_SERVICE,
	/** Sending broadcast */
	BROADCAST,
	/** Requesting content provider */
	CONTENT,
	/** Either {@link PackageManager#queryIntentServices(Intent, int)} or {@link PackageManager#resolveService(Intent, int)} */
	QUERY_SERVICES,
	/** @see PackageManager#queryBroadcastReceivers(Intent, int) */
	QUERY_RECEIVERS,
	/** @see PackageManager#getPackagesForUid(int) */
	QUERY_PACKAGES,
	/** @see PackageManager#getApplicationInfo(String, int)  */
	GET_APPLICATION_INFO,
	/** @see PackageManager#getPackageInfo(String, int) */
	GET_PACKAGE_INFO,
	/** @see PackageManager#checkPermission(String, String) */
	CHECK_PERMISSION,
}
