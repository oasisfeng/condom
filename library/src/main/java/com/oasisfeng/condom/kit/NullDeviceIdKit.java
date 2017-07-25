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

package com.oasisfeng.condom.kit;

import android.Manifest;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.oasisfeng.condom.BuildConfig;
import com.oasisfeng.condom.CondomKit;

import static android.telephony.PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR;
import static android.telephony.PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR;
import static android.telephony.PhoneStateListener.LISTEN_SIGNAL_STRENGTH;

/**
 * CondomKit to block access to IMEI.
 *
 * Created by Oasis on 2017/7/21.
 */
public class NullDeviceIdKit implements CondomKit, CondomKit.SystemServiceSupplier {

	@Override public void onRegister(final CondomKitRegistry registry) {
		registry.addPermissionSpoof(Manifest.permission.READ_PHONE_STATE);
		registry.registerSystemService(Context.TELEPHONY_SERVICE, this);
	}

	@Override public Object getSystemService(final Context context, final String name) {
		if (Context.CARRIER_CONFIG_SERVICE.equals(name)) throw new UnsupportedOperationException("CarrierConfigManager is not supported");
		return Context.TELEPHONY_SERVICE.equals(name) ? new CondomTelephonyManager(context) : null;
	}

	class CondomTelephonyManager extends TelephonyManager {

		private static final int UNSUPPORTED_LISTEN_EVENTS = LISTEN_SIGNAL_STRENGTH | LISTEN_MESSAGE_WAITING_INDICATOR | LISTEN_CALL_FORWARDING_INDICATOR;

		@Override public String getDeviceId() { return null; }
		@Override public String getDeviceId(final int slotIndex) { return null; }
		@Override public String getImei() { return null; }
		@Override public String getImei(final int slotIndex) { return null; }
		@Override public String getMeid() { return null; }
		@Override public String getMeid(final int slotIndex) { return null; }
		@Override public String getSimSerialNumber() { return null; }
		@Override public String getSimSerialNumber(final int slotIndex) { return null; }
		@Override public String getLine1Number() { return null; }
		@Override public String getLine1Number(final int slotIndex) { return null; }
		@Override public String getSubscriberId() { return null; }
		@Override public String getSubscriberId(final int slotIndex) { return null; }

		@Override public void listen(final PhoneStateListener listener, final int events) {
			if ((events & UNSUPPORTED_LISTEN_EVENTS) != 0) {
				if (BuildConfig.DEBUG) throw new UnsupportedOperationException("One of the event type is not supported due to permission READ_PHONE_STATE required: " + events);
				super.listen(listener, events & ~ UNSUPPORTED_LISTEN_EVENTS);
			} else super.listen(listener, events);
		}

		CondomTelephonyManager(final Context context) { super(context); }
	}
}
