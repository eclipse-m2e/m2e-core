/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * WorkspaceStateDelta
 *
 * @author igor
 */
public class MutableProjectRegistry extends BasicProjectRegistry implements IProjectRegistry {

  private static final long serialVersionUID = 4879169945594340946L;

  private final ProjectRegistry parent;

  private final int parentVersion;

  private boolean closed;

  public MutableProjectRegistry(ProjectRegistry state) {
    super(state);
    this.parent = state;
    this.parentVersion = state.getVersion();
  }

  private void assertNotClosed() {
    if(closed) {
      throw new IllegalStateException("Can't modify closed MutableProjectRegistry"); //$NON-NLS-1$
    }
  }

  public void setProject(IFile pom, MavenProjectFacade facade) {
    assertNotClosed();

    // remove
    MavenProjectFacade oldFacade = workspacePoms.remove(pom);
    if(oldFacade != null) {
      Set<IFile> paths = workspaceArtifacts.get(oldFacade.getArtifactKey());
      if(paths != null) {
        paths.remove(pom);
      }
    }
    if(facade != null) {
      // Add the project to workspaceProjects map
      workspacePoms.put(pom, facade);

      // Add the project to workspaceArtifacts map
      Set<IFile> paths = workspaceArtifacts.get(facade.getArtifactKey());
      if(paths == null) {
        paths = new LinkedHashSet<>();
        workspaceArtifacts.put(facade.getArtifactKey(), paths);
      }
      paths.add(pom);
    }
  }

  public void removeProject(IFile pom, ArtifactKey mavenProject) {
    assertNotClosed();

    // remove project from requiredCapabilities map
    removeRequiredCapabilities(pom);

    // Remove the project from workspaceProjects, projectRequirements and projectCapabilities maps
    MavenProjectFacade facade = workspacePoms.remove(pom);
    projectRequirements.remove(pom);
    projectCapabilities.remove(pom);

    // Remove the project from workspaceArtifacts map
    if(mavenProject != null) {
      Set<IFile> paths = workspaceArtifacts.get(mavenProject);
      if(paths != null) {
        paths.remove(pom);
      }
    }

    if(facade != null) {
      workspacePomFiles.remove(facade.getPomFile());
    }
  }

  static boolean isSameProject(IResource r1, IResource r2) {
    if(r1 == null || r2 == null) {
      return false;
    }
    return r1.getProject().equals(r2.getProject());
  }

  public Set<IFile> removeWorkspaceModules(IFile pom, ArtifactKey mavenProject) {
    assertNotClosed();

    return getDependents(MavenCapability.createMavenParent(mavenProject), true);
  }

  public boolean isStale() {
    return parentVersion != parent.getVersion();
  }

  public void close() {
    this.closed = true;

    clear();
  }

  private boolean isClosed() {
    return closed;
  }

  // IProjectRegistry

  @Override
  public MavenProjectFacade getProjectFacade(IFile pom) {
    if(isClosed()) {
      return parent.getProjectFacade(pom);
    }
    return super.getProjectFacade(pom);
  }

  @Override
  public MavenProjectFacade getProjectFacade(File pom) {
    if(isClosed()) {
      return parent.getProjectFacade(pom);
    }
    return super.getProjectFacade(pom);
  }

  @Override
  public MavenProjectFacade getProjectFacade(String groupId, String artifactId, String version) {
    if(isClosed()) {
      return parent.getProjectFacade(groupId, artifactId, version);
    }
    return super.getProjectFacade(groupId, artifactId, version);
  }

  @Override
  public MavenProjectFacade[] getProjects() {
    if(isClosed()) {
      return parent.getProjects();
    }
    return super.getProjects();
  }

  @Override
  public Map<ArtifactKey, Collection<IFile>> getWorkspaceArtifacts(String groupId, String artifactId) {
    if(isClosed()) {
      return parent.getWorkspaceArtifacts(groupId, artifactId);
    }
    return super.getWorkspaceArtifacts(groupId, artifactId);
  }

  // low level access and manipulation

