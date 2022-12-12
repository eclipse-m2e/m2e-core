/*******************************************************************************
 * Copyright (c) 2008-2023 Sonatype, Inc.
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

package org.eclipse.m2e.internal.launch;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.internal.launch.messages"; //$NON-NLS-1$

  public static String ExecutePomAction_dialog_debug_message;

  public static String ExecutePomAction_dialog_run_message;

  public static String ExecutePomAction_dialog_title;

  public static String ExecutePomAction_executing;

  public static String MavenLaunchDelegate_error_cannot_create_conf;

  public static String MavenLaunchDelegate_job_name;

  public static String MavenLaynchDelegate_unsupported_source_locator;

  public static String MavenLaunchDelegate_error_cannot_read_jvmConfig;

  public static String MavenLaunchMainTab_btnAfterClean;

  public static String MavenLaunchMainTab_btnAutoBuild;

  public static String MavenLaunchMainTab_btnCleanBuild;

  public static String MavenLaunchMainTab_btnConfigure;

  public static String MavenLaunchMainTab_btnDebugOutput;

  public static String MavenLaunchMainTab_btnManualBuild;

  public static String MavenLaunchMainTab_btnNotRecursive;

  public static String MavenLaunchMainTab_btnOffline;

  public static String MavenLaunchMainTab_btnResolveWorkspace;

  public static String MavenLaunchMainTab_btnSkipTests;

  public static String MavenLaunchMainTab_lblThreads;

  public static String MavenLaunchMainTab_lblEnableColorOutput;

  public static String MavenLaunchMainTab_lblEnableColorOutput_Auto;

  public static String MavenLaunchMainTab_lblEnableColorOutput_Always;

  public static String MavenLaunchMainTab_lblEnableColorOutput_Never;

  public static String MavenLaunchMainTab_btnUpdateSnapshots;

  public static String MavenLaunchMainTab_lblAfterClean;

  public static String MavenLaunchMainTab_lblAutoBuildGoals;

  public static String MavenLaunchMainTab_lblCleanBuild;

  public static String MavenLaunchMainTab_lblManualGoals;

  public static String MavenLaunchMainTab_lblRuntime;

  public static String MavenLaunchMainTab_property_dialog_edit_title;

  public static String MavenLaunchMainTab_property_dialog_title;

  public static String MavenJRETab_lblDefault;

  public static String MavenJRETab_lblDefaultDetailsRequiredJavaVersion;

  public static String MavenLaunchExtensionsTab_name;

  public static String MavenLaunchExtensionsTab_lblExtensions;

  public static String MavenLaunchUtils_error_no_maven_install;

  public static String launchPomGroup;

  public static String launchBrowseWorkspace;

  public static String launchChoosePomDir;

  public static String launchChooseSettingsFile;

  public static String launchBrowseFs;

  public static String launchBrowseVariables;

  public static String launchGoalsLabel;

  public static String launchGoals;

  public static String launchProfilesLabel;

  public static String launchPropName;

  public static String launchPropValue;

  public static String launchPropAddButton;

  public static String launchPropEditButton;

  public static String launchPropRemoveButton;

  public static String launchPropertyDialogBrowseVariables;

  public static String launchMainTabName;

  public static String launchPomDirectoryEmpty;

  public static String launchPomDirectoryDoesntExist;

  public static String launchErrorEvaluatingBaseDirectory;

  public static String MavenLaunchMainTab_lblUserSettings_text;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
