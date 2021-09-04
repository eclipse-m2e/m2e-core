/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public MavenProjectInfo getMavenProjectInfo() {
    return projectInfo;
  }

}
