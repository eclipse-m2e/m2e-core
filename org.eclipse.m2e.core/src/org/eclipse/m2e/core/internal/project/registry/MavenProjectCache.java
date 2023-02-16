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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

import org.eclipse.core.runtime.CoreException;

import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.PluginArtifactsCache;
import org.apache.maven.plugin.PluginRealmCache;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectRealmCache;
import org.apache.maven.project.artifact.MavenMetadataCache;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.PlexusContainerManager;
import org.eclipse.m2e.core.internal.project.IManagedCache;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfiguration;


/**
 * MavenProjectCache
 *
 * @author christoph
 */
@Component(service = MavenProjectCache.class)
public class MavenProjectCache {

  private static final int MAX_CACHE_SIZE = Integer.getInteger("m2e.project.cache.size", 50);

  private static final String CTX_MAVENPROJECTS = MavenProjectCache.class.getName() + "/mavenProjects";

  @Reference
  PlexusContainerManager containerManager;

  private LoadingCache<CacheKey, CacheLine> loadingCache;


  public MavenProjectCache() {
    this.loadingCache = CacheBuilder.newBuilder() //
        .maximumSize(MAX_CACHE_SIZE) //
        .removalListener((RemovalNotification<CacheKey, CacheLine> removed) -> {
          Map<IMavenProjectFacade, MavenProject> contextProjects = getContextProjectMap();
          removed.getValue().projects.values().forEach(mavenProject -> {
            if(!contextProjects.containsValue(mavenProject)) {
              flushMavenCaches(mavenProject.getFile(), removed.getKey().artifactKey(), false);
            }
          });
        }).build(CacheLoader.from(CacheLine::new));
  }

  /**
   * Invalidates stored data for this facade
   * 
   * @param facade the facade to invalidate
   */
  public void invalidateProjectFacade(IMavenProjectFacade facade) {
    CacheLine cacheLine = loadingCache.getIfPresent(new CacheKey(facade.getArtifactKey(), facade.getConfiguration()));
    if(cacheLine != null) {
      cacheLine.remove(facade.getPomFile());
    }
  }

  /**
   * Get (and optionally load) the MavenProject for this facade.
   * 
   * @param facade the facade for which a project should be returned
   * @param projectLoader if given will be used to load a value into this cache, if <code>null</code> no loading is
   *          attempted if no value is in the cache
   * @return the project or null
   */
  public MavenProject getMavenProject(IMavenProjectFacade facade, Function<IMavenProjectFacade, MavenProject> projectLoader) {
    ArtifactKey artifactKey = facade.getArtifactKey();
    CacheLine cacheLine = loadingCache.getUnchecked(new CacheKey(artifactKey, facade.getConfiguration()));
    return cacheLine.getProject(facade, projectLoader);
  }

  /**
   * Associates a new maven project with the given facade
   * 
   * @param facade the facade to update
   * @param mavenProject the new mavenproject value
   */
  public void updateMavenProject(IMavenProjectFacade facade, MavenProject mavenProject) {
    if(mavenProject == null) {
      invalidateProjectFacade(facade);
      return;
    }
    ArtifactKey artifactKey = facade.getArtifactKey();
    CacheLine cacheLine = loadingCache.getUnchecked(new CacheKey(artifactKey, facade.getConfiguration()));
    cacheLine.updateProject(facade, mavenProject);
  }

  /**
   * Flushes caches maintained by Maven core.
   */
  Set<File> flushMavenCaches(File pom, ArtifactKey key, boolean force) {
    Set<File> affected = new HashSet<>();
    IComponentLookup componentLookup = containerManager.getComponentLookup(pom);
    affected.addAll(flushMavenCache(componentLookup, ProjectRealmCache.class, pom, key, force));
    affected.addAll(flushMavenCache(componentLookup, ExtensionRealmCache.class, pom, key, force));
    affected.addAll(flushMavenCache(componentLookup, PluginRealmCache.class, pom, key, force));
    affected.addAll(flushMavenCache(componentLookup, MavenMetadataCache.class, pom, key, force));
    affected.addAll(flushMavenCache(componentLookup, PluginArtifactsCache.class, pom, key, force));
    return affected;
  }

  private Set<File> flushMavenCache(IComponentLookup componentLookup, Class<?> clazz, File pom, ArtifactKey key,
      boolean force) {
    try {
      Object lookup = componentLookup.lookup(clazz);
      if(lookup instanceof IManagedCache cache) {
        return cache.removeProject(pom, key, force);
      }
    } catch(CoreException ex) {
      // If flushing failed, we can't do really much here...
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

  private final class CacheLine {

    private ConcurrentMap<File, MavenProject> projects = new ConcurrentHashMap<>(1);

    void remove(File pomFile) {
      projects.remove(pomFile);
    }

    void updateProject(IMavenProjectFacade facade, MavenProject mavenProject) {
      File pomFile = facade.getPomFile();
      projects.compute(pomFile, (key, current) -> {
        distributeProjectToCache(mavenProject, facade.getConfiguration());
        return mavenProject;
      });

    }

    MavenProject getProject(IMavenProjectFacade facade, Function<IMavenProjectFacade, MavenProject> loader) {
      File pomFile = facade.getPomFile();
      if(loader == null) {
        return projects.get(pomFile);
      }
      return projects.computeIfAbsent(pomFile, f -> {
        MavenProject mavenProject = loader.apply(facade);
        distributeProjectToCache(mavenProject, facade.getConfiguration());
        return mavenProject;
      });
    }

  }

  private void distributeProjectToCache(MavenProject mavenProject, IProjectConfiguration configuration) {
    if(mavenProject == null) {
      return;
    }
    MavenProject parent = mavenProject.getParent();
    if(parent != null) {
      File file = parent.getFile();
      //check if this is a workspace artifact
      if(file != null) {
        ArtifactKey projectKey = new ArtifactKey(parent.getArtifact());
        MavenProject cacheItem = loadingCache.getUnchecked(new CacheKey(projectKey, configuration)).projects
            .computeIfAbsent(file, x -> parent);
        if(cacheItem == parent) {
          //the project was cached, go on with the parent of the parent...
          distributeProjectToCache(parent, configuration);
        } else {
          //the project was already in the cache, replace the reference and we are done
          mavenProject.setParent(cacheItem);
        }
      }
    }
  }
  
  private static final record CacheKey(ArtifactKey artifactKey, IProjectConfiguration configuration) {
  }

}
