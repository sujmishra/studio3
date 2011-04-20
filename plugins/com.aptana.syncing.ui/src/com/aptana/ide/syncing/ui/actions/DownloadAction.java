/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ide.syncing.ui.actions;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.aptana.core.io.efs.EFSUtils;
import com.aptana.core.util.StringUtil;
import com.aptana.ide.syncing.core.ISiteConnection;
import com.aptana.ide.ui.io.Utils;
import com.aptana.syncing.core.model.SyncOperation;
import com.aptana.syncing.ui.internal.SyncUIManager;

/**
 * @author Michael Xia (mxia@aptana.com)
 * @author Max Stepanov
 */
public class DownloadAction extends BaseSyncAction {

	private IJobChangeListener jobListener = null;

	private static String MESSAGE_TITLE = StringUtil.ellipsify(Messages.DownloadAction_MessageTitle);

	protected void performAction(final IAdaptable[] files, ISiteConnection siteConnection) throws CoreException {
		IPath[] paths = new IPath[files.length];
		for (int i = 0; i < files.length; ++i) {
			paths[i] = EFSUtils.getRelativePath(siteConnection.getSource(), Utils.getFileStore(files[i]));
		}
		Job job = SyncUIManager.getInstance().initiateOperation(siteConnection, paths, SyncOperation.DOWNLOAD);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				refreshWorkspaceResources(files);
			}
		});
		if (jobListener != null) {
			job.addJobChangeListener(jobListener);
		}		
	}

	public void addJobListener(IJobChangeListener listener) {
		jobListener = listener;
	}

	protected String getMessageTitle() {
		return MESSAGE_TITLE;
	}
}
