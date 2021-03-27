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

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * LifecycleMapping
 *
 * @author igor
 * @noimplement subclass AbstractLifecycleMapping instead
 */
public interface ILifecycleMapping {
  String getId();

  String getName();

  /**
   * Configure Eclipse workspace project according to Maven build project configuration.
   */
  void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  /**
   * Undo any Eclipse project configuration done during previous call(s) to
   * {@link #configure(ProjectConfigurationRequest, IProgressMonitor)}
   */
  void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  /**
   * Returns map of AbstractBuildParticipants by MojoExecutionKey that need to be executed during Eclipse workspace
   * build. Map can be empty but cannot be null.
   */
  Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipants(IMavenProjectFacade project,
      IProgressMonitor monitor) throws CoreException;

  //TODO Return Set instead of List
  List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade project, IProgressMonitor monitor)
      throws CoreException;

}
