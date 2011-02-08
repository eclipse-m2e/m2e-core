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

package org.eclipse.m2e.core.embedder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.embedder.MavenEmbeddedRuntime;
import org.eclipse.m2e.core.internal.embedder.MavenExternalRuntime;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


/**
 * Maven runtime manager
 * 
 * @author Eugene Kuleshov
 */
public class MavenRuntimeManager {

  public static final String DEFAULT = "DEFAULT"; //$NON-NLS-1$

  public static final String EMBEDDED = "EMBEDDED"; //$NON-NLS-1$

  public static final String WORKSPACE = "WORKSPACE"; //$NON-NLS-1$

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

  private final IPreferencesService preferenceStore;

  private Map<String, MavenRuntime> runtimes = new LinkedHashMap<String, MavenRuntime>();

  private MavenRuntime embeddedRuntime;

  private MavenRuntime workspaceRuntime;

  private MavenRuntime defaultRuntime;

  public MavenRuntimeManager() {
    this.preferenceStore = Platform.getPreferencesService();

    this.preferencesLookup[0] = new InstanceScope().getNode(IMavenConstants.PLUGIN_ID);
    this.preferencesLookup[1] = new DefaultScope().getNode(IMavenConstants.PLUGIN_ID);

    initRuntimes();
  }

  public void setEmbeddedRuntime(MavenRuntime embeddedRuntime) {
    this.embeddedRuntime = embeddedRuntime;
  }

  public void setWorkspaceRuntime(MavenRuntime workspaceRuntime) {
    this.workspaceRuntime = workspaceRuntime;
  }

  public MavenRuntime getDefaultRuntime() {
    if(defaultRuntime == null || !defaultRuntime.isAvailable()) {
      return embeddedRuntime;
    }
    return this.defaultRuntime;
  }

  public MavenRuntime getRuntime(String location) {
    if(location == null || location.length() == 0 || DEFAULT.equals(location)) {
      return getDefaultRuntime();
    }
    if(EMBEDDED.equals(location)) {
      return embeddedRuntime;
    }
    if(WORKSPACE.equals(location)) {
      return workspaceRuntime;
    }
    return runtimes.get(location);
  }

  public List<MavenRuntime> getMavenRuntimes() {
    ArrayList<MavenRuntime> runtimes = new ArrayList<MavenRuntime>();

    runtimes.add(embeddedRuntime);

    if(workspaceRuntime != null && workspaceRuntime.isAvailable()) {
      runtimes.add(workspaceRuntime);
    }

    for(MavenRuntime runtime : this.runtimes.values()) {
      if(runtime.isAvailable()) {
        runtimes.add(runtime);
      }
    }
    return runtimes;
  }

  public void reset() {
    preferencesLookup[0].remove(MavenPreferenceConstants.P_RUNTIMES);
    preferencesLookup[0].remove(MavenPreferenceConstants.P_DEFAULT_RUNTIME);

    initRuntimes();
  }

  public void setDefaultRuntime(MavenRuntime runtime) {
    this.defaultRuntime = runtime;

    if(runtime == null) {
      preferencesLookup[0].remove(MavenPreferenceConstants.P_DEFAULT_RUNTIME);
    } else {
      preferencesLookup[0].put(MavenPreferenceConstants.P_DEFAULT_RUNTIME, runtime.getLocation());
    }
  }

  public void setRuntimes(List<MavenRuntime> runtimes) {
    this.runtimes.clear();

    String separator = ""; //$NON-NLS-1$
    StringBuffer sb = new StringBuffer();
    for(MavenRuntime runtime : runtimes) {
      if(runtime.isEditable()) {
        this.runtimes.put(runtime.getLocation(), runtime);
        sb.append(separator).append(runtime.getLocation());
        separator = "|"; //$NON-NLS-1$
      }
    }
    preferencesLookup[0].put(MavenPreferenceConstants.P_RUNTIMES, sb.toString());
  }

  private void initRuntimes() {
    runtimes.clear();

    defaultRuntime = null;

    String selected = preferenceStore.get(MavenPreferenceConstants.P_DEFAULT_RUNTIME, null, preferencesLookup);

    String runtimesPreference = preferenceStore.get(MavenPreferenceConstants.P_RUNTIMES, null, preferencesLookup);
    if(runtimesPreference != null && runtimesPreference.length() > 0) {
      String[] locations = runtimesPreference.split("\\|"); //$NON-NLS-1$
      for(int i = 0; i < locations.length; i++ ) {
        MavenRuntime runtime = createExternalRuntime(locations[i]);
        runtimes.put(runtime.getLocation(), runtime);
        if(runtime.getLocation().equals(selected)) {
          defaultRuntime = runtime;
        }
      }
    }
  }

  public static MavenRuntime createExternalRuntime(String location) {
    return new MavenExternalRuntime(location);
  }

  public String getGlobalSettingsFile() {
    //only return the preference store value for the global settings file if its an embedded runtime
    if(defaultRuntime == null || defaultRuntime instanceof MavenEmbeddedRuntime) {
      String globalSettings = preferenceStore.get(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, null,
          preferencesLookup);
      return globalSettings.trim().length() == 0 ? null : globalSettings;
    }
    return defaultRuntime == null ? null : defaultRuntime.getSettings();
  }

}
