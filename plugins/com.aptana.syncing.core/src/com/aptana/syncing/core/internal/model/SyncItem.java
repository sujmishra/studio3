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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.runtime.IPath;

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
		return childItems != null ? childItems : EMPTY_LIST;
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
		if (syncPair.getLeftFileInfo().isDirectory()) {
			return Type.FOLDER;
		}
		if (syncPair.getLeftFileInfo().getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
			return Type.UNSUPPORTED;
		}
		return Type.FILE;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#hasChanges()
	 */
	@Override
	public boolean hasChanges() {
		return syncPair.getDefaultDirection() != Direction.SAME;
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.model.ISyncItem#resetOperation()
	 */
	@Override
	public void resetOperation() {
		syncPair.setForceDirection(null);
	}

}
