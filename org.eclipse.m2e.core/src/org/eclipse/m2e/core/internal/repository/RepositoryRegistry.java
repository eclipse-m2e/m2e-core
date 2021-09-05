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

package org.eclipse.m2e.core.internal.repository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

import org.eclipse.m2e.core.embedder.ArtifactRepositoryRef;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.ISettingsChangeListener;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


/**
 * RepositoryRegistry
 *
 * @author igor
 */
public class RepositoryRegistry implements IRepositoryRegistry, IMavenProjectChangedListener, ISettingsChangeListener {
  private static final Logger log = LoggerFactory.getLogger(RepositoryRegistry.class);

  private final IMaven maven;

  private final IMavenProjectRegistry projectManager;

  /**
   * Maps repositoryUrl to IndexInfo of repository index
   */
  private final Map<String, RepositoryInfo> repositories = new ConcurrentHashMap<>();

  /**
   * Lazy instantiated local repository instance.
   */
  private RepositoryInfo localRepository;

  /**
   * Lock guarding lazy instantiation of localRepository instance
   */
  private final Object localRepositoryLock = new Object();

  private final RepositoryInfo workspaceRepository;

  private final ArrayList<IRepositoryIndexer> indexers = new ArrayList<>();

  private final ArrayList<IRepositoryDiscoverer> discoverers = new ArrayList<>();

  private final RepositoryRegistryUpdateJob job = new RepositoryRegistryUpdateJob(this);

  public RepositoryRegistry(IMaven maven, IMavenProjectRegistry projectManager) {
    this.maven = maven;
    this.projectManager = projectManager;

    this.workspaceRepository = new RepositoryInfo(null/*id*/,
        "workspace://"/*url*/, null/*basedir*/, SCOPE_WORKSPACE, null/*auth*/); //$NON-NLS-1$
  }

  private RepositoryInfo newLocalRepositoryInfo() {
    File localBasedir = new File(maven.getLocalRepositoryPath());
    try {
      localBasedir = localBasedir.getCanonicalFile();
    } catch(IOException e) {
      // will never happen
      localBasedir = localBasedir.getAbsoluteFile();
    }

    String localUrl;
    try {
      localUrl = localBasedir.toURL().toExternalForm();
    } catch(MalformedURLException ex) {
      log.error("Could not parse local repository path", ex);
      localUrl = "file://" + localBasedir.getAbsolutePath(); //$NON-NLS-1$
    }

    // initialize local and workspace repositories
    RepositoryInfo localRepository = new RepositoryInfo(null/*id*/, localUrl, localBasedir, SCOPE_LOCAL, null/*auth*/);
    return localRepository;
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    /*
     * This method is called while holding workspace lock. Avoid long-running operations if possible.
     */

    Settings settings = null;
    try {
      settings = maven.getSettings();
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }

    for(MavenProjectChangedEvent event : events) {
      IMavenProjectFacade oldFacade = event.getOldMavenProject();
      if(oldFacade != null) {
        removeProjectRepositories(oldFacade, monitor);
      }
      IMavenProjectFacade facade = event.getMavenProject();
      if(facade != null) {
        try {
          addProjectRepositories(settings, facade, null /*asyncUpdate*/);
        } catch(CoreException ex) {
          log.error(ex.getMessage(), ex);
        }
      }
    }
  }

  private void addProjectRepositories(Settings settings, IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    ArrayList<ArtifactRepositoryRef> repositories = getProjectRepositories(facade);

    for(ArtifactRepositoryRef repo : repositories) {
      RepositoryInfo repository = getRepository(repo);
      if(repository != null) {
        repository.addProject(facade.getPom().getFullPath());
        continue;
      }
      AuthenticationInfo auth = getAuthenticationInfo(settings, repo.getId());
      repository = new RepositoryInfo(repo.getId(), repo.getUrl(), SCOPE_PROJECT, auth);
      repository.addProject(facade.getPom().getFullPath());

      addRepository(repository, monitor);
    }
  }

