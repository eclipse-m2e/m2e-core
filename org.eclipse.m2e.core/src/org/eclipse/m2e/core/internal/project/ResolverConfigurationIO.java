/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.project;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.embedder.MavenProperties;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceInitializer;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;


/**
 * @TODO anyone can think of a better name?
 */
public class ResolverConfigurationIO {
  private static final Logger log = LoggerFactory.getLogger(ResolverConfigurationIO.class);

  /**
   * Configuration version project preference key.
   */
  private static final String P_VERSION = "version"; //$NON-NLS-1$

  /**
   * Workspace dependency resolution project preference key. Boolean, <code>true</code> means workspace dependency
   * resolution is enabled.
   */
  private static final String P_RESOLVE_WORKSPACE_PROJECTS = "resolveWorkspaceProjects"; //$NON-NLS-1$

  private static final boolean P_RESOLVE_WORKSPACE_PROJECTS_DEFAULT = false;

  private static final String P_AUTO_UPDATE_CONFIGURATION = MavenPreferenceConstants.P_AUTO_UPDATE_CONFIGURATION;

  /**
   * Active profiles project preference key. Value is comma-separated list of enabled profiles.
   */
  //FIXME Bug 337353 Can't rename the preference key as it would break existing projects
  private static final String P_SELECTED_PROFILES = "activeProfiles"; //$NON-NLS-1$

  /**
   * Lifecycle mapping id configured for the project explicitly.
   */
  private static final String P_LIFECYCLE_MAPPING_ID = "lifecycleMappingId";

  private static final String P_PROPERTIES = "properties";

  private static final String P_BASEDIR = "basedir";

  private static final String PROPERTIES_KV_SEPARATOR = ">";

  private static final String PROPERTIES_SEPARATOR = "|";

  private static final String ENCODING = "UTF-8";

  /**
   * Current configuration version value. See {@link #P_VERSION}
   */
  private static final String VERSION = "1"; //$NON-NLS-1$

  public static boolean saveResolverConfiguration(IProject project, IProjectConfiguration configuration) {
    IEclipsePreferences projectNode = getMavenProjectPreferences(project);
    if(projectNode != null) {
      projectNode.put(P_VERSION, VERSION);

      projectNode.putBoolean(P_RESOLVE_WORKSPACE_PROJECTS, configuration.isResolveWorkspaceProjects());
      projectNode.put(P_SELECTED_PROFILES, configuration.getSelectedProfiles());

      if(configuration.getLifecycleMappingId() != null) {
        projectNode.put(P_LIFECYCLE_MAPPING_ID, configuration.getLifecycleMappingId());
      } else {
        projectNode.remove(P_LIFECYCLE_MAPPING_ID);
      }
      if(configuration.getConfigurationProperties() != null && !configuration.getConfigurationProperties().isEmpty()) {
        projectNode.put(P_PROPERTIES, propertiesAsString(configuration.getConfigurationProperties()));
      } else {
        projectNode.remove(P_PROPERTIES);
      }
      return savePreferences(projectNode);
    }
    return false;
  }

  public static IProjectConfiguration readResolverConfiguration(IProject project) {
    IEclipsePreferences projectNode = getMavenProjectPreferences(project);
    if(projectNode == null) {
      return new ResolverConfiguration(project);
    }
    String version = projectNode.get(P_VERSION, null);
    if(version == null) { // migrate from old config
      return new ResolverConfiguration(project);
    }
    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(
        projectNode.getBoolean(P_RESOLVE_WORKSPACE_PROJECTS, P_RESOLVE_WORKSPACE_PROJECTS_DEFAULT));
    configuration.setSelectedProfiles(projectNode.get(P_SELECTED_PROFILES, "")); //$NON-NLS-1$
    configuration.setLifecycleMappingId(projectNode.get(P_LIFECYCLE_MAPPING_ID, (String) null));
    configuration.setProperties(stringAsProperties(projectNode.get(P_PROPERTIES, null)));
    configuration.setMultiModuleProjectDirectory(getBasedir(projectNode, project));
    return configuration;
  }

  public static boolean isAutomaticallyUpdateConfiguration(IProject project) {
    IEclipsePreferences preferences = getMavenProjectPreferences(project);
    boolean defaultValue = MavenPreferenceInitializer.P_AUTO_UPDATE_CONFIGURATION_DEFAULT;
    return preferences != null ? preferences.getBoolean(P_AUTO_UPDATE_CONFIGURATION, defaultValue) : defaultValue;
  }

  public static void setAutomaticallyUpdateConfiguration(IProject project, boolean isAutomaticallyUpdateConfiguration) {
    IEclipsePreferences preferences = getMavenProjectPreferences(project);
    if(preferences != null) {
      preferences.putBoolean(P_AUTO_UPDATE_CONFIGURATION, isAutomaticallyUpdateConfiguration);
      savePreferences(preferences);
    }
  }

  public static boolean isResolveWorkspaceProjects(IProject project) {
    IEclipsePreferences preferences = getMavenProjectPreferences(project);
    if(preferences == null) {
      return P_RESOLVE_WORKSPACE_PROJECTS_DEFAULT;
    }
    return preferences.getBoolean(P_RESOLVE_WORKSPACE_PROJECTS, P_RESOLVE_WORKSPACE_PROJECTS_DEFAULT);
  }

  private static IEclipsePreferences getMavenProjectPreferences(IProject project) {
    return new ProjectScope(project).getNode(IMavenConstants.PLUGIN_ID);
  }

  private static boolean savePreferences(IEclipsePreferences node) {
    try {
      node.flush();
      return true;
    } catch(BackingStoreException ex) {
      log.error("Failed to save resolver configuration", ex);
    }
    return false;
  }

  private static File getBasedir(IEclipsePreferences projectNode, IProject project) {
    String basedirSetting = projectNode.get(P_BASEDIR, null);
    if(basedirSetting != null) {
      File directory = new File(basedirSetting);
      if(directory.isDirectory()) {
        return directory;
      }
    }
    return MavenProperties.computeMultiModuleProjectDirectory(project);
  }

  private static String propertiesAsString(Map<?, ?> properties) {
    return properties.entrySet().stream().map(e -> encodeEntry(e)).collect(Collectors.joining(PROPERTIES_SEPARATOR));
  }

  private static Properties stringAsProperties(String properties) {
    Properties p = new Properties();
    if(properties != null) {
      String[] entries = properties.split("\\" + PROPERTIES_SEPARATOR);
      Stream.of(entries).forEach(e -> convert(e, p));
    }
    return p;
  }

  private static void convert(String e, Properties p) {
    String[] kv = e.split(PROPERTIES_KV_SEPARATOR);
    String key = kv[0];
    String value = null;
    if(kv.length == 2) {
      value = kv[1];
    }
    p.put(urlDecode(key), urlDecode(value));
  }

  private static String encodeEntry(Entry<?, ?> e) {
    String key = e.getKey().toString();
    String value = e.getValue() == null ? "" : e.getValue().toString();
    return urlEncode(key) + PROPERTIES_KV_SEPARATOR + urlEncode(value);
  }

  private static String urlEncode(String string) {
    if(string == null) {
      return "";
    }
    try {
      return URLEncoder.encode(string, ENCODING);
    } catch(UnsupportedEncodingException notGonnaHappen) {
    }
    return string;
  }

  private static String urlDecode(String string) {
    if(string == null) {
      return "";
    }
    try {
      return URLDecoder.decode(string, ENCODING);
    } catch(UnsupportedEncodingException notGonnaHappen) {
    }
    return string;
  }
}
