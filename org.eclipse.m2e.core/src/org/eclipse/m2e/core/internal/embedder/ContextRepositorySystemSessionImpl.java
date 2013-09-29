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

import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.transfer.TransferListener;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import org.apache.maven.plugin.LegacySupport;


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

  /* (non-Javadoc)
   * @see org.eclipse.aether.RepositorySystemSession#isOffline()
   */
  public boolean isOffline() {
    return getSession().isOffline();
  }

  /* (non-Javadoc)
   * @see org.eclipse.aether.RepositorySystemSession#isIgnoreArtifactDescriptorRepositories()
   */
  public boolean isIgnoreArtifactDescriptorRepositories() {
    return getSession().isIgnoreArtifactDescriptorRepositories();
  }

  /* (non-Javadoc)
   * @see org.eclipse.aether.RepositorySystemSession#getResolutionErrorPolicy()
   */
  public ResolutionErrorPolicy getResolutionErrorPolicy() {
    return getSession().getResolutionErrorPolicy();
  }

  /* (non-Javadoc)
   * @see org.eclipse.aether.RepositorySystemSession#getArtifactDescriptorPolicy()
   */
  public ArtifactDescriptorPolicy getArtifactDescriptorPolicy() {
    return getSession().getArtifactDescriptorPolicy();
  }
}
