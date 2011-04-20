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

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;

import com.aptana.core.io.vfs.IExtendedFileInfo;
import com.aptana.core.io.vfs.IExtendedFileStore;
import com.aptana.syncing.core.internal.model.ItemState.Type;

/**
 * @author Max Stepanov
 *
 */
public final class SyncState {

	public static final short UNMODIFIED = 0x00;
	public static final short LENGTH_CHANGED = 0x01;
	public static final short MODIFICATION_TIME_CHANGED = 0x02;
	public static final short PERMISSIONS_CHANGED = 0x04;
	public static final short TYPE_CHANGED = 0x08;
	public static final short ADDED = 0x10;
	public static final short REMOVED = 0x20;
	
	private final SyncIdentifier id;
	private final short leftState;
	private final short rightState;
	
	/**
	 * 
	 */
	private SyncState(SyncIdentifier id, IFileInfo leftFileInfo, IFileInfo rightFileInfo) {
		this.id = id;
		this.leftState = getState(id, true, leftFileInfo);
		this.rightState = getState(id, false, rightFileInfo);
	}

	/**
	 * 
	 * @return
	 */
	public SyncIdentifier getSyncIdentifier() {
		return id;
	}

	/**
	 * @return the leftState
	 */
	public short getLeftState() {
		return leftState;
	}

	/**
	 * @return the rightState
	 */
	public short getRightState() {
		return rightState;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		stateToString(leftState, builder);
		builder.append(")("); //$NON-NLS-1$
		stateToString(rightState, builder);
		builder.append(')');
		return builder.toString();
	}
	
	private static void stateToString(short state, StringBuilder sb) {
		switch (state) {
		case UNMODIFIED:
			sb.append("UNMODIFIED"); //$NON-NLS-1$
			break;
		case TYPE_CHANGED:
			sb.append("TYPE_CHANGED"); //$NON-NLS-1$
			break;
		case ADDED:
			sb.append("ADDED"); //$NON-NLS-1$
			break;
		case REMOVED:
			sb.append("REMOVED"); //$NON-NLS-1$
			break;
		default:
			if ((state & LENGTH_CHANGED) != 0) {
				sb.append("LENGTH_CHANGED").append(','); //$NON-NLS-1$
			}
			if ((state & MODIFICATION_TIME_CHANGED) != 0) {
				sb.append("MODIFICATION_TIME_CHANGED").append(','); //$NON-NLS-1$
			}
			if ((state & PERMISSIONS_CHANGED) != 0) {
				sb.append("PERMISSIONS_CHANGED").append(','); //$NON-NLS-1$
			}
			sb.setLength(sb.length()-1);
		}		
	}

	/**
	 * 
	 * @param id
	 * @param leftFileInfo
	 * @param rightFileInfo
	 * @return
	 */
	public static SyncState get(SyncIdentifier id, IFileInfo leftFileInfo, IFileInfo rightFileInfo) {
		return new SyncState(id, leftFileInfo, rightFileInfo);
	}

	/**
	 * 
	 * @param leftFileStore
	 * @param leftFileInfo
	 * @param rightFileStore
	 * @param rightFileInfo
	 * @return
	 */
	public static SyncState get(IFileStore leftFileStore, IFileInfo leftFileInfo, IFileStore rightFileStore, IFileInfo rightFileInfo) {
		return get(new SyncIdentifier(getURI(leftFileStore), getURI(rightFileStore)), leftFileInfo, rightFileInfo);
	}

	/**
	 * 
	 * @param id
	 * @param leftFileInfo
	 * @param rightFileInfo
	 */
	public static void save(SyncIdentifier id, IFileInfo leftFileInfo, IFileInfo rightFileInfo) {
		ItemState left = createItemState(leftFileInfo);
		ItemState right = createItemState(rightFileInfo);
		ItemStatePair itemStatePair = null;
		if (left != null || right != null) {
			itemStatePair = new ItemStatePair(left, right);
		}
		ItemStateStorage.getInstance().saveState(id, itemStatePair);
	}
	
	public static void save(SyncState state, IFileInfo leftFileInfo, IFileInfo rightFileInfo) {
		save(state.getSyncIdentifier(), leftFileInfo, rightFileInfo);
	}


	private static URI getURI(IFileStore fileStore) {
		URI uri = fileStore.toURI();
		if (fileStore instanceof IExtendedFileStore) {
			uri = ((IExtendedFileStore) fileStore).toCanonicalURI();
		}
		return uri;
	}

	private static short getState(SyncIdentifier id, boolean left, IFileInfo fileInfo) {
		short state;
		ItemStatePair itemStatePair = ItemStateStorage.getInstance().getState(id);
		ItemState itemState = null;
		if (itemStatePair != null) {
			itemState = left ? itemStatePair.left : itemStatePair.right;
		}
		if (itemState == null) {
			state = fileInfo.exists() ? ADDED : UNMODIFIED;
		} else if (!fileInfo.exists()){
			state = REMOVED;
		} else if (fileInfo.isDirectory() != (itemState.getType() == Type.FOLDER)
				|| fileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK) != (itemState.getType() == Type.SYMLINK)) {
			state = TYPE_CHANGED;
		} else {
			state = UNMODIFIED;
			if (itemState.getType() == Type.FILE) {
				if (fileInfo.getLength() != itemState.getLength()) {
					state |= LENGTH_CHANGED;
				}
				if (fileInfo.getLastModified() != itemState.getModificationTime()) {
					state |= MODIFICATION_TIME_CHANGED;
				}
			}
			if (fileInfo instanceof IExtendedFileInfo) {
				if (((IExtendedFileInfo) fileInfo).getPermissions() != itemState.getPermissions()) {
					state |= PERMISSIONS_CHANGED;
				}
			}
		}
		return state;
	}
	
	private static ItemState createItemState(IFileInfo fileInfo) {
		ItemState itemState = null;
		if (fileInfo != null && fileInfo.exists()) {
			itemState = new ItemState();
			if (fileInfo.isDirectory()) {
				itemState.setType(Type.FOLDER);
			} else if (fileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
				itemState.setType(Type.SYMLINK);
			} else {
				itemState.setType(Type.FILE);
			}
			itemState.setLength(fileInfo.getLength());
			itemState.setModificationTime(fileInfo.getLastModified());
			if (fileInfo instanceof IExtendedFileInfo) {
				itemState.setPermissions(((IExtendedFileInfo) fileInfo).getPermissions());
			}
		}
		return itemState;
	}
	
	public static void flush() {
		ItemStateStorage.getInstance().close();
	}

	public static void clear() {
		ItemStateStorage.getInstance().clear();
	}

}
