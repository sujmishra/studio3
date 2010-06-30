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
import java.util.ArrayList;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.aptana.ide.syncing.core.SyncingPlugin;
import com.aptana.ide.syncing.ui.SyncingUIPlugin;
import com.aptana.ide.syncing.ui.old.editors.FileCompareEditorInput;
import com.aptana.ide.ui.io.navigator.FileTreeContentProvider;
import com.aptana.syncing.core.events.ISyncSessionListener;
import com.aptana.syncing.core.events.SyncSessionEvent;
import com.aptana.syncing.core.model.ISyncItem;
import com.aptana.syncing.core.model.ISyncSession;
import com.aptana.syncing.core.model.ISyncItem.Changes;
import com.aptana.syncing.core.model.ISyncItem.Operation;
import com.aptana.syncing.core.model.ISyncItem.Type;
import com.aptana.syncing.core.model.ISyncSession.Stage;
import com.aptana.syncing.ui.internal.FlatTreeContentProvider;
import com.aptana.syncing.ui.internal.SearchViewerFilter;
import com.aptana.syncing.ui.internal.SyncStatusViewerFilter;
import com.aptana.syncing.ui.internal.SyncUIManager;
import com.aptana.syncing.ui.internal.SyncViewerFilter;
import com.aptana.syncing.ui.internal.SyncViewerLabelProvider;
import com.aptana.syncing.ui.internal.SyncViewerSorter;
import com.aptana.syncing.ui.internal.widgets.SyncStatsComposite;
import com.aptana.ui.IDialogConstants;
import com.aptana.ui.actions.SearchToolbarControl;
import com.aptana.ui.io.epl.AccumulatingProgressMonitor;

/**
 * @author Max Stepanov
 *
 */
public class SyncDialog extends TitleAreaDialog implements ISyncSessionListener {

