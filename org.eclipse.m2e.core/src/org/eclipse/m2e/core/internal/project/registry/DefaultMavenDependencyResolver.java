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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * DefaultMavenDependencyResolver
 * 
 * @author igor
 */
public class DefaultMavenDependencyResolver extends AbstractMavenDependencyResolver {
  private static final Logger log = LoggerFactory.getLogger(DefaultMavenDependencyResolver.class);

  private final IMavenMarkerManager markerManager;

  public DefaultMavenDependencyResolver(ProjectRegistryManager manager, IMavenMarkerManager markerManager) {
    setManager(manager);
    this.markerManager = markerManager;
  }

  @Override
  public void resolveProjectDependencies(final IMavenProjectFacade facade, Set<Capability> capabilities,
      Set<RequiredCapability> requirements, final IProgressMonitor monitor) throws CoreException {
    long start = System.currentTimeMillis();
    log.debug("Resolving dependencies for {}", facade.toString()); //$NON-NLS-1$

    markerManager.deleteMarkers(facade.getPom(), IMavenConstants.MARKER_DEPENDENCY_ID);

    ProjectBuildingRequest configuration = getMaven().getExecutionContext().newProjectBuildingRequest();
    configuration.setProject(facade.getMavenProject()); // TODO do we need this?
    configuration.setResolveDependencies(true);
    MavenExecutionResult mavenResult = getMaven().readMavenProject(facade.getPomFile(), configuration);

    markerManager.addMarkers(facade.getPom(), IMavenConstants.MARKER_DEPENDENCY_ID, mavenResult);

    if(!facade.getResolverConfiguration().shouldResolveWorkspaceProjects()) {
      return;
    }

    MavenProject mavenProject = facade.getMavenProject();

    // dependencies

    // missing dependencies
    // should be added before dependencies from MavenProject#getArtifacts() since those
    // will be added with resolved flag set to true
    DependencyResolutionResult resolutionResult = mavenResult.getDependencyResolutionResult();
    if(resolutionResult != null && resolutionResult.getUnresolvedDependencies() != null) {
      for(Dependency dependency : resolutionResult.getUnresolvedDependencies()) {
        org.eclipse.aether.artifact.Artifact artifact = dependency.getArtifact();
        ArtifactKey dependencyKey = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getVersion(), null);
        MavenRequiredCapability req = MavenRequiredCapability.createMavenArtifact(dependencyKey, dependency.getScope(),
            dependency.isOptional());
        requirements.add(req);
      }
    }

    // resolved dependencies
    for(Artifact artifact : mavenProject.getArtifacts()) {
      requirements.add(MavenRequiredCapability.createResolvedMavenArtifact(new ArtifactKey(artifact),
          artifact.getScope(), artifact.isOptional()));
    }

    // extension plugins (affect packaging type calculation)
    for(Plugin plugin : mavenProject.getBuildPlugins()) {
      if(plugin.isExtensions()) {
        ArtifactKey artifactKey = new ArtifactKey(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(),
            null);
        requirements.add(MavenRequiredCapability.createMavenArtifact(artifactKey, "plugin", false)); //$NON-NLS-1$
      }
    }

    log.debug("Resolved dependencies for {} in {} ms", facade.toString(), System.currentTimeMillis() - start); //$NON-NLS-1$
  }

  public static void addParentRequirements(Set<RequiredCapability> requirements, MavenProject mavenProject) {
    Artifact parentArtifact = mavenProject.getParentArtifact();
    if(parentArtifact != null) {
      requirements.add(MavenRequiredCapability.createResolvedMavenParent(new ArtifactKey(parentArtifact)));
    }
  }
}
