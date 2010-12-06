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

package org.eclipse.m2e.core.internal.project;

import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;

public class MavenProjectImportResult implements IMavenProjectImportResult {

  private final IProject project;
  private final MavenProjectInfo projectInfo;

  public MavenProjectImportResult(MavenProjectInfo projectInfo, IProject project) {
    this.projectInfo = projectInfo;
    this.project = project;
  }

  public IProject getProject() {
    return project;
  }

  public MavenProjectInfo getMavenProjectInfo() {
    return projectInfo;
  }

}
