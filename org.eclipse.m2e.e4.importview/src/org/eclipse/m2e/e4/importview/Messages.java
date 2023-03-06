/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.e4.importview;

import org.eclipse.osgi.util.NLS;

/**
 * Messages
 *
 * @author mkleint
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.m2e.e4.importview.messages"; //$NON-NLS-1$

	public static String labelRootDirectory;
	public static String buttonBrowseRootDirectory;
	public static String buttonRemoveRootDirectory;
	public static String labelFilterProjects;
	public static String labelProjectTreeViewer;
	public static String labelProjectImportList;
	public static String buttonClearProjectList;
	public static String buttonImportProjects;
	public static String selectRootDirectoryDialogText;
	public static String selectRootDirectoryDialogMessage;
	public static String labelRemoveEclipseFiles;

	public static String selectRootDirectoryMessageNoProjectsFoundTitle;
	public static String selectRootDirectoryMessageNoProjectsFoundText;

	public static String buttonReloadTooltip;
	public static String buttonRemoveRootDirectoryTooltip;
	public static String buttonCollapseToLevel1Tooltip;
	public static String buttonExpandAllTooltip;
	public static String buttonAddAllTooltip;
	public static String buttonRemoveAllTooltip;
	public static String buttonExportListTooltip;
	public static String buttonImportListTooltip;

	public static String exportSelectionMessageTitle;
	public static String exportSelectionMessageNoProjectsSelected;
	public static String exportSelectionMessageIOError;

	public static String importSelectionMessageTitle;
	public static String importSelectionMessageNoRoot;
	public static String importSelectionMessageIOError;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
