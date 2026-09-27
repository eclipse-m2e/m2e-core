/*******************************************************************************
 * Copyright (c) 2026 Bas Gooren and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.apt.internal.compiler;

import java.io.File;
import java.util.Collection;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;

import org.eclipse.m2e.apt.internal.IMavenAptConstants;
import org.eclipse.m2e.apt.internal.Messages;
import org.eclipse.m2e.apt.internal.utils.PluginDependencyResolver;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Resolves annotation-processor artifacts from the local Maven repository when Aether substituted an open workspace
 * project. JDT APT only accepts JARs on its factory path, while regular Maven dependencies should remain
 * workspace-resolved.
 */
@SuppressWarnings("restriction")
final class WorkspaceAnnotationProcessorResolver extends PluginDependencyResolver {

  private final Collection<Dependency> dependencies;

  private final IProject consumerProject;

  private final IMavenMarkerManager markerManager;

  WorkspaceAnnotationProcessorResolver(Collection<Dependency> dependencies, IProject consumerProject,
      IMavenMarkerManager markerManager) {
    this.dependencies = dependencies;
    this.consumerProject = consumerProject;
    this.markerManager = markerManager;
  }

  @Override
  protected Collection<Dependency> getDependencies(Plugin plugin) {
    return dependencies;
  }

  @Override
  protected File getArtifactFile(ArtifactResult artifactResult, RepositorySystemSession session) {
    Artifact artifact = artifactResult.getArtifact();
    File resolvedFile = artifact.getFile();
    WorkspaceReader workspaceReader = session.getWorkspaceReader();
    if(workspaceReader == null || !"jar".equals(artifact.getExtension())) { //$NON-NLS-1$
      return resolvedFile;
    }

    File workspaceFile = workspaceReader.findArtifact(artifact);
    if(workspaceFile == null || !sameLocation(workspaceFile, resolvedFile)) {
      return resolvedFile;
    }

    IMavenProjectFacade workspaceFacade = MavenPlugin.getMavenProjectRegistry().getMavenProject(artifact.getGroupId(),
        artifact.getArtifactId(), artifact.getBaseVersion());
    if(workspaceFacade == null || !workspaceFacade.getProject().isOpen()) {
      return resolvedFile;
    }

    LocalRepositoryManager localRepositoryManager = session.getLocalRepositoryManager();
    File localJar = new File(localRepositoryManager.getRepository().getBasedir(),
        localRepositoryManager.getPathForLocalArtifact(artifact));
    String artifactCoordinates = getArtifactCoordinates(artifact);
    String workspaceProjectName = workspaceFacade.getProject().getName();

    if(!localJar.isFile()) {
      markerManager.addMarker(consumerProject, IMavenAptConstants.WORKSPACE_PROCESSOR_MARKER_ID,
          NLS.bind(Messages.WorkspaceProcessor_missing_jar,
              new Object[] {artifactCoordinates, workspaceProjectName}),
          1, IMarker.SEVERITY_WARNING);
      return resolvedFile;
    }

    if(hasNewerJavaSource(workspaceFacade, localJar.lastModified())) {
      markerManager.addMarker(consumerProject, IMavenAptConstants.WORKSPACE_PROCESSOR_MARKER_ID,
          NLS.bind(Messages.WorkspaceProcessor_stale_jar,
              new Object[] {artifactCoordinates, workspaceProjectName}),
          1, IMarker.SEVERITY_WARNING);
    }
    return localJar;
  }

  private static boolean sameLocation(File first, File second) {
    return first != null && second != null
        && first.toPath().toAbsolutePath().normalize().equals(second.toPath().toAbsolutePath().normalize());
  }

  private static boolean hasNewerJavaSource(IMavenProjectFacade projectFacade, long jarTimestamp) {
    IProject project = projectFacade.getProject();
    for(IPath sourceLocation : projectFacade.getCompileSourceLocations()) {
      IContainer sourceRoot = sourceLocation.isEmpty() ? project : project.getFolder(sourceLocation);
      if(hasNewerJavaSource(sourceRoot, jarTimestamp)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasNewerJavaSource(IContainer sourceRoot, long jarTimestamp) {
    if(!sourceRoot.isAccessible()) {
      return false;
    }
    boolean[] newerSourceFound = new boolean[1];
    try {
      sourceRoot.accept(resource -> {
        if(resource.getType() == IResource.FILE && "java".equalsIgnoreCase(resource.getFileExtension()) //$NON-NLS-1$
            && resource.getLocalTimeStamp() > jarTimestamp) {
          newerSourceFound[0] = true;
          return false;
        }
        return !newerSourceFound[0];
      });
    } catch(CoreException ex) {
      return false;
    }
    return newerSourceFound[0];
  }

  private static String getArtifactCoordinates(Artifact artifact) {
    StringBuilder coordinates = new StringBuilder().append(artifact.getGroupId()).append(':') //
        .append(artifact.getArtifactId()).append(':').append(artifact.getBaseVersion());
    String classifier = artifact.getClassifier();
    if(classifier != null && !classifier.isEmpty()) {
      coordinates.append(':').append(classifier);
    }
    return coordinates.toString();
  }
}
