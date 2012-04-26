/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.maven.apt.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.MavenPluginActivator;

import org.jboss.tools.maven.apt.MavenJdtAptPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.ArtifactTypeRegistry;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.sonatype.aether.util.graph.transformer.JavaEffectiveScopeCalculator;
import org.sonatype.aether.util.graph.transformer.NearestVersionConflictResolver;

public class PluginDependencyResolver {

  private static final Logger log = LoggerFactory.getLogger(PluginDependencyResolver.class);

  /**
   * Looks up a plugin's dependencies (including the transitive ones) and return them as a list of {@link File} 
   * <br/>
   * Some of {@link MavenModelManager#readDependencyTree(org.eclipse.m2e.core.project.IMavenProjectFacade, MavenProject, String, IProgressMonitor)}'s logic has been copied and reused in this implementation.
   */
  public synchronized List<File> getResolvedPluginDependencies(MavenSession mavenSession, MavenProject mavenProject, Plugin plugin, IProgressMonitor monitor) throws CoreException {
    
    monitor.setTaskName("Resolve plugin dependency");

    IMaven maven = MavenPlugin.getMaven();

    DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(mavenSession.getRepositorySession());

    DependencyGraphTransformer transformer = new ChainedDependencyGraphTransformer(new JavaEffectiveScopeCalculator(),
        new NearestVersionConflictResolver());
    session.setDependencyGraphTransformer(transformer);

    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    List<File> files = new ArrayList<File>();
    try {
      Thread.currentThread().setContextClassLoader(maven.getProjectRealm(mavenProject));

      ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

      CollectRequest request = new CollectRequest();
      request.setRequestContext("plugin"); //$NON-NLS-1$
      request.setRepositories(mavenProject.getRemoteProjectRepositories());

      for(org.apache.maven.model.Dependency dependency : plugin.getDependencies()) {
        request.addDependency(RepositoryUtils.toDependency(dependency, stereotypes));
      }

      DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter( JavaScopes.COMPILE, JavaScopes.RUNTIME);

      DependencyRequest dependencyRequest = new DependencyRequest( request, classpathFilter );
      try {
        RepositorySystem system = MavenPluginActivator.getDefault().getRepositorySystem(); 
        List<ArtifactResult> artifactResults = system.resolveDependencies( session, dependencyRequest ).getArtifactResults();

        for ( ArtifactResult artifactResult : artifactResults )
        {
            files.add(artifactResult.getArtifact().getFile() );
        }
      } catch(DependencyResolutionException e) {
        String msg = "Unable to collect dependencies for plugin";
        log.error(msg, e);
        throw new CoreException(new Status(IStatus.ERROR, MavenJdtAptPlugin.PLUGIN_ID, -1, msg, e));
      }

    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
    return files;
  }

  
}
