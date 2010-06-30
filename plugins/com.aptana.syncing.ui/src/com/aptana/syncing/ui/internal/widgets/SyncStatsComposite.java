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

package com.aptana.syncing.ui.internal.widgets;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;

import com.aptana.ide.syncing.ui.SyncingUIPlugin;
import com.aptana.ide.syncing.ui.internal.SyncPresentationUtils;
import com.aptana.ide.ui.io.dialogs.IDialogConstants;
import com.aptana.syncing.core.model.ISyncItem;

/**
 * @author Max Stepanov
 *
 */
public class SyncStatsComposite extends Composite {

	private Font boldFont;
	private FormText leftText;
	private FormText rightText;
	private HyperlinkGroup hyperlinkGroup;
		
	/**
	 * @param parent
	 * @param style
	 */
	public SyncStatsComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(true).create());
		
		FontData fontData = JFaceResources.getDialogFontDescriptor().getFontData()[0];
		boldFont = new Font(getDisplay(), fontData.getName(), fontData.getHeight(), fontData.getStyle() | SWT.BOLD);
		
		hyperlinkGroup = new HyperlinkGroup(getDisplay());
		hyperlinkGroup.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
		
		leftText = new FormText(this, SWT.NO_FOCUS);
		leftText.setLayoutData(GridDataFactory.fillDefaults().indent(IDialogConstants.HORIZONTAL_MARGIN, 0).grab(true, false).hint(SWT.DEFAULT, 80).create());
		setupFormText(leftText);
		
		rightText = new FormText(this, SWT.NO_FOCUS);
		rightText.setLayoutData(GridDataFactory.fillDefaults().indent(IDialogConstants.HORIZONTAL_MARGIN, 0).grab(true, false).hint(SWT.DEFAULT, 80).create());		
		setupFormText(rightText);
	}
	
	private void setupFormText(FormText formText) {
		formText.setWhitespaceNormalized(false);
		formText.setColor("red", getDisplay().getSystemColor(SWT.COLOR_RED));
		formText.setFont("bold", boldFont);
		formText.setImage("add", SyncingUIPlugin.getImage("/icons/full/elcl16/add.png"));
		formText.setImage("delete", SyncingUIPlugin.getImage("/icons/full/elcl16/delete.png"));
		formText.setImage("left", SyncingUIPlugin.getImage("/icons/full/obj16/sync_left.png"));
		formText.setImage("right", SyncingUIPlugin.getImage("/icons/full/obj16/sync_right.png"));
		formText.setHyperlinkSettings(hyperlinkGroup);
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		boldFont.dispose();
	}
	
	public void updateStats(List<ISyncItem> items) {
		int left_new = 0;
		int left_folder_new = 0;
		long left_new_size = 0;
		int left_file_delete = 0;
		int left_folder_delete = 0;
		int left_mod = 0;
		long left_mod_size = 0;
		int right_new = 0;
		int right_folder_new = 0;
		long right_new_size = 0;
		int right_file_delete = 0;
		int right_folder_delete = 0;
		int right_mod = 0;
		long right_mod_size = 0;
		if (items != null) {
			for (ISyncItem i : items) {
				IFileInfo left = i.getLeftFileInfo();
				IFileInfo right = i.getRightFileInfo();
				switch (i.getOperation()) {
				case RIGHT_TO_LEFT:
					if (!left.exists()) {
						if (right.isDirectory()) {
							++left_folder_new;
						} else {
							++left_new;
							left_new_size += right.getLength();
						}
					} else if (!right.exists()) {
						if (left.isDirectory()) {
							++left_folder_delete;
						} else {
							++left_file_delete;
						}
					} else {
						++left_mod;
						left_mod_size += right.getLength();
					}
					break;
				case LEFT_TO_RIGHT:
					if (!right.exists()) {
						if (left.isDirectory()) {
							++right_folder_new;
						} else {
							++right_new;
							right_new_size += left.getLength();
						}
					} else if (!left.exists()) {
						if (right.isDirectory()) {
							++right_folder_delete;
						} else {
							++right_file_delete;
						}
					} else {
						++right_mod;
						right_mod_size += left.getLength();
					}
					break;
				}
			}
		}
		leftText.setText(getLeftText(left_new, left_new_size, left_folder_new, left_file_delete, left_folder_delete, left_mod, left_mod_size), true, false);
		rightText.setText(getRightText(right_new, right_new_size, right_folder_new, right_file_delete, right_folder_delete, right_mod, right_mod_size), true, false);		
	}
	
	private String getLeftText(int created, long created_size, int created_folders, int deleted_files, int deleted_folders, int modified, long modified_size) {
		String createdText = generateCountString("<p><img href=\"add\"/> To be created: {0}</p>", created, created_size, created_folders);
		String deletedText = generateCountString("<p><img href=\"delete\"/> To be deleted: <span color=\"red\" font=\"bold\">{0}</span></p>", deleted_files, -1, deleted_folders);
		String modifiedText = generateCountString("<p><img href=\"left\"/> To be downloaded: {0}</p>", modified, modified_size);
		if (createdText.length() == 0 && deletedText.length() == 0 && modifiedText.length() == 0) {
			createdText = "None";
		}
		return MessageFormat.format("<form><p><b>Local Changes</b></p>{0}{1}{2}</form>", createdText, deletedText, modifiedText);
	}

	private String getRightText(int created, long created_size, int created_folders, int deleted_files, int deleted_fodlers, int modified, long modified_size) {
		String createdText = generateCountString("<p><img href=\"add\"/> To be created: {0}</p>", created, created_size, created_folders);
		String deletedText = generateCountString("<p><img href=\"delete\"/> To be deleted: <span color=\"red\" font=\"bold\">{0}</span></p>", deleted_files, -1, deleted_fodlers);
		String modifiedText = generateCountString("<p><img href=\"right\"/> To be uploaded: {0}</p>", modified, modified_size);
		if (createdText.length() == 0 && deletedText.length() == 0 && modifiedText.length() == 0) {
			createdText = "None";
		}
		return MessageFormat.format("<form><p><b>Remote Changes</b></p>{0}{1}{2}</form>", createdText, deletedText, modifiedText);
	}

	private static String generateCountString(String format, int nfiles, long totalsize) {
		return generateCountString(format, nfiles, totalsize, 0);
	}

	private static String generateCountString(String format, int nfiles, long totalsize, int nfolders) {
		StringBuffer sb = new StringBuffer();
		if (nfiles > 0) {
			sb.append(nfiles).append(" file");
			if (nfiles > 1) {
				sb.append('s');
			}
			if (totalsize > 0) {
				sb.append(" (").append(SyncPresentationUtils.sizeToString(totalsize)).append(')');
			}
		}
		if (nfolders > 0) {
			if (sb.length() > 0) {
				sb.append(" and ");
			}
			StringBuffer sb_folder = new StringBuffer();
			sb_folder.append(nfolders).append(" folder");
			if (nfolders > 1) {
				sb_folder.append('s');
			}
			sb.append(sb_folder.toString());
		}
		if (sb.length() == 0) {
			return "";
		}
		return MessageFormat.format(format, sb.toString());
	}

}
