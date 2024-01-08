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

package org.eclipse.m2e.core.internal;

import org.eclipse.osgi.util.NLS;


/**
 * Messages
 *
 * @author mkleint
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.core.internal.messages"; //$NON-NLS-1$

  public static String AbstractLifecycleMapping_could_not_update_project_configuration;

  public static String AbstractProjectConfigurator_error_missing_nature;

  public static String AbstractTransferListenerAdapter_4;

  public static String AbstractTransferListenerAdapter_byte;

  public static String AbstractTransferListenerAdapter_cancelled;

  public static String AbstractTransferListenerAdapter_kb;

  public static String AbstractTransferListenerAdapter_mb;

  public static String AbstractTransferListenerAdapter_subtask;

  public static String AetherClientConfigAdapter_error_sslContext;

  public static String ArchetypeCatalogFactory_default_local;

  public static String ArchetypeCatalogFactory_error_missing_catalog;

  public static String ArchetypeCatalogFactory_indexer_catalog;

  public static String ArchetypeCatalogFactory_internal;

  public static String ArchetypeCatalogFactory_local;

  public static String ArchetypeCatalogFactory_remote;

  public static String ArchetypeCatalogsWriter_error_parse;

  public static String ArchetypeCatalogsWriter_error_write;

  public static String AsyncFetcher_error_server;

  public static String AsyncFetcher_task_fetching;

  public static String AsyncFetcher_task_fetching2;

  public static String EclipseLogger_name;

  public static String IndexUpdaterJob_title;

  public static String LifecycleConfigurationPluginExecutionNotCovered;

  public static String LifecycleConfigurationPluginExecutionErrorMessage;

  public static String LifecycleDuplicate;

  public static String LifecycleMappingNotAvailable;

  public static String LifecycleMappingPackagingMismatch;

  public static String LifecycleMappingPluginVersionIncompatible;

  public static String PluginExecutionMappingDuplicate;

  public static String PluginExecutionMappingInvalid;

  public static String ProjectConfiguratorNotAvailable;

  public static String ProjectConfigurationUpdateRequired;

  public static String LocalProjectScanner_accessDeniedFromFolder;

  public static String LocalProjectScanner_task_scanning;

  public static String LocalProjectScanner_missingArtifactId;

  public static String MavenExternalRuntime_error_cannot_parse;

  public static String MavenExternalRuntime_exc_unsupported;

  public static String MavenExternalRuntime_unknown;

  public static String MavenEmbeddedRuntime_unknown;

  public static String MavenImpl_error_calc_build_plan;

  public static String MavenImpl_error_create_repo;

  public static String MavenImpl_error_init_maven;

  public static String MavenImpl_error_lookup;

  public static String MavenImpl_error_missing;

  public static String MavenImpl_error_mojo;

  public static String MavenImpl_error_no_exec_req;

  public static String MavenImpl_error_param;

  public static String MavenImpl_error_param_for_execution;

  public static String MavenImpl_error_read_config;

  public static String MavenImpl_error_read_lastUpdated;

  public static String MavenImpl_error_read_pom;

  public static String MavenImpl_error_read_project;

  public static String MavenImpl_error_read_settings;

  public static String MavenImpl_error_read_settings2;

  public static String MavenImpl_error_read_toolchains;

  public static String MavenImpl_error_resolve;

  public static String MavenImpl_error_sort;

  public static String MavenImpl_error_write_lastUpdated;

  public static String MavenImpl_error_write_pom;

  public static String MavenImpl_error_write_settings;

  public static String MavenMarkerManager_duplicate_groupid;

  public static String MavenMarkerManager_duplicate_version;

  public static String MavenMarkerManager_error_missing;

  public static String MavenMarkerManager_error_noschema;

  public static String MavenMarkerManager_error_offline;

  public static String MavenMarkerManager_managed_title;

  public static String MavenMarkerManager_redundant_managed_title;

  public static String MavenModelManager_error_create;

  public static String MavenModelManager_error_pom_exists;

  public static String MavenModelManager_error_read;

  public static String MavenModelManager_monitor_building;

  public static String MavenModelManager_monitor_reading;

  public static String MavenProjectFacade_error;

  public static String MavenProjectPomScanner_23;

  public static String MavenProjectPomScanner_task_resolving;

  public static String NexusIndexManager_78;

  public static String NexusIndexManager_error_add_repo;

  public static String NexusIndexManager_error_read_index;

  public static String NexusIndexManager_error_reindexing;

  public static String NexusIndexManager_error_root_grp;

  public static String NexusIndexManager_error_search;

  public static String NexusIndexManager_error_unexpected;

  public static String NexusIndexManager_error_write_index;

  public static String NexusIndexManager_inherited;

  public static String NexusIndexManager_task_updating;

  public static String PomFileContentDescriber_error;

  public static String ProjectConfigurationManager_0;

  public static String ProjectConfigurationManager_error_failed;

  public static String ProjectConfigurationManager_error_rename;

  public static String ProjectConfigurationManager_error_targetDir;

  public static String ProjectConfigurationManager_error_unable_archetype;

  public static String ProjectConfigurationManager_task_configuring;

  public static String ProjectConfigurationManager_task_creating;

  public static String ProjectConfigurationManager_task_creating_folders;

  public static String ProjectConfigurationManager_task_creating_pom;

  public static String ProjectConfigurationManager_task_creating_project;

  public static String ProjectConfigurationManager_task_creating_project1;

  public static String ProjectConfigurationManager_task_creating_workspace;

  public static String ProjectConfigurationManager_task_disable_nature;

  public static String ProjectConfigurationManager_task_enable_nature;

  public static String ProjectConfigurationManager_task_executing_archetype;

  public static String ProjectConfigurationManager_task_importing;

  public static String ProjectConfigurationManager_task_importing2;

  public static String ProjectConfigurationManager_task_refreshing;

  public static String ProjectConfigurationManager_task_updating;

  public static String ProjectConfigurationManager_task_updating_projects;

  public static String ProjectRegistryManager_task_project;

  public static String ProjectRegistryManager_task_refreshing;

  public static String ProjectRegistryRefreshJob_task_refreshing;

  public static String ProjectRegistryRefreshJob_title;

  public static String RepositoryRegistryUpdateJob_title;

  public static String pluginMarkerBuildError;

  public static String importProjectExists;

  public static String buildConextFileAccessOutsideOfProjectBasedir;

  public static String ProjectConversion_error_duplicate_conversion_participant;

  public static String AbstractMavenRuntime_unknownProject;

  public static String AnnotationMappingMetadataSource_ErrorParsingInstruction;

  public static String AnnotationMappingMetadataSource_UnsupportedInstructionFormat;

  public static String ProjectConfiguratorToRunBeforeNotAvailable;

  public static String ProjectConfiguratorToRunAfterNotAvailable;

  public static String MavenToolbox_lookuprequired;

  public static String MavenToolbox_sessionrequired;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
