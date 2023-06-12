/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.preferences;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.PreferenceFilterEntry;

import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;

import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.m2e.core.embedder.MavenConfigurationChangeEvent;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;


@Component(service = {IMavenConfiguration.class})
public class MavenConfigurationImpl implements IMavenConfiguration, IPreferenceChangeListener, INodeChangeListener {
  private static final Logger log = LoggerFactory.getLogger(MavenConfigurationImpl.class);

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

  @Reference
  private IPreferencesService preferenceStore;

  private final ListenerList<IMavenConfigurationChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

  private boolean exists(IEclipsePreferences preferenceNode) {
    if(preferenceNode == null) {
      return false;
    }
    try {
      return preferenceNode.nodeExists("");
    } catch(BackingStoreException ex) {
      log.error(ex.getMessage(), ex);
      return false;
    }
  }

  @Activate
  void init() {
    if(exists(preferencesLookup[0])) {
      ((IEclipsePreferences) preferencesLookup[0].parent()).removeNodeChangeListener(this);
      preferencesLookup[0].removePreferenceChangeListener(this);
    }
    preferencesLookup[0] = InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID);
    ((IEclipsePreferences) preferencesLookup[0].parent()).addNodeChangeListener(this);
    preferencesLookup[0].addPreferenceChangeListener(this);

