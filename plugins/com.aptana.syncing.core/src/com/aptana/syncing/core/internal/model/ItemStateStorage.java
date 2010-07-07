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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import com.aptana.ide.syncing.core.SyncingPlugin;

/**
 * @author Max Stepanov
 *
 */
/* package */ final class ItemStateStorage {
	// TODO: cleanup unused(deleted) states (hook siteConnection removal?)

	private static final String TABLE_NAME = "states"; //$NON-NLS-1$
	private static final String INDEX_NAME = "full_id"; //$NON-NLS-1$
	private static final String TABLE_SCHEMA = "CREATE TABLE "+TABLE_NAME+" (left_uri TEXT NOT NULL, right_uri TEXT NOT NULL, left_state TEXT, right_state TEXT)"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String INDEX_SCHEMA = "CREATE INDEX "+INDEX_NAME+" ON "+TABLE_NAME+"(left_uri, right_uri)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
	private static ItemStateStorage instance;
	private Map<SyncIdentifier, ItemStatePair> cache = new HashMap<SyncIdentifier, ItemStatePair>();
	private SqlJetDb db;
	
	/**
	 * 
	 */
	private ItemStateStorage() {
	}
	
	public static ItemStateStorage getInstance() {
		if (instance == null) {
			instance = new ItemStateStorage();
		}
		return instance;
	}

	public ItemStatePair getState(SyncIdentifier id) {
		boolean swap = !id.normalized();
		if (swap) {
			id = id.swap();
		}
		ItemStatePair state = cache.get(id);
		if (state == null && !cache.containsKey(id)) {
			state = loadStateInternal(id);
			//cache.put(id, state);
		}
		if (swap) {
			state = state != null ? state.swap() : state;
		}
		return state;
	}

	public void saveState(SyncIdentifier id, ItemStatePair state) {
		if (!id.normalized()) {
			id = id.swap();
			state = state != null ? state.swap() : state;
		}
		ItemStatePair prevState = cache.get(id);
		// TODO enable cache after making it LRU-like
		//cache.put(id, state);
		if (prevState == null || !prevState.equals(state)) {
			saveStateInternal(id, state);
		}
	}

	private synchronized ItemStatePair loadStateInternal(SyncIdentifier id) {
		try {
			initDB();
			db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			try {
				ISqlJetTable table = db.getTable(TABLE_NAME);
				ISqlJetCursor cursor = table.lookup(INDEX_NAME, id.left.toString(), id.right.toString());
				if (cursor.first()) {
					ItemState left = ItemState.fromString(cursor.getString(2));
					ItemState right = ItemState.fromString(cursor.getString(3));
					return new ItemStatePair(left, right);
				}
			} catch (SqlJetException e) {
				db.rollback();
				throw e;
			} finally {
				db.commit();
			}
		} catch (SqlJetException e) {
			SyncingPlugin.log(e);
		}
		return null;
	}
	
	private synchronized void saveStateInternal(SyncIdentifier id, ItemStatePair state) {
		try {
			initDB();
			db.beginTransaction(SqlJetTransactionMode.WRITE);
			try {
				ISqlJetTable table = db.getTable(TABLE_NAME);
				ISqlJetCursor cursor = table.lookup(INDEX_NAME, id.left.toString(), id.right.toString());
				if (state == null || state.isNull()) {
					if (cursor.first()) {
						cursor.delete();
					}
				} else if (cursor.first()) {
					cursor.update(id.left.toString(), id.right.toString(), state.left != null ? state.left.toString() : null, state.right != null ? state.right.toString() : null);
				} else {
					table.insert(id.left.toString(), id.right.toString(), state.left != null ? state.left.toString() : null, state.right != null ? state.right.toString() : null);
				}
			} catch (SqlJetException e) {
				db.rollback();
				throw e;
			} finally {
				db.commit();
			}
		} catch (SqlJetException e) {
			SyncingPlugin.log(e);
		}
	}
	
	private synchronized void initDB() throws SqlJetException {
		if (db != null) {
			return;
		}
		try {
			db = SqlJetDb.open(getDBPath().toFile(), true);
			try {
				db.getTable(TABLE_NAME);
				return;
			} catch (SqlJetException e) {
			}
			db.getOptions().setAutovacuum(true);
			db.beginTransaction(SqlJetTransactionMode.EXCLUSIVE);
			try {
				db.getOptions().setUserVersion(this.hashCode());
				db.createTable(TABLE_SCHEMA);
				db.createIndex(INDEX_SCHEMA);
			} catch(SqlJetException e) {
				db.rollback();
				throw e;
			} finally {
				db.commit();
			}
		} catch (SqlJetException e) {
			close();
			throw e;
		}
	}
	
	public static IPath getDBPath() {
		return SyncingPlugin.getDefault().getStateLocation().append("db").addFileExtension("sqlite"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public synchronized void close() {
		if (db != null) {
			try {
				db.close();
			} catch (SqlJetException e) {
				SyncingPlugin.log(e);
			}
			db = null;
		}
	}

	public synchronized void clear() {
		close();
		getDBPath().toFile().delete();
	}

}

