package com.oasisfeng.condom.ext;

import android.content.Context;
import android.content.pm.PackageManager;

import com.oasisfeng.condom.PackageManagerWrapper;

/**
 * Extend the functionality of {@link com.oasisfeng.condom.CondomPackageManager} by composition with {@link com.oasisfeng.condom.PackageManagerWrapper}.
 *
 * Created by Oasis on 2019-1-14.
 */
public interface PackageManagerFactory {

	/**
	 * Create a custom PackageManager instance, which will be returned to the caller of {@link com.oasisfeng.condom.CondomContext CondonContext}.
	 *
	 * @param base the base <code>Context</code>, not the <code>CondomContext</code>
	 * @param downstream the PackageManager instance with condom, which should be passed to the constructor of returning <code>PackageManagerWrapper</code>.
	 */
	PackageManagerWrapper getPackageManager(Context base, PackageManager downstream);
}
