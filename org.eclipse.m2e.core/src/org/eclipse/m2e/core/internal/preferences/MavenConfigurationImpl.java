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

import java.util.Map;

import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.m2e.core.embedder.MavenConfigurationChangeEvent;


public class MavenConfigurationImpl implements IMavenConfiguration, IPreferenceChangeListener, INodeChangeListener {
  private static final Logger log = LoggerFactory.getLogger(MavenConfigurationImpl.class);

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

  private final IPreferencesService preferenceStore;

  private final ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

  public MavenConfigurationImpl() {
    preferenceStore = Platform.getPreferencesService();

    init();
  }

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

  private void init() {
    if(exists(preferencesLookup[0])) {
      ((IEclipsePreferences) preferencesLookup[0].parent()).removeNodeChangeListener(this);
      preferencesLookup[0].removePreferenceChangeListener(this);
    }
    preferencesLookup[0] = new InstanceScope().getNode(IMavenConstants.PLUGIN_ID);
    ((IEclipsePreferences) preferencesLookup[0].parent()).addNodeChangeListener(this);
    preferencesLookup[0].addPreferenceChangeListener(this);

    if(exists(preferencesLookup[1])) {
      ((IEclipsePreferences) preferencesLookup[1].parent()).removeNodeChangeListener(this);
      preferencesLookup[1].removePreferenceChangeListener(this);
    }
    preferencesLookup[1] = new DefaultScope().getNode(IMavenConstants.PLUGIN_ID);
    ((IEclipsePreferences) preferencesLookup[1].parent()).addNodeChangeListener(this);
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

  public void setUserSettingsFile(String settingsFile) throws CoreException {
    preferencesLookup[0].put(MavenPreferenceConstants.P_USER_SETTINGS_FILE, nvl(settingsFile));
    preferenceStore.applyPreferences(preferencesLookup[0], new IPreferenceFilter[] {getPreferenceFilter()});
  }

  public void setGlobalSettingsFile(String globalSettingsFile) throws CoreException {
    preferencesLookup[0].put(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, nvl(globalSettingsFile));
    preferenceStore.applyPreferences(preferencesLookup[0], new IPreferenceFilter[] {getPreferenceFilter()});
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
        log.error("Could not deliver maven configuration change event", e);
      }
    }
  }

  public void added(NodeChangeEvent event) {
  }

  public void removed(NodeChangeEvent event) {
    if(event.getChild() == preferencesLookup[0] || event.getChild() == preferencesLookup[1]) {
      init();
    }
  }

  private IPreferenceFilter getPreferenceFilter() {
    return new IPreferenceFilter() {
      public String[] getScopes() {
        return new String[] {InstanceScope.SCOPE, DefaultScope.SCOPE};
      }

      public Map getMapping(String scope) {
        return null;
      }
    };
  }
}
