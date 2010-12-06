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

package org.eclipse.m2e.core.internal.preferences;


/**
 * Maven preferences constants 
 */
public interface MavenPreferenceConstants {
  
  static final String PREFIX = "eclipse.m2."; //$NON-NLS-1$

  /** String */
  // public static final String P_LOCAL_REPOSITORY_DIR = PREFIX+"localRepositoryDirectory";
  
  /** true or false */
  // public static final String P_CHECK_LATEST_PLUGIN_VERSION = PREFIX+"checkLatestPluginVersion";
  
  /** String ??? */
  // public static final String P_GLOBAL_CHECKSUM_POLICY = PREFIX+"globalChecksumPolicy";

  /** boolean */
  public static final String P_OFFLINE = PREFIX + "offline"; //$NON-NLS-1$

  /** boolean */
  // public static final String P_UPDATE_SNAPSHOTS = PREFIX+"updateSnapshots";
  
  /** boolean */
  public static final String P_DEBUG_OUTPUT = PREFIX + "debugOutput"; //$NON-NLS-1$

  /** boolean */
  public static final String P_DOWNLOAD_SOURCES = PREFIX + "downloadSources"; //$NON-NLS-1$

  /** boolean */
  public static final String P_DOWNLOAD_JAVADOC = PREFIX + "downloadJavadoc"; //$NON-NLS-1$

  /** String */
  public static final String P_GLOBAL_SETTINGS_FILE = PREFIX + "globalSettingsFile"; //$NON-NLS-1$

  /** String */
  public static final String P_USER_SETTINGS_FILE = PREFIX + "userSettingsFile"; //$NON-NLS-1$

  /** String */
  public static final String P_OUTPUT_FOLDER = PREFIX + "outputFolder"; //$NON-NLS-1$

  /** boolean */
  public static final String P_DISABLE_JDK_WARNING = PREFIX + "disableJdkwarning"; //$NON-NLS-1$

  /** boolean */
  public static final String P_DISABLE_JDK_CHECK = PREFIX + "disableJdkCheck"; //$NON-NLS-1$

  /** String */
  public static final String P_RUNTIMES = PREFIX + "runtimes"; //$NON-NLS-1$

  /** String */
  public static final String P_DEFAULT_RUNTIME = PREFIX + "defaultRuntime"; //$NON-NLS-1$

  /** boolean */
  public static final String P_UPDATE_INDEXES = PREFIX + "updateIndexes"; //$NON-NLS-1$

  /** boolean */
  public static final String P_UPDATE_PROJECTS = PREFIX + "updateProjects"; //$NON-NLS-1$

  /** String */
  public static final String P_JIRA_USERNAME = PREFIX + "jiraUsername"; //$NON-NLS-1$

  /** String */
  public static final String P_JIRA_PASSWORD = PREFIX + "jiraPassword"; //$NON-NLS-1$
  
  /** boolean */
  public static final String P_HIDE_FOLDERS_OF_NESTED_PROJECTS = PREFIX + "hideFoldersOfNestedProjects"; //$NON-NLS-1$
  
  public static final String P_SHOW_CONSOLE_ON_ERR = PREFIX+"showConsoleOnErr"; //$NON-NLS-1$
  
  public static final String P_SHOW_CONSOLE_ON_OUTPUT = PREFIX+"showConsoleOnOutput";  //$NON-NLS-1$
  
  /** boolean */
  public static final String P_FULL_INDEX= PREFIX+"fullIndex"; //$NON-NLS-1$
}