	public static final String TITLE = "Synchronization Dialog";
	
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
				addChildren(element, list);				
			}
			return list.toArray();
		}		

		private void addChildren(Object parent, List<Object> list) {
			for (Object element : getSortedChildren(parent)) {
				list.add(element);
				addChildren(element, list);
			}
		}

	}
	
	private SyncTreeViewer treeViewer;
	private ISyncSession session;
	private ProgressMonitorPart progressMonitorPart;
	private IProgressMonitor progressMonitorWrapper;
	private SyncStatsComposite statsComposite;
			
	private SyncViewerLabelProvider labelProvider;
	
	private IAction hideSameAction;
	private IAction flatModeAction;
	private IAction allFilterAction;
	private IAction incomingFilterAction;
	private IAction outgoingFilterAction;
	private IAction conflictsFilterAction;
	
	private IAction showDiffAction;
	private IAction expandFoldersAction;
	
	private ViewerFilter searchFilter;
	
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
		
		setTitle("Title");
		setMessage("message");
		
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
		
		MenuManager menuManager = new MenuManager("#PopupMenu");
		tree.setMenu(menuManager.createContextMenu(tree));
		menuManager.setRemoveAllWhenShown(true);

		statsComposite = new SyncStatsComposite(container, SWT.NONE) {
			@Override
			protected void expandFolders(Changes changes) {
				fetchFolders(getUnfetchedFolders(getActiveItems(), changes));
			}
		};
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
				}
			}
		}, progressMonitorPart.getDisplay());
				
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				ViewerCell cell = treeViewer.getCell(new Point(e.x, e.y));
				if (cell != null && cell.getColumnIndex() == 1) {
					changeOperationForItem((ISyncItem) cell.getElement());
				}
			}
		});
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISyncItem syncItem = (ISyncItem) ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (syncItem.getChanges() == Changes.CONFLICT) {
					showDiff(syncItem);
				} else {
					//changeOperationForItem(syncItem);
				}
			}
		});
		
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager, (IStructuredSelection) treeViewer.getSelection());
			}
		});
		
		createActions();
		fillToolBar(toolBarManager);
		
		updateFilters();
		updatePresentationMode();
		
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
			List<ISyncItem> list = getActiveItems();
			session.setSyncItems(list.toArray(new ISyncItem[list.size()]));
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

	private void postCreate() {
		getShell().setDefaultButton(null);
		session.addListener(this);
		showProgress(SyncingPlugin.getSyncManager().isSyncInProgress(session));
		treeViewer.setInput(session);
		if (hideSameAction.isChecked()) {
			treeViewer.expandAll();
		}
		updateState();
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
		getButton(IDialogConstants.OK_ID).setText(show ? "Run In Background" : "Synchronize");
		setButtonLayoutData(getButton(IDialogConstants.OK_ID));
		((Composite) getButtonBar()).layout();
	}
	
	private void showStats(boolean show) {
		((GridData) statsComposite.getLayoutData()).exclude = !show;
		statsComposite.setVisible(show);
		statsComposite.getParent().layout();		
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
		allowed.remove(syncItem.getOperation());
		switch (syncItem.getOperation()) {
		case LEFT_TO_RIGHT:
			syncItem.setOperation(allowed.contains(Operation.RIGHT_TO_LEFT) ? Operation.RIGHT_TO_LEFT : Operation.NONE);
			break;
		case RIGHT_TO_LEFT:
			syncItem.setOperation(Operation.NONE);
			break;
		case NONE:
			syncItem.setOperation(allowed.contains(Operation.LEFT_TO_RIGHT) ? Operation.LEFT_TO_RIGHT : allowed.contains(Operation.RIGHT_TO_LEFT) ? Operation.RIGHT_TO_LEFT : Operation.NONE);
			break;
		default:
			break;
		}
		treeViewer.update(syncItem, null);
		updateState();
	}

	public void setSession(ISyncSession session) {
		this.session = session;
	}
	
	private void createActions() {
		hideSameAction = new Action("Hide Identical Files", Action.AS_CHECK_BOX) {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/filter.png"));
			}
			@Override
			public void run() {
				updateFilters();
				treeViewer.expandAll();
			}
		};
		hideSameAction.setChecked(true);
		
		flatModeAction = new Action("Flat Mode", Action.AS_CHECK_BOX) {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/flat.png"));
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
		
		incomingFilterAction = new FilterAction("Incoming Only", SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/incoming.png"));
		outgoingFilterAction = new FilterAction("Outgoing Only", SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/outgoing.png"));
		conflictsFilterAction = new FilterAction("Conflicts Only", SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/conflict.png"));
		allFilterAction = new FilterAction("All", SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/both.png"));
		allFilterAction.setChecked(true);
		
		showDiffAction = new Action("Show Differences") {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/compare.png"));
			}
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				if (!selection.isEmpty()) {
					showDiff((ISyncItem) selection.getFirstElement());
				}
			}
		};
		expandFoldersAction = new Action("Expand selected folder(s)") {
			{
				setImageDescriptor(SyncingUIPlugin.getImageDescriptor("/icons/full/elcl16/expand_all.png"));
			}
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				fetchFolders(getUnfetchedFolders(((IStructuredSelection) treeViewer.getSelection()).toList(), null));
			}
		};
	}
	
	private void fillToolBar(IToolBarManager toolBarManager) {
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
				setSearchFilter(text.isEmpty() ? null : new SearchViewerFilter(text, isCaseSensitive, isRegularExpression));
			}
		});
		toolBarManager.update(true);
	}
	
	@SuppressWarnings("unchecked")
	private void fillContextMenu(IMenuManager menuManager, IStructuredSelection selection) {
		if (selection.size() == 1 && ((ISyncItem) selection.getFirstElement()).getType() == Type.FILE) {
			menuManager.add(showDiffAction);
		}
		if (!SyncingPlugin.getSyncManager().isSyncInProgress(session) && !getUnfetchedFolders(selection.toList(), null).isEmpty()) {
			menuManager.add(expandFoldersAction);
		}
	}
	
	private static List<ISyncItem> getUnfetchedFolders(List<? extends Object> items, Changes changes) {
		List<ISyncItem> list = new ArrayList<ISyncItem>();
		for (Object i : items) {
			ISyncItem syncItem = (ISyncItem) i;
			if (syncItem.getType() == Type.FOLDER && syncItem.getChildItems().length == 0 && syncItem.getChanges() != Changes.NONE) {
				if (changes == null || syncItem.getChanges() == changes) {
					list.add(syncItem);
				}
			}
		}
		return list;
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
			@Override
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
	
	private void updateState() {
		if (treeViewer.getInput() == null || SyncingPlugin.getSyncManager().isSyncInProgress(session)) {
			return;
		}
		List<ISyncItem> items = getActiveItems();
		getButton(IDialogConstants.OK_ID).setEnabled(!items.isEmpty());
		statsComposite.updateStats(items);
	}
	
	private void onFetchComplete() {
		showProgress(false);
		treeViewer.refresh(true);	
		updateState();
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
			SyncUIManager.getInstance().fetchTree(session, list.toArray(new ISyncItem[list.size()]));
			showProgress(true);	
		}
	}

}
