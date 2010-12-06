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



/**
 * IMavenConfiguration
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMavenConfiguration {
  
  // listeners
  
  public void addConfigurationChangeListener(IMavenConfigurationChangeListener listener);
  
  //

  public boolean isOffline();

  public String getGlobalSettingsFile();

  //settable for embedded maven
  public void setGlobalSettingsFile(String absolutePath);
  
  public String getUserSettingsFile();

  public void setUserSettingsFile(String absolutePath);

  // resolution

  public boolean isDownloadSources();

  public boolean isDownloadJavaDoc();

  // problem reporting

  public String getJiraUsername();

  public String getJiraPassword();

  // maven execution

  public boolean isDebugOutput();

  //

  public boolean isUpdateProjectsOnStartup();

  public boolean isUpdateIndexesOnStartup();

  // new experimental preferences

  public boolean isHideFoldersOfNestedProjects();

}
