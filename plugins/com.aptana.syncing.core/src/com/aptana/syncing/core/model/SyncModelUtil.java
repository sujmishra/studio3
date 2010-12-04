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

package com.aptana.syncing.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aptana.syncing.core.model.ISyncItem.Operation;

/**
 * @author Max Stepanov
 *
 */
public final class SyncModelUtil {

	/**
	 * 
	 */
	private SyncModelUtil() {
	}

	public static List<ISyncItem> getAllItems(ISyncSession session) {
		return getAllItems(session, null);
	}

	public static List<ISyncItem> getAllItems(ISyncSession session, ISyncItem.Changes changes) {
		return getAllItems(session.getItems(), changes);
	}

	public static List<ISyncItem> getAllItems(ISyncItem item) {
		return getAllItems(item, null);
	}

	public static List<ISyncItem> getAllItems(ISyncItem item, ISyncItem.Changes changes) {
		return getAllItems(new ISyncItem[] { item }, changes);
	}

	public static List<ISyncItem> getAllItems(ISyncItem[] items) {
		return getAllItems(items, null);
	}

	public static List<ISyncItem> getAllItems(ISyncItem[] items, ISyncItem.Changes changes) {
		List<ISyncItem> all = new ArrayList<ISyncItem>();
		for (ISyncItem item : items) {
			collectItems(item, all, changes);
		}
		return all;
	}

	private static void collectItems(ISyncItem syncItem, List<ISyncItem> list, ISyncItem.Changes changes) {
		if (changes == null || syncItem.getChanges() == changes) {
			list.add(syncItem);
		}
		ISyncItem[] childItems = syncItem.getChildItems();
		if (childItems != null && childItems.length > 0) {
			for (ISyncItem i : childItems) {
				collectItems(i, list, changes);
			}
		}
	}

	public static void setOperation(List<?> list, SyncOperation operation, boolean deep) {
		setOperation(list, operation.getAllowedOperations(), deep);
	}

	public static void setOperation(ISyncItem[] items, Operation operation, boolean deep) {
		setOperation(items, new Operation[] { operation }, deep);
	}

	public static void setOperation(List<?> list, Operation operation, boolean deep) {
		setOperation(list, new Operation[] { operation }, deep);
	}


	private static void setOperation(ISyncItem[] items, Operation[] operations, boolean deep) {
		setOperation(Arrays.asList(items), operations, deep);
	}

	private static void setOperation(List<?> list, Operation[] operations, boolean deep) {
		Set<Operation> set = new HashSet<Operation>();
		if (operations != null) {
			set.addAll(Arrays.asList(operations));
		}
		for (Object i : list) {
			if ( i instanceof ISyncItem) {
				ISyncItem item = (ISyncItem) i;
				if (!set.isEmpty()) {
					Set<ISyncItem.Operation> allowed = item.getAllowedOperations();
					allowed.retainAll(set);
					if (!allowed.isEmpty()) {
						for (Operation operation : operations) {
							if (allowed.contains(operation)) {
								item.setOperation(operation);
								break;
							}
						}
					} else if (deep) {
						ISyncItem[] children = item.getChildItems();
						if (children != null && children.length > 0) {
							setOperation(children, operations, deep);
						}
					}
				} else {
					item.setOperation(null);
				}
			}
		}
	}

}
