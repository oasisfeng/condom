package com.oasisfeng.condom.simulation;

import android.app.Application;

import com.oasisfeng.condom.CondomProcess;

/**
 * Mimic the real-world application, where {@link CondomProcess} is initialized.
 *
 * Created by Oasis on 2017/10/2.
 */
public class TestApplication extends Application {

	public static final String FAKE_PACKAGE_NAME = "a.b.c";

	@Override public String getPackageName() {
		return sEnablePackageNameFake ? FAKE_PACKAGE_NAME : super.getPackageName();
	}

	public static boolean sEnablePackageNameFake;
}
