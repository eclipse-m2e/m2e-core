/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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

  final Multimap<File, Key> projectKeys = HashMultimap.create();

  final Multimap<Key, File> keyProjects = HashMultimap.create();

  public void register(MavenProject project, Key cacheKey) {
    // project.file is null for parent pom.xml resolved from repositories
    File file = project.getFile();
    if(file != null) {
      projectKeys.put(file, cacheKey);
      keyProjects.put(cacheKey, file);
    }
  }

  public Set<File> removeProject(File pom, boolean forceDependencyUpdate) {
    MavenExecutionContext context = MavenExecutionContext.getThreadContext();
    RepositorySystemSession session = context != null ? context.getRepositorySession() : null;
    if(forceDependencyUpdate && session == null) {
      throw new IllegalArgumentException();
    }
    final Set<File> affectedProjects = new HashSet<>();

    for(Key cacheKey : projectKeys.removeAll(pom)) {
      keyProjects.remove(cacheKey, pom);
      if(forceDependencyUpdate && RepositoryPolicy.UPDATE_POLICY_ALWAYS.equals(session.getUpdatePolicy())
          && session.getCache().get(session, cacheKey) == null) {
        session.getCache().put(session, cacheKey, Boolean.TRUE);
        for(File affectedPom : keyProjects.removeAll(cacheKey)) {
          affectedProjects.add(affectedPom);
          projectKeys.remove(affectedPom, cacheKey);
        }
      }
      if(!keyProjects.containsKey(cacheKey)) {
        flush(cacheKey);
        log.debug("Flushed cache entry for {}", cacheKey);
      }
    }

    return affectedProjects;
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
