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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.MavenPluginActivator;


/**
 * Registry of all workspace projects and their dependencies.
 * 
 * @author igor
 */
abstract class BasicProjectRegistry implements Serializable {

  private static final long serialVersionUID = 6232274446642339434L;

  private final String m2e_version = MavenPluginActivator.getQualifiedVersion();

  /**
   * Maps ArtifactKey to IFile of the POM file that defines this artifact.
   */
  protected final Map<ArtifactKey, Set<IFile>> workspaceArtifacts = new HashMap<>();

  /**
   * Maps IFile to MavenProjectFacade
   */
  protected final Map<IFile, MavenProjectFacade> workspacePoms = new HashMap<>();

  /**
   * Maps File to MavenProjectFacade
   */
  protected final Map<File, MavenProjectFacade> workspacePomFiles = new HashMap<>();

  /**
   * Maps required capabilities to projects that require them.
   */
  protected final Map<VersionlessKey, Map<RequiredCapability, Set<IFile>>> requiredCapabilities = new HashMap<>();

  /**
   * Maps project pom.xml file to the capabilities provided by the project
   */
  protected final Map<IFile, Set<Capability>> projectCapabilities = new HashMap<>();

  /**
   * Maps project pom.xml file to the capabilities required by the project
   */
  protected final Map<IFile, Set<RequiredCapability>> projectRequirements = new HashMap<>();

  protected BasicProjectRegistry() {
  }

  protected BasicProjectRegistry(BasicProjectRegistry other) {
    replaceWith(other);
  }

  protected final void replaceWith(BasicProjectRegistry other) {
    clear();

    copy(other.workspaceArtifacts, workspaceArtifacts);
    copy(other.workspacePoms, workspacePoms);
    copy(other.projectCapabilities, projectCapabilities);
    copy(other.projectRequirements, projectRequirements);
    copy(other.requiredCapabilities, requiredCapabilities);

    for(MavenProjectFacade facade : workspacePoms.values()) {
      workspacePomFiles.put(facade.getPomFile(), facade);
    }
  }

  /**
   * THIS IS NOT A GENERIC DEEP COPY IMPLEMENTATION!
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void copy(Map from, Map to) {
    for(Map.Entry entry : (Set<Map.Entry>) from.entrySet()) {
      Object value = entry.getValue();
      if(value instanceof Map) {
        Map map = new LinkedHashMap();
        copy((Map) value, map);
        value = map;
      } else if(value instanceof Set) {
        Set set = new LinkedHashSet((Set) value);
        value = set;
      }
      to.put(entry.getKey(), value);
    }
  }

  public MavenProjectFacade getProjectFacade(IFile pom) {
    return workspacePoms.get(pom);
  }

  public MavenProjectFacade getProjectFacade(File pom) {
    return workspacePomFiles.get(pom);
  }

  public MavenProjectFacade getProjectFacade(String groupId, String artifactId, String version) {
    Set<IFile> paths = workspaceArtifacts.get(new ArtifactKey(groupId, artifactId, version, null));
    if(paths == null || paths.isEmpty()) {
      return null;
    }
    return workspacePoms.get(paths.iterator().next());
  }

  /**
   * @TODO return a List
   */
  public MavenProjectFacade[] getProjects() {
    return workspacePoms.values().toArray(new MavenProjectFacade[workspacePoms.size()]);
  }

  public IFile getWorkspaceArtifact(ArtifactKey key) {
    Set<IFile> paths = workspaceArtifacts.get(key);
    return paths == null || paths.isEmpty() ? null : paths.iterator().next();
  }

  protected void clear() {
    workspaceArtifacts.clear();
    workspacePoms.clear();
    workspacePomFiles.clear();
    requiredCapabilities.clear();
    projectCapabilities.clear();
    projectRequirements.clear();
  }

  public boolean isValid() {
    return MavenPluginActivator.getQualifiedVersion().equals(m2e_version) //
        && workspaceArtifacts != null //
        && workspacePoms != null //
        && workspacePomFiles != null //
        && requiredCapabilities != null //
        && projectCapabilities != null //
        && projectRequirements != null //
        && areFacadesValid();
  }

  private boolean areFacadesValid() {
    for(MavenProjectFacade facade : workspacePoms.values()) {
      if(facade == null || facade.getPom() == null || facade.getPom().getLocation() == null) {
        return false;
      }
    }
    return true;
  }

  protected Set<RequiredCapability> getProjectRequirements(IFile pom) {
    return projectRequirements.get(pom);
  }
}
