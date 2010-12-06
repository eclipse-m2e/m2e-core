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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * Registry of all workspace projects and their dependencies.
 * 
 * @author igor
 */
abstract class BasicProjectRegistry implements Serializable {

  private static final long serialVersionUID = 5542512601401896748L;

  private final String m2e_version = MavenPlugin.getQualifiedVersion();

  /**
   * Map<ArtifactKey, IPath> Maps ArtifactKey to full workspace IPath of the POM file that defines this artifact.
   */
  protected final Map<ArtifactKey, IFile> workspaceArtifacts = new HashMap<ArtifactKey, IFile>();

  /**
   * Maps full pom IPath to MavenProjectFacade
   */
  protected final Map<IFile, MavenProjectFacade> workspacePoms = new HashMap<IFile, MavenProjectFacade>();

  /**
   * Maps required capabilities to projects that require them.
   */
  protected final Map<VersionlessKey, Map<RequiredCapability, Set<IFile>>> requiredCapabilities = new HashMap<VersionlessKey, Map<RequiredCapability, Set<IFile>>>();

  /**
   * Maps project pom.xml file to the capabilities provided by the project
   */
  protected final Map<IFile, Set<Capability>> projectCapabilities = new HashMap<IFile, Set<Capability>>();

  /**
   * Maps project pom.xml file to the capabilities required by the project
   */
  protected final Map<IFile, Set<RequiredCapability>> projectRequirements = new HashMap<IFile, Set<RequiredCapability>>();

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
  }

  /**
   * THIS IS NOT A GENERIC DEEP COPY IMPLEMENTATION!
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void copy(Map from, Map to) {
    for(Map.Entry entry : (Set<Map.Entry>) from.entrySet()) {
      Object value = entry.getValue();
      if(value instanceof Map) {
        Map map = new HashMap();
        copy((Map) value, map);
        value = map;
      } else if(value instanceof Set) {
        Set set = new HashSet((Set) value);
        value = set;
      }
      to.put(entry.getKey(), value);
    }
  }

  public MavenProjectFacade getProjectFacade(IFile pom) {
    return workspacePoms.get(pom);
  }

  public MavenProjectFacade getProjectFacade(String groupId, String artifactId, String version) {
    IFile path = workspaceArtifacts.get(new ArtifactKey(groupId, artifactId, version, null));
    if(path == null) {
      return null;
    }
    return workspacePoms.get(path);
  }

  /**
   * @TODO return a List
   */
  public MavenProjectFacade[] getProjects() {
    return workspacePoms.values().toArray(new MavenProjectFacade[workspacePoms.size()]);
  }

  public IFile getWorkspaceArtifact(ArtifactKey key) {
    return workspaceArtifacts.get(key);
  }

  protected void clear() {
    workspaceArtifacts.clear();
    workspacePoms.clear();
    requiredCapabilities.clear();
    projectCapabilities.clear();
    projectRequirements.clear();
  }

  public boolean isValid() {
    return MavenPlugin.getQualifiedVersion().equals(m2e_version) //
        && workspaceArtifacts != null //
        && workspacePoms != null //
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
