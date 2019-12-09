/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.artifact.DefaultMavenMetadataCache;
import org.apache.maven.project.artifact.MavenMetadataCache;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * EclipseMavenMetadataCache
 * 
 * @author igor
 */
@Singleton
public class EclipseMavenMetadataCache extends DefaultMavenMetadataCache implements MavenMetadataCache, IManagedCache {

  @Override
  public void put(Artifact artifact, boolean resolveManagedVersions, ArtifactRepository localRepository,
      List<ArtifactRepository> remoteRepositories, ResolutionGroup result) {

    ArtifactKey gav = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), null);

    if("pom".equals(artifact.getType())) { //$NON-NLS-1$
      // new project pom, remove any existing project entries
      removeProject(gav);
    }

    super.put(artifact, resolveManagedVersions, localRepository, remoteRepositories, result);
  }

  @Override
  public Set<File> removeProject(File pom, ArtifactKey key, boolean force) {
    removeProject(key);
    return Collections.emptySet();
  }

  private void removeProject(ArtifactKey key) {
    if(key == null) {
      return;
    }

    Iterator<Entry<CacheKey, CacheRecord>> iter = cache.entrySet().iterator();

    while(iter.hasNext()) {
      Entry<CacheKey, CacheRecord> entry = iter.next();
      CacheRecord record = entry.getValue();

      if(equals(record.getArtifact(), key) || contains(record.getArtifacts(), key)) {
        iter.remove();
      }
    }
  }

  private boolean contains(List<Artifact> artifacts, ArtifactKey key) {
    for(Artifact artifact : artifacts) {
      if(equals(artifact, key)) {
        return true;
      }
    }
    return false;
  }

  private boolean equals(Artifact artifact, ArtifactKey key) {
    /*
     * maybe too conservative, but purge anything that matches GAbV (bV==baseVersion)
     */
    return eq(key.getGroupId(), artifact.getGroupId()) //
        && eq(key.getArtifactId(), artifact.getArtifactId()) //
        && eq(key.getVersion(), artifact.getBaseVersion());
  }

  private static <T> boolean eq(T a, T b) {
    return a != null ? a.equals(b) : b == null;
  }
}
