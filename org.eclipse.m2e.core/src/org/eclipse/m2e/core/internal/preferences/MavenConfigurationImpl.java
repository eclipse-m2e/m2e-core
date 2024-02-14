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

import java.io.File;
import java.util.Map;
import java.util.Objects;

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
import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor;

import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.m2e.core.embedder.MavenConfigurationChangeEvent;
import org.eclipse.m2e.core.embedder.MavenSettingsLocations;
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
    return getStringPreference(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, null);
  }

  @Override
  public String getUserSettingsFile() {
    return getStringPreference(MavenPreferenceConstants.P_USER_SETTINGS_FILE, null);
  }

  @Override
  public String getUserToolchainsFile() {
    return getStringPreference(MavenPreferenceConstants.P_USER_TOOLCHAINS_FILE, null);
  }

  @Override
  public boolean isDebugOutput() {
    return getBooleanPreference(MavenPreferenceConstants.P_DEBUG_OUTPUT);
  }

  public void setDebugOutput(boolean debug) {
    preferencesLookup[0].putBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT, debug);
  }

  @Override
  public boolean isDownloadJavaDoc() {
    return getBooleanPreference(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC);
  }

  @Override
  public boolean isDownloadSources() {
    return getBooleanPreference(MavenPreferenceConstants.P_DOWNLOAD_SOURCES);
  }

  public void setDownloadSources(boolean downloadSources) {
    preferencesLookup[0].putBoolean(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, downloadSources);
  }

  public void setDownloadJavadoc(boolean downloadJavadoc) {
    preferencesLookup[0].putBoolean(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, downloadJavadoc);
  }

  @Override
  public boolean isHideFoldersOfNestedProjects() {
    return getBooleanPreference(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS);
  }

  @Override
  public boolean isOffline() {
    return getBooleanPreference(MavenPreferenceConstants.P_OFFLINE);
  }

  @Override
  public void setUserSettingsFile(String settingsFile) throws CoreException {
    setSettingsFile(settingsFile, MavenPreferenceConstants.P_USER_SETTINGS_FILE);
  }

  @Override
  public void setGlobalSettingsFile(String globalSettingsFile) throws CoreException {
    setSettingsFile(globalSettingsFile, MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE);
  }

  @Override
  public void setUserToolchainsFile(String settingsFile) throws CoreException {
    setSettingsFile(settingsFile, MavenPreferenceConstants.P_USER_TOOLCHAINS_FILE);
  }

  private void setSettingsFile(String settingsFile, String preferenceKey) throws CoreException {
    if(settingsFile != null) {
      settingsFile = settingsFile.isBlank() ? null : settingsFile.strip();
    }
    if(!Objects.equals(settingsFile, preferencesLookup[0].get(preferenceKey, null))) {
      if(settingsFile != null) {
        preferencesLookup[0].put(preferenceKey, settingsFile);
      } else {
        preferencesLookup[0].remove(preferenceKey);
      }
      preferenceStore.applyPreferences(preferencesLookup[0], PREFERENCE_FILTERS);
    }
  }

  @Override
  public boolean isUpdateProjectsOnStartup() {
    return getBooleanPreference(MavenPreferenceConstants.P_UPDATE_PROJECTS);
  }

  @Override
  public boolean isUpdateIndexesOnStartup() {
    return getBooleanPreference(MavenPreferenceConstants.P_UPDATE_INDEXES);
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
  public MavenSettingsLocations getSettingsLocations() {
    File userSettings;
    File globalSettings;
    String configSettingsFile = getUserSettingsFile();
    if (configSettingsFile==null) {
      userSettings = SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE;
    } else {
      userSettings = new File(configSettingsFile);
    }
    String configGlobalSettingsFile = getGlobalSettingsFile();
    if(configGlobalSettingsFile == null) {
      globalSettings = null;
    } else {
      globalSettings = new File(configGlobalSettingsFile);
    }
    return new MavenSettingsLocations(globalSettings, userSettings);
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

  private static final IPreferenceFilter[] PREFERENCE_FILTERS = new IPreferenceFilter[] {new IPreferenceFilter() {
    @Override
    public String[] getScopes() {
      return new String[] {InstanceScope.SCOPE, DefaultScope.SCOPE};
    }

    @Override
    public Map<String, PreferenceFilterEntry[]> getMapping(String scope) {
      return null;
    }
  }};

  @Override
  public String getGlobalUpdatePolicy() {
    String string = getStringPreference(MavenPreferenceConstants.P_GLOBAL_UPDATE_POLICY, null);
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
    return getStringPreference(MavenPreferenceConstants.P_WORKSPACE_MAPPINGS_LOCATION, defaultValue);
  }

  @Override
  public void setWorkspaceLifecycleMappingMetadataFile(String location) throws CoreException {
    if(location != null) {
      preferencesLookup[0].put(MavenPreferenceConstants.P_WORKSPACE_MAPPINGS_LOCATION, location);
    } else {
      preferencesLookup[0].remove(MavenPreferenceConstants.P_WORKSPACE_MAPPINGS_LOCATION);
    }
    preferenceStore.applyPreferences(preferencesLookup[0], PREFERENCE_FILTERS);
  }

  @Override
  public String getOutOfDateProjectSeverity() {
    return getStringPreference(MavenPreferenceConstants.P_OUT_OF_DATE_PROJECT_CONFIG_PB,
        ProblemSeverity.error.toString());
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
    preferenceStore.applyPreferences(preferencesLookup[0], PREFERENCE_FILTERS);
  }

  @Override
  public String getGlobalChecksumPolicy() {
    return getStringPreference(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, null);
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
    return getStringPreference(MavenPreferenceConstants.P_NOT_COVERED_MOJO_EXECUTION_PB,
        ProblemSeverity.warning.toString());
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
    preferenceStore.applyPreferences(preferencesLookup[0], PREFERENCE_FILTERS);
  }

  @Override
  public String getOverridingManagedVersionExecutionSeverity() {
    return getStringPreference(MavenPreferenceConstants.P_OVERRIDING_MANAGED_VERSION_PB,
        ProblemSeverity.warning.toString());
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
    preferenceStore.applyPreferences(preferencesLookup[0], PREFERENCE_FILTERS);
  }

  @Override
  public boolean isAutomaticallyUpdateConfiguration() {
    return getBooleanPreference(MavenPreferenceConstants.P_AUTO_UPDATE_CONFIGURATION);
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
    String value = getStringPreference(MavenPreferenceConstants.P_DEFAULT_MOJO_EXECUTION_ACTION,
        PluginExecutionAction.DEFAULT_ACTION.toString());
    try {
      return PluginExecutionAction.valueOf(value);
    } catch(IllegalArgumentException e) {
      //fallback...
      return PluginExecutionAction.DEFAULT_ACTION;
    }
  }

  @Override
  public boolean buildWithNullSchedulingRule() {
    return getBooleanPreference(MavenPreferenceConstants.P_BUILDER_USE_NULL_SCHEDULING_RULE);
  }

  private boolean getBooleanPreference(String key) {
    return Boolean.parseBoolean(getStringPreference(key, null));
  }

  private String getStringPreference(String key, String defaultValue) {
    return preferenceStore.get(key, defaultValue, preferencesLookup);
  }

}
