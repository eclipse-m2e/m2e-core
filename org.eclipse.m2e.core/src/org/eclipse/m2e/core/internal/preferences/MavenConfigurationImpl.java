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

package org.eclipse.m2e.core.internal.preferences;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.m2e.core.embedder.MavenConfigurationChangeEvent;


public class MavenConfigurationImpl implements IMavenConfiguration, IPreferenceChangeListener {

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

  private final IPreferencesService preferenceStore;

  private final ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

  public MavenConfigurationImpl() {
    this.preferenceStore = Platform.getPreferencesService();

    this.preferencesLookup[0] = new InstanceScope().getNode(IMavenConstants.PLUGIN_ID);
    this.preferencesLookup[1] = new DefaultScope().getNode(IMavenConstants.PLUGIN_ID);

    preferencesLookup[0].addPreferenceChangeListener(this);
  }

  public String getGlobalSettingsFile() {
    return preferenceStore.get(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, null, preferencesLookup);
  }

  public String getUserSettingsFile() {
    return preferenceStore.get(MavenPreferenceConstants.P_USER_SETTINGS_FILE, null, preferencesLookup);
  }

  public boolean isDebugOutput() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_DEBUG_OUTPUT, null, preferencesLookup));
  }

  public boolean isDownloadJavaDoc() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, null,
        preferencesLookup));
  }

  public boolean isDownloadSources() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, null,
        preferencesLookup));
  }

  public void setDownloadSources(boolean downloadSources) {
    preferencesLookup[0].putBoolean(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, downloadSources);
  }

  public boolean isHideFoldersOfNestedProjects() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS, null,
        preferencesLookup));
  }

  public boolean isOffline() {
    return Boolean.parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_OFFLINE, null, preferencesLookup));
  }

  public void setUserSettingsFile(String settingsFile) {
    preferencesLookup[0].put(MavenPreferenceConstants.P_USER_SETTINGS_FILE, nvl(settingsFile));
  }

  public void setGlobalSettingsFile(String globalSettingsFile) {
    preferencesLookup[0].put(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, nvl(globalSettingsFile));
  }

  private static String nvl(String s) {
    return s == null ? "" : s; //$NON-NLS-1$
  }

  public boolean isUpdateProjectsOnStartup() {
    return Boolean.parseBoolean(preferenceStore
        .get(MavenPreferenceConstants.P_UPDATE_PROJECTS, null, preferencesLookup));
  }

  public boolean isUpdateIndexesOnStartup() {
    return Boolean
        .parseBoolean(preferenceStore.get(MavenPreferenceConstants.P_UPDATE_INDEXES, null, preferencesLookup));
  }

  public synchronized void addConfigurationChangeListener(IMavenConfigurationChangeListener listener) {
    this.listeners.add(listener);
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    MavenConfigurationChangeEvent mavenEvent = new MavenConfigurationChangeEvent(event.getKey(), event.getNewValue(),
        event.getOldValue());
    for(Object listener : listeners.getListeners()) {
      try {
        ((IMavenConfigurationChangeListener) listener).mavenConfigutationChange(mavenEvent);
      } catch(Exception e) {
        MavenLogger.log("Could not deliver maven configuration change event", e);
      }
    }
  }
}
