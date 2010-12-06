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

import java.util.List;

import org.codehaus.plexus.component.annotations.Component;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.internal.DefaultPluginDependenciesResolver;
import org.apache.maven.plugin.internal.PluginDependenciesResolver;

import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;


@Component(role = PluginDependenciesResolver.class)
public class EclipsePluginDependenciesResolver extends DefaultPluginDependenciesResolver {

  /*
   * Plugin realms are cached and there is currently no way to purge cached
   * realms due to http://jira.codehaus.org/browse/MNG-4194.
   * 
   * Workspace plugins cannot be cached, so we disable this until MNG-4194 is fixed.
   * 
   * Corresponding m2e JIRA https://issues.sonatype.org/browse/MNGECLIPSE-1448
   */

  @Override
  public Artifact resolve(Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session)
      throws PluginResolutionException {
    boolean disabled = EclipseWorkspaceArtifactRepository.isDisabled();
    EclipseWorkspaceArtifactRepository.setDisabled(true);
    try {
      return super.resolve(plugin, repositories, session);
    } finally {
      EclipseWorkspaceArtifactRepository.setDisabled(disabled);
    }
  }

  @Override
  public DependencyNode resolve(Plugin plugin, Artifact pluginArtifact, DependencyFilter dependencyFilter,
      List<RemoteRepository> repositories, RepositorySystemSession session) throws PluginResolutionException {
    boolean disabled = EclipseWorkspaceArtifactRepository.isDisabled();
    EclipseWorkspaceArtifactRepository.setDisabled(true);
    try {
      return super.resolve(plugin, pluginArtifact, dependencyFilter, repositories, session);
    } finally {
      EclipseWorkspaceArtifactRepository.setDisabled(disabled);
    }
  }

}
