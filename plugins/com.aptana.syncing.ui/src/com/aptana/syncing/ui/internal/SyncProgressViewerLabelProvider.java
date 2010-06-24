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

package com.aptana.syncing.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.aptana.ide.syncing.ui.SyncingUIPlugin;
import com.aptana.syncing.core.model.ISyncItem;
import com.aptana.syncing.core.model.ISyncItem.SyncStatus;

/**
 * @author Max Stepanov
 *
 */
public class SyncProgressViewerLabelProvider extends DecoratingLabelProvider implements ITableLabelProvider {


	/**
	 * 
	 */
	public SyncProgressViewerLabelProvider() {
		super(new WorkbenchLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return getImage(element);
        case 1:
        	return getStatusImage((ISyncItem) element);
        default:
            return null;
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.DecoratingLabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		ISyncItem syncItem = (ISyncItem) element;
		return syncItem.getPath().toPortableString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	@Override
	public String getColumnText(Object element, int columnIndex) {
        switch (columnIndex) {
        case 0:
        	return getText(element);
        case 2:
        	return getResultText((ISyncItem) element);
        default:
        	break;
        }
        return ""; //$NON-NLS-1$
	}
	
	private Image getStatusImage(ISyncItem syncItem) {
		SyncStatus result = syncItem.getSyncResult();
		if (result == null) {
			return null;
		}
		switch (result) {
		case SUCCEEDED:
			return SyncingUIPlugin.getImage("/icons/full/obj16/sync_ok.png");
		case FAILED:
			return SyncingUIPlugin.getImage("/icons/full/obj16/sync_failed.png");
		case IN_PROGRESS:
			switch (syncItem.getOperation()) {
			case LEFT_TO_RIGHT:
				return SyncingUIPlugin.getImage("/icons/full/obj16/sync_right.png");
			case RIGHT_TO_LEFT:
				return SyncingUIPlugin.getImage("/icons/full/obj16/sync_left.png");
			}
		default:
			return null;
		}
	}
	
	private String getResultText(ISyncItem syncItem) {
		if (syncItem.getSyncResult() == SyncStatus.FAILED) {
			CoreException exception = syncItem.getSyncError();
			if (exception != null) {
				return exception.getMessage();
			}
		} else if (syncItem.getSyncResult() == SyncStatus.IN_PROGRESS) {
			return ""+syncItem.getSyncProgress();
		}
		return "";
	}

}
