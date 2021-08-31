/*******************************************************************************
 * Copyright (c) 2011-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.sourcelookup.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.advanced.ISourceContainerResolver;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


public class MavenSourceContainerResolver implements ISourceContainerResolver {

  private static final MavenArtifactIdentifier INDENTIFIER = new MavenArtifactIdentifier();

  @Override
  public Collection<ISourceContainer> resolveSourceContainers(File classesLocation, IProgressMonitor monitor) {
    Collection<ArtifactKey> classesArtifacts = INDENTIFIER.identify(classesLocation);

    if (classesArtifacts == null) {
      return null;
    }

    List<ISourceContainer> result = new ArrayList<>();
    for (ArtifactKey classesArtifact : classesArtifacts) {
      ISourceContainer container = resolveSourceContainer(classesArtifact, monitor);
      if (container != null) {
        result.add(container);
      }
    }
    return result;
  }

  protected ISourceContainer resolveSourceContainer(ArtifactKey artifact, IProgressMonitor monitor) {
    String groupId = artifact.getGroupId();
    String artifactId = artifact.getArtifactId();
    String version = artifact.getVersion();

    IMaven maven = MavenPlugin.getMaven();
    IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();

    IMavenProjectFacade mavenProject = projectRegistry.getMavenProject(groupId, artifactId, version);
    if (mavenProject != null) {
      return new JavaProjectSourceContainer(JavaCore.create(mavenProject.getProject()));
    }

    try {
      List<ArtifactRepository> repositories = new ArrayList<>();
      repositories.addAll(maven.getArtifactRepositories());
      repositories.addAll(maven.getPluginArtifactRepositories());

      if (!maven.isUnavailable(groupId, artifactId, version, "jar", "sources", repositories)) {
        Artifact resolve = maven.resolve(groupId, artifactId, version, "jar", "sources", null, monitor);

        return new ExternalArchiveSourceContainer(resolve.getFile().getAbsolutePath(), true);
      }
    } catch (CoreException e) {
      // TODO maybe log, ignore otherwise
    }

    return null;
  }
}
