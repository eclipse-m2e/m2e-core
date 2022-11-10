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
 *      Christoph Läubrich - remove IMavenExecutionContext getExecutionContext()
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
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.IMavenToolbox;
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
    log.debug("Resolving dependencies for {}", facade); //$NON-NLS-1$

    markerManager.deleteMarkers(facade.getPom(), IMavenConstants.MARKER_DEPENDENCY_ID);

    IMavenExecutionContext executionContext = facade.createExecutionContext();
    MavenExecutionResult mavenResult = executionContext.execute((ctx, mon) -> {
      ProjectBuildingRequest configuration = ctx.newProjectBuildingRequest();
      configuration.setProject(facade.getMavenProject());
      configuration.setResolveDependencies(true);
      return IMavenToolbox.of(ctx).readMavenProject(facade.getPomFile(), configuration);
    }, monitor);

    markerManager.addMarkers(facade.getPom(), IMavenConstants.MARKER_DEPENDENCY_ID, mavenResult);

    if(!facade.getResolverConfiguration().isResolveWorkspaceProjects()) {
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

    log.debug("Resolved dependencies for {} in {} ms", facade, System.currentTimeMillis() - start); //$NON-NLS-1$
  }

  public static void addProjectStructureRequirements(Set<RequiredCapability> requirements, MavenProject mavenProject) {
    // parent requirement
    Artifact parentArtifact = mavenProject.getParentArtifact();
    if(parentArtifact != null) {
      requirements.add(MavenRequiredCapability.createResolvedMavenParent(new ArtifactKey(parentArtifact)));
    }

    // imported dependency management requirements
    while(mavenProject != null) {
      DependencyManagement dependencyManagement = mavenProject.getOriginalModel().getDependencyManagement();
      if(dependencyManagement != null) {
        for(org.apache.maven.model.Dependency managedDep : dependencyManagement.getDependencies()) {
          if("pom".equals(managedDep.getType()) && "import".equals(managedDep.getScope()) && managedDep.getVersion() != null) { //$NON-NLS-1$ $NON-NLS-2$
            ArtifactKey dependencyKey = new ArtifactKey(managedDep.getGroupId(), managedDep.getArtifactId(),
                managedDep.getVersion(), null);
            requirements.add(MavenRequiredCapability.createMavenArtifactImport(dependencyKey));
          }
        }
      }

      // add imports from all ancestors
      mavenProject = mavenProject.getParent();
    }
  }
}
