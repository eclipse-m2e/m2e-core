/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.transfer.TransferListener;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Profile;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.toolchain.model.ToolchainModel;


/**
 * Read-only MavenExecutionRequest that throws IllegalStateException from all modifiers.
 *
 * @since 1.4
 */
class ReadonlyMavenExecutionRequest implements MavenExecutionRequest {

  private final MavenExecutionRequest request;

  public ReadonlyMavenExecutionRequest(MavenExecutionRequest request) {
    this.request = request;
  }

  @Override
  public MavenExecutionRequest setBaseDirectory(File basedir) {
    throw new IllegalStateException();
  }

  @Override
  public String getBaseDirectory() {
    return request.getBaseDirectory();
  }

  @Override
  public MavenExecutionRequest setStartTime(Date start) {
    throw new IllegalStateException();
  }

  @Override
  public Date getStartTime() {
    return request.getStartTime();
  }

  @Override
  public MavenExecutionRequest setGoals(List<String> goals) {
    throw new IllegalStateException();
  }

  @Override
  public List<String> getGoals() {
    return Collections.unmodifiableList(request.getGoals());
  }

  @Override
  public MavenExecutionRequest setSystemProperties(Properties systemProperties) {
    throw new IllegalStateException();
  }

  @Override
  public Properties getSystemProperties() {
    // TODO unmodifiable properties?
    return request.getSystemProperties();
  }

  @Override
  public MavenExecutionRequest setUserProperties(Properties userProperties) {
    throw new IllegalStateException();
  }

  @Override
  public Properties getUserProperties() {
    // TODO unmodifiable properties?
    return request.getUserProperties();
  }

  @Override
  public MavenExecutionRequest setReactorFailureBehavior(String failureBehavior) {
    throw new IllegalStateException();
  }

  @Override
  public String getReactorFailureBehavior() {
    return request.getReactorFailureBehavior();
  }

  @Override
  public MavenExecutionRequest setSelectedProjects(List<String> projects) {
    throw new IllegalStateException();
  }

  @Override
  public List<String> getSelectedProjects() {
    return Collections.unmodifiableList(request.getSelectedProjects());
  }

  @Override
  public MavenExecutionRequest setResumeFrom(String project) {
    throw new IllegalStateException();
  }

  @Override
  public String getResumeFrom() {
    return request.getResumeFrom();
  }

  @Override
  public MavenExecutionRequest setMakeBehavior(String makeBehavior) {
    throw new IllegalStateException();
  }

  @Override
  public String getMakeBehavior() {
    return request.getMakeBehavior();
  }

  @Override
  public MavenExecutionRequest setRecursive(boolean recursive) {
    throw new IllegalStateException();
  }

  @Override
  public boolean isRecursive() {
    return request.isRecursive();
  }

  @Override
  public MavenExecutionRequest setPom(File pom) {
    throw new IllegalStateException();
  }

  @Override
  public File getPom() {
    return request.getPom();
  }

  @Override
  public MavenExecutionRequest setShowErrors(boolean showErrors) {
    throw new IllegalStateException();
  }

  @Override
  public boolean isShowErrors() {
    return request.isShowErrors();
  }

  @Override
  public MavenExecutionRequest setTransferListener(TransferListener transferListener) {
    throw new IllegalStateException();
  }

  @Override
  public TransferListener getTransferListener() {
    return request.getTransferListener();
  }

  @Override
  public MavenExecutionRequest setLoggingLevel(int loggingLevel) {
    throw new IllegalStateException();
  }

  @Override
  public int getLoggingLevel() {
    return request.getLoggingLevel();
  }

  @Override
  public MavenExecutionRequest setUpdateSnapshots(boolean updateSnapshots) {
    throw new IllegalStateException();
  }

  @Override
  public boolean isUpdateSnapshots() {
    return request.isUpdateSnapshots();
  }

  @Override
  public MavenExecutionRequest setNoSnapshotUpdates(boolean noSnapshotUpdates) {
    throw new IllegalStateException();
  }

  @Override
  public boolean isNoSnapshotUpdates() {
    return request.isNoSnapshotUpdates();
  }

  @Override
  public MavenExecutionRequest setGlobalChecksumPolicy(String globalChecksumPolicy) {
    throw new IllegalStateException();
  }

  @Override
  public String getGlobalChecksumPolicy() {
    return request.getGlobalChecksumPolicy();
  }

  @Override
  public MavenExecutionRequest setLocalRepositoryPath(String localRepository) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest setLocalRepositoryPath(File localRepository) {
    throw new IllegalStateException();
  }