  /**
   * Returns all workspace projects that require given Capability.
   */
  public Set<IFile> getDependents(Capability capability, boolean remove) {
    return getDependents(capability, false, remove);
  }

  /**
   * Returns all workspace projects that require given Capability of a certain version, if available
   */
  public Set<IFile> getVersionedDependents(Capability capability, boolean remove) {
    return getDependents(capability, true, remove);
  }

  private Set<IFile> getDependents(Capability capability, boolean versionMatch, boolean remove) {
    Map<RequiredCapability, Set<IFile>> rs = requiredCapabilities.get(capability.getVersionlessKey());
    if(rs == null) {
      return Collections.emptySet();
    }
    Set<IFile> result = new LinkedHashSet<>();
    Iterator<Entry<RequiredCapability, Set<IFile>>> iter = rs.entrySet().iterator();
    while(iter.hasNext()) {
      Entry<RequiredCapability, Set<IFile>> entry = iter.next();
      if(entry.getKey().isPotentialMatch(capability, versionMatch)) {
        result.addAll(entry.getValue());
        if(remove) {
          iter.remove();
        }
      }
    }
    if(remove && rs.isEmpty()) {
      requiredCapabilities.remove(capability.getVersionlessKey());
    }
    return result;
  }

  /**
   * Returns all workspace projects that require given versionless Capability.
   */
  public Set<IFile> getDependents(VersionlessKey capability, boolean remove) {
    Map<RequiredCapability, Set<IFile>> rs;
    if(remove) {
      rs = requiredCapabilities.remove(capability);
    } else {
      rs = requiredCapabilities.get(capability);
    }
    if(rs == null) {
      return Collections.emptySet();
    }
    Set<IFile> result = new LinkedHashSet<>();
    for(Set<IFile> dependents : rs.values()) {
      result.addAll(dependents);
    }
    return result;
  }

  private void addRequiredCapability(IFile pom, RequiredCapability req) {
    Map<RequiredCapability, Set<IFile>> keyEntry = requiredCapabilities.get(req.getVersionlessKey());
    if(keyEntry == null) {
      keyEntry = new HashMap<>();
      requiredCapabilities.put(req.getVersionlessKey(), keyEntry);
    }
    Set<IFile> poms = keyEntry.get(req);
    if(poms == null) {
      poms = new HashSet<>();
      keyEntry.put(req, poms);
    }
    poms.add(pom);
  }

  public Set<Capability> setCapabilities(IFile pom, Set<Capability> capabilities) {
    return capabilities != null ? projectCapabilities.put(pom, capabilities) : projectCapabilities.remove(pom);
  }

  public Set<RequiredCapability> setRequirements(IFile pom, Set<RequiredCapability> requirements) {
    removeRequiredCapabilities(pom);
    if(requirements != null) {
      for(RequiredCapability requirement : requirements) {
        addRequiredCapability(pom, requirement);
      }
      return projectRequirements.put(pom, requirements);
    }
    return projectRequirements.remove(pom);
  }

  private void removeRequiredCapabilities(IFile pom) {
    // TODO likely too slow
    Iterator<Entry<VersionlessKey, Map<RequiredCapability, Set<IFile>>>> keysIter = requiredCapabilities.entrySet()
        .iterator();
    while(keysIter.hasNext()) {
      Entry<VersionlessKey, Map<RequiredCapability, Set<IFile>>> keysEntry = keysIter.next();
      Iterator<Entry<RequiredCapability, Set<IFile>>> requirementsIter = keysEntry.getValue().entrySet().iterator();
      while(requirementsIter.hasNext()) {
        Entry<RequiredCapability, Set<IFile>> requirementsEntry = requirementsIter.next();
        requirementsEntry.getValue().remove(pom);
        if(requirementsEntry.getValue().isEmpty()) {
          // was last project that required this capability
          requirementsIter.remove();
        }
      }
      if(keysEntry.getValue().isEmpty()) {
        // was last project that required this capability versionless key
        keysIter.remove();
      }
    }
  }

}
