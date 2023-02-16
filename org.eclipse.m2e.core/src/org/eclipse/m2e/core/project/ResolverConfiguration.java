/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Mickael Istria (Red Hat Inc.) - equals/hashCode
 *******************************************************************************/

package org.eclipse.m2e.core.project;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.embedder.MavenProperties;


/**
 * Resolver configuration holder. TODO need a better name, this configures all aspects of maven project in eclipse, not
 * just dependency resolution.
 *
 * @author Eugene Kuleshov
 */
public class ResolverConfiguration implements Serializable, IProjectConfiguration {
  private static final long serialVersionUID = 1258510761534886581L;

  private boolean resolveWorkspaceProjects = true;

  private String selectedProfiles = ""; //$NON-NLS-1$

  private String lifecycleMappingId;

  private Properties properties;

  private Map<String, String> userProperties;

  private File multiModuleProjectDirectory;

  private List<String> activeProfiles;

  private List<String> inactiveProfiles;

  private List<String> userActiveProfiles;

  private List<String> userInactiveProfiles;

  public ResolverConfiguration() {
  }

  public ResolverConfiguration(IProject project) {
    setMultiModuleProjectDirectory(MavenProperties.computeMultiModuleProjectDirectory(project));
  }

  /**
   * @param resolverConfiguration
   */
  public ResolverConfiguration(IProjectConfiguration resolverConfiguration) {
    setLifecycleMappingId(resolverConfiguration.getLifecycleMappingId());
    setMultiModuleProjectDirectory(resolverConfiguration.getMultiModuleProjectDirectory());
    Properties properties2 = new Properties();
    properties2.putAll(resolverConfiguration.getConfigurationProperties());
    setProperties(properties2);
    setResolveWorkspaceProjects(resolverConfiguration.isResolveWorkspaceProjects());
    setSelectedProfiles(resolverConfiguration.getSelectedProfiles());
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.IProjectConfiguration#getProperties()
   */
  public Properties getProperties() {
    return this.properties;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.IProjectConfiguration#getConfigurationProperties()
   */
  @Override
  public Map<String, String> getConfigurationProperties() {
    if(properties == null) {
      return Collections.emptyMap();
    }
    Set<String> names = properties.stringPropertyNames();
    Map<String, String> map = new HashMap<>();
    for(String key : names) {
      map.put(key, properties.getProperty(key));
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public Map<String, String> getUserProperties() {
    if(userProperties == null) {
      return Collections.emptyMap();
    }
    return userProperties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  /**
   * @deprecated use {@link #isResolveWorkspaceProjects()}
   */
  @Deprecated(forRemoval = true)
  public boolean shouldResolveWorkspaceProjects() {
    return isResolveWorkspaceProjects();
  }

  @Override
  public boolean isResolveWorkspaceProjects() {
    return this.resolveWorkspaceProjects;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.IProjectConfiguration#getSelectedProfiles()
   */
  @Override
  public String getSelectedProfiles() {
    return this.selectedProfiles;
  }

  public void setResolveWorkspaceProjects(boolean resolveWorkspaceProjects) {
    this.resolveWorkspaceProjects = resolveWorkspaceProjects;
  }

  public void setSelectedProfiles(String profiles) {
    this.selectedProfiles = profiles;
    this.activeProfiles = parseProfiles(profiles, true);
    this.inactiveProfiles = parseProfiles(profiles, false);
  }


  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.IProjectConfiguration#getLifecycleMappingId()
   */
  @Override
  public String getLifecycleMappingId() {
    return lifecycleMappingId;
  }

  /**
   * Explicitly set project lifecycle mapping id. Non-null value takes precedence over id derived from lifecycle mapping
   * metadata source, including project pom.xml and workspace preferences.
   *
   * @since 1.3
   */
  public void setLifecycleMappingId(String lifecycleMappingId) {
    this.lifecycleMappingId = lifecycleMappingId;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj instanceof IProjectConfiguration other) {
      return IProjectConfiguration.contentsEquals(this, other);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return IProjectConfiguration.contentsHashCode(this);
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.IProjectConfiguration#getMultiModuleProjectDirectory()
   */
  @Override
  public File getMultiModuleProjectDirectory() {
    return multiModuleProjectDirectory;
  }

  /**
   * @param multiModuleProjectDirectory The multiModuleProjectDirectory to set.
   */
  public void setMultiModuleProjectDirectory(File multiModuleProjectDirectory) {
    if(!Objects.equals(this.multiModuleProjectDirectory, multiModuleProjectDirectory)) {
      this.multiModuleProjectDirectory = multiModuleProjectDirectory;
      try {
        CommandLine commandLine = MavenProperties.getMavenArgs(multiModuleProjectDirectory);
        Map<String, String> props = new LinkedHashMap<>();
        MavenProperties.getCliProperties(commandLine, props::put);
        userProperties = Collections.unmodifiableMap(props);
        userActiveProfiles = new ArrayList<>();
        userInactiveProfiles = new ArrayList<>();
        MavenProperties.getProfiles(commandLine, userActiveProfiles::add, userInactiveProfiles::add);
      } catch(IOException | ParseException ex) {
        //can't use it then... :-(
        MavenPluginActivator.getDefault().getLog().error("can't read maven args from " + multiModuleProjectDirectory,
            ex);
      }
    }
  }

  @Override
  public List<String> getActiveProfileList() {
    if(activeProfiles == null) {
      if(userActiveProfiles != null) {
        return List.copyOf(userActiveProfiles);
      }
      return List.of();
    }
    if(userActiveProfiles != null) {
      return Stream.concat(userActiveProfiles.stream(), activeProfiles.stream()).distinct().toList();
    }
    return List.copyOf(activeProfiles);
  }

  @Override
  public List<String> getInactiveProfileList() {
    if(inactiveProfiles == null) {
      if(userInactiveProfiles != null) {
        return List.copyOf(userInactiveProfiles);
      }
      return List.of();
    }
    if(userInactiveProfiles != null) {
      return Stream.concat(userInactiveProfiles.stream(), inactiveProfiles.stream()).distinct().toList();
    }
    return List.copyOf(inactiveProfiles);
  }

  private static List<String> parseProfiles(String profilesAsText, boolean status) {
    List<String> profiles;
    if(profilesAsText != null && profilesAsText.trim().length() > 0) {
      String[] profilesArray = profilesAsText.split("[,\\s\\|]");
      profiles = new ArrayList<>(profilesArray.length);
      for(String profile : profilesArray) {
        boolean isActive = !profile.startsWith("!");
        if(status == isActive) {
          profile = (isActive) ? profile : profile.substring(1);
          profiles.add(profile);
        }
      }
    } else {
      profiles = new ArrayList<>(0);
    }
    return profiles;
  }
}
