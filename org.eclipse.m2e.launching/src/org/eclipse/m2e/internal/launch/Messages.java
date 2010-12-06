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


package org.eclipse.m2e.internal.launch;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.internal.launch.messages"; //$NON-NLS-1$

  public static String ExecutePomAction_dialog_debug_message;

  public static String ExecutePomAction_dialog_run_message;

  public static String ExecutePomAction_dialog_title;

  public static String ExecutePomAction_executing;

  public static String MavenFileEditorInput_0;

  public static String MavenLaunchDelegate_error_cannot_create_conf;

  public static String MavenLaunchDelegate_job_name;

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

  public static String MavenLaunchMainTab_btnUpdateSnapshots;

  public static String MavenLaunchMainTab_lblAfterClean;

  public static String MavenLaunchMainTab_lblAutoBuildGoals;

  public static String MavenLaunchMainTab_lblCleanBuild;

  public static String MavenLaunchMainTab_lblManualGoals;

  public static String MavenLaunchMainTab_lblRuntime;

  public static String MavenLaunchMainTab_property_dialog_edit_title;

  public static String MavenLaunchMainTab_property_dialog_title;

  public static String MavenLaunchUtils_error_no_maven_install;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
