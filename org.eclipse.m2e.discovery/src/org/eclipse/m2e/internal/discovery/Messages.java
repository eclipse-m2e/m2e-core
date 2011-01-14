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

  public static String DiscoveryWizardProposal_Label;

  public static String MavenCatalogPage_Descripton;

  public static String MavenCatalogPage_Title;

  public static String MavenCatalogViewer_Error_loading_lifecycle;

  public static String MavenCatalogViewer_Missing_packaging_type;

  public static String MavenDiscoveryWizard_Title;

  public static String MavenDiscovery_Wizard_Applicable_Tag;

  public static String MavenDiscovery_Wizard_ExtrasTag;

  public static String MavenDiscovery_Wizard_LifecyclesTag;

  public static String MavenDiscovery_Wizard_MavenTag;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