  @Override
  public File getLocalRepositoryPath() {
    return request.getLocalRepositoryPath();
  }

  @Override
  public MavenExecutionRequest setLocalRepository(ArtifactRepository repository) {
    throw new IllegalStateException();
  }

  @Override
  public ArtifactRepository getLocalRepository() {
    return request.getLocalRepository();
  }

  @Override
  public MavenExecutionRequest setInteractiveMode(boolean interactive) {
    throw new IllegalStateException();
  }

  @Override
  public boolean isInteractiveMode() {
    return request.isInteractiveMode();
  }

  @Override
  public MavenExecutionRequest setOffline(boolean offline) {
    throw new IllegalStateException();
  }

  @Override
  public boolean isOffline() {
    return request.isOffline();
  }

  @Override
  public boolean isCacheTransferError() {
    return request.isCacheTransferError();
  }

  @Override
  public MavenExecutionRequest setCacheTransferError(boolean cacheTransferError) {
    throw new IllegalStateException();
  }

  @Override
  public boolean isCacheNotFound() {
    return request.isCacheNotFound();
  }

  @Override
  public MavenExecutionRequest setCacheNotFound(boolean cacheNotFound) {
    throw new IllegalStateException();
  }

  @Override
  public List<Profile> getProfiles() {
    return Collections.unmodifiableList(request.getProfiles());
  }

  @Override
  public MavenExecutionRequest addProfile(Profile profile) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest setProfiles(List<Profile> profiles) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addActiveProfile(String profile) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addActiveProfiles(List<String> profiles) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest setActiveProfiles(List<String> profiles) {
    throw new IllegalStateException();
  }

  @Override
  public List<String> getActiveProfiles() {
    return Collections.unmodifiableList(request.getActiveProfiles());
  }

  @Override
  public MavenExecutionRequest addInactiveProfile(String profile) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addInactiveProfiles(List<String> profiles) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest setInactiveProfiles(List<String> profiles) {
    throw new IllegalStateException();
  }

  @Override
  public List<String> getInactiveProfiles() {
    return Collections.unmodifiableList(request.getInactiveProfiles());
  }

  @Override
  public List<Proxy> getProxies() {
    return Collections.unmodifiableList(request.getProxies());
  }

  @Override
  public MavenExecutionRequest setProxies(List<Proxy> proxies) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addProxy(Proxy proxy) {
    throw new IllegalStateException();
  }

  @Override
  public List<Server> getServers() {
    return Collections.unmodifiableList(request.getServers());
  }

  @Override
  public MavenExecutionRequest setServers(List<Server> servers) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addServer(Server server) {
    throw new IllegalStateException();
  }

  @Override
  public List<Mirror> getMirrors() {
    return Collections.unmodifiableList(request.getMirrors());
  }

  @Override
  public MavenExecutionRequest setMirrors(List<Mirror> mirrors) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addMirror(Mirror mirror) {
    throw new IllegalStateException();
  }

  @Override
  public List<String> getPluginGroups() {
    return Collections.unmodifiableList(request.getPluginGroups());
  }

  @Override
  public MavenExecutionRequest setPluginGroups(List<String> pluginGroups) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addPluginGroup(String pluginGroup) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addPluginGroups(List<String> pluginGroups) {
    throw new IllegalStateException();
  }

  @Override
  public boolean isProjectPresent() {
    return request.isProjectPresent();
  }

  @Override
  public MavenExecutionRequest setProjectPresent(boolean isProjectPresent) {
    throw new IllegalStateException();
  }

  @Override
  public File getUserSettingsFile() {
    return request.getUserSettingsFile();
  }

  @Override
  public MavenExecutionRequest setUserSettingsFile(File userSettingsFile) {
    throw new IllegalStateException();
  }

  @Override
  public File getGlobalSettingsFile() {
    return request.getGlobalSettingsFile();
  }

  @Override
  public MavenExecutionRequest setGlobalSettingsFile(File globalSettingsFile) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addRemoteRepository(ArtifactRepository repository) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest addPluginArtifactRepository(ArtifactRepository repository) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest setRemoteRepositories(List<ArtifactRepository> repositories) {
    throw new IllegalStateException();
  }

  @Override
  public List<ArtifactRepository> getRemoteRepositories() {
    return Collections.unmodifiableList(request.getRemoteRepositories());
  }

  @Override
  public MavenExecutionRequest setPluginArtifactRepositories(List<ArtifactRepository> repositories) {
    throw new IllegalStateException();
  }

