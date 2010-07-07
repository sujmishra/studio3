package com.aptana.syncing.core.internal.model;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.aptana.syncing.core.internal.model.messages"; //$NON-NLS-1$
	public static String SyncManager_FetchingData;
	public static String SyncManager_Synchronizing;
	public static String SyncManager_SyncWorker_Name;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
