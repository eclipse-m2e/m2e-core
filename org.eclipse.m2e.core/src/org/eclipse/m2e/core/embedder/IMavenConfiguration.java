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

package org.eclipse.m2e.core.embedder;

import org.eclipse.core.runtime.CoreException;


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
   * One of org.sonatype.aether.repository.RepositoryPolicy.UPDATE constants or null. If not null, the specified update
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
}
