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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import org.apache.maven.repository.LocalArtifactRepository;

import org.eclipse.m2e.core.embedder.ArtifactKey;


public final class EclipseWorkspaceArtifactRepository extends LocalArtifactRepository implements WorkspaceReader {

  private final transient ProjectRegistryManager.Context context;

  private static final ThreadLocal<Boolean> disabled = new ThreadLocal<Boolean>();

  private WorkspaceRepository workspaceRepository;

  public EclipseWorkspaceArtifactRepository(ProjectRegistryManager.Context context) {
    this.context = context;
    this.workspaceRepository = new WorkspaceRepository("ide", getClass()); //$NON-NLS-1$
  }

  protected File resolveAsEclipseProject(String groupId, String artifactId, String baseVersion, String classifier,
      String extension) {
    if(isDisabled()) {
      return null;
    }

    if(context == null) { // XXX this is actually a bug 
      return null;
    }

    // check in the workspace, note that workspace artifacts never have classifiers
    ArtifactKey key = new ArtifactKey(groupId, artifactId, baseVersion, null);
    IFile pom = context.state.getWorkspaceArtifact(key);
    if(pom == null || !pom.isAccessible()) {
      return null;
    }
    if(context.pom != null && pom.equals(context.pom)) {
      return null;
    }

//    if(!"pom".equals(artifact.getType())) {
//      return false;
//    }

    if(context.resolverConfiguration.shouldResolveWorkspaceProjects()) {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IPath file = pom.getLocation();
      if(!"pom".equals(extension)) { //$NON-NLS-1$
        MavenProjectFacade facade = context.state.getProjectFacade(pom);
        if(facade.getOutputLocation() != null) {
          IFolder outputLocation = root.getFolder(facade.getOutputLocation());
          if(outputLocation.exists()) {
            file = outputLocation.getLocation();
          }
        }
      }

      return file.toFile();
    }

    return null;
  }

  public File findArtifact(Artifact artifact) {
    return resolveAsEclipseProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(),
        artifact.getClassifier(), artifact.getExtension());
  }

  public org.apache.maven.artifact.Artifact find(org.apache.maven.artifact.Artifact artifact) {
    File file = resolveAsEclipseProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(),
        artifact.getClassifier(), artifact.getType());

    if(file != null) {
      artifact.setFile(file);
      artifact.setResolved(true);
    }

    return artifact;
  }

  public boolean hasLocalMetadata() {
    return false; // XXX
  }

  public static void setDisabled(boolean disable) {
    disabled.set(disable ? Boolean.TRUE : null);
  }

  public static boolean isDisabled() {
    return Boolean.TRUE.equals(disabled.get());
  }

  public int hashCode() {
    return 0; // no state
  }

  public boolean equals(Object obj) {
    return obj instanceof EclipseWorkspaceArtifactRepository;
  }

  public List<String> findVersions(Artifact artifact) {
    return findVersions(artifact.getGroupId(), artifact.getArtifactId());
  }

  @Override
  public List<String> findVersions(org.apache.maven.artifact.Artifact artifact) {
    return findVersions(artifact.getGroupId(), artifact.getArtifactId());
  }

  private List<String> findVersions(String groupId, String artifactId) {
    ArrayList<String> versions = new ArrayList<String>();

    if(isDisabled()) {
      return versions;
    }

    if(context == null) { // XXX this is actually a bug 
      return versions;
    }

    for(MavenProjectFacade facade : context.state.getProjects()) {
      ArtifactKey artifactKey = facade.getArtifactKey();
      if(groupId.equals(artifactKey.getGroupId()) && artifactId.equals(artifactKey.getArtifactId())) {
        versions.add(artifactKey.getVersion());
      }
    }

    return versions;
  }

  public WorkspaceRepository getRepository() {
    return workspaceRepository;
  }

}
