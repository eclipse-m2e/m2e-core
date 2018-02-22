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

  public static String BuildPathManager_update_module_path_job_name;

  public static String DownloadSourcesJob_job_download;

  public static String MavenClasspathContainer_description;

  public static String MavenClasspathContainerInitializer_error_cannot_persist;

  public static String MavenClasspathContainerInitializer_job_name;

  public static String MavenJdtPlugin_job_name;

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
