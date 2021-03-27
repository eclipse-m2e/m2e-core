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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.m2e.core.internal.IMavenConstants;
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

  private static final String PROPERTIES_KV_SEPARATOR = ">";

  private static final String PROPERTIES_SEPARATOR = "|";

  private static final String ENCODING = "UTF-8";

  /**
   * Current configuration version value. See {@link #P_VERSION}
   */
  private static final String VERSION = "1"; //$NON-NLS-1$

  public static boolean saveResolverConfiguration(IProject project, ResolverConfiguration configuration) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(IMavenConstants.PLUGIN_ID);
    if(projectNode != null) {
      projectNode.put(P_VERSION, VERSION);

      projectNode.putBoolean(P_RESOLVE_WORKSPACE_PROJECTS, configuration.shouldResolveWorkspaceProjects());

      projectNode.put(P_SELECTED_PROFILES, configuration.getSelectedProfiles());

      if(configuration.getLifecycleMappingId() != null) {
        projectNode.put(P_LIFECYCLE_MAPPING_ID, configuration.getLifecycleMappingId());
      } else {
        projectNode.remove(P_LIFECYCLE_MAPPING_ID);
      }

      if(configuration.getProperties() != null && !configuration.getProperties().isEmpty()) {
        projectNode.put(P_PROPERTIES, propertiesAsString(configuration.getProperties()));
      } else {
        projectNode.remove(P_PROPERTIES);
      }

      try {
        projectNode.flush();
        return true;
      } catch(BackingStoreException ex) {
        log.error("Failed to save resolver configuration", ex);
      }
    }

    return false;
  }

  public static ResolverConfiguration readResolverConfiguration(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(IMavenConstants.PLUGIN_ID);
    if(projectNode == null) {
      return new ResolverConfiguration();
    }

    String version = projectNode.get(P_VERSION, null);
    if(version == null) { // migrate from old config
      // return LegacyBuildPathManager.getResolverConfiguration(project);
      return new ResolverConfiguration();
    }

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(projectNode.getBoolean(P_RESOLVE_WORKSPACE_PROJECTS, false));
    configuration.setSelectedProfiles(projectNode.get(P_SELECTED_PROFILES, "")); //$NON-NLS-1$
    configuration.setLifecycleMappingId(projectNode.get(P_LIFECYCLE_MAPPING_ID, (String) null));
    configuration.setProperties(stringAsProperties(projectNode.get(P_PROPERTIES, null)));
    return configuration;
  }

  private static String propertiesAsString(Properties properties) {
    Stream<Entry<Object, Object>> stream = properties.entrySet().stream();
    String propAsString = stream.map(e -> encodeEntry(e)).collect(Collectors.joining(PROPERTIES_SEPARATOR));
    return propAsString;
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

  private static String encodeEntry(Entry<Object, Object> e) {
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
