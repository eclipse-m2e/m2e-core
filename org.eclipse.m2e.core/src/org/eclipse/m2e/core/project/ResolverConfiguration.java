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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;


/**
 * Resolver configuration holder. TODO need a better name, this configures all aspects of maven project in eclipse, not
 * just dependency resolution.
 *
 * @author Eugene Kuleshov
 */
public class ResolverConfiguration implements Serializable {
  private static final long serialVersionUID = 1258510761534886581L;

  private boolean resolveWorkspaceProjects = true;

  private String selectedProfiles = ""; //$NON-NLS-1$

  private String lifecycleMappingId;

  private Properties properties;

  public Properties getProperties() {
    return this.properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public boolean shouldResolveWorkspaceProjects() {
    return this.resolveWorkspaceProjects;
  }

  public String getSelectedProfiles() {
    return this.selectedProfiles;
  }

  public List<String> getActiveProfileList() {
    return parseProfiles(selectedProfiles, true);
  }

  public List<String> getInactiveProfileList() {
    return parseProfiles(selectedProfiles, false);
  }

  public void setResolveWorkspaceProjects(boolean resolveWorkspaceProjects) {
    this.resolveWorkspaceProjects = resolveWorkspaceProjects;
  }

  public void setSelectedProfiles(String profiles) {
    this.selectedProfiles = profiles;
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

  /**
   * @since 1.3
   */
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
    if(obj == null) {
      return false;
    }
    if(obj.getClass() != this.getClass()) {
      return false;
    }
    ResolverConfiguration other = (ResolverConfiguration) obj;
    return this.resolveWorkspaceProjects == other.resolveWorkspaceProjects
        && Objects.equals(this.selectedProfiles, other.selectedProfiles)
        && Objects.equals(this.lifecycleMappingId, other.lifecycleMappingId)
        && Objects.equals(this.properties, other.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resolveWorkspaceProjects, selectedProfiles, lifecycleMappingId, properties);
  }
}
