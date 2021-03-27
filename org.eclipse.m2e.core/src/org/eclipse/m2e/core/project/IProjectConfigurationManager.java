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

package org.eclipse.m2e.core.project;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;

import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;


public interface IProjectConfigurationManager {

  ISchedulingRule getRule();

  List<IMavenProjectImportResult> importProjects(Collection<MavenProjectInfo> projects, //
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException;

  /**
   * @since 1.8
   */
  List<IMavenProjectImportResult> importProjects(Collection<MavenProjectInfo> projects, //
      ProjectImportConfiguration configuration, IProjectCreationListener importListener, IProgressMonitor monitor)
          throws CoreException;

  void createSimpleProject(IProject project, IPath location, Model model, String[] folders,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException;

  /**
   * @since 1.8
   */
  void createSimpleProject(IProject project, IPath location, Model model, String[] folders,
      ProjectImportConfiguration configuration, IProjectCreationListener importListener, IProgressMonitor monitor)
          throws CoreException;

  /**
   * @deprecated use
   *             {@link #createArchetypeProjects(IPath, Archetype, String, String, String, String, Properties, ProjectImportConfiguration, IProgressMonitor)}
   */
  @Deprecated
  void createArchetypeProject(IProject project, IPath location, Archetype archetype, //
      String groupId, String artifactId, String version, String javaPackage, Properties properties, //
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException;

  /**
   * Creates project structure using Archetype and then imports the created project(s)
   *
   * @return an unmodifiable list of created projects.
   * @since 1.1
   */
  List<IProject> createArchetypeProjects(IPath location, Archetype archetype, //
      String groupId, String artifactId, String version, String javaPackage, Properties properties, //
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException;

  /**
   * Creates project structure using Archetype and then imports the created project(s)
   *
   * @return an unmodifiable list of created projects.
   * @since 1.8
   */
  List<IProject> createArchetypeProjects(IPath location, Archetype archetype, //
      String groupId, String artifactId, String version, String javaPackage, Properties properties, //
      ProjectImportConfiguration configuration, IProjectCreationListener importListener, IProgressMonitor monitor)
          throws CoreException;

  Set<MavenProjectInfo> collectProjects(Collection<MavenProjectInfo> projects);

  void enableMavenNature(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException;

  void disableMavenNature(IProject project, IProgressMonitor monitor) throws CoreException;

  void updateProjectConfiguration(IProject project, IProgressMonitor monitor) throws CoreException;

  void updateProjectConfiguration(MavenUpdateRequest request, IProgressMonitor monitor) throws CoreException;

  ILifecycleMapping getLifecycleMapping(IMavenProjectFacade projectFacade) throws CoreException;

  /**
   * Adds the maven builder to the specified project.
   *
   * @return true if the maven builder was added or its position in the list of builders was changed
   */
  boolean addMavenBuilder(IProject project, IProjectDescription description, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Removes the maven builder from the specified project.
   *
   * @return true if the maven builder was removed
   */
  boolean removeMavenBuilder(IProject project, IProjectDescription description, IProgressMonitor monitor)
      throws CoreException;

  /**
   * PROVISIONAL
   */
  public ResolverConfiguration getResolverConfiguration(IProject project);

  /**
   * PROVISIONAL
   */
  public boolean setResolverConfiguration(IProject project, ResolverConfiguration configuration);

}
