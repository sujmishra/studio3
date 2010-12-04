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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.aptana.syncing.core.events.ISyncSessionListener;
import com.aptana.syncing.core.events.SyncSessionEvent;
import com.aptana.syncing.core.model.ISyncItem;
import com.aptana.syncing.core.model.ISyncSession;
import com.aptana.syncing.core.model.ISyncItem.Operation;
import com.aptana.syncing.core.model.ISyncItem.Type;
import com.aptana.syncing.core.model.SyncModelUtil;

/**
 * @author Max Stepanov
 *
 */
/* package */ final class SyncDispatcher {

	private List<ISyncItem> syncItems = new ArrayList<ISyncItem>();
	private Stack<ISyncItem> workQueue = new Stack<ISyncItem>();
	private List<ISyncItem> active = new ArrayList<ISyncItem>();
	
	/**
	 * 
	 */
	public SyncDispatcher(List<ISyncItem> list) {
		ISyncItem[] items = sort(list.toArray(new ISyncItem[list.size()]));
		for (ISyncItem i : items) {
			syncItems.add(i);
		}
	}
	
	public synchronized void start(ISyncSession session, IProgressMonitor monitor) throws CoreException {
		List<ISyncItem> unread = new ArrayList<ISyncItem>();
		for (ISyncItem item : syncItems) {
			Operation op = item.getOperation();
			if (item.getType() == Type.FOLDER && (op == Operation.ADD_TO_LEFT || op == Operation.ADD_TO_RIGHT) && item.getChildItems() == null) {
				unread.add(item);
			}
		}
		if (!unread.isEmpty()) {
			ISyncSessionListener listener;
			int oldCount = syncItems.size();
			session.addListener(listener = new  ISyncSessionListener() {

				public void handleEvent(SyncSessionEvent event) {
					if (event.getKind() == SyncSessionEvent.ITEMS_ADDED && event.getSource() instanceof ISyncItem) {
						Operation parentOp = ((ISyncItem) event.getSource()).getOperation();
						ISyncItem[] items = event.getItems();
						SyncModelUtil.setOperation(items, parentOp, false);
						syncItems.addAll(Arrays.asList(items));
					}
				}
			});
			try {
				session.fetchTree(unread.toArray(new ISyncItem[unread.size()]), true, monitor);
			} finally {
				session.removeListener(listener);
			}
			if (oldCount != syncItems.size()) {
				ISyncItem[] items = sort(syncItems.toArray(new ISyncItem[syncItems.size()]));
				syncItems.clear();
				for (ISyncItem i : items) {
					syncItems.add(i);
				}
			}
		}
		for (int i = syncItems.size()-1; i >=0; --i) {
			ISyncItem item = syncItems.get(i);
			((SyncItem) item).resetState();
			workQueue.add(item);
		}		
	}
	
	/**
	 * @return the syncItems
	 */
	public ISyncItem[] getSyncItems() {
		return syncItems.toArray(new ISyncItem[syncItems.size()]);
	}

	public synchronized ISyncItem next() {
		while (!workQueue.isEmpty()) {
			ISyncItem next = workQueue.pop();
			if (!hasBlocker(next)) {
				active.add(next);
				return next;
			} else {
				workQueue.push(next);
				try {
					wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		return null;
	}
	
	public synchronized int getRemainingCount() {
		return workQueue.size() + active.size();
	}
	
	public synchronized void done(ISyncItem item) {
		active.remove(item);
		notifyAll();
	}
	
	public synchronized ISyncItem[] getActiveItems() {
		return active.toArray(new ISyncItem[active.size()]);
	}
	
	private static ISyncItem[] sort(ISyncItem[] items) {
		Arrays.sort(items, new Comparator<ISyncItem>() {

			public int compare(ISyncItem o1, ISyncItem o2) {
				int cat1 = category(o1);
				int cat2 = category(o2);
				if (cat1 != cat2) {
					return cat1 - cat2;
				}
				return (-1*cat1)*o1.getPath().toPortableString().compareTo(o2.getPath().toPortableString());
			}
			
			private int category(ISyncItem o) {
				if (o.getType() == Type.FOLDER) {
					Operation operation = o.getOperation();
					if (operation == Operation.DELETE_ON_LEFT || operation == Operation.DELETE_ON_RIGHT) {
						return 1;
					}
				}
				return -1;
			}
		});
		return items;
	}
	
	private boolean hasBlocker(ISyncItem item) {
		for (ISyncItem i : active) {
			if (depends(i, item)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean depends(ISyncItem o1, ISyncItem o2) {
		IPath path1 = o1.getPath();
		IPath path2 = o2.getPath();
		if (path1.segmentCount() < path2.segmentCount()) {
			return path1.isPrefixOf(path2);
		} else {
			return path2.isPrefixOf(path1);
		}
	}

}
