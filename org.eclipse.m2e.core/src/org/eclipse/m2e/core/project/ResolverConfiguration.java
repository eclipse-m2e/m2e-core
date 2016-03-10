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

package org.eclipse.m2e.core.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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

  /**
   * @deprecated use {@link #getSelectedProfiles()} instead.
   */
  @Deprecated
  public String getActiveProfiles() {
    return getSelectedProfiles();
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

  /**
   * @deprecated use {@link #setSelectedProfiles(String)} instead.
   */
  @Deprecated
  public void setActiveProfiles(String activeProfiles) {
    setSelectedProfiles(activeProfiles);
  }

  public void setSelectedProfiles(String profiles) {
    this.selectedProfiles = profiles;
  }

  private static List<String> parseProfiles(String profilesAsText, boolean status) {
    List<String> profiles;
    if(profilesAsText != null && profilesAsText.trim().length() > 0) {
      String[] profilesArray = profilesAsText.split("[,\\s\\|]");
      profiles = new ArrayList<String>(profilesArray.length);
      for(String profile : profilesArray) {
        boolean isActive = !profile.startsWith("!");
        if(status == isActive) {
          profile = (isActive) ? profile : profile.substring(1);
          profiles.add(profile);
        }
      }
    } else {
      profiles = new ArrayList<String>(0);
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

}
