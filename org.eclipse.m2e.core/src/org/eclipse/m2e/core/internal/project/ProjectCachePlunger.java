/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RepositoryPolicy;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;


/**
 * @since 1.6
 */
abstract class ProjectCachePlunger<Key> {

  private final Logger log = LoggerFactory.getLogger(getClass());

  final Map<File, Set<Key>> projectKeys = new HashMap<>();

  final Map<Key, Set<File>> keyProjects = new HashMap<>();

  public void register(MavenProject project, Key cacheKey) {
    // project.file is null for parent pom.xml resolved from repositories
    File file = project.getFile();
    if(file != null) {
      projectKeys.computeIfAbsent(file, f -> new HashSet<>()).add(cacheKey);
      keyProjects.computeIfAbsent(cacheKey, f -> new HashSet<>()).add(file);
    }
  }

  public Set<File> removeProject(File pom, boolean forceDependencyUpdate) {
    MavenExecutionContext context = MavenExecutionContext.getThreadContext();
    RepositorySystemSession session = context != null ? context.getRepositorySession() : null;
    if(forceDependencyUpdate && session == null) {
      throw new IllegalArgumentException();
    }
    final Set<File> affectedProjects = new HashSet<>();

    for(Key cacheKey : removeAll(projectKeys, pom)) {
      remove(keyProjects, cacheKey, pom);
      if(forceDependencyUpdate && RepositoryPolicy.UPDATE_POLICY_ALWAYS.equals(session.getUpdatePolicy())
          && session.getCache().get(session, cacheKey) == null) {
        session.getCache().put(session, cacheKey, Boolean.TRUE);
        for(File affectedPom : removeAll(keyProjects, cacheKey)) {
          affectedProjects.add(affectedPom);
          remove(projectKeys, affectedPom, cacheKey);
        }
      }
      if(!keyProjects.containsKey(cacheKey)) {
        flush(cacheKey);
        log.debug("Flushed cache entry for {}", cacheKey);
      }
    }
    return affectedProjects;
  }

  private static <K, V> Set<V> removeAll(Map<K, Set<V>> map, K key) {
    Set<V> removed = map.remove(key);
    return removed != null ? removed : Collections.emptySet();
  }

  private static <K, V> void remove(Map<K, Set<V>> map, K key, V value) {
    map.computeIfPresent(key, (k, values) -> {
      values.remove(value);
      return values.isEmpty() ? null : values;
    });
  }

  protected void disposeClassRealm(ClassRealm realm) {
    try {
      realm.getWorld().disposeRealm(realm.getId());
    } catch(NoSuchRealmException e) {
      // ignore
    }
  }

  protected abstract void flush(Key cacheKey);

  public void flush() {
    projectKeys.clear();
    keyProjects.clear();
  }
}
