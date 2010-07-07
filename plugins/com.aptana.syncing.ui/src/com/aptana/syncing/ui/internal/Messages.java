package com.aptana.syncing.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.aptana.syncing.ui.internal.messages"; //$NON-NLS-1$
	public static String SyncUIManager_CompletedDialog_Message;
	public static String SyncUIManager_CompletedDialog_Title;
	public static String SyncUIManager_downloaded;
	public static String SyncUIManager_Opening_0;
	public static String SyncUIManager_uploaded;
	public static String SyncViewerLabelProvider_0_items;
	public static String SyncViewerLabelProvider_empty;
	public static String SyncViewerLabelProvider_unknown;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
