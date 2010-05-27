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

package com.aptana.syncing.core.model;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;

import com.aptana.ide.core.io.vfs.IExtendedFileInfo;
import com.aptana.ide.core.io.vfs.IExtendedFileStore;

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
	private final short state;
	private final IFileInfo fileInfo;
	
	/**
	 * 
	 */
	private SyncState(SyncIdentifier id, short state, IFileInfo fileInfo) {
		this.id = id;
		this.state = state;
		this.fileInfo = fileInfo;
	}
		
	/**
	 * @return the state
	 */
	public short getState() {
		return state;
	}

	/**
	 * @return the id
	 */
	public SyncIdentifier getSyncIdentifier() {
		return id;
	}

	/**
	 * @return the fileInfo
	 */
	public IFileInfo getFileInfo() {
		return fileInfo;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + state;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SyncState)) {
			return false;
		}
		SyncState other = (SyncState) obj;
		if (state != other.state) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SyncState [uri=").append(id.getTarget()).append(", context=").append(id.getContext()).append("]=");
		switch (state) {
		case UNMODIFIED:
			builder.append("UNMODIFIED");
			break;
		case TYPE_CHANGED:
			builder.append("TYPE_CHANGED");
			break;
		case ADDED:
			builder.append("ADDED");
			break;
		case REMOVED:
			builder.append("REMOVED");
			break;
		default:
			if ((state & LENGTH_CHANGED) != 0) {
				builder.append("LENGTH_CHANGED").append(',');
			}
			if ((state & MODIFICATION_TIME_CHANGED) != 0) {
				builder.append("MODIFICATION_TIME_CHANGED").append(',');
			}
			if ((state & PERMISSIONS_CHANGED) != 0) {
				builder.append("PERMISSIONS_CHANGED").append(',');
			}
			builder.setLength(builder.length()-1);
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param id
	 * @param fileInfo
	 * @return
	 */
	public static SyncState get(SyncIdentifier id, IFileInfo fileInfo) {
		return new SyncState(id, getState(id, fileInfo), fileInfo);
	}

	/**
	 * 
	 * @param fileStore
	 * @param fileInfo
	 * @param context
	 * @return
	 */
	public static SyncState get(IFileStore fileStore, IFileInfo fileInfo, String context) {
		return get(new SyncIdentifier(getURI(fileStore), context), fileInfo);
	}
	
	/**
	 * 
	 * @param id
	 * @param fileInfo
	 */
	public static void save(SyncIdentifier id, IFileInfo fileInfo) {
		ItemState itemState = null;
		if (fileInfo != null && fileInfo.exists()) {
			itemState = new ItemState();
			if (fileInfo.isDirectory()) {
				itemState.setType(ItemState.TYPE_FOLDER);
			} else if (fileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
				itemState.setType(ItemState.TYPE_SYMLINK);
			} else {
				itemState.setType(ItemState.TYPE_FILE);
			}
			itemState.setLength(fileInfo.getLength());
			itemState.setModificationTime(fileInfo.getLastModified());
			if (fileInfo instanceof IExtendedFileInfo) {
				itemState.setPermissions(((IExtendedFileInfo) fileInfo).getPermissions());
			}
		}
		ItemStateStorage.getInstance().saveState(id, itemState);
	}
	
	public static void save(SyncState state, IFileInfo fileInfo) {
		save(state.getSyncIdentifier(), fileInfo);
	}


	private static URI getURI(IFileStore fileStore) {
		URI uri = fileStore.toURI();
		if (fileStore instanceof IExtendedFileStore) {
			uri = ((IExtendedFileStore) fileStore).toCanonicalURI();
		}
		return uri;
	}

	private static short getState(SyncIdentifier id, IFileInfo fileInfo) {
		short state;
		ItemState itemState = ItemStateStorage.getInstance().getState(id);
		if (itemState == null) {
			state = ADDED;
		} else if (!fileInfo.exists()){
			state = REMOVED;
		} else if (fileInfo.isDirectory() != (itemState.getType() == ItemState.TYPE_FOLDER)
				|| fileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK) != (itemState.getType() == ItemState.TYPE_SYMLINK)) {
			state = TYPE_CHANGED;
		} else {
			state = UNMODIFIED;
			if (itemState.getType() == ItemState.TYPE_FILE) {
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
	
}
