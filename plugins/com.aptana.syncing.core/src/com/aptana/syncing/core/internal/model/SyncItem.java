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

package com.aptana.syncing.core.internal.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;

import com.aptana.syncing.core.events.ISyncSessionListener;
import com.aptana.syncing.core.events.SyncSessionEvent;
import com.aptana.syncing.core.internal.model.SyncPair.Direction;
import com.aptana.syncing.core.model.ISyncItem;

/**
 * @author Max Stepanov
 *
 */
/* package */ final class SyncItem implements ISyncItem {

	protected static final ISyncItem[] EMPTY_LIST = new ISyncItem[0];

	private IPath path;
	private SyncPair syncPair;
	private ISyncItem[] childItems;
	private SyncStatus syncStatus;
	private int syncProgress;
	private CoreException syncError;
	
	/**
	 * 
	 */
	protected SyncItem(IPath path, SyncPair syncPair) {
		this.path = path;
		this.syncPair = syncPair;
	}
	
	/* package*/ void setChildItems(ISyncItem[] childItems) {
		this.childItems = childItems;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getChildItems()
	 */
	@Override
	public ISyncItem[] getChildItems() {
		return childItems;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getLeftFileStore()
	 */
	@Override
	public IFileStore getLeftFileStore() {
		return syncPair.getLeftFileStore();
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getRightFileStore()
	 */
	@Override
	public IFileStore getRightFileStore() {
		return syncPair.getRightFileStore();
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getLeftFileInfo()
	 */
	@Override
	public IFileInfo getLeftFileInfo() {
		return syncPair.getLeftFileInfo();
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getRightFileInfo()
	 */
	@Override
	public IFileInfo getRightFileInfo() {
		return syncPair.getRightFileInfo();
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getName()
	 */
	@Override
	public String getName() {
		return path.lastSegment();
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getOperation()
	 */
	@Override
	public Operation getOperation() {
		switch(syncPair.getDirection()) {
		case LEFT_TO_RIGHT:
			return Operation.LEFT_TO_RIGHT;
		case RIGHT_TO_LEFT:
			return Operation.RIGHT_TO_LEFT;
		case SAME:
		case AMBIGUOUS:
		case INCONSISTENT:
		default:
			return Operation.NONE;
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getAllowedOperations()
	 */
	@Override
	public Set<Operation> getAllowedOperations() {
		Set<Operation> set = new HashSet<Operation>();
		set.add(Operation.NONE);
		if (syncPair.getDefaultDirection() != Direction.INCONSISTENT
				&& !(getType() == Type.FOLDER && getChanges() == Changes.NONE)) {
			set.add(Operation.LEFT_TO_RIGHT);
			set.add(Operation.RIGHT_TO_LEFT);
		}
		return set;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#setOperation(com.aptana.syncing.core.model.ISyncItem.Operation)
	 */
	@Override
	public void setOperation(Operation operation) {
		switch (operation) {
		case LEFT_TO_RIGHT:
			syncPair.setForceDirection(Direction.LEFT_TO_RIGHT);
			break;
		case RIGHT_TO_LEFT:
			syncPair.setForceDirection(Direction.RIGHT_TO_LEFT);
			break;
		case NONE:
			syncPair.setForceDirection(Direction.NONE);
			break;
		default:
			syncPair.setForceDirection(null);
			break;
		}
		if (childItems != null) {
			for (ISyncItem child: childItems) {
				child.setOperation(operation);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getStatus()
	 */
	@Override
	public Changes getChanges() {
		switch(syncPair.getDefaultDirection()) {
		case SAME:
			return Changes.NONE;
		case LEFT_TO_RIGHT:
			return Changes.LEFT_TO_RIGHT;
		case RIGHT_TO_LEFT:
			return Changes.RIGHT_TO_LEFT;
		case AMBIGUOUS:
		case INCONSISTENT:
			return Changes.CONFLICT;
		default:
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getPath()
	 */
	@Override
	public IPath getPath() {
		return path;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getType()
	 */
	@Override
	public Type getType() {
		if (syncPair.getDirection() == Direction.INCONSISTENT) {
			return Type.UNSUPPORTED;
		}
		if (syncPair.getLeftFileInfo().isDirectory() || syncPair.getRightFileInfo().isDirectory()) {
			return Type.FOLDER;
		}
		if (syncPair.getLeftFileInfo().getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
			return Type.UNSUPPORTED;
		}
		return Type.FILE;
	}


	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getSyncProgress()
	 */
	@Override
	public int getSyncProgress() {
		return syncProgress;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getSyncResult()
	 */
	@Override
	public SyncStatus getSyncResult() {
		return syncStatus;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#getSyncError()
	 */
	@Override
	public CoreException getSyncError() {
		return syncError;
	}

	public SyncStatus synchronize(IProgressMonitor monitor, ISyncSessionListener listener) {
		try {
			syncStatus = SyncStatus.IN_PROGRESS;
			syncStatus = syncPair.synchronize(new ProgressMonitor(monitor, listener)) ? SyncStatus.SUCCEEDED : SyncStatus.FAILED;
		} catch (CoreException e) {
			syncError = e;
			syncStatus = SyncStatus.FAILED;			
		}
		return syncStatus;
	}
	
	/* package */ void resetState() {
		syncStatus = null;
		syncProgress = 0;
		syncError = null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SyncItem [path=").append(path).append("]");
		return builder.toString();
	}

	private class ProgressMonitor extends ProgressMonitorWrapper {

		private ISyncSessionListener listener;
		private int totalWork = Integer.MAX_VALUE;
		private int worked = 0;
		
		protected ProgressMonitor(IProgressMonitor monitor, ISyncSessionListener listener) {
			super(monitor);
			this.listener = listener;
			notifyListener();
		}
		
		private void notifyListener() {
			if (listener != null) {
				listener.handleEvent(new SyncSessionEvent(SyncItem.this, SyncSessionEvent.ITEMS_UPDATED, new ISyncItem[] { SyncItem.this }));
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#beginTask(java.lang.String, int)
		 */
		@Override
		public void beginTask(String name, int totalWork) {
			this.totalWork = totalWork;
			syncProgress = 0;
			notifyListener();
			super.beginTask(name, totalWork);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#done()
		 */
		@Override
		public void done() {
			syncProgress = 100;
			notifyListener();
			super.done();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#internalWorked(double)
		 */
		@Override
		public void internalWorked(double work) {
			worked += work;
			syncProgress = (int) ((worked*100)/totalWork);
			notifyListener();
			super.internalWorked(work);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#worked(int)
		 */
		@Override
		public void worked(int work) {
			worked += work;
			syncProgress = (worked*100)/totalWork;
			notifyListener();
			super.worked(work);
		}
		
	}

}
