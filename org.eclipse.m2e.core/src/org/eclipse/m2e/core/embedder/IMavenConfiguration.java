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

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;


/**
 * IMavenConfiguration
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMavenConfiguration {

  // listeners

  void addConfigurationChangeListener(IMavenConfigurationChangeListener listener);

  // remote dependency resolution

  boolean isOffline();

  /**
   * One of org.eclipse.aether.repository.RepositoryPolicy.UPDATE constants or null. If not null, the specified update
   * policy overrides the update policies of the remote repositories being used for resolution.
   */
  String getGlobalUpdatePolicy();

  // maven settings.xml

  String getGlobalSettingsFile();

  //settable for embedded maven
  void setGlobalSettingsFile(String absolutePath) throws CoreException;

  String getUserSettingsFile();

  void setUserSettingsFile(String absolutePath) throws CoreException;

  // resolution

  boolean isDownloadSources();

  boolean isDownloadJavaDoc();

  // maven execution

  boolean isDebugOutput();

  // startup update behaviour

  boolean isUpdateProjectsOnStartup();

  boolean isUpdateIndexesOnStartup();

  // new experimental preferences

  boolean isHideFoldersOfNestedProjects();

  String getWorkspaceLifecycleMappingMetadataFile();

  void setWorkspaceLifecycleMappingMetadataFile(String location) throws CoreException;

  /**
   * Returns {@link IMarker} severity of "out-of-date" project problem
   *
   * @return One of <code>ignore</code>, <code>warning</code> or <code>error</code>.
   * @since 1.5
   */
  String getOutOfDateProjectSeverity();

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
  String getGlobalChecksumPolicy();

  /**
   * Returns {@link IMarker} severity of "Not Covered Mojo Execution" problem.
   *
   * @return One of <code>ignore</code>, <code>warning</code> or <code>error</code>.
   * @since 1.5
   */
  String getNotCoveredMojoExecutionSeverity();

  /**
   * Returns <code>true</code> if project configuration should be automatically updated when out-of-date.
   *
   * @return <code>true</code> if project configuration should be automatically updated when out-of-date.
   * @since 1.6
   */
  boolean isAutomaticallyUpdateConfiguration();

  /**
   * Returns {@link IMarker} severity of "Overriding Managed version" problem.
   *
   * @return One of <code>ignore</code>, <code>warning</code> or <code>error</code>.
   * @since 1.7
   */
  String getOverridingManagedVersionExecutionSeverity();

  /**
   * @experimental This can cause builds to run in parallel, and m2e is not yet protected against parallel execution of
   *               Maven and its plugins (which is usually not supported).
   * @return whether to use null as scheduling rule for builder.
   */
  boolean buildWithNullSchedulingRule();

  static IMavenConfiguration getWorkspaceConfiguration() {
    MavenPluginActivator activator = MavenPluginActivator.getDefault();
    if(activator == null) {
      throw new IllegalStateException("m2e is shut down!");
    }
    return activator.getMavenConfiguration();
  }

  void setDefaultMojoExecutionAction(PluginExecutionAction mojoAction);

  PluginExecutionAction getDefaultMojoExecutionAction();

}
