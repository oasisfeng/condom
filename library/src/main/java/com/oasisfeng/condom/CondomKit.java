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

/**
 * Kit interface to extend the functionality of Condom.
 *
 * Created by Oasis on 2017/7/21.
 */
public interface CondomKit {

	interface SystemServiceSupplier {
		/** @return the system service instance (may be cached by caller if appropriate). */
		Object getSystemService(final Context context, String name);
	}

	interface CondomKitRegistry {
		void addPermissionSpoof(String permission);
		void registerSystemService(String name, SystemServiceSupplier supplier);
	}

	/**
	 * Register desired functionality with the methods in {@link CondomKitRegistry}.
	 * The registry instance must never be used outside this method.
	 */
	void onRegister(final CondomKitRegistry registry);
}
