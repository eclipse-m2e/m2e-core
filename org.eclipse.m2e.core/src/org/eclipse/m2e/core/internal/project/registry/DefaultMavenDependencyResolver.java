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

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;

import org.sonatype.aether.graph.Dependency;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * DefaultMavenDependencyResolver
 * 
 * @author igor
 */
public class DefaultMavenDependencyResolver extends AbstractMavenDependencyResolver {
  private final IMavenMarkerManager markerManager;

  public DefaultMavenDependencyResolver(ProjectRegistryManager manager, IMavenMarkerManager markerManager) {
    setManager(manager);
    this.markerManager = markerManager;
  }

  public void resolveProjectDependencies(IMavenProjectFacade facade, MavenExecutionRequest mavenRequest,
      Set<Capability> capabilities, Set<RequiredCapability> requirements, IProgressMonitor monitor)
      throws CoreException {
    markerManager.deleteMarkers(facade.getPom(), IMavenConstants.MARKER_DEPENDENCY_ID);

    MavenExecutionResult mavenResult = getMaven().readProject(mavenRequest, monitor);

    markerManager.addMarkers(facade.getPom(), IMavenConstants.MARKER_DEPENDENCY_ID, mavenResult);

    if(!facade.getResolverConfiguration().shouldResolveWorkspaceProjects()) {
      return;
    }

    MavenProject mavenProject = facade.getMavenProject();

    // dependencies

    // parent
    Artifact parentArtifact = mavenProject.getParentArtifact();
    if(parentArtifact != null) {
      requirements.add(MavenRequiredCapability.createMavenParent(new ArtifactKey(parentArtifact)));
    }

    // resolved dependencies
    for(Artifact artifact : mavenProject.getArtifacts()) {
      requirements.add(MavenRequiredCapability.createMaven(new ArtifactKey(artifact), artifact.getScope(),
          artifact.isOptional()));
    }

    // extension plugins (affect packaging type calculation)
    for(Plugin plugin : mavenProject.getBuildPlugins()) {
      if(plugin.isExtensions()) {
        ArtifactKey artifactKey = new ArtifactKey(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(),
            null);
        requirements.add(MavenRequiredCapability.createMaven(artifactKey, "plugin", false)); //$NON-NLS-1$
      }
    }

    // missing dependencies
    DependencyResolutionResult resolutionResult = mavenResult.getDependencyResolutionResult();
    if(resolutionResult != null && resolutionResult.getUnresolvedDependencies() != null) {
      for(Dependency dependency : resolutionResult.getUnresolvedDependencies()) {
        org.sonatype.aether.artifact.Artifact artifact = dependency.getArtifact();
        ArtifactKey dependencyKey = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getVersion(), null);
        requirements.add(MavenRequiredCapability.createMaven(dependencyKey, dependency.getScope(),
            dependency.isOptional()));
      }
    }

  }
}
