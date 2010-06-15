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

package com.aptana.syncing.ui.dialogs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.aptana.ide.syncing.core.ISiteConnection;
import com.aptana.ide.syncing.core.SyncingPlugin;
import com.aptana.ide.ui.io.navigator.FileTreeContentProvider;
import com.aptana.syncing.core.events.ISyncSessionListener;
import com.aptana.syncing.core.events.SyncItemEvent;
import com.aptana.syncing.core.model.ISyncSession;
import com.aptana.syncing.ui.internal.SyncViewerLabelProvider;
import com.aptana.syncing.ui.internal.SyncViewerSorter;
import com.aptana.ui.IDialogConstants;
import com.aptana.ui.io.epl.AccumulatingProgressMonitor;

/**
 * @author Max Stepanov
 *
 */
public class SyncDialog extends TitleAreaDialog implements ISyncSessionListener {

	private TreeViewer treeViewer;
	private ISyncSession session;
	private ProgressMonitorPart progressMonitorPart;
	private IProgressMonitor progressMonitorWrapper;
	
	/**
	 * @param parentShell
	 */
	public SyncDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		setHelpAvailable(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Synchronization Dialog");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#getInitialSize()
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(700, 600);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		
		setTitle("Title");
		setMessage("message");
		
		Composite container = new Composite(dialogArea, SWT.NONE);
		container.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		container.setLayout(GridLayoutFactory.swtDefaults()
				.margins(convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN), convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN))
				.spacing(convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING), convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING))
				.create());
		
		treeViewer = new TreeViewer(container, SWT.VIRTUAL);
		treeViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		treeViewer.setContentProvider(new FileTreeContentProvider());
		treeViewer.setLabelProvider(new SyncViewerLabelProvider());
		treeViewer.setComparator(new SyncViewerSorter());
		
		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		TreeColumn column = new TreeColumn(tree, SWT.LEFT);
		column.setText("File");
		column.setWidth(200);

		column = new TreeColumn(tree, SWT.LEFT);
		column.setText("State");
		column.setWidth(30);

		column = new TreeColumn(tree, SWT.RIGHT);
		column.setText("Local Size");
		column.setWidth(70);

		column = new TreeColumn(tree, SWT.RIGHT);
		column.setText("Remote Size");
		column.setWidth(70);

		column = new TreeColumn(tree, SWT.LEFT);
		column.setText("Local Time");
		column.setWidth(140);

		column = new TreeColumn(tree, SWT.LEFT);
		column.setText("Remote Time");
		column.setWidth(140);

		progressMonitorPart = new ProgressMonitorPart(container, GridLayoutFactory.fillDefaults().create());
		progressMonitorPart.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).exclude(true).create());
		progressMonitorWrapper = new AccumulatingProgressMonitor(new ProgressMonitorWrapper(progressMonitorPart) {
			@Override
			public void beginTask(String name, int totalWork) {
				super.beginTask(name, totalWork);
				if (((GridData) progressMonitorPart.getLayoutData()).exclude) {
					((GridData) progressMonitorPart.getLayoutData()).exclude = false;
					progressMonitorPart.getParent().layout();
				}
			}

			@Override
			public void done() {
				super.done();
				if (!((GridData) progressMonitorPart.getLayoutData()).exclude) {
					((GridData) progressMonitorPart.getLayoutData()).exclude = true;
					progressMonitorPart.getParent().layout();
				}
			}
		}, progressMonitorPart.getDisplay());
		
		return dialogArea;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		postCreate();
		return control;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TrayDialog#close()
	 */
	@Override
	public boolean close() {
		showProgress(false);
		return super.close();
	}

	private void postCreate() {
		session.addListener(this);
		if (SyncingPlugin.getSyncManager().isSessionInProgress(session)) {
			showProgress(true);
		}
		treeViewer.setInput(session);
	}
	
	private void showProgress(boolean show) {
		((GridData) progressMonitorPart.getLayoutData()).exclude = !show;
		progressMonitorPart.getParent().layout();
		if (show) {
			SyncingPlugin.getSyncManager().addProgressMonitorListener(session, progressMonitorWrapper);
		} else {
			SyncingPlugin.getSyncManager().removeProgressMonitorListener(session, progressMonitorWrapper);
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.events.ISyncSessionListener#handleEvent(com.aptana.syncing.core.events.SyncItemEvent)
	 */
	@Override
	public void handleEvent(final SyncItemEvent event) {
		getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				handleEventUI(event);
			}
		});
	}
	
	private void handleEventUI(SyncItemEvent event) {
		switch (event.getKind()) {
		case SyncItemEvent.ITEMS_ADDED:
			treeViewer.add(event.getSource(), event.getItems());
			break;
		case SyncItemEvent.ITEMS_REMOVED:
			break;
		case SyncItemEvent.ITEMS_UPDATED:
			break;
		}

	}

	public void setSiteConnection(ISiteConnection siteConnection) {
		session = SyncingPlugin.getSyncManager().getSyncSession(siteConnection);
		/*if (session != null) {
			if (!MessageDialog.openQuestion(getShell(), "Question", "Do you want to use saved synchronization state?")) {
				session = null;
			}
		}*/
		if (session == null) {
			session = SyncingPlugin.getSyncManager().createSyncSession(siteConnection);
			SyncingPlugin.getSyncManager().runFetchTree(session);
		}
	}

}
