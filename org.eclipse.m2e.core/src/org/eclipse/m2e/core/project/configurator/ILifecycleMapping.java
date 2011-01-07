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

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.project.IMavenProjectFacade;

/**
 * LifecycleMapping
 *
 * @author igor
 */
public interface ILifecycleMapping {
  String getId();

  String getName();
  
  void initialize(IMavenProjectFacade mavenProjectFacade, IProgressMonitor monitor) throws CoreException;

  /**
   * Configure Eclipse workspace project according to Maven build project configuration.
   */
  void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  /**
   * Undo any Eclipse project configuration done during previous call(s) to {@link #configure(ProjectConfigurationRequest, IProgressMonitor)}
   */
  void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  /**
   * Returns list of AbstractBuildParticipant that need to be executed during 
   * Eclipse workspace build. List can be empty but cannot be null.
   */
  List<AbstractBuildParticipant> getBuildParticipants(IProgressMonitor monitor) throws CoreException;

  List<AbstractProjectConfigurator> getProjectConfigurators(IProgressMonitor monitor) throws CoreException;

  Set<AbstractProjectConfigurator> getProjectConfiguratorsForMojoExecution(MojoExecution mojoExecution,
      IProgressMonitor monitor)
      throws CoreException;

  List<MojoExecution> getNotCoveredMojoExecutions(IProgressMonitor monitor)
      throws CoreException;

  boolean isInterestingPhase(String phase);
}
