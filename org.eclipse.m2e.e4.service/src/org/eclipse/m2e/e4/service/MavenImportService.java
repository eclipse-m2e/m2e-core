/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.e4.service;

import java.io.File;

/**
 * MavenImportService
 *
 * @author Nikolaus Winter, comdirect bank AG
 */
public interface MavenImportService {
	/**
	 * Imports Maven Project to Workspace. If desired, this methods cleans the
	 * Eclipse files prior to import.
	 *
	 * @param projectFolder     Folder where Maven Project is located.
	 * @param cleanEclipseFiles Should the Eclipse files be cleaned prior to import?
	 */
	public void importProject(File projectFolder, boolean cleanEclipseFiles);
}
