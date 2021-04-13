/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.editor.xml.internal.messages"; //$NON-NLS-1$

  public static String LifecycleMappingProposal_workspaceIgnore_label;

  public static String InsertArtifactProposal_additionals;

  public static String InsertArtifactProposal_display_name;

  public static String InsertArtifactProposal_insert_dep_desc;

  public static String InsertArtifactProposal_insert_dep_display_name;

  public static String InsertArtifactProposal_insert_dep_title;

  public static String InsertArtifactProposal_insert_plugin_description;

  public static String InsertArtifactProposal_insert_plugin_display_name;

  public static String InsertArtifactProposal_insert_plugin_title;

  public static String InsertArtifactProposal_searchDialog_title;

  public static String InsertExpressionProposal_hint1;

  public static String InsertExpressionProposal_hint2;

  public static String InsertSPDXLicenseProposal_0;

  public static String LifecycleMappingProposal_all_desc;

  public static String LifecycleMappingProposal_execute_desc;

  public static String LifecycleMappingProposal_execute_label;

  public static String LifecycleMappingProposal_ignore_desc;

  public static String LifecycleMappingProposal_ignore_label;

  public static String MavenMarkerResolution_openManaged_label;

  public static String MavenMarkerResolution_openManaged_description;

  public static String PomContentAssistProcessor_insert_relPath_title;

  public static String PomContentAssistProcessor_set_relPath_title;

  public static String PomQuickAssistProcessor_name;

  public static String PomQuickAssistProcessor_remove_hint;

  public static String PomQuickAssistProcessor_title_groupId;

  public static String PomQuickAssistProcessor_title_version;

  public static String SelectSPDXLicenseDialog_noWorkspacePomSelected_status;

  public static String SelectSPDXLicenseDialog_Title;

  public static String SelectSPDXLicenseDialog_lblLicenses_text;

  public static String SelectSPDXLicenseDialog_lblLicenseNameFilter_text;

  public static String SelectSPDXLicenseDialog_noLicenseSelected_status;

  public static String SelectSPDXLicenseDialog_lblPomxml_text;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
