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

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
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

  void createSimpleProject(IProject project, IPath location, Model model, String[] folders,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException;

  void createArchetypeProject(IProject project, IPath location, Archetype archetype, //
      String groupId, String artifactId, String version, String javaPackage, Properties properties, //
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException;

  Set<MavenProjectInfo> collectProjects(Collection<MavenProjectInfo> projects);

  void enableMavenNature(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException;

  void disableMavenNature(IProject project, IProgressMonitor monitor) throws CoreException;

  void updateProjectConfiguration(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException;

  ILifecycleMapping getLifecycleMapping(IMavenProjectFacade projectFacade, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Validates that the project configuration is valid. It does not actually (re)configure the project, but it validates
   * that the project configure action will not fail for obvious reasons like missing lifecycle mapping, missing project
   * configuration, etc.
   * 
   * @return true if the configuration is valid
   */
  boolean validateProjectConfiguration(IMavenProjectFacade projectFacade, IProgressMonitor monitor);
}
