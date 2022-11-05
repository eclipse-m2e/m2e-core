/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation, based on ProjectRegistryManager
 *******************************************************************************/
package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

import org.eclipse.core.runtime.CoreException;

import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.PluginArtifactsCache;
import org.apache.maven.plugin.PluginRealmCache;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectRealmCache;
import org.apache.maven.project.artifact.MavenMetadataCache;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.PlexusContainerManager;
import org.eclipse.m2e.core.internal.project.IManagedCache;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * MavenProjectCache
 *
 * @author christoph
 */
@Component(service = MavenProjectCache.class)
public class MavenProjectCache {

  private static final int MAX_CACHE_SIZE = Integer.getInteger("m2e.project.cache.size", 20);

  private static final String CTX_MAVENPROJECTS = MavenProjectCache.class.getName() + "/mavenProjects";

  @Reference
  PlexusContainerManager containerManager;

  private Cache<IMavenProjectFacade, MavenProject> mavenProjectCache;


  /**
   * 
   */
  public MavenProjectCache() {
    this.mavenProjectCache = CacheBuilder.newBuilder() //
        .maximumSize(MAX_CACHE_SIZE) //
        .removalListener((RemovalNotification<IMavenProjectFacade, MavenProject> removed) -> {
          Map<IMavenProjectFacade, MavenProject> contextProjects = getContextProjectMap();
          IMavenProjectFacade facade = removed.getKey();
          if(!contextProjects.containsKey(facade)) {
            flushMavenCaches(facade.getPomFile(), facade.getArtifactKey(), false);
          }
        }).build();
  }

  /**
   * @param facade
   */
  public void invalidateProjectFacade(IMavenProjectFacade facade) {
    mavenProjectCache.invalidate(facade);
  }

  /**
   * @param facade
   * @return
   */
  public MavenProject getMavenProject(IMavenProjectFacade facade, Function<IMavenProjectFacade, MavenProject> projectLoader) {
    if(projectLoader == null) {
      return mavenProjectCache.getIfPresent(facade);
    }
    try {
      return mavenProjectCache.get(facade, () -> projectLoader.apply(facade));
    } catch(ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * @param newFacade
   * @param mavenProject
   */
  public void updateMavenProject(IMavenProjectFacade newFacade, MavenProject mavenProject) {
    mavenProjectCache.put(newFacade, mavenProject);
  }

  /**
   * Flushes caches maintained by Maven core.
   */
  Set<File> flushMavenCaches(File pom, ArtifactKey key, boolean force) {
    Set<File> affected = new HashSet<>();
    affected.addAll(flushMavenCache(ProjectRealmCache.class, pom, key, force));
    affected.addAll(flushMavenCache(ExtensionRealmCache.class, pom, key, force));
    affected.addAll(flushMavenCache(PluginRealmCache.class, pom, key, force));
    affected.addAll(flushMavenCache(MavenMetadataCache.class, pom, key, force));
    affected.addAll(flushMavenCache(PluginArtifactsCache.class, pom, key, force));
    return affected;
  }

  private Set<File> flushMavenCache(Class<?> clazz, File pom, ArtifactKey key, boolean force) {
    try {
      Object lookup = containerManager.getComponentLookup(pom).lookup(clazz);
      if(lookup instanceof IManagedCache cache) {
        return cache.removeProject(pom, key, force);
      }
    } catch(CoreException ex) {
      // can't really happen
      throw new AssertionError(ex);
    }
    return Collections.emptySet();
  }

  /**
   * Do not modify this map directly, use {@link #putMavenProject(MavenProjectFacade, MavenProject)}
   *
   * @return
   */
  static Map<IMavenProjectFacade, MavenProject> getContextProjectMap() {
    MavenExecutionContext context = MavenExecutionContext.getThreadContext(false);
    if(context != null) {
      Map<IMavenProjectFacade, MavenProject> projects = context.getValue(CTX_MAVENPROJECTS);
      if(projects == null) {
        projects = new IdentityHashMap<>();
        context.setValue(CTX_MAVENPROJECTS, projects);
      }
      return projects;
    }
    return new IdentityHashMap<>(1);
  }

}
