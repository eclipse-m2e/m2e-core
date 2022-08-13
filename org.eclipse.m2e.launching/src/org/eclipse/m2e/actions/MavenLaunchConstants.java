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

package org.eclipse.m2e.actions;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;


public interface MavenLaunchConstants {
  String PLUGIN_ID = "org.eclipse.m2e.launching";

  // this should correspond with launchConfigurationType.id attribute in plugin.xml!
  String LAUNCH_CONFIGURATION_TYPE_ID = "org.eclipse.m2e.Maven2LaunchConfigurationType"; //$NON-NLS-1$

  // pom directory automatically became working directory for maven embedder launch
  String ATTR_POM_DIR = IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY;

  String ATTR_GOALS = "M2_GOALS"; //$NON-NLS-1$

  String ATTR_PROFILES = "M2_PROFILES"; //$NON-NLS-1$

  String ATTR_PROPERTIES = "M2_PROPERTIES"; //$NON-NLS-1$

  String ATTR_OFFLINE = "M2_OFFLINE"; //$NON-NLS-1$

  String ATTR_UPDATE_SNAPSHOTS = "M2_UPDATE_SNAPSHOTS"; //$NON-NLS-1$

  String ATTR_DEBUG_OUTPUT = "M2_DEBUG_OUTPUT"; //$NON-NLS-1$

  String ATTR_SKIP_TESTS = "M2_SKIP_TESTS"; //$NON-NLS-1$

  String ATTR_NON_RECURSIVE = "M2_NON_RECURSIVE"; //$NON-NLS-1$

  String ATTR_WORKSPACE_RESOLUTION = "M2_WORKSPACE_RESOLUTION"; //$NON-NLS-1$

  String ATTR_USER_SETTINGS = "M2_USER_SETTINGS"; //$NON-NLS-1$

  String ATTR_RUNTIME = "M2_RUNTIME"; //$NON-NLS-1$

  String ATTR_DISABLED_EXTENSIONS = "M2_DISABLED_EXTENSIONS";

  String ATTR_THREADS = "M2_THREADS"; //$NON-NLS-1$

  String ATTR_COLOR = "M2_COLORS"; //$NON-NLS-1$

  String ATTR_SAVE_BEFORE_LAUNCH = "M2_SAVE_BEFORE_LAUNCH";

  String ATTR_BATCH = "M2_BATCH";

  /**
   * Not passed directly to Maven.
   * <p>
   * The auto as handled by Maven tries to detect if {@code stdout} is a terminal by invoking {@code isatty}, which
   * returns `false` for the Eclipse console. <br />
   * So we will solve the detection part and will pass {@code always} / {@code never} to Maven.
   * </p>
   */
  int ATTR_COLOR_VALUE_AUTO = 0;

  /** Instruct Maven to always output colors. */
  int ATTR_COLOR_VALUE_ALWAYS = 1;

  /** Instruct Maven to never output colors. */
  int ATTR_COLOR_VALUE_NEVER = 2;
}
