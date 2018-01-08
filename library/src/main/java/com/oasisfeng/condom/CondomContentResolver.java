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

import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.PackageManager;

/**
 * Condom wrapper of {@link ContentResolver}
 *
 * Created by Oasis on 2018/1/6.
 */
class CondomContentResolver extends ContentResolverWrapper {

	@Override public IContentProvider acquireUnstableProvider(final Context context, final String name) {
		if (! mCondom.shouldAllowProvider(context, name, PackageManager.MATCH_ALL)) return null;
		return super.acquireUnstableProvider(context, name);
	}

	@Override public IContentProvider acquireProvider(final Context context, final String name) {
		if (! mCondom.shouldAllowProvider(context, name, PackageManager.MATCH_ALL)) return null;
		return super.acquireProvider(context, name);
	}

	CondomContentResolver(final CondomCore condom, final Context context, final ContentResolver base) {
		super(context, base);
		mCondom = condom;
	}

	private final CondomCore mCondom;
}
