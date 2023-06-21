/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.apache.maven.Maven;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


/**
 * @since 1.5
 */
@Component(service = MavenRuntimeManagerImpl.class)
public class MavenRuntimeManagerImpl {
  public static final String DEFAULT = "DEFAULT"; //$NON-NLS-1$

  public static final String EMBEDDED = "EMBEDDED"; //$NON-NLS-1$

  public static final String WORKSPACE = "WORKSPACE"; //$NON-NLS-1$

  public static final String EXTERNAL = "EXTERNAL"; //$NON-NLS-1$

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

  @Reference
  private IPreferencesService preferenceStore;

  public MavenRuntimeManagerImpl() {
    this.preferencesLookup[0] = InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID);
    this.preferencesLookup[1] = DefaultScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID);
  }

  public AbstractMavenRuntime getRuntime(String name) {
    if(name == null || name.trim().isEmpty() || DEFAULT.equals(name.trim())) {
      return getDefaultRuntime();
    }
    AbstractMavenRuntime runtime = getRuntimes().get(name);
    if(runtime == null) {
      runtime = getDefaultRuntime();
    }
    return runtime;
  }

  private AbstractMavenRuntime getDefaultRuntime() {
    String name = preferenceStore.get(MavenPreferenceConstants.P_DEFAULT_RUNTIME, null, preferencesLookup);
    Map<String, AbstractMavenRuntime> runtimes = getRuntimes();
    AbstractMavenRuntime runtime = runtimes.get(name);
    if(runtime == null || !runtime.isAvailable()) {
      runtime = runtimes.get(EMBEDDED);
    }
    return runtime;
  }

  public List<AbstractMavenRuntime> getMavenRuntimes() {
    List<AbstractMavenRuntime> mavenRuntimes = new ArrayList<>();
    for(AbstractMavenRuntime mavenRuntime : getRuntimes().values()) {
      if(mavenRuntime.isAvailable()) {
        mavenRuntimes.add(mavenRuntime);
      }
    }
    return mavenRuntimes;
  }

  /**
   * @param available is {@code true} only available runtimes are returned, all runtimes are returned if {@code false}
   * @since 1.5
   */
  public List<AbstractMavenRuntime> getMavenRuntimes(boolean available) {
    List<AbstractMavenRuntime> mavenRuntimes = new ArrayList<>();
    for(AbstractMavenRuntime mavenRuntime : getRuntimes().values()) {
      if(!available || mavenRuntime.isAvailable()) {
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

  public void setDefaultRuntime(AbstractMavenRuntime runtime) {
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

  public void setRuntimes(List<AbstractMavenRuntime> runtimes) {
    removeRuntimePreferences();
    Set<String> names = new HashSet<>();
    StringBuilder sb = new StringBuilder();
    for(AbstractMavenRuntime runtime : runtimes) {
      String name = runtime.getName();
      if(!names.add(name)) {
        throw new IllegalArgumentException();
      }
      if(runtime.isEditable()) {
        if(sb.length() > 0) {
          sb.append('|');
        }
        sb.append(name);
        if(!runtime.isLegacy()) {
          Preferences runtimeNode = getRuntimePreferences(name, true);
          runtimeNode.put("type", getRuntimeType(runtime));
          runtimeNode.put("location", runtime.getLocation());
          String extensions = encodeClasspath(runtime.getExtensions());
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

  private String getRuntimeType(AbstractMavenRuntime runtime) {
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
    List<ClasspathEntry> result = new ArrayList<>();
    for(String entry : string.split("\\|")) {
      ClasspathEntry decoded = ClasspathEntry.fromExternalForm(entry);
      if(decoded != null) {
        result.add(decoded);
      }
    }
    return result;
  }

  public Map<String, AbstractMavenRuntime> getRuntimes() {
    Map<String, AbstractMavenRuntime> runtimes = new LinkedHashMap<>();
    Bundle defaultRuntimeBundle = FrameworkUtil.getBundle(Maven.class);
    runtimes.put(EMBEDDED, new MavenEmbeddedRuntime(defaultRuntimeBundle, EMBEDDED,
        MavenEmbeddedRuntime.getMavenVersionFromBundle(defaultRuntimeBundle)));
    getMavenRuntimeBundles().filter(rtb -> rtb != defaultRuntimeBundle).forEach(rtb -> {
      String mavenVersionFromBundle = MavenEmbeddedRuntime.getMavenVersionFromBundle(rtb);
      String key;
      if(mavenVersionFromBundle == null) {
        Version version = rtb.getVersion();
        key = EMBEDDED + "_" + version.getMajor() + "_" + version.getMinor() + "_" + version.getMicro();
      } else {
        key = EMBEDDED + "_" + mavenVersionFromBundle.replace('.', '_');
      }
      runtimes.put(key, new MavenEmbeddedRuntime(rtb, key, mavenVersionFromBundle));
    });
    runtimes.put(WORKSPACE, new DefaultWorkspaceRuntime());

    String runtimesPreference = preferenceStore.get(MavenPreferenceConstants.P_RUNTIMES, null, preferencesLookup);
    if(runtimesPreference != null && runtimesPreference.length() > 0) {
      for(String name : runtimesPreference.split("\\|")) { //$NON-NLS-1$
        Preferences preferences = getRuntimePreferences(name, false);
        AbstractMavenRuntime runtime;
        if(preferences == null) {
          runtime = new MavenExternalRuntime(name);
        } else {
          runtime = createRuntime(name, preferences);
        }
        runtimes.put(runtime.getName(), runtime);
      }
    }

    return runtimes;
  }

  private Stream<Bundle> getMavenRuntimeBundles() {
    Bundle myBundle = FrameworkUtil.getBundle(MavenRuntimeManagerImpl.class);
    if(myBundle == null) {
      return Stream.empty();
    }
    BundleContext bundleContext = myBundle.getBundleContext();
    if(bundleContext == null) {
      return Stream.empty();
    }
    return Arrays.stream(bundleContext.getBundles())
        .filter(bundle -> "org.eclipse.m2e.maven.runtime".equals(bundle.getSymbolicName()));
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

}
