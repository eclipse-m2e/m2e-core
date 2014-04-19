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

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.ClasspathEntry;
import org.eclipse.m2e.core.internal.launch.DefaultWorkspaceRuntime;
import org.eclipse.m2e.core.internal.launch.MavenEmbeddedRuntime;
import org.eclipse.m2e.core.internal.launch.MavenExternalRuntime;
import org.eclipse.m2e.core.internal.launch.MavenWorkspaceRuntime;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


/**
 * Maven runtime manager
 * 
 * @author Eugene Kuleshov
 * @author Jason van Zyl
 */
public class MavenRuntimeManager {

  public static final String DEFAULT = "DEFAULT"; //$NON-NLS-1$

  public static final String EMBEDDED = "EMBEDDED"; //$NON-NLS-1$

  public static final String WORKSPACE = "WORKSPACE"; //$NON-NLS-1$

  private static final String EXTERNAL = "EXTERNAL"; //$NON-NLS-1$

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

  private final IPreferencesService preferenceStore;

  public MavenRuntimeManager() {
    this.preferenceStore = Platform.getPreferencesService();
    this.preferencesLookup[0] = InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID);
    this.preferencesLookup[1] = DefaultScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID);
  }

  /**
   * @deprecated this method does nothing
   */
  public void setEmbeddedRuntime(MavenRuntime embeddedRuntime) {
  }

  /**
   * @deprecated this method does nothing
   */
  @Deprecated
  public void setWorkspaceRuntime(MavenRuntime workspaceRuntime) {
  }

  public MavenRuntime getDefaultRuntime() {
    String selected = preferenceStore.get(MavenPreferenceConstants.P_DEFAULT_RUNTIME, null, preferencesLookup);
    if(selected == null) {
      return new MavenEmbeddedRuntime();
    }
    MavenRuntime runtime = getRuntimeByName(selected);
    return runtime != null && runtime.isAvailable() ? runtime : new MavenEmbeddedRuntime();
  }

  /**
   * @deprecated use {@link #getRuntimeByName(String)}
   */
  public MavenRuntime getRuntime(String location) {
    if(location == null || location.length() == 0 || DEFAULT.equals(location)) {
      return getDefaultRuntime();
    }
    for(MavenRuntime runtime : getRuntimes().values()) {
      if(location.equals(runtime.getLocation())) {
        return runtime;
      }
    }

    return null;
  }

  /**
   * @since 1.5
   */
  public MavenRuntime getRuntimeByName(String name) {
    return getRuntimes().get(name);
  }

  public List<MavenRuntime> getMavenRuntimes() {
    List<MavenRuntime> mavenRuntimes = new ArrayList<MavenRuntime>();
    for(MavenRuntime mavenRuntime : getRuntimes().values()) {
      if(mavenRuntime.isAvailable()) {
        mavenRuntimes.add(mavenRuntime);
      }
    }
    return mavenRuntimes;
  }

  public void reset() {
    preferencesLookup[0].remove(MavenPreferenceConstants.P_RUNTIMES);
    preferencesLookup[0].remove(MavenPreferenceConstants.P_DEFAULT_RUNTIME);
    removeRuntimePreferences();
    flush();
  }

  public void setDefaultRuntime(MavenRuntime runtime) {
    if(runtime == null) {
      preferencesLookup[0].remove(MavenPreferenceConstants.P_DEFAULT_RUNTIME);
    } else {
      preferencesLookup[0].put(MavenPreferenceConstants.P_DEFAULT_RUNTIME, runtime.getName());
    }
    flush();
  }

  private void flush() {
    try {
      preferencesLookup[0].flush();
    } catch(BackingStoreException ex) {
      // TODO do nothing
    }
  }

  public void setRuntimes(List<MavenRuntime> runtimes) {
    removeRuntimePreferences();
    StringBuilder sb = new StringBuilder();
    for(MavenRuntime runtime : runtimes) {
      if(runtime.isEditable()) {
        AbstractMavenRuntime impl = (AbstractMavenRuntime) runtime;
        if(sb.length() > 0) {
          sb.append('|');
        }
        sb.append(runtime.getName());
        if(!impl.isLegacy()) {
          Preferences runtimeNode = getRuntimePreferences(runtime.getName(), true);
          runtimeNode.put("type", getRuntimeType(runtime));
          runtimeNode.put("location", runtime.getLocation());
          String extensions = encodeClasspath(impl.getExtensions());
          if(extensions != null) {
            runtimeNode.put("extensions", extensions);
          } else {
            runtimeNode.remove("extensions");
          }
        }
      }
    }
    preferencesLookup[0].put(MavenPreferenceConstants.P_RUNTIMES, sb.toString());
    flush();
  }

  private void removeRuntimePreferences() {
    try {
      if(preferencesLookup[0].nodeExists(MavenPreferenceConstants.P_RUNTIMES_NODE)) {
        preferencesLookup[0].node(MavenPreferenceConstants.P_RUNTIMES_NODE).removeNode();
      }
    } catch(BackingStoreException ex) {
      // assume the node does not exist
    }
  }

  private String getRuntimeType(MavenRuntime runtime) {
    if(runtime instanceof MavenExternalRuntime) {
      return EXTERNAL;
    } else if(runtime instanceof MavenWorkspaceRuntime) {
      return WORKSPACE;
    }
    throw new IllegalArgumentException();
  }

  private Preferences getRuntimePreferences(String name, boolean create) {
    Preferences runtimesNode = preferencesLookup[0].node(MavenPreferenceConstants.P_RUNTIMES_NODE);
    try {
      if(runtimesNode.nodeExists(name) || create) {
        return runtimesNode.node(name);
      }
    } catch(BackingStoreException ex) {
      // assume the node does not exist
    }
    return null;
  }

  private String encodeClasspath(List<ClasspathEntry> classpath) {
    if(classpath == null || classpath.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for(ClasspathEntry cpe : classpath) {
      if(sb.length() > 0) {
        sb.append('|');
      }
      sb.append(cpe.toExternalForm());
    }
    return sb.toString();
  }

  private List<ClasspathEntry> decodeClasspath(String string) {
    if(string == null || string.isEmpty()) {
      return null;
    }
    List<ClasspathEntry> result = new ArrayList<ClasspathEntry>();
    for(String entry : string.split("\\|")) {
      ClasspathEntry decoded = ClasspathEntry.fromExternalForm(entry);
      if(decoded != null) {
        result.add(decoded);
      }
    }
    return result;
  }

  private Map<String, AbstractMavenRuntime> getRuntimes() {
    Map<String, AbstractMavenRuntime> runtimes = new LinkedHashMap<String, AbstractMavenRuntime>();
    runtimes.put(EMBEDDED, new MavenEmbeddedRuntime());
    runtimes.put(WORKSPACE, new DefaultWorkspaceRuntime());

    String runtimesPreference = preferenceStore.get(MavenPreferenceConstants.P_RUNTIMES, null, preferencesLookup);
    if(runtimesPreference != null && runtimesPreference.length() > 0) {
      for(String name : runtimesPreference.split("\\|")) { //$NON-NLS-1$
        Preferences preferences = getRuntimePreferences(name, false);
        AbstractMavenRuntime runtime;
        if(preferences == null) {
          runtime = (AbstractMavenRuntime) createExternalRuntime(name);
        } else {
          runtime = createRuntime(name, preferences);
        }
        runtimes.put(runtime.getName(), runtime);
      }
    }

    return runtimes;
  }

  private AbstractMavenRuntime createRuntime(String name, Preferences preferences) {
    String location = preferences.get("location", null);
    String type = preferences.get("type", EXTERNAL);
    AbstractMavenRuntime runtime;
    if(WORKSPACE.equals(type)) {
      runtime = new MavenWorkspaceRuntime(name);
    } else {
      runtime = new MavenExternalRuntime(name, location);
    }
    runtime.setExtensions(decodeClasspath(preferences.get("extensions", null)));
    return runtime;
  }

  /**
   * @deprecated as of version 1.5, m2e does not provide public API to create MavenRuntime instances
   */
  public static MavenRuntime createExternalRuntime(String location) {
    return new MavenExternalRuntime(location);
  }

  /**
   * @deprecated global setting file is only used to determine localRepository location, which does not make much sense
   */
  public String getGlobalSettingsFile() {
    String globalSettings = preferenceStore.get(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, null,
        preferencesLookup);
    return globalSettings.trim().length() == 0 ? null : globalSettings;
  }

}
