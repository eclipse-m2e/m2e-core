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
  public final String PLUGIN_ID = "org.eclipse.m2e.launching";

  // this should correspond with launchConfigurationType.id attribute in plugin.xml!
  public final String LAUNCH_CONFIGURATION_TYPE_ID = "org.eclipse.m2e.Maven2LaunchConfigurationType"; //$NON-NLS-1$

  /**
   * @deprecated this constant is not used by m2e
   */
  @Deprecated
  public final String BUILDER_CONFIGURATION_TYPE_ID = "org.eclipse.m2e.Maven2BuilderConfigurationType"; //$NON-NLS-1$

  // pom directory automatically became working directory for maven embedder launch
  public final String ATTR_POM_DIR = IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY;

  public final String ATTR_GOALS = "M2_GOALS"; //$NON-NLS-1$

  /**
   * @deprecated this constant is not used by m2e
   */
  @Deprecated
  public final String ATTR_GOALS_AUTO_BUILD = "M2_GOALS_AUTO_BUILD"; //$NON-NLS-1$

  /**
   * @deprecated this constant is not used by m2e
   */
  @Deprecated
  public final String ATTR_GOALS_MANUAL_BUILD = "M2_GOALS_MANUAL_BUILD"; //$NON-NLS-1$

  /**
   * @deprecated this constant is not used by m2e
   */
  @Deprecated
  public final String ATTR_GOALS_CLEAN = "M2_GOALS_CLEAN"; //$NON-NLS-1$

  /**
   * @deprecated this constant is not used by m2e
   */
  @Deprecated
  public final String ATTR_GOALS_AFTER_CLEAN = "M2_GOALS_AFTER_CLEAN"; //$NON-NLS-1$

  public final String ATTR_PROFILES = "M2_PROFILES"; //$NON-NLS-1$

  public final String ATTR_PROPERTIES = "M2_PROPERTIES"; //$NON-NLS-1$

  public final String ATTR_OFFLINE = "M2_OFFLINE"; //$NON-NLS-1$

  public final String ATTR_UPDATE_SNAPSHOTS = "M2_UPDATE_SNAPSHOTS"; //$NON-NLS-1$

  public final String ATTR_DEBUG_OUTPUT = "M2_DEBUG_OUTPUT"; //$NON-NLS-1$

  public final String ATTR_SKIP_TESTS = "M2_SKIP_TESTS"; //$NON-NLS-1$

  public final String ATTR_NON_RECURSIVE = "M2_NON_RECURSIVE"; //$NON-NLS-1$

  public final String ATTR_WORKSPACE_RESOLUTION = "M2_WORKSPACE_RESOLUTION"; //$NON-NLS-1$

  public final String ATTR_USER_SETTINGS = "M2_USER_SETTINGS"; //$NON-NLS-1$

  public final String ATTR_RUNTIME = "M2_RUNTIME"; //$NON-NLS-1$

  public final String ATTR_DISABLED_EXTENSIONS = "M2_DISABLED_EXTENSIONS";

  public final String ATTR_THREADS = "M2_THREADS"; //$NON-NLS-1$
}
