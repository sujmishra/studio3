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

package com.aptana.core.progress;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.jobs.Job;

/**
 * @author Max Stepanov
 *
 */
public final class JobProgressMonitor extends ProgressMonitorWrapper {

	private static WeakHashMap<Job, JobProgressMonitor> progressMonitors = new WeakHashMap<Job, JobProgressMonitor>();
	
	private List<IProgressMonitor> listeners = new ArrayList<IProgressMonitor>();
	
	private String taskName;
	private int totalWork;
	private String subtaskName;
	private int preWork;
	private double internalWorked;
	
	/**
	 * @param monitor
	 */
	public JobProgressMonitor(Job job, IProgressMonitor monitor) {
		super(monitor);
		synchronized (getClass()) {
			progressMonitors.put(job, this);			
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#beginTask(java.lang.String, int)
	 */
	@Override
	public synchronized void beginTask(String name, int totalWork) {
		super.beginTask(name, totalWork);
		for (IProgressMonitor listener : listeners) {
			listener.beginTask(name, totalWork);
		}
		this.taskName = name;
		this.totalWork = totalWork;
		preWork = 0;
		internalWorked = 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#subTask(java.lang.String)
	 */
	@Override
	public synchronized void subTask(String name) {
		super.subTask(name);
		for (IProgressMonitor listener : listeners) {
			listener.subTask(name);
		}
		this.subtaskName = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#worked(int)
	 */
	@Override
	public synchronized void worked(int work) {
		super.worked(work);
		for (IProgressMonitor listener : listeners) {
			listener.worked(work);
		}
		preWork += work;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#setTaskName(java.lang.String)
	 */
	@Override
	public synchronized void setTaskName(String name) {
		super.setTaskName(name);
		for (IProgressMonitor listener : listeners) {
			listener.setTaskName(name);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#clearBlocked()
	 */
	@Override
	public synchronized void clearBlocked() {
		super.clearBlocked();
		for (IProgressMonitor listener : listeners) {
			if (listener instanceof IProgressMonitorWithBlocking) {
				((IProgressMonitorWithBlocking) listener).clearBlocked();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#done()
	 */
	@Override
	public synchronized void done() {
		super.done();
		for (IProgressMonitor listener : listeners) {
			listener.done();
		}
		taskName = null;
		subtaskName = null;
		totalWork = 0;
		preWork = 0;
		internalWorked = 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#internalWorked(double)
	 */
	@Override
	public synchronized void internalWorked(double work) {
		super.internalWorked(work);
		for (IProgressMonitor listener : listeners) {
			listener.internalWorked(work);
		}
		internalWorked = work;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#isCanceled()
	 */
	@Override
	public synchronized boolean isCanceled() {
		if (super.isCanceled()) {
			return true;
		}
		for (IProgressMonitor listener : listeners) {
			if (listener.isCanceled()) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#setBlocked(org.eclipse.core.runtime.IStatus)
	 */
	@Override
	public synchronized void setBlocked(IStatus reason) {
		super.setBlocked(reason);
		for (IProgressMonitor listener : listeners) {
			if (listener instanceof IProgressMonitorWithBlocking) {
				((IProgressMonitorWithBlocking) listener).setBlocked(reason);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#setCanceled(boolean)
	 */
	@Override
	public synchronized void setCanceled(boolean b) {
		super.setCanceled(b);
		for (IProgressMonitor listener : listeners) {
			listener.setCanceled(b);
		}
	}

	/**
	 * Get progress monitor for job
	 * @param job
	 * @return
	 */
	public static synchronized JobProgressMonitor getProgressMonitor(Job job) {
		return progressMonitors.get(job);
	}
	
	/**
	 * Add progress monitor listener
	 * @param listener
	 */
	public synchronized void addProgressMonitorListener(IProgressMonitor listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
			if (taskName != null) {
				listener.beginTask(taskName, totalWork);
			}
			if (subtaskName != null) {
				listener.setTaskName(subtaskName);
			}
			if (internalWorked != 0) {
				listener.internalWorked(internalWorked);
			} else if (preWork != 0) {
				listener.worked(preWork);
			}
		}
	}

	/**
	 * Remove progress monitor listener
	 * @param listener
	 */
	public synchronized void removeProgressMonitorListener(IProgressMonitor listener) {	
		listeners.remove(listener);
	}

}
