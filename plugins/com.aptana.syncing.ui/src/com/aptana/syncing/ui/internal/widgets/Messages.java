package com.aptana.syncing.ui.internal.widgets;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.aptana.syncing.ui.internal.widgets.messages"; //$NON-NLS-1$
	public static String SyncStatsComposite_and;
	public static String SyncStatsComposite_file;
	public static String SyncStatsComposite_files;
	public static String SyncStatsComposite_fodlers;
	public static String SyncStatsComposite_folder;
	public static String SyncStatsComposite_LocalChanges;
	public static String SyncStatsComposite_None;
	public static String SyncStatsComposite_RemoteChanges;
	public static String SyncStatsComposite_ToBeCreated_0;
	public static String SyncStatsComposite_ToBeDeleted_0;
	public static String SyncStatsComposite_ToBeDownloaded_0;
	public static String SyncStatsComposite_ToBeUploaded_0;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
