/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.embedder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.execution.MavenExecutionRequest;


/**
 * IMavenConfiguration
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMavenConfiguration {

  // listeners

  public void addConfigurationChangeListener(IMavenConfigurationChangeListener listener);

  // remote dependency resolution

  public boolean isOffline();

  /**
   * One of org.eclipse.aether.repository.RepositoryPolicy.UPDATE constants or null. If not null, the specified update
   * policy overrides the update policies of the remote repositories being used for resolution.
   */
  public String getGlobalUpdatePolicy();

  // maven settings.xml

  public String getGlobalSettingsFile();

  //settable for embedded maven
  public void setGlobalSettingsFile(String absolutePath) throws CoreException;

  public String getUserSettingsFile();

  public void setUserSettingsFile(String absolutePath) throws CoreException;

  // resolution

  public boolean isDownloadSources();

  public boolean isDownloadJavaDoc();

  // maven execution

  public boolean isDebugOutput();

  // startup update behaviour

  public boolean isUpdateProjectsOnStartup();

  public boolean isUpdateIndexesOnStartup();

  // new experimental preferences

  public boolean isHideFoldersOfNestedProjects();

  public String getWorkspaceLifecycleMappingMetadataFile();

  public void setWorkspaceLifecycleMappingMetadataFile(String location) throws CoreException;

  /**
   * Returns {@link IMarker} severity of "out-of-date" project problem
   *
   * @return One of <code>ignore</code>, <code>warning</code> or <code>error</code>.
   * @since 1.5
   */
  public String getOutOfDateProjectSeverity();

  /**
   * Returns the global checksum policy applied on {@link MavenExecutionRequest}s.
   *
   * @return <code>fail</code>, <code>warn</code> or <code>ignore</code> to override repositories specific checksum
   *         policies or <code>null</code> to follow default behavior.
   * @see {@link ArtifactRepositoryPolicy#CHECKSUM_POLICY_FAIL}
   * @see {@link ArtifactRepositoryPolicy#CHECKSUM_POLICY_WARN}
   * @see {@link ArtifactRepositoryPolicy#CHECKSUM_POLICY_IGNORE}
   * @since 1.5
   */
  public String getGlobalChecksumPolicy();

  /**
   * Returns {@link IMarker} severity of "Not Covered Mojo Execution" problem.
   *
   * @return One of <code>ignore</code>, <code>warning</code> or <code>error</code>.
   * @since 1.5
   */
  public String getNotCoveredMojoExecutionSeverity();

  /**
   * Returns <code>true</code> if project configuration should be automatically updated when out-of-date.
   *
   * @return <code>true</code> if project configuration should be automatically updated when out-of-date.
   * @since 1.6
   */
  public boolean isAutomaticallyUpdateConfiguration();

  /**
   * Returns {@link IMarker} severity of "Overriding Managed version" problem.
   *
   * @return One of <code>ignore</code>, <code>warning</code> or <code>error</code>.
   * @since 1.7
   */
  public String getOverridingManagedVersionExecutionSeverity();

  /**
   * @experimental This can cause builds to run in parallel, and m2e is not yet protected against parallel execution of
   *               Maven and its plugins (which is usually not supported).
   * @return whether to use null as scheduling rule for builder.
   */
  public boolean buildWithNullSchedulingRule();

}
