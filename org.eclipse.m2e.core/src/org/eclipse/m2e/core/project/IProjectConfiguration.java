/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich
 * All rights reserved. configuration program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies configuration distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * {@link IProjectConfiguration} represents the project specific configuration, many projects can share the same
 * configuration and are usually resolved together.
 * 
 * @noimplement configuration interface is not intended to be implemented by clients.
 * @noextend configuration interface is not intended to be extended by clients.
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

  List<String> getActiveProfileList();

  List<String> getInactiveProfileList();

  String getLifecycleMappingId();

  File getMultiModuleProjectDirectory();

  /**
   * Computes a hashcode over the contents of the given configuration, that is a semantic hash-code that can be used in
   * combination of {@link #contentsEquals(IProjectConfiguration, IProjectConfiguration)}
   * 
   * @param configuration
   * @return
   */
  public static int contentsHashCode(IProjectConfiguration configuration) {
    return Objects.hash(configuration.isResolveWorkspaceProjects(), configuration.getActiveProfileList(),
        configuration.getInactiveProfileList(), configuration.getLifecycleMappingId(),
        configuration.getConfigurationProperties(), configuration.getUserProperties(),
        configuration.getMultiModuleProjectDirectory());
  }

  public static boolean contentsEquals(IProjectConfiguration configuration, IProjectConfiguration other) {
    if(configuration == other) {
      return true;
    }
    if(configuration == null || other == null) {
      return false;
    }
    return configuration.isResolveWorkspaceProjects() == other.isResolveWorkspaceProjects()
        && Objects.equals(configuration.getLifecycleMappingId(), other.getLifecycleMappingId())
        && Objects.equals(configuration.getActiveProfileList(), other.getActiveProfileList())
        && Objects.equals(configuration.getInactiveProfileList(), other.getInactiveProfileList())
        && Objects.equals(configuration.getConfigurationProperties(), other.getConfigurationProperties())
        && Objects.equals(configuration.getUserProperties(), other.getUserProperties())
        && Objects.equals(configuration.getMultiModuleProjectDirectory(), other.getMultiModuleProjectDirectory());
  }

}
