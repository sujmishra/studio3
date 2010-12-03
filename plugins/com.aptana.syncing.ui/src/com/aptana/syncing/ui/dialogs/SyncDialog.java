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

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.aptana.core.util.StringUtil;
import com.aptana.ide.syncing.core.SyncingPlugin;
import com.aptana.ide.syncing.ui.SyncingUIPlugin;
import com.aptana.ide.syncing.ui.old.editors.FileCompareEditorInput;
import com.aptana.ide.ui.io.navigator.FileTreeContentProvider;
import com.aptana.syncing.core.events.ISyncSessionListener;
import com.aptana.syncing.core.events.SyncSessionEvent;
import com.aptana.syncing.core.model.ISyncItem;
import com.aptana.syncing.core.model.ISyncItem.Changes;
import com.aptana.syncing.core.model.ISyncItem.Operation;
import com.aptana.syncing.core.model.ISyncItem.Type;
import com.aptana.syncing.core.model.ISyncSession;
import com.aptana.syncing.core.model.ISyncSession.Stage;
import com.aptana.syncing.core.model.SyncModelUtil;
import com.aptana.syncing.ui.internal.FlatTreeContentProvider;
import com.aptana.syncing.ui.internal.SearchViewerFilter;
import com.aptana.syncing.ui.internal.SyncStatusViewerFilter;
import com.aptana.syncing.ui.internal.SyncUIManager;
import com.aptana.syncing.ui.internal.SyncViewerFilter;
import com.aptana.syncing.ui.internal.SyncViewerLabelProvider;
import com.aptana.syncing.ui.internal.SyncViewerSorter;
import com.aptana.syncing.ui.internal.widgets.SyncStatsComposite;
import com.aptana.ui.UIPlugin;
import com.aptana.ui.actions.SearchToolbarControl;
import com.aptana.ui.io.epl.AccumulatingProgressMonitor;

/**
 * @author Max Stepanov
 *
 */
public class SyncDialog extends TitleAreaDialog implements ISyncSessionListener {

	public static final String TITLE = Messages.SyncDialog_WindowTitle;
	
	private class FilterAction extends Action {

		public FilterAction(String text, ImageDescriptor imageDescriptor) {
			super(text, AS_RADIO_BUTTON);
			setImageDescriptor(imageDescriptor);
		}

		@Override
		public void run() {
			updateFilters();
			treeViewer.expandAll();
		}
	}
	
	private class SyncTreeViewer extends TreeViewer {

		public SyncTreeViewer(Composite parent, int style) {
			super(parent, style);
		}

		public Object[] getAllSortedChildren() {
			List<Object> list = new ArrayList<Object>();
			for (Object element : getSortedChildren(getInput())) {
				list.add(element);
				addSortedChildren(element, list);				
			}
			return list.toArray();
		}		

		public Object[] getAllFilteredChildren() {
			List<Object> list = new ArrayList<Object>();
			for (Object element : getFilteredChildren(getInput())) {
				list.add(element);
				addFilteredChildren(element, list);
			}
			return list.toArray();
		}		

		private void addSortedChildren(Object parent, List<Object> list) {
			for (Object element : getSortedChildren(parent)) {
				list.add(element);
				addSortedChildren(element, list);
			}
		}

		private void addFilteredChildren(Object parent, List<Object> list) {
			for (Object element : getFilteredChildren(parent)) {
				list.add(element);
				addFilteredChildren(element, list);
			}
		}

	}

	private static final String IMAGE = "/icons/full/wizban/sync.png"; //$NON-NLS-1$

	private SyncTreeViewer treeViewer;
	private ISyncSession session;
	private ProgressMonitorPart progressMonitorPart;
	private IProgressMonitor progressMonitorWrapper;
	private SyncStatsComposite statsComposite;
	private Composite conflictsOptionsComposite;
	private Composite syncedComposite;
			
	private SyncViewerLabelProvider labelProvider;
	
	private IAction hideSameAction;
	private IAction flatModeAction;
	private IAction allFilterAction;
	private IAction incomingFilterAction;
	private IAction outgoingFilterAction;
	private IAction conflictsFilterAction;
	
	private IAction showDiffAction;
	private IAction expandSelectionAction;
	private IAction expandAllAction;
	
	private IAction syncToLeft;
	private IAction syncToRight;
	private IAction syncNoOp;
	private IAction syncReset;
	
