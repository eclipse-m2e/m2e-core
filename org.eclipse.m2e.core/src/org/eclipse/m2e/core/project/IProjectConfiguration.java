/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * {@link IProjectConfiguration} represents the project specific configuration, many projects can share the same
 * configuration and are usually resolved together.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IProjectConfiguration {

  /**
   * Additional properties of a project can be stored in the org.eclipse.m2e.core.prefs file under the key
   * <code>properties</code>, will be handled as user-properties
   * 
   * @return return project configuration properties
   */
  Map<String, String> getConfigurationProperties();

  /**
   * User properties are stored in the <code>.mvn/maven.config</code> file with <code>-Dkey=value</code>, they are
   * therefore not persisted in the project configuration
   * 
   * @return the user properties
   */
  Map<String, String> getUserProperties();

  boolean isResolveWorkspaceProjects();

  String getSelectedProfiles();

  default List<String> getActiveProfileList() {
    return parseProfiles(getSelectedProfiles(), true);
  }

  default List<String> getInactiveProfileList() {
    return parseProfiles(getSelectedProfiles(), false);
  }

  String getLifecycleMappingId();

  File getMultiModuleProjectDirectory();

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
