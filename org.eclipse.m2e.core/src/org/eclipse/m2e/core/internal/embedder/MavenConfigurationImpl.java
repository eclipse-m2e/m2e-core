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

package org.eclipse.m2e.core.internal.embedder;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.m2e.core.embedder.MavenConfigurationChangeEvent;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


public class MavenConfigurationImpl implements IMavenConfiguration, IPropertyChangeListener {

  private final IPreferenceStore preferenceStore;
  private final ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

  public MavenConfigurationImpl(IPreferenceStore preferenceStore) {
    this.preferenceStore = preferenceStore;
    preferenceStore.addPropertyChangeListener(this);
  }

  public String getGlobalSettingsFile() {
    return preferenceStore.getString(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE);
  }

  public String getJiraPassword() {
    return preferenceStore.getString(MavenPreferenceConstants.P_JIRA_PASSWORD);
  }

  public String getJiraUsername() {
    return preferenceStore.getString(MavenPreferenceConstants.P_JIRA_USERNAME);
  }

  public String getUserSettingsFile() {
    return preferenceStore.getString(MavenPreferenceConstants.P_USER_SETTINGS_FILE);
  }

  public boolean isDebugOutput() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT);
  }

  public boolean isDownloadJavaDoc() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC);
  }

  public boolean isDownloadSources() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_DOWNLOAD_SOURCES);
  }

  public boolean isHideFoldersOfNestedProjects() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS);
  }

  public boolean isOffline() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_OFFLINE);
  }

  public void setUserSettingsFile(String settingsFile) {
    preferenceStore.setValue(MavenPreferenceConstants.P_USER_SETTINGS_FILE, nvl(settingsFile));
  }
  
  public void setGlobalSettingsFile(String globalSettingsFile){
    preferenceStore.setValue(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, nvl(globalSettingsFile));
  }

  private static String nvl(String s) {
    return s == null ? "" : s; //$NON-NLS-1$
  }

  public boolean isUpdateProjectsOnStartup() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_UPDATE_PROJECTS);
  }

  public boolean isUpdateIndexesOnStartup() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_UPDATE_INDEXES);
  }

  public synchronized void addConfigurationChangeListener(IMavenConfigurationChangeListener listener) {
    this.listeners.add(listener);
  }

  public void propertyChange(PropertyChangeEvent event) {
    MavenConfigurationChangeEvent mavenEvent = new MavenConfigurationChangeEvent(event.getProperty(), event.getNewValue(), event.getOldValue());
    for (Object listener : listeners.getListeners()) {
      try {
        ((IMavenConfigurationChangeListener) listener).mavenConfigutationChange(mavenEvent);
      } catch (Exception e) {
        MavenLogger.log("Could not deliver maven configuration change event", e);
      }
    }
  
  }
}