	private ViewerFilter searchFilter;
	private Boolean hasUnfetched;

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

		setTitleImage(SyncingUIPlugin.getImage(IMAGE));
		setTitle(Messages.SyncDialog_Title);
		setMessage(MessageFormat.format(Messages.SyncDialog_Message, session.getSourceConnectionPoint().getName(),
				session.getDestinationConnectionPoint().getName()));

		Composite container = new Composite(dialogArea, SWT.NONE);
		container.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		container.setLayout(GridLayoutFactory.swtDefaults()
				.margins(convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN), convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN))
				.spacing(convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING), convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING))
				.create());
		
		ToolBarManager toolBarManager = new ToolBarManager(SWT.HORIZONTAL | SWT.FLAT | SWT.RIGHT);
		ToolBar toolBar = toolBarManager.createControl(container);
		toolBar.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).create());
		
		treeViewer = new SyncTreeViewer(container, SWT.VIRTUAL | SWT.MULTI);
		treeViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		treeViewer.setLabelProvider(labelProvider = new SyncViewerLabelProvider());
		treeViewer.setComparator(new SyncViewerSorter());
		
		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		TreeColumn column = new TreeColumn(tree, SWT.LEFT);
		column.setText(Messages.SyncDialog_Table_File);

		column = new TreeColumn(tree, SWT.LEFT);
		column.setText(Messages.SyncDialog_Table_State);

		column = new TreeColumn(tree, SWT.RIGHT);
		column.setText(Messages.SyncDialog_Table_LocalSize);

		column = new TreeColumn(tree, SWT.RIGHT);
		column.setText(Messages.SyncDialog_Table_RemoteSize);

		column = new TreeColumn(tree, SWT.LEFT);
		column.setText(Messages.SyncDialog_Table_LocalTime);

		column = new TreeColumn(tree, SWT.LEFT);
		column.setText(Messages.SyncDialog_Table_RemoteTime);
		
		tree.setLayout(createTableLayout());
		
		MenuManager menuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		tree.setMenu(menuManager.createContextMenu(tree));
		menuManager.setRemoveAllWhenShown(false);
		
		conflictsOptionsComposite = createConflictsOptions(container);
		conflictsOptionsComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).exclude(true).create());

		statsComposite = new SyncStatsComposite(container, SWT.NONE);
		statsComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).exclude(true).create());

		progressMonitorPart = new ProgressMonitorPart(container, GridLayoutFactory.fillDefaults().create());
		progressMonitorPart.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).exclude(true).create());
		progressMonitorWrapper = new AccumulatingProgressMonitor(new ProgressMonitorWrapper(progressMonitorPart) {
			@Override
			public void beginTask(String name, int totalWork) {
				super.beginTask(name, totalWork);
				if (((GridData) progressMonitorPart.getLayoutData()).exclude) {
					((GridData) progressMonitorPart.getLayoutData()).exclude = false;
					progressMonitorPart.setVisible(true);
					progressMonitorPart.getParent().layout();
				}
			}

			@Override
			public void done() {
				super.done();
				if (!((GridData) progressMonitorPart.getLayoutData()).exclude) {
					((GridData) progressMonitorPart.getLayoutData()).exclude = true;
					progressMonitorPart.setVisible(false);
					progressMonitorPart.getParent().layout();
					updateState();
				}
			}
		}, progressMonitorPart.getDisplay());

		syncedComposite = createSyncedSection(dialogArea);
		syncedComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).exclude(true).create());

		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				ViewerCell cell = treeViewer.getCell(new Point(e.x, e.y));
				if (cell != null && cell.getColumnIndex() == 1) {
					changeOperationForItem((ISyncItem) cell.getElement());
				}
			}
		});
		tree.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				((Tree) e.widget).setLayout(createTableLayout());
			}
		});
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				updateActions((IStructuredSelection) event.getSelection());
			}
		});
		
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISyncItem syncItem = (ISyncItem) ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (syncItem.getChanges() == Changes.CONFLICT) {
					showDiff(syncItem);
				} else if (syncItem.getType() == Type.FOLDER && syncItem.getChildItems() == null){
					fetchFolders(Arrays.asList(syncItem));
				}
			}
		});
		
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				updateActions((IStructuredSelection) treeViewer.getSelection());
			}
		});

		createActions();
		fillToolBar(toolBarManager);
		fillContextMenu(menuManager);
		
		updateFilters();
		updatePresentationMode();
		
		return dialogArea;
	}

	private Composite createSyncedSection(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
		composite.setBackgroundMode(SWT.INHERIT_DEFAULT);

		Label label = new Label(composite, SWT.CENTER);
		label.setImage(SyncingUIPlugin.getImage("icons/full/obj16/synced.png")); //$NON-NLS-1$
		label.setLayoutData(GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).grab(true, true).create());

		label = new Label(composite, SWT.CENTER | SWT.WRAP);
		Font font = new Font(composite.getDisplay(), "Arial", 12, SWT.NONE); //$NON-NLS-1$
		label.setFont(font);
		label.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				((Control)e.widget).getFont().dispose();
			}
		});
		label.setLayoutData(GridDataFactory.swtDefaults().grab(true, true).create());
		label.setText(MessageFormat.format(Messages.SyncDialog_inSyncMessage,
				session.getSourceConnectionPoint().getName(), session.getDestinationConnectionPoint().getName()));

		return composite;
	}
	
	private Composite createConflictsOptions(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayout(GridLayoutFactory.swtDefaults().numColumns(4).equalWidth(false).create());

		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object data = e.widget.getData();
				if (data instanceof Runnable) {
					((Runnable) data).run();
				} else if (data instanceof Operation) {
					Operation op = (Operation) data;
					List<ISyncItem> items = getVisibleItems(Changes.CONFLICT);
					for (ISyncItem i : items) {
						i.setOperation(op);
					}
					treeViewer.update(items.toArray(), null);
					updateState();
				}
			}
		};
		// row 1
		Composite composite = new Composite(group, SWT.NONE);
		composite.setLayoutData(GridDataFactory.fillDefaults().span(4, 1).create());
		composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
	
		Label label = new Label(composite, SWT.LEAD);
		Image image = AbstractUIPlugin.imageDescriptorFromPlugin(UIPlugin.PLUGIN_ID, "/icons/full/obj16/warning.png").createImage(); //$NON-NLS-1$
		label.setImage(image);
		label.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Label label = (Label) e.widget;
				Image image = label.getImage();
				if (image != null) {
					label.setImage(null);
					image.dispose();
				}
			}
		});
		label.setLayoutData(GridDataFactory.fillDefaults().create());

		label = new Label(composite, SWT.LEAD);
		label.setText(StringUtil.makeFormLabel("Some file have been found conflicting. What you would like to do"));
		label.setLayoutData(GridDataFactory.swtDefaults().grab(true, false).create());
		
		// row 2
		Button radio = new Button(group, SWT.RADIO);
		radio.setText("Do nothing");
		radio.setToolTipText("Ignore conflicting file for now");
		radio.setData(Operation.NONE);
		radio.addSelectionListener(listener);
		radio.setSelection(true);
		radio.setLayoutData(GridDataFactory.swtDefaults().create());

		radio = new Button(group, SWT.RADIO);
		radio.setText("Use local files");
		radio.setToolTipText("Replace conflicting remote files with local copies");
		radio.setData(Operation.COPY_TO_RIGHT);
		radio.addSelectionListener(listener);
		radio.setLayoutData(GridDataFactory.swtDefaults().create());

		Button markSameButton = new Button(group, SWT.PUSH);
		markSameButton.setText("Mark as same (Ignore)");
		markSameButton.setToolTipText("Files will not appear during synchronizations until they are changed");
		markSameButton.setLayoutData(GridDataFactory.swtDefaults().hint(
				new PixelConverter(markSameButton).convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH*2),
				SWT.DEFAULT).create());
		markSameButton.setData(new Runnable() {
			public void run() {
				List<ISyncItem> items = getVisibleItems(Changes.CONFLICT);
				if (new IgnoreFilesPromptDialog(getShell(), items).open() == Window.OK) {
					for (ISyncItem i : items) {
						i.setOperation(Operation.MARK_SAME);
					}
					treeViewer.refresh(true);
					updateState();
					analyseItems();
				}
			}
		});
		markSameButton.addSelectionListener(listener);

		Button compareContentButton = new Button(group, SWT.PUSH);
		compareContentButton.setText("Compare content");
		compareContentButton.setToolTipText("Compare local and remote file contents");
		compareContentButton.setLayoutData(GridDataFactory.swtDefaults().hint(
				new PixelConverter(compareContentButton).convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH*2),
				SWT.DEFAULT).create());

		// row 3
		radio = new Button(group, SWT.RADIO);
		radio.setText("Use newer files");
		radio.setToolTipText("Use files that have latest modification time");
		radio.setData(new Runnable() {
			public void run() {
				List<ISyncItem> items = getVisibleItems(Changes.CONFLICT);
				for (ISyncItem i : items) {
					i.setOperation(i.getLeftFileInfo().getLastModified() >= i.getRightFileInfo().getLastModified() ? Operation.COPY_TO_RIGHT : Operation.COPY_TO_LEFT);
				}
				treeViewer.update(items.toArray(), null);
				updateState();
			}
		});
		radio.addSelectionListener(listener);
		radio.setLayoutData(GridDataFactory.swtDefaults().create());

		radio = new Button(group, SWT.RADIO);
		radio.setText("Use remote files");
		radio.setToolTipText("Replace conflicting local files with remote copies");
		radio.setData(Operation.COPY_TO_LEFT);
		radio.addSelectionListener(listener);
		radio.setLayoutData(GridDataFactory.swtDefaults().create());

		compareContentButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MessageDialog.openInformation(getShell(), "Aptana Studio 3 Beta", "To be implemented.");
			}
		});
		return group;
	}

	private TableLayout createTableLayout() {
		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(new ColumnWeightData(50, 200));
		tableLayout.addColumnData(new ColumnPixelData(30, false));
		tableLayout.addColumnData(new ColumnPixelData(70, true));
		tableLayout.addColumnData(new ColumnPixelData(70, true));
		tableLayout.addColumnData(new ColumnPixelData(140, true));
		tableLayout.addColumnData(new ColumnPixelData(140, true));
		return tableLayout; 
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
		session.removeListener(this);
		showProgress(false);
		SyncUIManager.getInstance().onCloseUI(session);
		return super.close();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		if (!SyncingPlugin.getSyncManager().isSyncInProgress(session)) {
			session.setSyncItems(getActiveItems());
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
				&& !MessageDialog.openQuestion(getShell(), Messages.SyncDialog_StopConfirmation, Messages.SyncDialog_StopMessage)) {
			return;
		}
		SyncingPlugin.getSyncManager().closeSession(session);
		super.cancelPressed();
	}

	private void postCreate() {
		getShell().setDefaultButton(null);
		session.addListener(this);
		boolean inProgress = SyncingPlugin.getSyncManager().isSyncInProgress(session);
		showProgress(inProgress);
		treeViewer.setInput(session);
		if (hideSameAction.isChecked()) {
			treeViewer.expandAll();
		}
		updateState();
		if (!inProgress) {
			analyseItems();
		}
	}
	
	private void showProgress(boolean show) {
		showStats(!show);
		((GridData) progressMonitorPart.getLayoutData()).exclude = !show;
		progressMonitorPart.setVisible(show);
		progressMonitorPart.getParent().layout();
		if (show) {
			SyncingPlugin.getSyncManager().addProgressMonitorListener(session, progressMonitorWrapper);
		} else {
			SyncingPlugin.getSyncManager().removeProgressMonitorListener(session, progressMonitorWrapper);			
		}
		getButton(IDialogConstants.OK_ID).setText(show ? Messages.SyncDialog_RunInBackground : Messages.SyncDialog_SynchronizeButton);
		setButtonLayoutData(getButton(IDialogConstants.OK_ID));
		((Composite) getButtonBar()).layout();
	}
	
	private void showStats(boolean show) {
		((GridData) statsComposite.getLayoutData()).exclude = !show;
		statsComposite.setVisible(show);
		statsComposite.getParent().layout();		
	}
	
	private void showConflictOptions(boolean show) {
		((GridData) conflictsOptionsComposite.getLayoutData()).exclude = !show;
		conflictsOptionsComposite.setVisible(show);
		conflictsOptionsComposite.getParent().layout();				
	}

	private void showSyncedSection(boolean show) {
		((GridData) treeViewer.getControl().getParent().getLayoutData()).exclude = show;
		treeViewer.getControl().getParent().setVisible(!show);
		((GridData) syncedComposite.getLayoutData()).exclude = !show;
		syncedComposite.setVisible(show);
		syncedComposite.getParent().layout();
		getButton(IDialogConstants.CANCEL_ID).setText(Messages.SyncDialog_Close);
	}

	/* (non-Javadoc)
	 * @see com.aptana.syncing.core.events.ISyncSessionListener#handleEvent(com.aptana.syncing.core.events.SyncSessionEvent)
	 */
	public void handleEvent(final SyncSessionEvent event) {
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				handleEventUI(event);
			}
		});
	}
	
	private void handleEventUI(SyncSessionEvent event) {
		switch (event.getKind()) {
		case SyncSessionEvent.ITEMS_ADDED:
		case SyncSessionEvent.ITEMS_REMOVED:
			treeViewer.refresh(event.getSource());
			treeViewer.setExpandedState(event.getSource(), true);
			break;
		case SyncSessionEvent.ITEMS_UPDATED:
			treeViewer.update(event.getItems(), null);
			break;
		case SyncSessionEvent.SESSION_STAGE_CHANGED:
			if (session.getStage() != Stage.FETCHING) {
				onFetchComplete();
			}
			break;
		}
	}
	
	private void changeOperationForItem(ISyncItem syncItem) {
		Set<Operation> allowed = syncItem.getAllowedOperations();
		Operation current = syncItem.getOperation();
		Operation next = allowed.iterator().next(); // first
		for (Iterator<Operation> i = allowed.iterator(); i.hasNext(); ) {
			if (i.next() == current) {
				if (i.hasNext()) {
					next = i.next(); // next
				}
				break;
			}
		}
		syncItem.setOperation(next);
		if (syncItem.getType() == Type.FOLDER) {
			treeViewer.refresh(syncItem);
		} else {
			treeViewer.update(syncItem, null);
		}
		updateState();
	}

	public void setSession(ISyncSession session) {
		this.session = session;
	}
	
	private void createActions() {
		hideSameAction = new Action(Messages.SyncDialog_Action_HideIdenticalFiles, Action.AS_CHECK_BOX) {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/filter.png")); //$NON-NLS-1$
			}
			@Override
			public void run() {
				updateFilters();
				treeViewer.expandAll();
			}
		};
		hideSameAction.setChecked(true);
		
		flatModeAction = new Action(Messages.SyncDialog_Action_FlatMode, Action.AS_CHECK_BOX) {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/flat.png")); //$NON-NLS-1$
			}
			@Override
			public void run() {
				try {
					treeViewer.getTree().setRedraw(false);
					updateFilters();
					updatePresentationMode();
				} finally {
					treeViewer.getTree().setRedraw(true);
				}
			}
		};
		
		incomingFilterAction = new FilterAction(Messages.SyncDialog_Action_IncomingOnly, SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/incoming.png")); //$NON-NLS-1$
		outgoingFilterAction = new FilterAction(Messages.SyncDialog_Actioin_OutgoingOnly, SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/outgoing.png")); //$NON-NLS-1$
		conflictsFilterAction = new FilterAction(Messages.SyncDialog_Action_ConflictsOnly, SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/conflict.png")); //$NON-NLS-1$
		allFilterAction = new FilterAction(Messages.SyncDialog_Action_All, SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/both.png")); //$NON-NLS-1$
		allFilterAction.setChecked(true);
		
		showDiffAction = new Action(Messages.SyncDialog_Action_ShowDiff) {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/compare.png")); //$NON-NLS-1$
			}
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				if (!selection.isEmpty()) {
					showDiff((ISyncItem) selection.getFirstElement());
				}
			}
		};
		expandSelectionAction = new Action(Messages.SyncDialog_Action_ExpandSelected) {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/expand_all.png")); //$NON-NLS-1$
			}
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				fetchFolders(getUnfetchedFolders(((IStructuredSelection) treeViewer.getSelection()).toList(), null));
			}
		};
		expandAllAction = new Action(Messages.SyncDialog_Action_ExpandAll) {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/expand_all.png")); //$NON-NLS-1$
			}
			@Override
			public void run() {
				fetchFolders(getAllUnfetchedFolders());
			}
		};
		syncToLeft = new Action("Update local files") {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/obj16/sync_left.png")); //$NON-NLS-1$
			}
			@Override
			public void run() {
				List<?> selection = ((IStructuredSelection) treeViewer.getSelection()).toList();
				SyncModelUtil.setOperation(selection, Operation.COPY_TO_LEFT);
				treeViewer.update(selection.toArray(), null);
			}
		};
		syncToRight = new Action("Update remote files") {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/obj16/sync_right.png")); //$NON-NLS-1$
			}
			@Override
			public void run() {
				List<?> selection = ((IStructuredSelection) treeViewer.getSelection()).toList();
				SyncModelUtil.setOperation(selection, Operation.COPY_TO_RIGHT);
				treeViewer.update(selection.toArray(), null);
			}
		};
		syncNoOp = new Action("Skip update") {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/obj16/sync_none.png")); //$NON-NLS-1$
			}
			@Override
			public void run() {
				List<?> selection = ((IStructuredSelection) treeViewer.getSelection()).toList();
				SyncModelUtil.setOperation(selection, Operation.NONE);
				treeViewer.update(selection.toArray(), null);
			}
		};
		syncReset = new Action("Reset to default") {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/obj16/sync_none.png")); //$NON-NLS-1$
			}
			@Override
			public void run() {
				List<?> selection = ((IStructuredSelection) treeViewer.getSelection()).toList();
				SyncModelUtil.setOperation(selection, null);
				treeViewer.update(selection.toArray(), null);
			}
		};
	}
	
	private void fillToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(expandAllAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(hideSameAction);
		toolBarManager.add(flatModeAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(incomingFilterAction);
		toolBarManager.add(allFilterAction);
		toolBarManager.add(outgoingFilterAction);
		toolBarManager.add(conflictsFilterAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(new SearchToolbarControl() {
			@Override
			public void search(String text, boolean isCaseSensitive, boolean isRegularExpression) {
				setSearchFilter(StringUtil.isEmpty(text) ? null : new SearchViewerFilter(text, isCaseSensitive,
						isRegularExpression));
			}
		});
		toolBarManager.update(true);
	}
	
	private void fillContextMenu(IMenuManager menuManager) {
		menuManager.add(showDiffAction);
		menuManager.add(expandSelectionAction);
		menuManager.add(new Separator());
		menuManager.add(syncToLeft);
		menuManager.add(syncToRight);
		menuManager.add(syncNoOp);
		menuManager.add(new Separator());
		menuManager.add(syncReset);
	}
		
	@SuppressWarnings("unchecked")
	private void updateActions(IStructuredSelection selection) {
		showDiffAction.setEnabled(selection.size() == 1 && ((ISyncItem) selection.getFirstElement()).getType() == Type.FILE);
		expandSelectionAction.setEnabled(!SyncingPlugin.getSyncManager().isSyncInProgress(session) && !getUnfetchedFolders(selection.toList(), null).isEmpty());
		expandAllAction.setEnabled(!SyncingPlugin.getSyncManager().isSyncInProgress(session) && hasUnfetchedFolders());
	}
		
	private void updateFilters() {
		List<ViewerFilter> filters = new ArrayList<ViewerFilter>();
		if (incomingFilterAction.isChecked()) {
			filters.add(new SyncStatusViewerFilter(Changes.RIGHT_TO_LEFT));
		} else if (outgoingFilterAction.isChecked()) {
			filters.add(new SyncStatusViewerFilter(Changes.LEFT_TO_RIGHT));			
		} else if (conflictsFilterAction.isChecked()) {
			filters.add(new SyncStatusViewerFilter(Changes.CONFLICT));			
		}
		if (hideSameAction.isChecked()) {
			filters.add(new SyncViewerFilter());
		}
		if (flatModeAction.isChecked()) {
			filters.add(new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (element instanceof ISyncItem) {
						return ((ISyncItem) element).getType() != Type.FOLDER || ((ISyncItem) element).getChanges() != Changes.NONE;
					}
					return true;
				}
			});
		}
		if (searchFilter != null) {
			filters.add(searchFilter);
		}
		treeViewer.setFilters(filters.toArray(new ViewerFilter[filters.size()]));
		updateState();
	}
	
	private void setSearchFilter(final ViewerFilter filter) {
		this.searchFilter = filter;
		getShell().getDisplay().timerExec(500, new Runnable() {

			public void run() {
				if (searchFilter == filter) {
					updateFilters();
					treeViewer.expandAll();
				}
			}
		});
	}
	
	private void updatePresentationMode() {
		labelProvider.setFlatMode(flatModeAction.isChecked());
		if (flatModeAction.isChecked()) {
			treeViewer.setContentProvider(new FlatTreeContentProvider(new FileTreeContentProvider()));
		} else {
			treeViewer.setContentProvider(new FileTreeContentProvider());
		}
		treeViewer.expandAll();
		updateState();
	}
	
	private List<ISyncItem> getActiveItems() {
		List<ISyncItem> items = new ArrayList<ISyncItem>();
		for (Object element : treeViewer.getAllSortedChildren()) {
			if (element instanceof ISyncItem) {
				ISyncItem syncItem = (ISyncItem) element;
				if (syncItem.getOperation() != Operation.NONE) {
					items.add(syncItem);
				}
			}
		}
		return items;
	}

	private List<ISyncItem> getVisibleItems(Changes changes) {
		List<ISyncItem> conflicts = new ArrayList<ISyncItem>();
		for (Object element : treeViewer.getAllFilteredChildren()) {
			if (element instanceof ISyncItem) {
				ISyncItem item = (ISyncItem) element;
				if (item.getType() != Type.UNSUPPORTED && item.getChanges() == changes) {
					conflicts.add(item);
				}
			}
		}
		return conflicts;
	}

	private static List<ISyncItem> getUnfetchedFolders(List<? extends Object> items, Changes changes) {
		List<ISyncItem> list = new ArrayList<ISyncItem>();
		for (Object i : items) {
			ISyncItem syncItem = (ISyncItem) i;
			if (syncItem.getType() == Type.FOLDER && syncItem.getChildItems() == null && syncItem.getChanges() != Changes.NONE) {
				if (changes == null || syncItem.getChanges() == changes) {
					list.add(syncItem);
				}
			}
		}
		return list;
	}

	private List<ISyncItem> getAllUnfetchedFolders() {
		List<ISyncItem> items = new ArrayList<ISyncItem>();
		for (Object element : treeViewer.getAllSortedChildren()) {
			if (element instanceof ISyncItem) {
				ISyncItem syncItem = (ISyncItem) element;
				if (syncItem.getType() == Type.FOLDER && syncItem.getChildItems() == null && syncItem.getChanges() != Changes.NONE) {
					items.add(syncItem);
				}
			}
		}
		return items;
	}
	
	private boolean hasUnfetchedFolders() {
		if (hasUnfetched == null) {
			hasUnfetched = Boolean.valueOf(!getAllUnfetchedFolders().isEmpty());
		}
		return hasUnfetched;		
	}
	
	private void updateState() {
		if (treeViewer.getInput() == null || SyncingPlugin.getSyncManager().isSyncInProgress(session)) {
			return;
		}
		List<ISyncItem> items = getActiveItems();
		getButton(IDialogConstants.OK_ID).setEnabled(!items.isEmpty());
		statsComposite.updateStats(items);
		showConflictOptions((allFilterAction.isChecked() || conflictsFilterAction.isChecked()) && !getVisibleItems(Changes.CONFLICT).isEmpty());
		updateActions((IStructuredSelection) treeViewer.getSelection());
	}
	
	private void onFetchComplete() {
		showProgress(false);
		treeViewer.refresh(true);
		updateState();
		analyseItems();
	}

	private void showDiff(final ISyncItem syncItem) {
		FileCompareEditorInput compareInput = new FileCompareEditorInput(new CompareConfiguration()) {
			@Override
			protected void prepareFiles(IProgressMonitor monitor) throws CoreException {
				File leftFile = syncItem.getLeftFileStore().toLocalFile(EFS.NONE, monitor);
				setLeftResource(leftFile != null ? leftFile : syncItem.getLeftFileStore().toLocalFile(EFS.CACHE, monitor));
				File rightFile = syncItem.getRightFileStore().toLocalFile(EFS.NONE, monitor);
				setRightResource(rightFile != null ? rightFile : syncItem.getRightFileStore().toLocalFile(EFS.CACHE, monitor));
			}
		};
		CompareUI.openCompareDialog(compareInput);
	}
	
	private void fetchFolders(List<ISyncItem> list) {
		if (!list.isEmpty()) {
			hasUnfetched = null;
			SyncUIManager.getInstance().fetchTree(session, list.toArray(new ISyncItem[list.size()]));
			showProgress(true);	
		}
	}
	
	private void analyseItems() {
		boolean hasChanges = false;
		List<ISyncItem> all = SyncModelUtil.getAllItems(session);
		for (Iterator<ISyncItem> i = all.listIterator(); i.hasNext(); ) {
			ISyncItem item = i.next();
			if (item.getType() != Type.UNSUPPORTED && item.getChanges() != Changes.NONE) {
				hasChanges = true;
			}
		}
		showSyncedSection(!hasChanges);
	}
	
}