  @Override
  public List<ArtifactRepository> getPluginArtifactRepositories() {
    return Collections.unmodifiableList(request.getPluginArtifactRepositories());
  }

  @Override
  public MavenExecutionRequest setRepositoryCache(RepositoryCache repositoryCache) {
    throw new IllegalStateException();
  }

  @Override
  public RepositoryCache getRepositoryCache() {
    return request.getRepositoryCache();
  }

  @Override
  public WorkspaceReader getWorkspaceReader() {
    return request.getWorkspaceReader();
  }

  @Override
  public MavenExecutionRequest setWorkspaceReader(WorkspaceReader workspaceReader) {
    throw new IllegalStateException();
  }

  @Override
  public File getUserToolchainsFile() {
    return request.getUserToolchainsFile();
  }

  @Override
  public MavenExecutionRequest setUserToolchainsFile(File userToolchainsFile) {
    throw new IllegalStateException();
  }

  @Override
  public ExecutionListener getExecutionListener() {
    return request.getExecutionListener();
  }

  @Override
  public MavenExecutionRequest setExecutionListener(ExecutionListener executionListener) {
    throw new IllegalStateException();
  }

  @Override
  public ProjectBuildingRequest getProjectBuildingRequest() {
    // TODO unmodifiable ProjectBuildingRequest
    return request.getProjectBuildingRequest();
  }

  @Override
  public boolean isUseLegacyLocalRepository() {
    return request.isUseLegacyLocalRepository();
  }

  @Override
  public MavenExecutionRequest setUseLegacyLocalRepository(boolean useLegacyRepository) {
    throw new IllegalStateException();
  }

  private static final Method IS_IGNORE_TRANSITIVE_REPOSITORIES;
  static {
    Method method = null;
    try { // Tycho somehow compiles against the oldest version making compilation fail if methods from new Maven methods are referenced.
      method = MavenExecutionRequest.class.getMethod("isIgnoreTransitiveRepositories");
    } catch(Exception e) {
    }
    IS_IGNORE_TRANSITIVE_REPOSITORIES = method;
  }

  /**
   * @deprecated DO NOT CALL to maintain Maven 3.8 compatibility
   */
//  @Override
  @Deprecated(since = "to maintain compatibility with Maven 3.8")
  public boolean isIgnoreTransitiveRepositories() {
    if(IS_IGNORE_TRANSITIVE_REPOSITORIES != null) {
      try {
        return (boolean) IS_IGNORE_TRANSITIVE_REPOSITORIES.invoke(request);
      } catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new IllegalStateException(e);
      }
    }
    throw new UnsupportedOperationException();
  }

//  @Override
  public MavenExecutionRequest setIgnoreTransitiveRepositories(boolean ignoreTransitiveRepositories) {
    throw new IllegalStateException();
  }

  @Override
  public String getBuilderId() {
    return request.getBuilderId();
  }

  @Override
  public int getDegreeOfConcurrency() {
    return request.getDegreeOfConcurrency();
  }

  @Override
  public List<String> getExcludedProjects() {
    return Collections.unmodifiableList(request.getExcludedProjects());
  }

  @Override
  public MavenExecutionRequest setBuilderId(String builderId) {
    throw new IllegalStateException();
  }

  @Override
  public void setDegreeOfConcurrency(int degree) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest setExcludedProjects(List<String> excludedProjects) {
    throw new IllegalStateException();
  }

  @Override
  public Map<String, Object> getData() {
    return Collections.unmodifiableMap(request.getData());
  }

  @Override
  public EventSpyDispatcher getEventSpyDispatcher() {
    return request.getEventSpyDispatcher();
  }

  @Override
  public File getGlobalToolchainsFile() {
    return request.getGlobalToolchainsFile();
  }

  @Override
  public File getMultiModuleProjectDirectory() {
    return request.getMultiModuleProjectDirectory();
  }

  @Override
  public Map<String, List<ToolchainModel>> getToolchains() {
    return Collections.unmodifiableMap(request.getToolchains());
  }

  @Override
  public MavenExecutionRequest setEventSpyDispatcher(EventSpyDispatcher eventSpyDispatcher) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest setGlobalToolchainsFile(File globalToolchainsFile) {
    throw new IllegalStateException();
  }

  @Override
  public void setMultiModuleProjectDirectory(File file) {
    throw new IllegalStateException();
  }

  @Override
  public MavenExecutionRequest setToolchains(Map<String, List<ToolchainModel>> toolchains) {
    throw new IllegalStateException();
  }

}
