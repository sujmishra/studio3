/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
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

package com.aptana.syncing.core.internal.model;

import java.util.WeakHashMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.aptana.core.progress.JobProgressMonitor;
import com.aptana.ide.syncing.core.ISiteConnection;
import com.aptana.syncing.core.model.ISyncManager;
import com.aptana.syncing.core.model.ISyncSession;

/**
 * @author Max Stepanov
 *
 */
public final class SyncManager implements ISyncManager {

	private static SyncManager instance;
	private WeakHashMap<ISiteConnection, ISyncSession> sessions = new WeakHashMap<ISiteConnection, ISyncSession>();
		
	/**
	 * 
	 */
	private SyncManager() {
	}
	
	public static SyncManager getInstance() {
		if (instance == null) {
			instance = new SyncManager();
		}
		return instance;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#getSyncSession(com.aptana.ide.syncing.core.ISiteConnection)
	 */
	@Override
	public ISyncSession getSyncSession(ISiteConnection siteConnection) {
		return sessions.get(siteConnection);
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#createSyncSession(com.aptana.ide.syncing.core.ISiteConnection)
	 */
	@Override
	public ISyncSession createSyncSession(ISiteConnection siteConnection) {
		ISyncSession session =  new SyncSession(siteConnection.getSource(), siteConnection.getDestination());
		sessions.put(siteConnection, session);
		return session;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#closeSession(com.aptana.syncing.core.model.ISyncSession)
	 */
	@Override
	public void closeSession(ISyncSession session) {
		for (Entry<ISiteConnection, ISyncSession> i : sessions.entrySet()) {
			if (i.getValue() == session) {
				sessions.remove(i.getKey());
				break;
			}
		}
		Job job = findSessionJob(session);
		if (job != null) {
			job.cancel();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#runFetchTree(com.aptana.syncing.core.model.ISyncSession)
	 */
	@Override
	public Job runFetchTree(final ISyncSession session) {
		Job job = new Job("Fetching synchronization data for "+session.toString()) {
			@Override
			public boolean belongsTo(Object family) {
				return (family == session);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor = new JobProgressMonitor(this, monitor);
				try {
					if (!monitor.isCanceled()) {
						session.fetchTree(monitor);
					}
				} catch (CoreException e) {
					monitor.done();
					return e.getStatus();
				}
				monitor.done();
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setPriority(Job.LONG);
		job.setRule((SyncSession) session);
		job.schedule();
		return job;
	}
	
	private Job findSessionJob(ISyncSession session) {
		Job[] jobs = Job.getJobManager().find(session);
		return jobs.length > 0 ? jobs[0] : null;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#isSessionInProgress(com.aptana.syncing.core.model.ISyncSession)
	 */
	@Override
	public boolean isSessionInProgress(ISyncSession session) {
		Job job = findSessionJob(session);
		if (job != null) {
			return job.getState() != Job.NONE;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#addProgressMonitorListener(com.aptana.syncing.core.model.ISyncSession, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void addProgressMonitorListener(ISyncSession session, IProgressMonitor monitor) {
		Job job = findSessionJob(session);
		if (job != null) {
			JobProgressMonitor jobProgressMonitor = JobProgressMonitor.getProgressMonitor(job);
			if (jobProgressMonitor != null) {
				jobProgressMonitor.addProgressMonitorListener(monitor);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#removeProgressMonitorListener(com.aptana.syncing.core.model.ISyncSession, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void removeProgressMonitorListener(ISyncSession session, IProgressMonitor monitor) {
		Job job = findSessionJob(session);
		if (job != null) {
			JobProgressMonitor jobProgressMonitor = JobProgressMonitor.getProgressMonitor(job);
			if (jobProgressMonitor != null) {
				jobProgressMonitor.removeProgressMonitorListener(monitor);
			}
		}
	}

}
