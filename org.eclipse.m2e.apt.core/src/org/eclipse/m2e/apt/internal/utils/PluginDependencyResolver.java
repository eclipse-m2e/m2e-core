/*************************************************************************************
 * Copyright (c) 2008-2019 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc. - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.apt.internal.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.MavenPluginActivator;


@SuppressWarnings("restriction")
public class PluginDependencyResolver {

  private static final Logger log = LoggerFactory.getLogger(PluginDependencyResolver.class);

  /**
   * Looks up a plugin's dependencies (including the transitive ones) and return them as a list of {@link File} <br/>
   * Some of
   * {@link MavenModelManager#readDependencyTree(org.eclipse.m2e.core.project.IMavenProjectFacade, MavenProject, String, IProgressMonitor)}
   * 's logic has been copied and reused in this implementation.
   */
  public synchronized List<File> getResolvedPluginDependencies(MavenSession mavenSession, MavenProject mavenProject,
      Plugin plugin, IProgressMonitor monitor) throws CoreException {

    monitor.setTaskName("Resolve plugin dependency");

    IMaven maven = MavenPlugin.getMaven();

    DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(mavenSession.getRepositorySession());

    DependencyGraphTransformer transformer = new ConflictResolver(new NearestVersionSelector(), new JavaScopeSelector(),
        new SimpleOptionalitySelector(), new JavaScopeDeriver());
    session.setDependencyGraphTransformer(
        new ChainedDependencyGraphTransformer(transformer, new JavaDependencyContextRefiner()));

    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    List<File> files = new ArrayList<>();
    try {
      Thread.currentThread().setContextClassLoader(maven.getProjectRealm(mavenProject));

      ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

      CollectRequest request = new CollectRequest();
      request.setRequestContext("plugin"); //$NON-NLS-1$
      request.setRepositories(mavenProject.getRemoteProjectRepositories());

      Collection<Dependency> dependencies = getDependencies(plugin);
      for(Dependency dependency : dependencies) {
        if(dependency.getVersion() == null) {
          DependencyManagement depMngt = mavenProject.getDependencyManagement();
          if(depMngt != null) {
            Dependency mngtDep = depMngt.getDependencies().stream()
                .filter(d -> Objects.equals(d.getGroupId(), dependency.getGroupId())
                    && Objects.equals(d.getArtifactId(), dependency.getArtifactId())
                    && Objects.equals(d.getType(), dependency.getType()))
                .findFirst().get();
            if(mngtDep != null) {
              dependency.setVersion(mngtDep.getVersion());
            }
          }
        }
        request.addDependency(RepositoryUtils.toDependency(dependency, stereotypes));
      }

      DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE, JavaScopes.RUNTIME);

      DependencyRequest dependencyRequest = new DependencyRequest(request, classpathFilter);
      try {
        RepositorySystem system = MavenPluginActivator.getDefault().getRepositorySystem();
        List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest)
            .getArtifactResults();

        for(ArtifactResult artifactResult : artifactResults) {
          files.add(artifactResult.getArtifact().getFile());
        }
      } catch(DependencyResolutionException e) {
        String msg = "Unable to collect dependencies for plugin";
        log.error(msg, e);
        throw new CoreException(Status.error(msg, e));
      }

    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
    return files;
  }

  protected Collection<Dependency> getDependencies(Plugin plugin) {
    return plugin.getDependencies();
  }
}
