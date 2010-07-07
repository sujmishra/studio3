package com.aptana.syncing.core.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.aptana.syncing.core.internal.messages"; //$NON-NLS-1$
	public static String SyncUtils_Copying_0;
	public static String SyncUtils_FailedRead_0;
	public static String SyncUtils_FailedWrite_0;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
