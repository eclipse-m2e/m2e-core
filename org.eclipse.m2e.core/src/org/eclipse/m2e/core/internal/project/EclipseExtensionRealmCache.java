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
import java.util.Set;

import javax.inject.Singleton;

import org.apache.maven.plugin.DefaultExtensionRealmCache;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * @since 1.6
 */
@Singleton
@SuppressWarnings("synthetic-access")
public class EclipseExtensionRealmCache extends DefaultExtensionRealmCache implements IManagedCache {

  private final ProjectCachePlunger<Key> plunger = new ProjectCachePlunger<>() {
    @Override
    protected void flush(Key cacheKey) {
      CacheRecord cacheRecord = cache.remove(cacheKey);
      if(cacheRecord != null) {
        disposeClassRealm(cacheRecord.getRealm());
      }
    }
  };

  @Override
  public void register(MavenProject project, Key key, CacheRecord record) {
    plunger.register(project, key);
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
