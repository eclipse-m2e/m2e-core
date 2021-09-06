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
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transform.FileTransformerManager;

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

  @Override
  public ArtifactTypeRegistry getArtifactTypeRegistry() {
    return getSession().getArtifactTypeRegistry();
  }

  @Override
  public AuthenticationSelector getAuthenticationSelector() {
    return getSession().getAuthenticationSelector();
  }

  @Override
  public RepositoryCache getCache() {
    return getSession().getCache();
  }

  @Override
  public String getChecksumPolicy() {
    return getSession().getChecksumPolicy();
  }

  @Override
  public Map<String, Object> getConfigProperties() {
    return getSession().getConfigProperties();
  }

  @Override
  public SessionData getData() {
    return getSession().getData();
  }

  @Override
  public DependencyGraphTransformer getDependencyGraphTransformer() {
    return getSession().getDependencyGraphTransformer();
  }

  @Override
  public DependencyManager getDependencyManager() {
    return getSession().getDependencyManager();
  }

  @Override
  public DependencySelector getDependencySelector() {
    return getSession().getDependencySelector();
  }

  @Override
  public DependencyTraverser getDependencyTraverser() {
    return getSession().getDependencyTraverser();
  }

  @Override
  public LocalRepository getLocalRepository() {
    return getSession().getLocalRepository();
  }

  @Override
  public LocalRepositoryManager getLocalRepositoryManager() {
    return getSession().getLocalRepositoryManager();
  }

  @Override
  public MirrorSelector getMirrorSelector() {
    return getSession().getMirrorSelector();
  }

  @Override
  public ProxySelector getProxySelector() {
    return getSession().getProxySelector();
  }

  @Override
  public RepositoryListener getRepositoryListener() {
    return getSession().getRepositoryListener();
  }

  @Override
  public Map<String, String> getSystemProperties() {
    return getSession().getSystemProperties();
  }

  @Override
  public TransferListener getTransferListener() {
    return getSession().getTransferListener();
  }

  @Override
  public String getUpdatePolicy() {
    return getSession().getUpdatePolicy();
  }

  @Override
  public Map<String, String> getUserProperties() {
    return getSession().getUserProperties();
  }

  @Override
  public WorkspaceReader getWorkspaceReader() {
    return getSession().getWorkspaceReader();
  }

  @Override
  public boolean isOffline() {
    return getSession().isOffline();
  }

  @Override
  public boolean isIgnoreArtifactDescriptorRepositories() {
    return getSession().isIgnoreArtifactDescriptorRepositories();
  }

  @Override
  public ResolutionErrorPolicy getResolutionErrorPolicy() {
    return getSession().getResolutionErrorPolicy();
  }

  @Override
  public ArtifactDescriptorPolicy getArtifactDescriptorPolicy() {
    return getSession().getArtifactDescriptorPolicy();
  }

  @Override
  public VersionFilter getVersionFilter() {
    return getSession().getVersionFilter();
  }

  @Override
  public FileTransformerManager getFileTransformerManager() {
    return getSession().getFileTransformerManager();
  }

}
