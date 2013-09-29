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

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.transfer.TransferListener;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Profile;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;


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

  public MavenExecutionRequest setBaseDirectory(File basedir) {
    throw new IllegalStateException();
  }

  public String getBaseDirectory() {
    return request.getBaseDirectory();
  }

  public MavenExecutionRequest setStartTime(Date start) {
    throw new IllegalStateException();
  }

  public Date getStartTime() {
    return request.getStartTime();
  }

  public MavenExecutionRequest setGoals(List<String> goals) {
    throw new IllegalStateException();
  }

  public List<String> getGoals() {
    return Collections.unmodifiableList(request.getGoals());
  }

  public MavenExecutionRequest setSystemProperties(Properties systemProperties) {
    throw new IllegalStateException();
  }

  public Properties getSystemProperties() {
    // TODO unmodifiable properties?
    return request.getSystemProperties();
  }

  public MavenExecutionRequest setUserProperties(Properties userProperties) {
    throw new IllegalStateException();
  }

  public Properties getUserProperties() {
    // TODO unmodifiable properties?
    return request.getUserProperties();
  }

  public MavenExecutionRequest setReactorFailureBehavior(String failureBehavior) {
    throw new IllegalStateException();
  }

  public String getReactorFailureBehavior() {
    return request.getReactorFailureBehavior();
  }

  public MavenExecutionRequest setSelectedProjects(List<String> projects) {
    throw new IllegalStateException();
  }

  public List<String> getSelectedProjects() {
    return Collections.unmodifiableList(request.getSelectedProjects());
  }

  public MavenExecutionRequest setResumeFrom(String project) {
    throw new IllegalStateException();
  }

  public String getResumeFrom() {
    return request.getResumeFrom();
  }

  public MavenExecutionRequest setMakeBehavior(String makeBehavior) {
    throw new IllegalStateException();
  }

  public String getMakeBehavior() {
    return request.getMakeBehavior();
  }

  public void setThreadCount(String threadCount) {
    throw new IllegalStateException();
  }

  public String getThreadCount() {
    return request.getThreadCount();
  }

  public boolean isThreadConfigurationPresent() {
    return request.isThreadConfigurationPresent();
  }

  public void setPerCoreThreadCount(boolean perCoreThreadCount) {
    throw new IllegalStateException();
  }

  public boolean isPerCoreThreadCount() {
    return request.isPerCoreThreadCount();
  }

  public MavenExecutionRequest setRecursive(boolean recursive) {
    throw new IllegalStateException();
  }

  public boolean isRecursive() {
    return request.isRecursive();
  }

  public MavenExecutionRequest setPom(File pom) {
    throw new IllegalStateException();
  }

  public File getPom() {
    return request.getPom();
  }

  public MavenExecutionRequest setShowErrors(boolean showErrors) {
    throw new IllegalStateException();
  }

  public boolean isShowErrors() {
    return request.isShowErrors();
  }

  public MavenExecutionRequest setTransferListener(TransferListener transferListener) {
    throw new IllegalStateException();
  }

  public TransferListener getTransferListener() {
    return request.getTransferListener();
  }

  public MavenExecutionRequest setLoggingLevel(int loggingLevel) {
    throw new IllegalStateException();
  }

  public int getLoggingLevel() {
    return request.getLoggingLevel();
  }

  public MavenExecutionRequest setUpdateSnapshots(boolean updateSnapshots) {
    throw new IllegalStateException();
  }

  public boolean isUpdateSnapshots() {
    return request.isUpdateSnapshots();
  }

  public MavenExecutionRequest setNoSnapshotUpdates(boolean noSnapshotUpdates) {
    throw new IllegalStateException();
  }

  public boolean isNoSnapshotUpdates() {
    return request.isNoSnapshotUpdates();
  }

  public MavenExecutionRequest setGlobalChecksumPolicy(String globalChecksumPolicy) {
    throw new IllegalStateException();
  }

  public String getGlobalChecksumPolicy() {
    return request.getGlobalChecksumPolicy();
  }

  public MavenExecutionRequest setLocalRepositoryPath(String localRepository) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest setLocalRepositoryPath(File localRepository) {
    throw new IllegalStateException();
  }

  public File getLocalRepositoryPath() {
    return request.getLocalRepositoryPath();
  }

  public MavenExecutionRequest setLocalRepository(ArtifactRepository repository) {
    throw new IllegalStateException();
  }

  public ArtifactRepository getLocalRepository() {
    return request.getLocalRepository();
  }

  public MavenExecutionRequest setInteractiveMode(boolean interactive) {
    throw new IllegalStateException();
  }

  public boolean isInteractiveMode() {
    return request.isInteractiveMode();
  }

  public MavenExecutionRequest setOffline(boolean offline) {
    throw new IllegalStateException();
  }

  public boolean isOffline() {
    return request.isOffline();
  }

  public boolean isCacheTransferError() {
    return request.isCacheTransferError();
  }

  public MavenExecutionRequest setCacheTransferError(boolean cacheTransferError) {
    throw new IllegalStateException();
  }

  public boolean isCacheNotFound() {
    return request.isCacheNotFound();
  }

  public MavenExecutionRequest setCacheNotFound(boolean cacheNotFound) {
    throw new IllegalStateException();
  }

  public List<Profile> getProfiles() {
    return Collections.unmodifiableList(request.getProfiles());
  }

  public MavenExecutionRequest addProfile(Profile profile) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest setProfiles(List<Profile> profiles) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addActiveProfile(String profile) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addActiveProfiles(List<String> profiles) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest setActiveProfiles(List<String> profiles) {
    throw new IllegalStateException();
  }

  public List<String> getActiveProfiles() {
    return Collections.unmodifiableList(request.getActiveProfiles());
  }

  public MavenExecutionRequest addInactiveProfile(String profile) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addInactiveProfiles(List<String> profiles) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest setInactiveProfiles(List<String> profiles) {
    throw new IllegalStateException();
  }

  public List<String> getInactiveProfiles() {
    return Collections.unmodifiableList(request.getInactiveProfiles());
  }

  public List<Proxy> getProxies() {
    return Collections.unmodifiableList(request.getProxies());
  }

  public MavenExecutionRequest setProxies(List<Proxy> proxies) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addProxy(Proxy proxy) {
    throw new IllegalStateException();
  }

  public List<Server> getServers() {
    return Collections.unmodifiableList(request.getServers());
  }

  public MavenExecutionRequest setServers(List<Server> servers) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addServer(Server server) {
    throw new IllegalStateException();
  }

  public List<Mirror> getMirrors() {
    return Collections.unmodifiableList(request.getMirrors());
  }

  public MavenExecutionRequest setMirrors(List<Mirror> mirrors) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addMirror(Mirror mirror) {
    throw new IllegalStateException();
  }

  public List<String> getPluginGroups() {
    return Collections.unmodifiableList(request.getPluginGroups());
  }

  public MavenExecutionRequest setPluginGroups(List<String> pluginGroups) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addPluginGroup(String pluginGroup) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addPluginGroups(List<String> pluginGroups) {
    throw new IllegalStateException();
  }

  public boolean isProjectPresent() {
    return request.isProjectPresent();
  }

  public MavenExecutionRequest setProjectPresent(boolean isProjectPresent) {
    throw new IllegalStateException();
  }

  public File getUserSettingsFile() {
    return request.getUserSettingsFile();
  }

  public MavenExecutionRequest setUserSettingsFile(File userSettingsFile) {
    throw new IllegalStateException();
  }

  public File getGlobalSettingsFile() {
    return request.getGlobalSettingsFile();
  }

  public MavenExecutionRequest setGlobalSettingsFile(File globalSettingsFile) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addRemoteRepository(ArtifactRepository repository) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest addPluginArtifactRepository(ArtifactRepository repository) {
    throw new IllegalStateException();
  }

  public MavenExecutionRequest setRemoteRepositories(List<ArtifactRepository> repositories) {
    throw new IllegalStateException();
  }

  public List<ArtifactRepository> getRemoteRepositories() {
    return Collections.unmodifiableList(request.getRemoteRepositories());
  }

  public MavenExecutionRequest setPluginArtifactRepositories(List<ArtifactRepository> repositories) {
    throw new IllegalStateException();
  }

  public List<ArtifactRepository> getPluginArtifactRepositories() {
    return Collections.unmodifiableList(request.getPluginArtifactRepositories());
  }

  public MavenExecutionRequest setRepositoryCache(RepositoryCache repositoryCache) {
    throw new IllegalStateException();
  }

  public RepositoryCache getRepositoryCache() {
    return request.getRepositoryCache();
  }

  public WorkspaceReader getWorkspaceReader() {
    return request.getWorkspaceReader();
  }

  public MavenExecutionRequest setWorkspaceReader(WorkspaceReader workspaceReader) {
    throw new IllegalStateException();
  }

  public File getUserToolchainsFile() {
    return request.getUserToolchainsFile();
  }

  public MavenExecutionRequest setUserToolchainsFile(File userToolchainsFile) {
    throw new IllegalStateException();
  }

  public ExecutionListener getExecutionListener() {
    return request.getExecutionListener();
  }

  public MavenExecutionRequest setExecutionListener(ExecutionListener executionListener) {
    throw new IllegalStateException();
  }

  public ProjectBuildingRequest getProjectBuildingRequest() {
    // TODO unmodifiable ProjectBuildingRequest
    return request.getProjectBuildingRequest();
  }

  /* (non-Javadoc)
   * @see org.apache.maven.execution.MavenExecutionRequest#isUseLegacyLocalRepository()
   */
  public boolean isUseLegacyLocalRepository() {
    return request.isUseLegacyLocalRepository();
  }

  /* (non-Javadoc)
   * @see org.apache.maven.execution.MavenExecutionRequest#setUseLegacyLocalRepository(boolean)
   */
  public MavenExecutionRequest setUseLegacyLocalRepository(boolean useLegacyRepository) {
    return request.setUseLegacyLocalRepository(useLegacyRepository);
  }

}