    if(exists(preferencesLookup[1])) {
      ((IEclipsePreferences) preferencesLookup[1].parent()).removeNodeChangeListener(this);
      preferencesLookup[1].removePreferenceChangeListener(this);
    }
    preferencesLookup[1] = DefaultScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID);
    ((IEclipsePreferences) preferencesLookup[1].parent()).addNodeChangeListener(this);
  }

  @Override
  public String getGlobalSettingsFile() {
    return preferenceStore.get(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, null, preferencesLookup);
  }

  @Override
  public String getUserSettingsFile() {
    return preferenceStore.get(MavenPreferenceConstants.P_USER_SETTINGS_FILE, null, preferencesLookup);
  }

  @Override
  public boolean isDebugOutput() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_DEBUG_OUTPUT, null, preferencesLookup));
  }

  public void setDebugOutput(boolean debug) {
    preferencesLookup[0].putBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT, debug);
  }

  @Override
  public boolean isDownloadJavaDoc() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, null,
        preferencesLookup));
  }

  @Override
  public boolean isDownloadSources() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, null,
        preferencesLookup));
  }

  public void setDownloadSources(boolean downloadSources) {
    preferencesLookup[0].putBoolean(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, downloadSources);
  }

  public void setDownloadJavadoc(boolean downloadJavadoc) {
    preferencesLookup[0].putBoolean(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, downloadJavadoc);
  }

  @Override
  public boolean isHideFoldersOfNestedProjects() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS, null,
        preferencesLookup));
  }

  @Override
  public boolean isOffline() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_OFFLINE, null, preferencesLookup));
  }

  @Override
  public void setUserSettingsFile(String settingsFile) throws CoreException {
    settingsFile = trim(settingsFile);
    if(!eq(settingsFile, preferencesLookup[0].get(MavenPreferenceConstants.P_USER_SETTINGS_FILE, null))) {
      if(settingsFile != null) {
        preferencesLookup[0].put(MavenPreferenceConstants.P_USER_SETTINGS_FILE, settingsFile);
      } else {
        preferencesLookup[0].remove(MavenPreferenceConstants.P_USER_SETTINGS_FILE);
      }
      preferenceStore.applyPreferences(preferencesLookup[0], new IPreferenceFilter[] {getPreferenceFilter()});
    }
  }

  @Override
  public void setGlobalSettingsFile(String globalSettingsFile) throws CoreException {
    globalSettingsFile = trim(globalSettingsFile);
    if(!eq(globalSettingsFile, preferencesLookup[0].get(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, null))) {
      if(globalSettingsFile != null) {
        preferencesLookup[0].put(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, globalSettingsFile);
      } else {
        preferencesLookup[0].remove(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE);
      }
      preferenceStore.applyPreferences(preferencesLookup[0], new IPreferenceFilter[] {getPreferenceFilter()});
    }
  }

  private boolean eq(String a, String b) {
    return a != null ? a.equals(b) : b == null;
  }

  private String trim(String str) {
    if(str == null) {
      return null;
    }
    str = str.trim();
    return !str.isEmpty() ? str : null;
  }

  @Override
  public boolean isUpdateProjectsOnStartup() {
    return Boolean.parseBoolean(preferenceStore
        .get(MavenPreferenceConstants.P_UPDATE_PROJECTS, null, preferencesLookup));
  }

  @Override
  public boolean isUpdateIndexesOnStartup() {
    return Boolean
        .parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_UPDATE_INDEXES, null, preferencesLookup));
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  @Override
  public synchronized void addConfigurationChangeListener(IMavenConfigurationChangeListener listener) {
    this.listeners.add(listener);
  }

  public synchronized void removeConfigurationChangeListener(IMavenConfigurationChangeListener listener) {
    this.listeners.remove(listener);
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent event) {
    MavenConfigurationChangeEvent mavenEvent = new MavenConfigurationChangeEvent(event.getKey(), event.getNewValue(),
        event.getOldValue());
    for(IMavenConfigurationChangeListener listener : listeners) {
      try {
        listener.mavenConfigurationChange(mavenEvent);
      } catch(Exception e) {
        log.error("Could not deliver maven configuration change event", e);
      }
    }
  }

  @Override
  public void added(NodeChangeEvent event) {
  }

  @Override
  public void removed(NodeChangeEvent event) {
    if(event.getChild() == preferencesLookup[0] || event.getChild() == preferencesLookup[1]) {
      init();
    }
  }

  private IPreferenceFilter getPreferenceFilter() {
    return new IPreferenceFilter() {
      @Override
      public String[] getScopes() {
        return new String[] {InstanceScope.SCOPE, DefaultScope.SCOPE};
      }

      @Override
      public Map<String, PreferenceFilterEntry[]> getMapping(String scope) {
        return null;
      }
    };
  }

  @Override
  public String getGlobalUpdatePolicy() {
    String string = preferenceStore.get(MavenPreferenceConstants.P_GLOBAL_UPDATE_POLICY, null, preferencesLookup);
    //for backward compat
    if(string == null || "true".equalsIgnoreCase(string)) {
      return RepositoryPolicy.UPDATE_POLICY_NEVER;
    }
    if(MavenPreferenceConstants.GLOBAL_UPDATE_POLICY_DEFAULT.equals(string) || "false".equals(string)) {
      return null;
    }
    return string;
  }

  public void setGlobalUpdatePolicy(String policy) {
    if(policy == null) {
      preferencesLookup[0].remove(MavenPreferenceConstants.P_GLOBAL_UPDATE_POLICY);
    } else {
      preferencesLookup[0].put(MavenPreferenceConstants.P_GLOBAL_UPDATE_POLICY, policy);
    }
  }

  @Override
  public String getWorkspaceLifecycleMappingMetadataFile() {
    IPath stateLocation = MavenPluginActivator.getDefault().getStateLocation();
    String defaultValue = stateLocation.append(LifecycleMappingFactory.LIFECYCLE_MAPPING_METADATA_SOURCE_NAME)
        .toString();
    return preferenceStore.get(MavenPreferenceConstants.P_WORKSPACE_MAPPINGS_LOCATION, defaultValue, preferencesLookup);
  }

  @Override
  public void setWorkspaceLifecycleMappingMetadataFile(String location) throws CoreException {
    if(location != null) {
      preferencesLookup[0].put(MavenPreferenceConstants.P_WORKSPACE_MAPPINGS_LOCATION, location);
    } else {
      preferencesLookup[0].remove(MavenPreferenceConstants.P_WORKSPACE_MAPPINGS_LOCATION);
    }
    preferenceStore.applyPreferences(preferencesLookup[0], new IPreferenceFilter[] {getPreferenceFilter()});
  }

  @Override
  public String getOutOfDateProjectSeverity() {
    return preferenceStore.get(MavenPreferenceConstants.P_OUT_OF_DATE_PROJECT_CONFIG_PB,
        ProblemSeverity.error.toString(), preferencesLookup);
  }

  /**
   * For testing purposes only
   */
  public void setOutOfDateProjectSeverity(String severity) throws CoreException {
    if(severity == null) {
      preferencesLookup[0].remove(MavenPreferenceConstants.P_OUT_OF_DATE_PROJECT_CONFIG_PB);
    } else {
      preferencesLookup[0].put(MavenPreferenceConstants.P_OUT_OF_DATE_PROJECT_CONFIG_PB, severity);
    }
    preferenceStore.applyPreferences(preferencesLookup[0], new IPreferenceFilter[] {getPreferenceFilter()});
  }

  @Override
  public String getGlobalChecksumPolicy() {
    return preferenceStore.get(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, null, preferencesLookup);
  }

  /**
   * For testing purposes only.
   */
  public void setGlobalChecksumPolicy(String checksumPolicy) {
    if(checksumPolicy == null) {
      preferencesLookup[0].remove(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    } else if(ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL.equals(checksumPolicy) //will fail eclipse builds in case checksum fails
        || ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN.equals(checksumPolicy) //XXX checksum warnings should be rendered as markers
        || ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE.equals(checksumPolicy)) {//will simply be ignored
      preferencesLookup[0].put(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, checksumPolicy);
    } else {
      throw new IllegalArgumentException(checksumPolicy + " is not a valid checksum policy");
    }
  }

  @Override
  public String getNotCoveredMojoExecutionSeverity() {
    return preferenceStore.get(MavenPreferenceConstants.P_NOT_COVERED_MOJO_EXECUTION_PB,
        ProblemSeverity.warning.toString(), preferencesLookup);
  }

  /**
   * For testing purposes only
   */
  public void setNotCoveredMojoExecutionSeverity(String severity) throws CoreException {
    if(severity == null) {
      preferencesLookup[0].remove(MavenPreferenceConstants.P_NOT_COVERED_MOJO_EXECUTION_PB);
    } else {
      preferencesLookup[0].put(MavenPreferenceConstants.P_NOT_COVERED_MOJO_EXECUTION_PB, severity);
    }
    preferenceStore.applyPreferences(preferencesLookup[0], new IPreferenceFilter[] {getPreferenceFilter()});
  }

  @Override
  public String getOverridingManagedVersionExecutionSeverity() {
    return preferenceStore.get(MavenPreferenceConstants.P_OVERRIDING_MANAGED_VERSION_PB,
        ProblemSeverity.warning.toString(), preferencesLookup);
  }

  /**
   * For testing purposes only
   */
  public void setOverridingManagedVersionExecutionSeverity(String severity) throws CoreException {
    if(severity == null) {
      preferencesLookup[0].remove(MavenPreferenceConstants.P_OVERRIDING_MANAGED_VERSION_PB);
    } else {
      preferencesLookup[0].put(MavenPreferenceConstants.P_OVERRIDING_MANAGED_VERSION_PB, severity);
    }
    preferenceStore.applyPreferences(preferencesLookup[0], new IPreferenceFilter[] {getPreferenceFilter()});
  }

  @Override
  public boolean isAutomaticallyUpdateConfiguration() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_AUTO_UPDATE_CONFIGURATION, null,
        preferencesLookup));
  }

  public void setAutomaticallyUpdateConfiguration(boolean value) {
    preferencesLookup[0].putBoolean(MavenPreferenceConstants.P_AUTO_UPDATE_CONFIGURATION, value);
  }

  @Override
  public void setDefaultMojoExecutionAction(PluginExecutionAction mojoAction) {
    preferencesLookup[0].put(MavenPreferenceConstants.P_DEFAULT_MOJO_EXECUTION_ACTION, mojoAction.name());
  }

  @Override
  public PluginExecutionAction getDefaultMojoExecutionAction() {
    String value = preferenceStore.get(MavenPreferenceConstants.P_DEFAULT_MOJO_EXECUTION_ACTION,
        PluginExecutionAction.DEFAULT_ACTION.toString(),
        preferencesLookup);
    try {
      return PluginExecutionAction.valueOf(value);
    } catch(IllegalArgumentException e) {
      //fallback...
      return PluginExecutionAction.DEFAULT_ACTION;
    }
  }

  @Override
  public boolean buildWithNullSchedulingRule() {
    return Boolean.parseBoolean(
        preferenceStore.get(MavenPreferenceConstants.P_BUILDER_USE_NULL_SCHEDULING_RULE, null, preferencesLookup));
  }
}
