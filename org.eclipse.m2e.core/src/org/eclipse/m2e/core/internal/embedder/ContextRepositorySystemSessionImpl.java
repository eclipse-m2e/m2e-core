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

package org.eclipse.m2e.core.internal.embedder;

import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import org.apache.maven.plugin.LegacySupport;

import org.sonatype.aether.RepositoryCache;
import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.SessionData;
import org.sonatype.aether.artifact.ArtifactTypeRegistry;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.DependencyManager;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.collection.DependencyTraverser;
import org.sonatype.aether.repository.AuthenticationSelector;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.MirrorSelector;
import org.sonatype.aether.repository.ProxySelector;
import org.sonatype.aether.repository.WorkspaceReader;
import org.sonatype.aether.transfer.TransferListener;


@Component(role = ContextRepositorySystemSession.class)
public class ContextRepositorySystemSessionImpl implements ContextRepositorySystemSession {

  @Requirement
  private LegacySupport context;

  private RepositorySystemSession getSession() {
    RepositorySystemSession session = context.getRepositorySession();
    if(session == null) {
      throw new IllegalStateException("no context maven session"); //$NON-NLS-1$
    }
    return session;
  }

  public ArtifactTypeRegistry getArtifactTypeRegistry() {
    return getSession().getArtifactTypeRegistry();
  }

  public AuthenticationSelector getAuthenticationSelector() {
    return getSession().getAuthenticationSelector();
  }

  public RepositoryCache getCache() {
    return getSession().getCache();
  }

  public String getChecksumPolicy() {
    return getSession().getChecksumPolicy();
  }

  public Map<String, Object> getConfigProperties() {
    return getSession().getConfigProperties();
  }

  public SessionData getData() {
    return getSession().getData();
  }

  public DependencyGraphTransformer getDependencyGraphTransformer() {
    return getSession().getDependencyGraphTransformer();
  }

  public DependencyManager getDependencyManager() {
    return getSession().getDependencyManager();
  }

  public DependencySelector getDependencySelector() {
    return getSession().getDependencySelector();
  }

  public DependencyTraverser getDependencyTraverser() {
    return getSession().getDependencyTraverser();
  }

  public LocalRepository getLocalRepository() {
    return getSession().getLocalRepository();
  }

  public LocalRepositoryManager getLocalRepositoryManager() {
    return getSession().getLocalRepositoryManager();
  }

  public MirrorSelector getMirrorSelector() {
    return getSession().getMirrorSelector();
  }

  public ProxySelector getProxySelector() {
    return getSession().getProxySelector();
  }

  public RepositoryListener getRepositoryListener() {
    return getSession().getRepositoryListener();
  }

  public Map<String, String> getSystemProperties() {
    return getSession().getSystemProperties();
  }

  public TransferListener getTransferListener() {
    return getSession().getTransferListener();
  }

  public String getUpdatePolicy() {
    return getSession().getUpdatePolicy();
  }

  public Map<String, String> getUserProperties() {
    return getSession().getUserProperties();
  }

  public WorkspaceReader getWorkspaceReader() {
    return getSession().getWorkspaceReader();
  }

  public boolean isIgnoreInvalidArtifactDescriptor() {
    return getSession().isIgnoreInvalidArtifactDescriptor();
  }

  public boolean isIgnoreMissingArtifactDescriptor() {
    return getSession().isIgnoreMissingArtifactDescriptor();
  }

  public boolean isNotFoundCachingEnabled() {
    return getSession().isNotFoundCachingEnabled();
  }

  public boolean isOffline() {
    return getSession().isOffline();
  }

  public boolean isTransferErrorCachingEnabled() {
    return getSession().isTransferErrorCachingEnabled();
  }

}
