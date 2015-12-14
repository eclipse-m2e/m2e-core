/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  public static String MarkerHoverControl_openDiscoveryPrefs;

  public static String MarkerHoverControl_openLifecyclePrefs;

  public static String MarkerHoverControl_openParentDefinition;

  public static String MarkerHoverControl_openWarningsPrefs;

  public static String MavenMarkerResolution_error;

  public static String MavenMarkerResolution_error_title;

  public static String MavenMarkerResolution_schema_label;

  public static String PomContentAssistProcessor_insert_relPath_title;

  public static String PomContentAssistProcessor_set_relPath_title;

  public static String PomHyperlinkDetector_error_message;

  public static String PomHyperlinkDetector_error_title;

  public static String PomHyperlinkDetector_hyperlink_pattern;

  public static String PomHyperlinkDetector_job_name;

  public static String PomHyperlinkDetector_link_managed;

  public static String PomHyperlinkDetector_open_module;

  public static String PomHyperlinkDetector_open_property;

  public static String PomQuickAssistProcessor_name;

  public static String PomQuickAssistProcessor_remove_hint;

  public static String PomQuickAssistProcessor_title_groupId;

  public static String PomQuickAssistProcessor_title_version;

  public static String PomTemplateContext_candidate;

  public static String PomTemplateContext_clean;

  public static String PomTemplateContext_compile;

  public static String PomTemplateContext_deploy;

  public static String PomTemplateContext_expression_description;

  public static String PomTemplateContext_generateresources;

  public static String PomTemplateContext_generatesources;

  public static String PomTemplateContext_generatetestresources;

  public static String PomTemplateContext_generatetestsources;

  public static String PomTemplateContext_install;

  public static String PomTemplateContext_integrationtest;

  public static String PomTemplateContext_insertParameter;

  public static String PomTemplateContext_resolvingPlugin;

  public static String PomTemplateContext_package;

  public static String PomTemplateContext_param;

  public static String PomTemplateContext_param_def;

  public static String PomTemplateContext_param_expr;

  public static String PomTemplateContext_postclean;

  public static String PomTemplateContext_postintegrationtest;

  public static String PomTemplateContext_postsite;

  public static String PomTemplateContext_preclean;

  public static String PomTemplateContext_preintegrationtest;

  public static String PomTemplateContext_preparepackage;

  public static String PomTemplateContext_presite;

  public static String PomTemplateContext_processclasses;

  public static String PomTemplateContext_processresources;

  public static String PomTemplateContext_processsources;

  public static String PomTemplateContext_processtestclasses;

  public static String PomTemplateContext_processtestresources;

  public static String PomTemplateContext_processtestsources;

  public static String PomTemplateContext_project_version_hint;

  public static String PomTemplateContext_site;

  public static String PomTemplateContext_sitedeploy;

  public static String PomTemplateContext_test;

  public static String PomTemplateContext_testcompile;

  public static String PomTemplateContext_validate;

  public static String PomTemplateContext_verify;

  public static String PomTextHover_category_fix;

  public static String PomTextHover_eval1;

  public static String PomTextHover_eval2;

  public static String PomTextHover_jump_to;

  public static String PomTextHover_managed_location;

  public static String PomTextHover_managed_location_missing;

  public static String PomTextHover_managed_version;

  public static String PomTextHover_managed_version_missing;

  public static String PomTextHover_more_quickfixes;

  public static String PomTextHover_one_quickfix;

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
