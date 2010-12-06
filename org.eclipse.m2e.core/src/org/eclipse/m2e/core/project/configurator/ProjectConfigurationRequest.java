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

package org.eclipse.m2e.core.project.configurator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;

/**
 * ProjectConfigurationRequest
 *
 * @author igor
 */
public class ProjectConfigurationRequest {
  private final boolean updateSources;
  private final IMavenProjectFacade facade;
  private final MavenProject mavenProject;
  private final MavenSession mavenSession;

  public ProjectConfigurationRequest(IMavenProjectFacade facade, MavenProject mavenProject, MavenSession mavenSession, boolean updateSources) {
    this.facade = facade;
    this.mavenSession = mavenSession;
    this.updateSources = updateSources;
    this.mavenProject = mavenProject;
  }

  public IProject getProject() {
    return facade.getProject();
  }

  public ResolverConfiguration getResolverConfiguration() {
    return facade.getResolverConfiguration();
  }

  public boolean isProjectConfigure() {
    return updateSources;
  }

  public boolean isProjectImport() {
    return !updateSources;
  }

  public MavenProject getMavenProject() {
    return mavenProject;
  }

  public MavenSession getMavenSession() {
    return mavenSession;
  }

  public IFile getPom() {
    return facade.getPom();
  }

  public IMavenProjectFacade getMavenProjectFacade() {
    return facade;
  }
}
