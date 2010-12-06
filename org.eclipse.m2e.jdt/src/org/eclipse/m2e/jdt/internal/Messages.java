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

package org.eclipse.m2e.jdt.internal;

import org.eclipse.osgi.util.NLS;


/**
 * Messages
 *
 * @author mkleint
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.jdt.internal.messages"; //$NON-NLS-1$

  public static String AbstractJavaProjectConfigurator_task_name;

  public static String BuildPathManager_monitor_setting_cp;

  public static String DownloadSourcesJob_job_download;

  public static String GenericJavaProjectConfigurator_subtask;

  public static String GenericJavaProjectConfigurator_subtask_refreshing;

  public static String MavenClasspathContainer_description;

  public static String MavenClasspathContainerInitializer_error_cannot_persist;

  public static String MavenClasspathContainerInitializer_job_name;

  public static String MavenClasspathContainerPage_control_desc;

  public static String MavenClasspathContainerPage_control_title;

  public static String MavenClasspathContainerPage_link;

  public static String MavenClasspathContainerPage_title;

  public static String MavenDependencyResolver_additional_info;

  public static String MavenDependencyResolver_error_message;

  public static String MavenDependencyResolver_error_message2;

  public static String MavenDependencyResolver_error_message3;

  public static String MavenDependencyResolver_error_message4;

  public static String MavenDependencyResolver_error_message5;

  public static String MavenDependencyResolver_error_message6;

  public static String MavenDependencyResolver_error_message7;

  public static String MavenDependencyResolver_error_title;

  public static String MavenDependencyResolver_proposal_search;

  public static String MavenDependencyResolver_searchDialog_title;

  public static String MavenJdtMenuCreator_action_downloadJavadoc;

  public static String MavenJdtMenuCreator_action_downloadSources;

  public static String MavenJdtMenuCreator_action_javadoc;

  public static String MavenJdtMenuCreator_action_materialize1;

  public static String MavenJdtMenuCreator_action_materializeMany;

  public static String MavenJdtMenuCreator_action_open_issue;

  public static String MavenJdtMenuCreator_action_openCI;

  public static String MavenJdtMenuCreator_action_openJavadoc;

  public static String MavenJdtMenuCreator_action_openPom;

  public static String MavenJdtMenuCreator_action_openProject;

  public static String MavenJdtMenuCreator_action_sources;

  public static String MavenJdtMenuCreator_axtion_openScm;

  public static String MavenJdtPlugin_job_name;

  public static String MavenQueryParticipant_job_name;

  public static String MavenQueryParticipant_searchDialog_title;

  public static String MavenRuntimeClasspathProvider_error_unsupported;
  public static String OpenJavaDocAction_error_download;

  public static String OpenJavaDocAction_error_message;

  public static String OpenJavaDocAction_error_title;

  public static String OpenJavaDocAction_info_title;

  public static String OpenJavaDocAction_job_open_javadoc;

  public static String OpenJavaDocAction_message1;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
