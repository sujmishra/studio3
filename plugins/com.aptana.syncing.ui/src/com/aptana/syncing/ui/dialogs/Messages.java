package com.aptana.syncing.ui.dialogs;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.aptana.syncing.ui.dialogs.messages"; //$NON-NLS-1$
	public static String SyncDialog_Actioin_OutgoingOnly;
	public static String SyncDialog_Action_All;
	public static String SyncDialog_Action_ConflictsOnly;
	public static String SyncDialog_Action_ExpandAll;
	public static String SyncDialog_Action_ExpandSelected;
	public static String SyncDialog_Action_FlatMode;
	public static String SyncDialog_Action_HideIdenticalFiles;
	public static String SyncDialog_Action_IncomingOnly;
	public static String SyncDialog_Action_ShowDiff;
	public static String SyncDialog_Message;
	public static String SyncDialog_RunInBackground;
	public static String SyncDialog_StopConfirmation;
	public static String SyncDialog_StopMessage;
	public static String SyncDialog_SynchronizeButton;
	public static String SyncDialog_Table_File;
	public static String SyncDialog_Table_LocalSize;
	public static String SyncDialog_Table_LocalTime;
	public static String SyncDialog_Table_RemoteSize;
	public static String SyncDialog_Table_RemoteTime;
	public static String SyncDialog_Table_State;
	public static String SyncDialog_Title;
	public static String SyncDialog_WindowTitle;
	public static String SyncProgressDialog_BackButton;
	public static String SyncProgressDialog_CloseButton;
	public static String SyncProgressDialog_Message;
	public static String SyncProgressDialog_RunInBackground;
	public static String SyncProgressDialog_StopConfirmation;
	public static String SyncProgressDialog_StopMessage;
	public static String SyncProgressDialog_Table_File;
	public static String SyncProgressDialog_Table_State;
	public static String SyncProgressDialog_Title;
	public static String SyncProgressDialog_WindowTitle;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
