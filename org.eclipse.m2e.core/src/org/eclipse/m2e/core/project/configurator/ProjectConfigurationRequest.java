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

package org.eclipse.m2e.core.project.configurator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;


public class ProjectConfigurationRequest {
  private final IMavenProjectFacade facade;

  private final MavenProject mavenProject;

  public ProjectConfigurationRequest(IMavenProjectFacade facade, MavenProject mavenProject) {
    this.facade = facade;
    this.mavenProject = mavenProject;
  }

  public IProject getProject() {
    return facade.getProject();
  }

  public ResolverConfiguration getResolverConfiguration() {
    return facade.getResolverConfiguration();
  }

  public MavenProject getMavenProject() {
    return mavenProject;
  }

  public IFile getPom() {
    return facade.getPom();
  }

  public IMavenProjectFacade getMavenProjectFacade() {
    return facade;
  }
}
