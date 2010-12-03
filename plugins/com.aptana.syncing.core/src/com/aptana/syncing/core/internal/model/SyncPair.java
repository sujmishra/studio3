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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.aptana.ide.core.io.vfs.IExtendedFileInfo;
import com.aptana.ide.core.io.vfs.IExtendedFileStore;
import com.aptana.syncing.core.internal.SyncUtils;

/**
 * @author Max Stepanov
 *
 */
public final class SyncPair {

	public enum Direction {
		NONE, SAME, LEFT_TO_RIGHT, RIGHT_TO_LEFT, AMBIGUOUS, INCONSISTENT
	}
	
	private final IFileStore leftFileStore;
	private final IFileStore rightFileStore;

	private IFileInfo leftFileInfo;
	private IFileInfo rightFileInfo;

	private SyncState state;
	
	private Direction defaultDirection;
	private Direction forceDirection;
	
	/**
	 * 
	 */
	public SyncPair(IFileStore leftFileStore, IFileStore rightFileStore) {
		this(leftFileStore, null, rightFileStore, null);
	}

	/**
	 * 
	 */
	public SyncPair(IFileStore leftFileStore, IFileInfo leftFileInfo, IFileStore rightFileStore, IFileInfo rightFileInfo) {
		this.leftFileStore = leftFileStore;
		this.rightFileStore = rightFileStore;
		this.leftFileInfo = leftFileInfo;
		this.rightFileInfo = rightFileInfo;
	}
	
	public synchronized Direction calculateDirection(IProgressMonitor monitor) throws CoreException {
		if (defaultDirection == null) {
			if (state == null) {
				if (leftFileInfo == null) {
					leftFileInfo = leftFileStore.fetchInfo(IExtendedFileStore.DETAILED, monitor);
				}
				if (rightFileInfo == null) {
					rightFileInfo = rightFileStore.fetchInfo(IExtendedFileStore.DETAILED, monitor);
				}
				state = SyncState.get(leftFileStore, leftFileInfo, rightFileStore, rightFileInfo);
			}
			// ignore permissions changes for now
			int left = state.getLeftState() & ~SyncState.PERMISSIONS_CHANGED;
			int right = state.getRightState() & ~SyncState.PERMISSIONS_CHANGED;
			if (left == SyncState.UNMODIFIED
					&& right == SyncState.UNMODIFIED) {
				// both sides are unmodified
				defaultDirection = Direction.SAME;
			} else if (left == SyncState.REMOVED && right == SyncState.REMOVED) {
				defaultDirection = Direction.SAME;
			} else 	if (left == SyncState.ADDED && right == SyncState.ADDED) {
				defaultDirection = compareFileInfos();
			} else if (left == SyncState.TYPE_CHANGED || right == SyncState.TYPE_CHANGED) {
				defaultDirection = compareFileInfos();
			} else if (left == SyncState.UNMODIFIED) {
				// right is modified
				if (right == SyncState.ADDED) {
					defaultDirection = compareFileInfos();
				} else {
					defaultDirection = Direction.RIGHT_TO_LEFT;
				}
			} else if (right == SyncState.UNMODIFIED) {
				// left is modified
				if (left == SyncState.ADDED) {
					defaultDirection = compareFileInfos();
				} else {
					defaultDirection = Direction.LEFT_TO_RIGHT;
				}
			} else {
				// both sized are modified
				defaultDirection = Direction.AMBIGUOUS;
			}
			// TODO: process permission changes
		}
		return defaultDirection;
	}

	/**
	 * @return the defaultDirection
	 */
	public Direction getDefaultDirection() {
		return defaultDirection;
	}
	
	/**
	 * @return the forceDirection
	 */
	public Direction getForceDirection() {
		return forceDirection;
	}

	/**
	 * @return the direction
	 */
	public Direction getDirection() {
		return forceDirection != null ? forceDirection : defaultDirection;
	}

	/**
	 * @param forceDirection the forceDirection to set
	 */
	public void setForceDirection(Direction forceDirection) {
		Assert.isLegal(forceDirection == null || forceDirection == Direction.NONE || forceDirection == Direction.SAME || forceDirection == Direction.LEFT_TO_RIGHT || forceDirection == Direction.RIGHT_TO_LEFT, "Invalid force direction"); //$NON-NLS-1$
		this.forceDirection = forceDirection;
	}
	
	/**
	 * @return the leftFileStore
	 */
	public IFileStore getLeftFileStore() {
		return leftFileStore;
	}

	/**
	 * @return the rightFileStore
	 */
	public IFileStore getRightFileStore() {
		return rightFileStore;
	}

	/**
	 * @return the leftFileInfo
	 */
	public IFileInfo getLeftFileInfo() {
		return leftFileInfo;
	}

	/**
	 * @return the rightFileInfo
	 */
	public IFileInfo getRightFileInfo() {
		return rightFileInfo;
	}

	public synchronized boolean synchronize(IProgressMonitor monitor) throws CoreException {
		calculateDirection(monitor);
		switch(getDirection()) {
		case SAME:
			if (state.getLeftState() == SyncState.ADDED || state.getRightState() == SyncState.ADDED) {
				SyncState.save(state, leftFileInfo, rightFileInfo);
			}
			break;
		case LEFT_TO_RIGHT:
			if (!leftFileInfo.exists()) {
				rightFileStore.delete(EFS.NONE, monitor);
			} else {
				SyncUtils.copy(leftFileStore, leftFileInfo, rightFileStore, IExtendedFileInfo.SET_PERMISSIONS, monitor);
			}
			SyncState.save(state, leftFileInfo, rightFileStore.fetchInfo(IExtendedFileStore.DETAILED, monitor));
			break;
		case RIGHT_TO_LEFT:
			if (!rightFileInfo.exists()) {
				leftFileStore.delete(EFS.NONE, monitor);
			} else {
				SyncUtils.copy(rightFileStore, rightFileInfo, leftFileStore, IExtendedFileInfo.SET_PERMISSIONS, monitor);
			}
			SyncState.save(state, leftFileStore.fetchInfo(IExtendedFileStore.DETAILED, monitor), rightFileInfo);
			break;
		case AMBIGUOUS:
		case INCONSISTENT:
		default:
			return false;
		}
		return true;
	}
	
	public synchronized void markAsSame() {
		if (leftFileInfo != null && rightFileInfo != null) {
			SyncState.save(state, leftFileInfo, rightFileInfo);
			defaultDirection = null;
			state = null;
			try {
				calculateDirection(new NullProgressMonitor());
			} catch (CoreException ignore) {
			}
		}
	}

	private Direction compareFileInfos() {
		if (!leftFileInfo.exists()) {
			return Direction.RIGHT_TO_LEFT;
		} else if (!rightFileInfo.exists()) {
			return Direction.LEFT_TO_RIGHT;
		} else if (leftFileInfo.isDirectory() != rightFileInfo.isDirectory()
				|| leftFileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK) != rightFileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
			return Direction.INCONSISTENT;
		} else if (leftFileInfo.isDirectory()
				|| leftFileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK)
				|| (leftFileInfo.getLength() == rightFileInfo.getLength()
						&& leftFileInfo.getLastModified() == rightFileInfo.getLastModified())) {
			return Direction.SAME;
		}
		return Direction.AMBIGUOUS;
	}
		
}
