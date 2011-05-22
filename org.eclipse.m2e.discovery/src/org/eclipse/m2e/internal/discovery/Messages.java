/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.internal.discovery.messages"; //$NON-NLS-1$

  public static String BundleDiscoveryStrategy_3;

  public static String BundleDiscoveryStrategy_categoryDisallowed;

  public static String BundleDiscoveryStrategy_task_processing_extensions;

  public static String BundleDiscoveryStrategy_unexpected_element;

  public static String DiscoveryWizardProposal_description;

  public static String DiscoveryWizardProposal_Label;

  public static String ConnectorDiscoveryExtensionReader_unexpected_element_icon;

  public static String ConnectorDiscoveryExtensionReader_unexpected_element_overview;

  public static String ConnectorDiscoveryExtensionReader_unexpected_value_kind;

  public static String MavenCatalogPage_Descripton;

  public static String MavenCatalogPage_Title;

  public static String MavenCatalogViewer_allInstalled;

  public static String MavenCatalogViewer_Error_loading_lifecycle;

  public static String MavenCatalogViewer_noApplicableMarketplaceItems;

  public static String MavenCatalogViewer_unexpectedException;

  public static String MavenDiscoveryWizard_Title;

  public static String MavenDiscovery_Wizard_Applicable_Tag;

  public static String MavenDiscovery_Wizard_ExtrasTag;

  public static String MavenDiscovery_Wizard_LifecyclesTag;

  public static String MavenDiscovery_Wizard_MavenTag;

  public static String MavenDiscoveryInstallOperation_Configuring;

  public static String MavenDiscoveryInstallOperation_ErrorMessage;

  public static String MavenDiscoveryInstallOperation_missingIU;

  public static String MavenDiscoveryInstallOperation_missingRepository;

  public static String UpdateConfigurationStartup_MarkerError;

  public static String DiscoveryPreferencePage_title;

  public static String DiscoveryPreferencePage_catalogUrl;

  public static String DiscoveryPreferencePage_openCatalog;
  public static String DiscoveryPreferencePage_link_text;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
