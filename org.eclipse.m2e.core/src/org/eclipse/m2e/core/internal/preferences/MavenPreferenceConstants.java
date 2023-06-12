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
 *      Rob Newton - added warning preferences page
 *******************************************************************************/

package org.eclipse.m2e.core.internal.preferences;

import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;


/**
 * Maven preferences constants
 */
public interface MavenPreferenceConstants {

  String PREFIX = "eclipse.m2."; //$NON-NLS-1$

  /** String */
  // public static final String P_LOCAL_REPOSITORY_DIR = PREFIX+"localRepositoryDirectory";

  /** true or false */
  // public static final String P_CHECK_LATEST_PLUGIN_VERSION = PREFIX+"checkLatestPluginVersion";

  /** String */
  String P_GLOBAL_CHECKSUM_POLICY = PREFIX + "globalChecksumPolicy";

  /** boolean */
  String P_OFFLINE = PREFIX + "offline"; //$NON-NLS-1$

  /**
   * configure the global update policy
   */
  String P_GLOBAL_UPDATE_POLICY = PREFIX + "globalUpdatePolicy"; //$NON-NLS-1$

  String GLOBAL_UPDATE_POLICY_DEFAULT = "default"; //$NON-NLS-1$

  /** boolean */
  // public static final String P_UPDATE_SNAPSHOTS = PREFIX+"updateSnapshots";

  /** boolean */
  String P_DEBUG_OUTPUT = PREFIX + "debugOutput"; //$NON-NLS-1$

  /** boolean */
  String P_DOWNLOAD_SOURCES = PREFIX + "downloadSources"; //$NON-NLS-1$

  /** boolean */
  String P_DOWNLOAD_JAVADOC = PREFIX + "downloadJavadoc"; //$NON-NLS-1$

  /** String */
  String P_GLOBAL_SETTINGS_FILE = PREFIX + "globalSettingsFile"; //$NON-NLS-1$

  /** String */
  String P_USER_SETTINGS_FILE = PREFIX + "userSettingsFile"; //$NON-NLS-1$

  /** String */
  String P_OUTPUT_FOLDER = PREFIX + "outputFolder"; //$NON-NLS-1$

  /** boolean */
  String P_DISABLE_JDK_WARNING = PREFIX + "disableJdkwarning"; //$NON-NLS-1$

  /** boolean */
  String P_DISABLE_JDK_CHECK = PREFIX + "disableJdkCheck"; //$NON-NLS-1$

  /** String, list of configured maven installations separated by '|', see {@link MavenRuntimeManagerImpl} */
  String P_RUNTIMES = PREFIX + "runtimes"; //$NON-NLS-1$

  /** Root node of extended maven installation attributes, see {@link MavenRuntimeManagerImpl} */
  String P_RUNTIMES_NODE = PREFIX + "runtimesNodes"; //$NON-NLS-1$

  /** String */
  String P_DEFAULT_RUNTIME = PREFIX + "defaultRuntime"; //$NON-NLS-1$

  /** boolean */
  String P_UPDATE_INDEXES = PREFIX + "updateIndexes"; //$NON-NLS-1$

  /** boolean */
  String P_UPDATE_PROJECTS = PREFIX + "updateProjects"; //$NON-NLS-1$

  /** boolean */
  String P_HIDE_FOLDERS_OF_NESTED_PROJECTS = PREFIX + "hideFoldersOfNestedProjects"; //$NON-NLS-1$

  String P_SHOW_CONSOLE_ON_ERR = PREFIX + "showConsoleOnErr"; //$NON-NLS-1$

  String P_SHOW_CONSOLE_ON_OUTPUT = PREFIX + "showConsoleOnOutput"; //$NON-NLS-1$

  /** boolean */
  String P_FULL_INDEX = PREFIX + "fullIndex"; //$NON-NLS-1$

  /** boolean **/
  String P_DEFAULT_POM_EDITOR_PAGE = "eclipse.m2.defaultPomEditorPage"; //$NON-NLS-1$

  /** boolean **/
  String P_RESOLVE_MISSING_PROJECTS = "eclipse.m2.resolveMissingWorkspaceProjects"; //$NON-NLS-1$

  String P_DEFAULT_COMPLETION_PROPOSAL_RELEVANCE = "eclipse.m2.defaultCompletionRelevance"; //$NON-NLS-1$

  /**
   * @since 1.5
   **/
  String PROBLEM_PREFIX = PREFIX + "problem.";

  /**
   * Valid values : ignore, warning or error
   *
   * @since 1.5
   **/
  String P_DUP_OF_PARENT_GROUPID_PB = PROBLEM_PREFIX + "duplicateParentGroupId"; //$NON-NLS-1$

  /**
   * Valid values : ignore, warning or error
   *
   * @since 1.5
   **/
  String P_DUP_OF_PARENT_VERSION_PB = PROBLEM_PREFIX + "duplicateParentVersion"; //$NON-NLS-1$

  /**
   * Valid values : ignore, warning or error
   *
   * @since 1.7
   **/
  String P_OVERRIDING_MANAGED_VERSION_PB = PROBLEM_PREFIX + "overridingManagedVersion"; //$NON-NLS-1$

  /**
   * Valid values : ignore, warning or error
   *
   * @since 1.5
   **/
  String P_OUT_OF_DATE_PROJECT_CONFIG_PB = PROBLEM_PREFIX + "outofdateProjectConfig"; //$NON-NLS-1$

  /**
   * Valid values : ignore, warning or error
   *
   * @since 1.5
   **/
  String P_NOT_COVERED_MOJO_EXECUTION_PB = PROBLEM_PREFIX + "notCoveredMojoExecution"; //$NON-NLS-1$

  /** string **/
  String P_LIFECYCLE_MAPPINGS = PREFIX + "lifecycleMappings"; //$NON-NLS-1$

  /** string **/
  String P_WORKSPACE_MAPPINGS_LOCATION = PREFIX + "WorkspacelifecycleMappingsLocation"; //$NON-NLS-1$

  /**
   * boolean
   *
   * @since 1.6
   **/
  String P_AUTO_UPDATE_CONFIGURATION = PREFIX + "autoUpdateProjects"; //$NON-NLS-1$

  String P_DEFAULT_MOJO_EXECUTION_ACTION = PREFIX + "unkownMojoExecutionAction"; //$NON-NLS-1$

  /** boolean */
  String P_QUERY_CENTRAL_TO_IDENTIFY_ARTIFACT = PREFIX + "queryCentralToIdentifyArtifact"; //$NON-NLS-1$

  /**
   * boolean.
   *
   * @experimental
   */
  String P_BUILDER_USE_NULL_SCHEDULING_RULE = "builderUsesNullSchedulingRule"; //$NON-NLS-1$

  /**
   * Enable SNAPSHOT Archetypes
   *
   * @since 1.15
   */
  String P_ENABLE_SNAPSHOT_ARCHETYPES = "enableSnapshotArchetypes";

}
