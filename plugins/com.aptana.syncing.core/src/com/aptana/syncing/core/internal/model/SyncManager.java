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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.aptana.core.progress.JobProgressMonitor;
import com.aptana.ide.syncing.core.ISiteConnection;
import com.aptana.syncing.core.model.ISyncItem;
import com.aptana.syncing.core.model.ISyncManager;
import com.aptana.syncing.core.model.ISyncSession;
import com.aptana.syncing.core.model.ISyncItem.Operation;
import com.aptana.syncing.core.model.ISyncSession.Stage;

/**
 * @author Max Stepanov
 *
 */
public final class SyncManager implements ISyncManager {
	
	private interface IOperationRunnable {
		void run(ISyncSession session, IProgressMonitor monitor) throws CoreException;
	}
	
	private static final int CONCURRENT_JOBS = 4;
	
	private static SyncManager instance;
	private WeakHashMap<ISiteConnection, ISyncSession> sessions = new WeakHashMap<ISiteConnection, ISyncSession>();
		
	/**
	 * 
	 */
	private SyncManager() {
	}
	
	public static SyncManager getInstance() {
		if (instance == null) {
			synchronized (SyncManager.class) {
				if (instance == null) {
					instance = new SyncManager();
				}
			}
		}
		return instance;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#getSyncSession(com.aptana.ide.syncing.core.ISiteConnection)
	 */
	@Override
	public synchronized ISyncSession getSyncSession(ISiteConnection siteConnection) {
		return sessions.get(siteConnection);
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#createSyncSession(com.aptana.ide.syncing.core.ISiteConnection)
	 */
	@Override
	public synchronized ISyncSession createSyncSession(ISiteConnection siteConnection) {
		ISyncSession session =  new SyncSession(siteConnection.getSource(), siteConnection.getDestination());
		sessions.put(siteConnection, session);
		return session;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#closeSession(com.aptana.syncing.core.model.ISyncSession)
	 */
	@Override
	public synchronized void closeSession(ISyncSession session) {
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
	 * @see com.aptana.syncing.core.model.ISyncManager#runFetchTree(com.aptana.syncing.core.model.ISyncSession, com.aptana.syncing.core.model.ISyncItem[])
	 */
	@Override
	public Job runFetchTree(final ISyncSession session, final ISyncItem[] items) {
		((SyncSession) session).setStage(Stage.FETCHING);
		Job job = new Job(MessageFormat.format(Messages.SyncManager_FetchingData_0, session.toString())) {
			@Override
			public boolean belongsTo(Object family) {
				return (family == session);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor = new JobProgressMonitor(this, monitor);
				try {
					if (!monitor.isCanceled()) {
						if (items != null) {
							session.fetchTree(items, true, monitor);
						} else {
							session.fetchTree(monitor);
						}
					}
				} catch (CoreException e) {
					((SyncSession) session).setStage(Stage.CANCELLED);
					closeSession(session);
					return e.getStatus();
				} finally {
					monitor.done();					
				}
				if (monitor.isCanceled()) {
					((SyncSession) session).setStage(Stage.CANCELLED);
					closeSession(session);
					return Status.CANCEL_STATUS;
				}
				((SyncSession) session).setStage(Stage.FETCHED);
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setPriority(Job.LONG);
		job.setRule((SyncSession) session);
		job.schedule();
		return job;
	}
	
	private Job doOperationInternal(final ISyncSession session, final IOperationRunnable operationRunnable) {		
		((SyncSession) session).setStage(Stage.PRESYNCING);
		Job job = new Job(MessageFormat.format(Messages.SyncManager_Synchronizing_0, session.toString())) {
			@Override
			public boolean belongsTo(Object family) {
				return (family == session);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SyncDispatcher dispatcher;
				try {
					if (operationRunnable != null) {
						operationRunnable.run(session, monitor);
					}
					dispatcher = ((SyncSession) session).getSyncDispatcher();
					dispatcher.start(session, monitor);
				} catch (CoreException e) {
					monitor.done();
					return e.getStatus();
				}
				((SyncSession) session).setStage(Stage.SYNCING);
				int lastRemainingCount = dispatcher.getRemainingCount();
				SubMonitor progress = SubMonitor.convert(monitor, lastRemainingCount);
				if (!monitor.isCanceled()) {
					Job[] workers = new Job[CONCURRENT_JOBS];
					final Object updateLock = new Object();
					synchronized (updateLock) {
						for (int i = 0; i < workers.length; ++i) {
							workers[i] = createSyncWorker(session, dispatcher, updateLock);
						}
						int remaining;
						while (!monitor.isCanceled() && (remaining = dispatcher.getRemainingCount()) > 0) {
							if (remaining < lastRemainingCount) {
								progress.worked(lastRemainingCount-remaining);
								lastRemainingCount = remaining;
							}
							progress.setWorkRemaining(remaining);
							StringBuffer sb = new StringBuffer();
							for (ISyncItem i : dispatcher.getActiveItems()) {
								if (sb.length() > 0) {
									sb.append('\n');
								}
								sb.append(i.getPath());
							}
							progress.subTask(sb.toString());
							try {
								updateLock.wait(2000);
							} catch (InterruptedException e) {
							}
						}
					}
					if (monitor.isCanceled()) {
						for (Job job : workers) {
							job.cancel();
						}
					}
					for (Job job : workers) {
						try {
							job.join();
						} catch (InterruptedException e) {
						}
					}
				}
				monitor.done();
				if (monitor.isCanceled()) {
					((SyncSession) session).setStage(Stage.CANCELLED);
					closeSession(session);
					return Status.CANCEL_STATUS;
				}
				((SyncSession) session).setStage(Stage.SYNCED);
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.setPriority(Job.LONG);
		job.setRule((SyncSession) session);
		job.schedule();
		return job;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#synchronize(com.aptana.syncing.core.model.ISyncSession)
	 */
	@Override
	public Job synchronize(final ISyncSession session) {
		return doOperationInternal(session, null);
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#doOperation(com.aptana.syncing.core.model.ISyncSession, org.eclipse.core.runtime.IPath[], com.aptana.syncing.core.model.ISyncItem.Operation)
	 */
	@Override
	public Job doOperation(final ISyncSession session, final IPath[] paths, final Operation operation) {
		return doOperationInternal(session, new IOperationRunnable() {
			@Override
			public void run(ISyncSession session, IProgressMonitor monitor) throws CoreException {
				session.fetchTree(paths, false, monitor);
				ISyncItem[] items = session.getItems();
				List<ISyncItem> all = new ArrayList<ISyncItem>();
				for (ISyncItem item : items) {
					item.setOperation(operation);
					collectItems(item, all);
				}
				session.setSyncItems(all);
			}
		});
	}
	
	private static void collectItems(ISyncItem syncItem, List<ISyncItem> list) {
		list.add(syncItem);
		ISyncItem[] childItems = syncItem.getChildItems();
		if (childItems != null && childItems.length > 0) {
			for (ISyncItem i : childItems) {
				collectItems(i, list);
			}
		}
	}

	private Job createSyncWorker(final ISyncSession session, final SyncDispatcher dispatcher, final Object updateLock) {
		Job job = new Job(Messages.SyncManager_SyncWorker_Name) {
			@Override
			public boolean belongsTo(Object family) {
				return (family == dispatcher);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				int lastRemainingCount = dispatcher.getRemainingCount();
				SubMonitor progress = SubMonitor.convert(monitor, lastRemainingCount);
				try {
					ISyncItem item;
					while (!monitor.isCanceled() && (item = dispatcher.next()) != null) {
						synchronized (updateLock) {
							updateLock.notify();							
						}
						try {
							session.synchronize(new ISyncItem[] { item }, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
						} finally {
							dispatcher.done(item);
						}
						synchronized (updateLock) {
							int remianing = dispatcher.getRemainingCount();
							if (remianing < lastRemainingCount - 1) {
								progress.worked(lastRemainingCount-remianing-1);
								lastRemainingCount = remianing;
							}
							progress.setWorkRemaining(remianing);
							updateLock.notify();							
						}
					}
				} catch (CoreException e) {
					return e.getStatus();
				} finally {
					progress.done();
					monitor.done();					
				}
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.LONG);
		job.schedule();
		return job;		
	}
	
	private Job findSessionJob(ISyncSession session) {
		Job[] jobs = Job.getJobManager().find(session);
		return jobs.length > 0 ? jobs[0] : null;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncManager#isSyncInProgress(com.aptana.syncing.core.model.ISyncSession)
	 */
	@Override
	public boolean isSyncInProgress(ISyncSession session) {
		Job job = findSessionJob(session);
		return (job != null && job.getState() != Job.NONE);
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