  public void addRepository(RepositoryInfo repository, IProgressMonitor monitor) {
    if(!repositories.containsKey(repository.getUid())) {
      repositories.put(repository.getUid(), repository);

      for(IRepositoryIndexer indexer : indexers) {
        try {
          indexer.repositoryAdded(repository, monitor);
        } catch(CoreException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  private void removeProjectRepositories(IMavenProjectFacade facade, IProgressMonitor monitor) {
    ArrayList<ArtifactRepositoryRef> repositories = getProjectRepositories(facade);

    for(ArtifactRepositoryRef repo : repositories) {
      RepositoryInfo repository = getRepository(repo);
      if(repository != null && repository.isScope(SCOPE_PROJECT)) {
        repository.removeProject(facade.getPom().getFullPath());
        if(repository.getProjects().isEmpty()) {
          removeRepository(repository, monitor);
        }
      }
    }
  }

  private void removeRepository(RepositoryInfo repository, IProgressMonitor monitor) {
    repositories.remove(repository.getUid());

    for(IRepositoryIndexer indexer : indexers) {
      try {
        indexer.repositoryRemoved(repository, monitor);
      } catch(CoreException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  private ArrayList<ArtifactRepositoryRef> getProjectRepositories(IMavenProjectFacade facade) {
    ArrayList<ArtifactRepositoryRef> repositories = new ArrayList<>();
    repositories.addAll(facade.getArtifactRepositoryRefs());
    repositories.addAll(facade.getPluginArtifactRepositoryRefs());
    return repositories;
  }

  public AuthenticationInfo getAuthenticationInfo(Settings settings, String id) throws CoreException {
    if(settings == null) {
      return null;
    }

    Server server = settings.getServer(id);
    if(server == null || server.getUsername() == null) {
      return null;
    }

    server = maven.decryptPassword(server);

    AuthenticationInfo info = new AuthenticationInfo();
    info.setUserName(server.getUsername());
    info.setPassword(server.getPassword());
    return info;
  }

  public void updateRegistry(IProgressMonitor monitor) throws CoreException {
    Settings settings = maven.getSettings();
    List<Mirror> mirrors = maven.getMirrors();

    // initialize indexers
    for(IRepositoryIndexer indexer : indexers) {
      indexer.initialize(monitor);
    }

    // process configured repositories

    Map<String, RepositoryInfo> oldRepositories = new HashMap<>(repositories);
    repositories.clear();

    addRepository(this.workspaceRepository, monitor);

    synchronized(localRepositoryLock) {
      this.localRepository = newLocalRepositoryInfo();
    }
    addRepository(this.localRepository, monitor);

    // mirrors
    for(Mirror mirror : mirrors) {
      AuthenticationInfo auth = getAuthenticationInfo(settings, mirror.getId());
      RepositoryInfo repository = new RepositoryInfo(mirror.getId(), mirror.getUrl(), SCOPE_SETTINGS, auth);
      repository.setMirrorOf(mirror.getMirrorOf());
      addRepository(repository, monitor);
    }

    // repositories from settings.xml
    ArrayList<ArtifactRepository> repos = new ArrayList<>();
    repos.addAll(maven.getArtifactRepositories(false));
    repos.addAll(maven.getPluginArtifactRepositories(false));

    for(ArtifactRepository repo : repos) {
      Mirror mirror = maven.getMirror(repo);
      AuthenticationInfo auth = getAuthenticationInfo(settings, repo.getId());
      RepositoryInfo repository = new RepositoryInfo(repo.getId(), repo.getUrl(), SCOPE_SETTINGS, auth);
      if(mirror != null) {
        repository.setMirrorId(mirror.getId());
      }
      addRepository(repository, monitor);
    }

    // project-specific repositories
    for(IMavenProjectFacade facade : projectManager.getProjects()) {
      addProjectRepositories(settings, facade, monitor);
    }

    // custom repositories
    for(IRepositoryDiscoverer discoverer : discoverers) {
      discoverer.addRepositories(this, monitor);
    }

    oldRepositories.keySet().removeAll(repositories.keySet());
    for(RepositoryInfo repository : oldRepositories.values()) {
      removeRepository(repository, monitor);
    }
  }

  @Override
  public List<IRepository> getRepositories(int scope) {
    ArrayList<IRepository> result = new ArrayList<>();
    for(RepositoryInfo repository : repositories.values()) {
      if(repository.isScope(scope)) {
        result.add(repository);
      }
    }
    return result;
  }

  public void updateRegistry() {
    job.updateRegistry();
  }

  public void addRepositoryIndexer(IRepositoryIndexer indexer) {
    this.indexers.add(indexer);
  }

  public void addRepositoryDiscoverer(IRepositoryDiscoverer discoverer) {
    this.discoverers.add(discoverer);
  }

  @Override
  public RepositoryInfo getRepository(ArtifactRepositoryRef ref) {
    String uid = RepositoryInfo.getUid(ref.getId(), ref.getUrl(), ref.getUsername());
    return repositories.get(uid);
  }

  @Override
  public IRepository getWorkspaceRepository() {
    return workspaceRepository;
  }

  @Override
  public IRepository getLocalRepository() {
    synchronized(localRepositoryLock) {
      if(localRepository == null) {
        localRepository = newLocalRepositoryInfo();
      }
    }

    return localRepository;
  }

  @Override
  public void settingsChanged(Settings settings) {
    updateRegistry();
  }

}
