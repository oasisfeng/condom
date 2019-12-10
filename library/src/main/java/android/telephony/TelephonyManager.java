/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.telephony;

import android.content.Context;
import androidx.annotation.RequiresApi;

import static android.os.Build.VERSION_CODES.O;

/**
 * Stub class for compilation purpose.
 *
 * Created by Oasis on 2017/7/21.
 */
public class TelephonyManager {

	public TelephonyManager(final Context context) {
		throw new RuntimeException("Stub!");
	}

	public String getDeviceId() {
		throw new RuntimeException("Stub!");
	}

	public String getDeviceId(final int slotIndex) {
		throw new RuntimeException("Stub!");
	}

	public String getImei() {
		throw new RuntimeException("Stub!");
	}

	public String getImei(final int slotIndex) {
		throw new RuntimeException("Stub!");
	}

	@RequiresApi(O) public String getMeid() {
		throw new RuntimeException("Stub!");
	}

	@RequiresApi(O) public String getMeid(final int slotIndex) {
		throw new RuntimeException("Stub!");
	}

	public String getSimSerialNumber() {
		throw new RuntimeException("Stub!");
	}

	public String getSimSerialNumber(final int subId) {
		throw new RuntimeException("Stub!");
	}

	public String getLine1Number() {
		throw new RuntimeException("Stub!");
	}

	public String getLine1Number(final int subId) {
		throw new RuntimeException("Stub!");
	}

	public String getSubscriberId() {
		throw new RuntimeException("Stub!");
	}

	public String getSubscriberId(final int subId) {
		throw new RuntimeException("Stub!");
	}

	public void listen(final PhoneStateListener listener, final int events) {
		throw new RuntimeException("Stub!");
	}

}
