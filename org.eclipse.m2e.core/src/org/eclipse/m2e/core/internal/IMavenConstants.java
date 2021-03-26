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

package org.eclipse.m2e.core.internal;

/**
 * Maven Constants
 *
 * @author Eugene Kuleshov
 */
public interface IMavenConstants {

  public static final String PLUGIN_ID = "org.eclipse.m2e.core"; //$NON-NLS-1$

  public static final String NATURE_ID = PLUGIN_ID + ".maven2Nature"; //$NON-NLS-1$

  public static final String BUILDER_ID = PLUGIN_ID + ".maven2Builder"; //$NON-NLS-1$

  public static final String MARKER_ID = PLUGIN_ID + ".maven2Problem"; //$NON-NLS-1$

  public static final String MARKER_POM_LOADING_ID = MARKER_ID + ".pomloading"; //$NON-NLS-1$

  public static final String MARKER_CONFIGURATION_ID = MARKER_ID + ".configuration"; //$NON-NLS-1$

  public static final String MARKER_LIFECYCLEMAPPING_ID = MARKER_ID + ".lifecycleMapping"; //$NON-NLS-1$

  public static final String MARKER_DEPENDENCY_ID = MARKER_ID + ".dependency"; //$NON-NLS-1$

  public static final String MARKER_BUILD_ID = MARKER_ID + ".build"; //$NON-NLS-1$

  public static final String MARKER_BUILD_PARTICIPANT_ID = MARKER_BUILD_ID + ".participant"; //$NON-NLS-1$

  /**
   * string that gets included in pom.xml file comments and makes the marker manager to ignore the managed version
   * override marker
   */
  public static final String MARKER_IGNORE_MANAGED = "$NO-MVN-MAN-VER$";//$NON-NLS-1$

  public static final String MAVEN_COMPONENT_CONTRIBUTORS_XPT = PLUGIN_ID + ".mavenComponentContributors"; //$NON-NLS-1$

  public static final String POM_FILE_NAME = "pom.xml"; //$NON-NLS-1$

  public static final String PREFERENCE_PAGE_ID = PLUGIN_ID + ".MavenProjectPreferencePage"; //$NON-NLS-1$

  public static final String NO_WORKSPACE_PROJECTS = "noworkspace"; //$NON-NLS-1$

  public static final String ACTIVE_PROFILES = "profiles"; //$NON-NLS-1$

  public static final String FILTER_RESOURCES = "filterresources"; //$NON-NLS-1$

  public static final String JAVADOC_CLASSIFIER = "javadoc"; //$NON-NLS-1$

  public static final String SOURCES_CLASSIFIER = "sources"; //$NON-NLS-1$

  /**
   * The name of the folder containing metadata information for the workspace.
   */
  public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

  public static final String INDEX_UPDATE_PROP = "indexUpdate"; //$NON-NLS-1$

  /**
   * marker containing this attribute (with whatever value) will be considered to contain a quick fix and will be marker
   * appropriately in the editor.
   */
  public static final String MARKER_ATTR_EDITOR_HINT = "editor_hint";//$NON-NLS-1$

  public static final String MARKER_ATTR_PACKAGING = "packaging"; //$NON-NLS-1$

  public static final String MARKER_ATTR_ARTIFACT_ID = "artifactId";//$NON-NLS-1$

  public static final String MARKER_ATTR_CONFIGURATOR_ID = "configuratorId";//$NON-NLS-1$

  public static final String MARKER_ATTR_EXECUTION_ID = "executionId";//$NON-NLS-1$

  public static final String MARKER_ATTR_GOAL = "goal";//$NON-NLS-1$

  public static final String MARKER_ATTR_GROUP_ID = "groupId";//$NON-NLS-1$

  public static final String MARKER_ATTR_LIFECYCLE_PHASE = "lifecyclePhase";//$NON-NLS-1$

  public static final String MARKER_ATTR_VERSION = "version";//$NON-NLS-1$

  /**
   * @since 1.4.0
   */
  public static final String MARKER_ATTR_CLASSIFIER = "classifier";//$NON-NLS-1$

  public static final String EDITOR_HINT_PARENT_GROUP_ID = "parent_groupid";//$NON-NLS-1$

  public static final String EDITOR_HINT_PARENT_VERSION = "parent_version";//$NON-NLS-1$

  public static final String EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE = "managed_dependency_override";//$NON-NLS-1$

  public static final String EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE = "managed_plugin_override";//$NON-NLS-1$

  public static final String EDITOR_HINT_MISSING_SCHEMA = "missing_schema";//$NON-NLS-1$

  public static final String EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION = "not_covered_mojo_execution";//$NON-NLS-1$

  public static final String EDITOR_HINT_IMPLICIT_LIFECYCLEMAPPING = "implicit_lifecyclemaping";//$NON-NLS-1$

  public static final String EDITOR_HINT_UNKNOWN_LIFECYCLE_ID = "unknown_lifecycle_id";//$NON-NLS-1$

  public static final String EDITOR_HINT_MISSING_CONFIGURATOR = "missing_configurator";//$NON-NLS-1$

  public static final String MARKER_COLUMN_START = "columnStart"; //$NON-NLS-1$

  public static final String MARKER_COLUMN_END = "columnEnd"; //$NON-NLS-1$

  public static final String MARKER_CAUSE_RESOURCE_PATH = "causeResourcePath"; //$NON-NLS-1$

  public static final String MARKER_CAUSE_RESOURCE_ID = "causeResourceId"; //$NON-NLS-1$

  public static final String MARKER_CAUSE_COLUMN_START = "causeColumnStart"; //$NON-NLS-1$

  public static final String MARKER_CAUSE_COLUMN_END = "causeColumnEnd"; //$NON-NLS-1$

  public static final String MARKER_CAUSE_LINE_NUMBER = "causeLineNumber"; //$NON-NLS-1$
}
