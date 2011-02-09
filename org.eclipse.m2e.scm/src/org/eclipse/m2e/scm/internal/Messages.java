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

package org.eclipse.m2e.scm.internal;

import org.eclipse.osgi.util.NLS;


/**
 * Messages
 * 
 * @author mkleint
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.scm.internal.messages"; //$NON-NLS-1$

  public static String MavenCheckoutLocationPage_btnBrowse;

  public static String MavenCheckoutLocationPage_btnCheckout;

  public static String MavenCheckoutLocationPage_btnHead;

  public static String MavenCheckoutLocationPage_btnRevSelect;

  public static String MavenCheckoutLocationPage_description;

  public static String MavenCheckoutLocationPage_error_empty;

  public static String MavenCheckoutLocationPage_error_empty_url;

  public static String MavenCheckoutLocationPage_error_scm_empty;

  public static String MavenCheckoutLocationPage_error_scm_invalid;

  public static String MavenCheckoutLocationPage_error_url_empty;

  public static String MavenCheckoutLocationPage_lblRevision;

  public static String MavenCheckoutLocationPage_lblurl;

  public static String MavenCheckoutLocationPage_title;

  public static String MavenCheckoutWizard_location1;

  public static String MavenCheckoutWizard_location2;

  public static String MavenCheckoutWizard_title;

  public static String MavenMaterializePomWizard_btnCheckout;

  public static String MavenMaterializePomWizard_btnDev;

  public static String MavenMaterializePomWizard_dialog_message;

  public static String MavenMaterializePomWizard_dialog_title;

  public static String MavenMaterializePomWizard_location_message;

  public static String MavenMaterializePomWizard_location_title;

  public static String MavenMaterializePomWizard_title;

  public static String ScmUrl_error;

  public static String MavenProjectCheckoutJob_title;

  public static String MavenProjectCheckoutJob_confirm_title;

  public static String MavenProjectCheckoutJob_confirm_message;

  public static String MavenProjectCheckoutJob_confirm2_title;

  public static String MavenProjectCheckoutJob_confirm2_message;

  public static String MavenProjectCheckoutJob_job;

  public static String MavenCheckoutOperation_task_scanning;

  public static String MavenCheckoutOperation_task_checking;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
