/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * WorkspaceStateDelta
 *
 * @author igor
 */
public class MutableProjectRegistry extends BasicProjectRegistry implements IProjectRegistry, AutoCloseable {

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
      Set<IFile> paths = workspaceArtifacts.computeIfAbsent(facade.getArtifactKey(), k -> new LinkedHashSet<>());
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

  public Set<IFile> removeWorkspaceModules(ArtifactKey mavenProject) {
    assertNotClosed();

    return getDependents(MavenCapability.createMavenParent(mavenProject), true);
  }

  public boolean isStale() {
    return parentVersion != parent.getVersion();
  }

  @Override
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
  public List<MavenProjectFacade> getProjects() {
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
    rs.entrySet().removeIf(entry -> {
      if(entry.getKey().isPotentialMatch(capability, versionMatch)) {
        result.addAll(entry.getValue());
        return remove;
      }
      return false;
    });
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

  public Set<Capability> setCapabilities(IFile pom, Set<Capability> capabilities) {
    return capabilities != null ? projectCapabilities.put(pom, capabilities) : projectCapabilities.remove(pom);
  }

  public Set<RequiredCapability> setRequirements(IFile pom, Set<RequiredCapability> requirements) {
    removeRequiredCapabilities(pom);
    if(requirements != null) {
      for(RequiredCapability requirement : requirements) {
        var poms = requiredCapabilities.computeIfAbsent(requirement.getVersionlessKey(), k -> new HashMap<>());
        poms.computeIfAbsent(requirement, r -> new HashSet<>()).add(pom);
      }
      return projectRequirements.put(pom, requirements);
    }
    return projectRequirements.remove(pom);
  }

  private void removeRequiredCapabilities(IFile pom) {
    for(RequiredCapability requiredCapability : projectRequirements.getOrDefault(pom, Set.of())) {
      requiredCapabilities.computeIfPresent(requiredCapability.getVersionlessKey(), (k, rc2pom) -> {
        rc2pom.computeIfPresent(requiredCapability, (rc, requiringPoms) -> {
          requiringPoms.remove(pom);
          return !requiringPoms.isEmpty() ? requiringPoms : null; // remove if was last project that required this capability 
        });
        return !rc2pom.isEmpty() ? rc2pom : null; // remove if was last project that required this capability version-less key
      });
    }
  }
}
