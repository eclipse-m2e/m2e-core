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

package org.eclipse.m2e.core.internal.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.MavenPlugin;


public class MavenNature implements IProjectNature {
  private IProject project;

  @Override
  public void configure() throws CoreException {
    IProjectDescription description = project.getDescription();
    MavenPlugin.getProjectConfigurationManager().addMavenBuilder(project, description, null /*monitor*/);
    project.setDescription(description, null);
  }

  @Override
  public void deconfigure() throws CoreException {
    IProjectDescription description = project.getDescription();
    MavenPlugin.getProjectConfigurationManager().removeMavenBuilder(project, description, null /*monitor*/);
  }

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public void setProject(IProject project) {
    this.project = project;
  }

}
