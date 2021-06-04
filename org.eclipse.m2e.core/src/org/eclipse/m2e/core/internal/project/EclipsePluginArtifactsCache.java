/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
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

package org.eclipse.m2e.core.internal.project;

import java.io.File;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.maven.plugin.DefaultPluginArtifactsCache;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;


@Singleton
@SuppressWarnings("synthetic-access")
public class EclipsePluginArtifactsCache extends DefaultPluginArtifactsCache implements IManagedCache {

  private final ProjectCachePlunger<Key> plunger = new ProjectCachePlunger<>() {
    protected void flush(Key cacheKey) {
      cache.remove(cacheKey);
    }
  };

  @Override
  public void register(MavenProject project, Key cacheKey, CacheRecord record) {
    plunger.register(project, cacheKey);
  }

  @Override
  public Set<File> removeProject(File pom, ArtifactKey mavenProject, boolean forceDependencyUpdate) {
    return plunger.removeProject(pom, forceDependencyUpdate);
  }

  @Override
  public void flush() {
    super.flush();
    plunger.flush();
  }

}
