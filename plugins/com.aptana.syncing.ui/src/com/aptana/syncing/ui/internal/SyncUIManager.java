/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.syncing.ui.internal;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.progress.WorkbenchJob;

import com.aptana.ide.syncing.core.ISiteConnection;
import com.aptana.ide.syncing.core.SyncingPlugin;
import com.aptana.ide.syncing.ui.SyncingUIPlugin;
import com.aptana.syncing.core.model.ISyncSession;
import com.aptana.syncing.core.model.ISyncSession.Stage;
import com.aptana.syncing.ui.dialogs.SyncDialog;
import com.aptana.syncing.ui.dialogs.SyncProgressDialog;
import com.aptana.ui.PopupSchedulingRule;

/**
 * @author Max Stepanov
 *
 */
public final class SyncUIManager {

	private static final QualifiedName PROP_SESSION = new QualifiedName(SyncUIManager.class.getName(), "session");
	
	private static SyncUIManager instance;
	private Set<ISyncSession> uiSessions = new HashSet<ISyncSession>();
	
	/**
	 * 
	 */
	private SyncUIManager() {
	}

	public static SyncUIManager getInstance() {
		if (instance == null) {
			instance = new SyncUIManager();
		}
		return instance;
	}
	
	public void initiateSynchronization(ISiteConnection siteConnection, boolean showUI) {
		ISyncSession session = SyncingPlugin.getSyncManager().getSyncSession(siteConnection);
		if (session == null) {
			session = SyncingPlugin.getSyncManager().createSyncSession(siteConnection);
			Job job = SyncingPlugin.getSyncManager().runFetchTree(session);
			setupJob(job, session);
			if (!showUI) {
				return;
			}
		}
		showUI(session);
	}

	public void startSynchronization(ISyncSession session, boolean showUI) {
		Job job = SyncingPlugin.getSyncManager().synchronize(session);
		setupJob(job, session);
		if (showUI) {
			showUI(session);
		}
	}

	public synchronized void onCloseUI(ISyncSession session) {
		uiSessions.remove(session);
	}
	
	private void setupJob(final Job job, ISyncSession session) {
		job.setProperty(PROP_SESSION, session);
		job.setProperty(IProgressConstants.ACTION_PROPERTY, new Action() {
			@Override
			public void run() {
				showUI(job);
			}
		});
		job.setProperty(IProgressConstants.ICON_PROPERTY, SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/sync.png"));
		job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					WorkbenchJob uiJob = new WorkbenchJob(MessageFormat.format("Opening {0}", SyncDialog.TITLE)) {
						@Override
						public IStatus runInUIThread(IProgressMonitor monitor) {
							showUI(event.getJob());
							return Status.OK_STATUS;
						}
					};
					uiJob.setSystem(true);
					uiJob.setPriority(Job.INTERACTIVE);
					uiJob.setRule(PopupSchedulingRule.INSTANCE);
					uiJob.schedule();
				}
			}
		});		
	}
	
	private void showUI(Job job) {
		showUI((ISyncSession) job.getProperty(PROP_SESSION));
	}
	
	private synchronized void showUI(final ISyncSession session) {
		if (uiSessions.contains(session)) {
			return;
		}
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		if (session.getStage() == Stage.SYNCING || session.getStage() == Stage.SYNCED) {
			SyncProgressDialog dlg = new SyncProgressDialog(shell);
			dlg.setSession(session);
			uiSessions.add(session);
			dlg.open();
		} else {
			SyncDialog dlg = new SyncDialog(shell);
			dlg.setSession(session);
			uiSessions.add(session);
			if (dlg.open() == Window.OK) {
				startSynchronization(session, false);
			}
		}
	}
}
