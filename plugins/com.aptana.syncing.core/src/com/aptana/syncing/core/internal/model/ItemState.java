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

/**
 * @author Max Stepanov
 *
 */
/* package */ final class ItemState {

	public static final short TYPE_FILE = 0;
	public static final short TYPE_FOLDER = 1;
	public static final short TYPE_SYMLINK = 2;
	
	private long modificationTime;
	private long length;
	private long permissions;
	private short type;

	/**
	 * 
	 */
	public ItemState() {
	}
	
	private ItemState(short type, long length, long modificationTime, long permissions) {
		this.type = type;
		this.length = length;
		this.modificationTime = modificationTime;
		this.permissions = permissions;
		
	}

	/**
	 * @return the modificationTime
	 */
	public long getModificationTime() {
		return modificationTime;
	}

	/**
	 * @param modificationTime the modificationTime to set
	 */
	public void setModificationTime(long modificationTime) {
		this.modificationTime = modificationTime;
	}

	/**
	 * @return the length
	 */
	public long getLength() {
		return length;
	}

	/**
	 * @param length the length to set
	 */
	public void setLength(long length) {
		this.length = length;
	}

	/**
	 * @return the permissions
	 */
	public long getPermissions() {
		return permissions;
	}

	/**
	 * @param permissions the permissions to set
	 */
	public void setPermissions(long permissions) {
		this.permissions = permissions;
	}

	/**
	 * @return the type
	 */
	public short getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(short type) {
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (length ^ (length >>> 32));
		result = prime * result
				+ (int) (modificationTime ^ (modificationTime >>> 32));
		result = prime * result + (int) (permissions ^ (permissions >>> 32));
		result = prime * result + type;
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
		if (!(obj instanceof ItemState)) {
			return false;
		}
		ItemState other = (ItemState) obj;
		if (type != other.type
				|| length != other.length
				|| modificationTime != other.modificationTime
				|| permissions != other.permissions) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		switch (type) {
		case TYPE_FILE:
			sb.append('F');
			break;
		case TYPE_FOLDER:
			sb.append('D');
			break;
		case TYPE_SYMLINK:
			sb.append('S');
			break;
		default:
			sb.append('X');
			break;
		}
		sb.append(';');
		sb.append(Long.toHexString(length)).append(';');
		sb.append(Long.toHexString(modificationTime)).append(';');
		sb.append(Long.toHexString(permissions));
		return sb.toString();
	}
	
	public static ItemState fromString(String string) {
		if (string != null && string.length() > 0) {
			String[] list = string.split(";"); //$NON-NLS-1$
			if (list.length == 4 && list[0].length() > 0) {
				short type;
				switch(list[0].charAt(0)) {
				case 'F':
					type = TYPE_FILE;
					break;
				case 'D':
					type = TYPE_FOLDER;
					break;
				case 'S':
					type = TYPE_SYMLINK;
					break;
				default:
					return null;
				}
				try {
					long length = Long.parseLong(list[1], 16);
					long modificationTime = Long.parseLong(list[2], 16);
					long permissions = Long.parseLong(list[3], 16);
					return new ItemState(type, length, modificationTime, permissions);
				} catch (NumberFormatException e) {
				}
				
			}
		}
		return null;
	}
	
}
