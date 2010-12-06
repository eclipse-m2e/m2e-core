/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project;

import org.eclipse.core.resources.IProject;

/**
 * Holds IProject that was created during project import
 *  
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMavenProjectImportResult {

  /**
   * @return MavenProjectInfo maven project import request
   */
  MavenProjectInfo getMavenProjectInfo();

  /**
   * @return IProject imported project or <code>null</code> if the project could not be imported. 
   */
  IProject getProject();
}
