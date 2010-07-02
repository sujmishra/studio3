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

package com.aptana.syncing.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.aptana.ide.syncing.core.SyncingPlugin;
import com.aptana.syncing.core.events.ISyncSessionListener;
import com.aptana.syncing.core.events.SyncSessionEvent;
import com.aptana.syncing.core.model.ISyncItem;
import com.aptana.syncing.core.model.ISyncSession;
import com.aptana.syncing.core.model.ISyncItem.SyncStatus;
import com.aptana.syncing.core.model.ISyncSession.Stage;
import com.aptana.syncing.ui.internal.SyncProgressViewerLabelProvider;
import com.aptana.syncing.ui.internal.SyncUIManager;
import com.aptana.ui.IDialogConstants;

/**
 * @author Max Stepanov
 *
 */
public class SyncProgressDialog extends TitleAreaDialog implements ISyncSessionListener {

	public static final String TITLE = "Synchronization Progress";
	private static final String PROGRESSBAR_TABLEEDITOR_KEY = "table_editor.progress_bar";

	private TableViewer tableViewer;
	private ISyncSession session;
	private List<ISyncItem> active = new ArrayList<ISyncItem>();
	
	/**
	 * @param parentShell
	 */
	public SyncProgressDialog(Shell parentShell) {
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
		newShell.setText(TITLE);
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
		
		tableViewer = new TableViewer(container, SWT.VIRTUAL | SWT.HIDE_SELECTION);
		tableViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		tableViewer.setLabelProvider(new SyncProgressViewerLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		//tableViewer.setSorter(new SyncProgressViewerSorter());
		
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn column = new TableColumn(table, SWT.LEAD);
		column.setText("File");
		column.setWidth(300);

		column = new TableColumn(table, SWT.LEAD);
		column.setText("State");
		column.setWidth(30);

		column = new TableColumn(table, SWT.LEAD);
		column.setWidth(400);
		
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
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Run In Background", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	private void postCreate() {
		session.addListener(this);
		tableViewer.setInput(session.getSyncItems());
		if (session.getStage() == Stage.SYNCED) {
			onSyncComplete();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TrayDialog#close()
	 */
	@Override
	public boolean close() {
		session.removeListener(this);
		clearActiveProgress();
		SyncUIManager.getInstance().onCloseUI(session);
		return super.close();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		if (!SyncingPlugin.getSyncManager().isSyncInProgress(session)) {
			super.okPressed();
		} else {
			super.cancelPressed();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	@Override
	protected void cancelPressed() {
		if (SyncingPlugin.getSyncManager().isSyncInProgress(session)
				&& !MessageDialog.openQuestion(getShell(), "Confirmation", "Do you really want to stop synchronization ?")) {
			return;
		}
		SyncingPlugin.getSyncManager().closeSession(session);
		super.cancelPressed();
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.events.ISyncSessionListener#handleEvent(com.aptana.syncing.core.events.SyncSessionEvent)
	 */
	@Override
	public void handleEvent(final SyncSessionEvent event) {
		getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				handleEventUI(event);
			}
		});
	}
	
	private void handleEventUI(SyncSessionEvent event) {
		switch (event.getKind()) {
		case SyncSessionEvent.ITEMS_UPDATED:
			updateItems(event.getItems());
			break;
		case SyncSessionEvent.SESSION_STAGE_CHANGED:
			if (session.getStage() == Stage.SYNCING || session.getStage() == Stage.PRESYNCING) {
				clearActiveProgress();
				tableViewer.refresh();
			} else {
				clearActiveProgress();
				onSyncComplete();
			}
			break;
		}
	}

	public void setSession(ISyncSession session) {
		this.session = session;
	}
		
	private void updateItems(ISyncItem[] items) {
		tableViewer.update(items, null);
		for (ISyncItem i : items) {
			SyncStatus result = i.getSyncResult();
			if (result != null) {
				TableItem tableItem = (TableItem) tableViewer.testFindItem(i);
				if (tableItem != null) {
					TableEditor tableEditor = (TableEditor) tableItem.getData(PROGRESSBAR_TABLEEDITOR_KEY);
					if (tableEditor == null && result == SyncStatus.IN_PROGRESS) {
						ProgressBar progressBar = new ProgressBar(tableItem.getParent(), SWT.HORIZONTAL);
						progressBar.setMaximum(100);
						progressBar.setSelection(i.getSyncProgress());
						tableEditor = new TableEditor(tableItem.getParent());
						tableEditor.grabHorizontal = tableEditor.grabVertical = true;
						tableEditor.setEditor(progressBar, tableItem, 2);
						tableItem.setData(PROGRESSBAR_TABLEEDITOR_KEY, tableEditor);
						active.add(i);
					} else if (tableEditor != null && result == SyncStatus.IN_PROGRESS) {
						((ProgressBar) tableEditor.getEditor()).setSelection(i.getSyncProgress());						
					} else if (tableEditor != null) {
						tableItem.setData(PROGRESSBAR_TABLEEDITOR_KEY, null);
						tableEditor.getEditor().dispose();
						tableEditor.dispose();
						active.remove(i);
					}
				}
			}
		}
		tableViewer.reveal(items[items.length-1]);
	}
	
	private void clearActiveProgress() {
		for (ISyncItem i : active) {
			TableItem tableItem = (TableItem) tableViewer.testFindItem(i);
			if (tableItem != null) {
				TableEditor tableEditor = (TableEditor) tableItem.getData(PROGRESSBAR_TABLEEDITOR_KEY);
				if (tableEditor != null) {
					tableItem.setData(PROGRESSBAR_TABLEEDITOR_KEY, null);
					tableEditor.getEditor().dispose();
					tableEditor.dispose();
					active.remove(i);
				}
			}
			
		}
	}
	
	private void onSyncComplete() {
		getButton(IDialogConstants.CANCEL_ID).setText("Close");
		getButton(IDialogConstants.OK_ID).setText("Back");
	}

}
