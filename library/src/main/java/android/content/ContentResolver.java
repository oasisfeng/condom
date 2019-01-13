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

package android.content;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.P;

/**
 * Stub class of real ContentResolver, only for compilation purpose.
 *
 * Created by Oasis on 2017/4/11.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ContentResolver {

	/* Critical hidden APIs implemented by ContextImpl.ApplicationContentResolver. They must be overridden by ContentResolver forwarder. */

	public/* protected */abstract IContentProvider acquireProvider(Context c, String name);
	public/* protected */IContentProvider acquireExistingProvider(final Context c, final String name) { return acquireProvider(c, name); }
	public abstract boolean releaseProvider(IContentProvider icp);
	public/* protected */abstract IContentProvider acquireUnstableProvider(Context c, String name);
	public abstract boolean releaseUnstableProvider(IContentProvider icp);
	public abstract void unstableProviderDied(IContentProvider icp);
	public void appNotRespondingViaProvider(final IContentProvider icp) { throw new UnsupportedOperationException("appNotRespondingViaProvider"); }

	/* Pure stubs without final and static methods */

	public ContentResolver(Context context) {
		throw new RuntimeException("Stub!");
	}

	public String[] getStreamTypes(Uri url, String mimeTypeFilter) {
		throw new RuntimeException("Stub!");
	}

	public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations) throws RemoteException, OperationApplicationException {
		throw new RuntimeException("Stub!");
	}

	public void notifyChange(Uri uri, ContentObserver observer) {
		throw new RuntimeException("Stub!");
	}

	public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork) {
		throw new RuntimeException("Stub!");
	}

	@RequiresApi(N) public void notifyChange(Uri uri, ContentObserver observer, int flags) {
		throw new RuntimeException("Stub!");
	}

	@RequiresApi(KITKAT) public void takePersistableUriPermission(Uri uri, int modeFlags) {
		throw new RuntimeException("Stub!");
	}

	/** @hide */
	@RequiresApi(P) public void takePersistableUriPermission(String toPackage, Uri uri, int modeFlags) {
		throw new RuntimeException("Stub!");
	}

	@RequiresApi(KITKAT) public void releasePersistableUriPermission(Uri uri, int modeFlags) {
		throw new RuntimeException("Stub!");
	}

	@RequiresApi(KITKAT) public List<UriPermission> getPersistedUriPermissions() {
		throw new RuntimeException("Stub!");
	}

	@RequiresApi(KITKAT) public List<UriPermission> getOutgoingPersistedUriPermissions() {
		throw new RuntimeException("Stub!");
	}

	@Deprecated public void startSync(Uri uri, Bundle extras) {
		throw new RuntimeException("Stub!");
	}

	@Deprecated public void cancelSync(Uri uri) {
		throw new RuntimeException("Stub!");
	}
}
