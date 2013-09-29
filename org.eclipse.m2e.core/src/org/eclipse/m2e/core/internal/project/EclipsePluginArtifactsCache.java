/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import javax.inject.Singleton;

import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RepositoryPolicy;

import org.apache.maven.plugin.DefaultPluginArtifactsCache;
import org.apache.maven.plugin.PluginResolutionException;

import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;


@Singleton
public class EclipsePluginArtifactsCache extends DefaultPluginArtifactsCache {

  public CacheRecord get(Key key) throws PluginResolutionException {
    MavenExecutionContext context = MavenExecutionContext.getThreadContext();

    if(context == null) {
      return super.get(key);
    }

    RepositorySystemSession session = context.getRepositorySession();
    RepositoryCache sessionCache = session.getCache();

    if(!RepositoryPolicy.UPDATE_POLICY_ALWAYS.equals(session.getUpdatePolicy())
        || sessionCache.get(session, key) != null) {
      return super.get(key);
    }

    // only force refresh once per repository session
    cache.remove(key);
    sessionCache.put(session, key, Boolean.TRUE);
    return null;
  }

  public EclipsePluginArtifactsCache() {
    super();
  }
}
