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

package org.eclipse.m2e.core.embedder;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.ConfigurationContainer;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;


/**
 * Entry point for all Maven functionality in m2e. Note that this component does not directly support workspace artifact
 * resolution.
 * <p>
 * Unless specified otherwise or implied by method parameters, IMaven methods will join {@link IMavenExecutionContext}
 * associated with the current thread or create new default {@link IMavenExecutionContext}.
 * 
 * @author igor
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMaven {

  /**
   * Creates new Maven execution request. This method is not long running, but created execution request is configured
   * to report progress to provided progress monitor. Monitor can be null.
   * 
   * @deprecated see {@link IMavenExecutionContext}.
   */
  public MavenExecutionRequest createExecutionRequest(IProgressMonitor monitor) throws CoreException;

  // POM Model read/write operations

  public Model readModel(InputStream in) throws CoreException;

  /**
   * Using {@link File} representations in Eclipse workspaces is prone to errors, since remote filesystems must be
   * cached via {@link IFileStore#toLocalFile}. Simple transformations via {@link IPath#toFile()} do not work for remote
   * files.
   * 
   * @deprecated use {@link #readModel(InputStream)} instead.
   */
  @Deprecated
  public Model readModel(File pomFile) throws CoreException;

  public void writeModel(Model model, OutputStream out) throws CoreException;

  // artifact resolution

  /**
   * Resolves specified artifact from specified remote repositories.
   * 
   * @return Artifact resolved artifact
   * @throws CoreException if the artifact cannot be resolved.
   */
  public Artifact resolve(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> artifactRepositories, IProgressMonitor monitor) throws CoreException;

  /**
   * Returns path of the specified artifact relative to repository baseDir. Can use used to access local repository
   * files bypassing maven resolution logic.
   */
  public String getArtifactPath(ArtifactRepository repository, String groupId, String artifactId, String version,
      String type, String classifier) throws CoreException;

  /**
   * Returns true if the artifact does NOT exist in the local repository and known to be UNavailable from all specified
   * repositories.
   */
  public boolean isUnavailable(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> repositories) throws CoreException;

  // read MavenProject

  public MavenProject readProject(File pomFile, IProgressMonitor monitor) throws CoreException;

  /**
   * @deprecated this method does not properly join {@link IMavenExecutionContext}, use
   *             {@link #readMavenProject(File, IProgressMonitor)} instead.
   */
  public MavenExecutionResult readProject(MavenExecutionRequest request, IProgressMonitor monitor) throws CoreException;

  /**
   * @since 1.4
   * @see {@link #readMavenProjects(File, ProjectBuildingRequest)} to group requests and improve performance (RAM and
   *      CPU)
   */
  public MavenExecutionResult readMavenProject(File pomFile, ProjectBuildingRequest configuration) throws CoreException;

  /**
   * @since 1.10
   */
  public Map<File, MavenExecutionResult> readMavenProjects(Collection<File> pomFiles,
      ProjectBuildingRequest configuration)
      throws CoreException;

  /**
   * Makes MavenProject instances returned by #readProject methods suitable for caching and reuse with other
   * MavenSession instances.<br/>
   * Do note that MavenProject.getParentProject() cannot be used for detached MavenProject instances,
   * #resolveParentProject to read parent project instance.
   */
  public void detachFromSession(MavenProject project) throws CoreException;

  /**
   * Returns MavenProject parent project or null if no such project.
   * 
   * @deprecated this method does not properly join {@link IMavenExecutionContext}, use
   *             {@link #resolveParentProject(MavenProject, IProgressMonitor)} instead.
   * @TODO Currently returns null in case of resolution error, consider if it should throw CoreException instead
   */
  public MavenProject resolveParentProject(MavenExecutionRequest request, MavenProject project, IProgressMonitor monitor)
      throws CoreException;

  public MavenProject resolveParentProject(MavenProject project, IProgressMonitor monitor) throws CoreException;

  // execution

  /**
   * @deprecated this method does not properly join {@link IMavenExecutionContext}
   */
  public MavenExecutionResult execute(MavenExecutionRequest request, IProgressMonitor monitor);

  /**
   * @deprecated this method does not properly join {@link IMavenExecutionContext}
   */
  public MavenSession createSession(MavenExecutionRequest request, MavenProject project);

  /**
   * @deprecated this method does not properly join {@link IMavenExecutionContext}, use
   *             {@link #execute(MojoExecution, IProgressMonitor)} instead.
   */
  public void execute(MavenSession session, MojoExecution execution, IProgressMonitor monitor);

  /**
   * @since 1.4
   */
  public void execute(MavenProject project, MojoExecution execution, IProgressMonitor monitor) throws CoreException;

  /**
   * @deprecated this method does not properly join {@link IMavenExecutionContext}, use
   *             {@link #calculateExecutionPlan(MavenProject, List, boolean, IProgressMonitor)} instead.
   */
  public MavenExecutionPlan calculateExecutionPlan(MavenSession session, MavenProject project, List<String> goals,
      boolean setup, IProgressMonitor monitor) throws CoreException;

  /**
   * @since 1.4
   */
  public MavenExecutionPlan calculateExecutionPlan(MavenProject project, List<String> goals, boolean setup,
      IProgressMonitor monitor) throws CoreException;

  /**
   * @deprecated this method does not properly join {@link IMavenExecutionContext}, use
   *             {@link #setupMojoExecution(MavenProject, MojoExecution)} instead.
   */
  public MojoExecution setupMojoExecution(MavenSession session, MavenProject project, MojoExecution execution)
      throws CoreException;

  /**
   * @since 1.4
   */
  public MojoExecution setupMojoExecution(MavenProject project, MojoExecution execution, IProgressMonitor monitor)
      throws CoreException;

  /**
   * @deprecated this method does not properly join {@link IMavenExecutionContext}, use
   *             {@link #getMojoParameterValue(MojoExecution, String, Class)} instead.
   */
  public <T> T getMojoParameterValue(MavenSession session, MojoExecution mojoExecution, String parameter,
      Class<T> asType) throws CoreException;

  /**
   * @since 1.4
   */
  public <T> T getMojoParameterValue(MavenProject project, MojoExecution mojoExecution, String parameter,
      Class<T> asType, IProgressMonitor monitor) throws CoreException;

  /**
   * @deprecated this method does not properly join {@link IMavenExecutionContext}, use
   *             {@link #getMojoParameterValue(String, Class, Plugin, ConfigurationContainer, String)} instead.
   */
  public <T> T getMojoParameterValue(String parameter, Class<T> type, MavenSession session, Plugin plugin,
      ConfigurationContainer configuration, String goal) throws CoreException;

  /**
   * @since 1.4
   */
  public <T> T getMojoParameterValue(MavenProject project, String parameter, Class<T> type, Plugin plugin,
      ConfigurationContainer configuration, String goal, IProgressMonitor monitor) throws CoreException;

  // configuration

  /**
   * TODO should we expose Settings or provide access to servers and proxies instead?
   */
  public Settings getSettings() throws CoreException;

  public String getLocalRepositoryPath();

  public ArtifactRepository getLocalRepository() throws CoreException;

  public void populateDefaults(MavenExecutionRequest request) throws CoreException;

  public ArtifactRepository createArtifactRepository(String id, String url) throws CoreException;

  /**
   * Convenience method, fully equivalent to getArtifactRepositories(true)
   */
  public List<ArtifactRepository> getArtifactRepositories() throws CoreException;

  /**
   * Returns list of remote artifact repositories configured in settings.xml. Only profiles active by default are
   * considered when calculating the list. If injectSettings=true, mirrors, authentication and proxy info will be
   * injected. If injectSettings=false, raw repository definition will be used.
   */
  public List<ArtifactRepository> getArtifactRepositories(boolean injectSettings) throws CoreException;

  public List<ArtifactRepository> getPluginArtifactRepositories() throws CoreException;

  public List<ArtifactRepository> getPluginArtifactRepositories(boolean injectSettings) throws CoreException;

  public Settings buildSettings(String globalSettings, String userSettings) throws CoreException;

  public void writeSettings(Settings settings, OutputStream out) throws CoreException;

  public List<SettingsProblem> validateSettings(String settings);

  public List<Mirror> getMirrors() throws CoreException;

  public Mirror getMirror(ArtifactRepository repo) throws CoreException;

  public void addSettingsChangeListener(ISettingsChangeListener listener);

  public void removeSettingsChangeListener(ISettingsChangeListener listener);

  public void reloadSettings() throws CoreException;

  public Server decryptPassword(Server server) throws CoreException;

  /** @provisional */
  public void addLocalRepositoryListener(ILocalRepositoryListener listener);

  /** @provisional */
  public void removeLocalRepositoryListener(ILocalRepositoryListener listener);

  /**
   * Creates wagon TransferListener that can be used with Archetype, NexusIndexer and other components that use wagon
   * API directly. The listener will adopt wagon transfer events to corresponding calls to IProgressMonitor and all
   * registered ILocalRepositoryListeners.
   * 
   * @deprecated IMaven API should not expose maven.repository.ArtifactTransferListener
   */
  public TransferListener createTransferListener(IProgressMonitor monitor);

  public ProxyInfo getProxyInfo(String protocol) throws CoreException;

  /**
   * Sort projects by build order
   */
  public List<MavenProject> getSortedProjects(List<MavenProject> projects) throws CoreException;

  public String resolvePluginVersion(String groupId, String artifactId, MavenSession session) throws CoreException;

  /**
   * Returns new mojo instances configured according to provided mojoExecution. Caller must release returned mojo with
   * {@link #releaseMojo(Object, MojoExecution)}. This method is intended to allow introspection of mojo configuration
   * parameters, use {@link #execute(MavenSession, MojoExecution, IProgressMonitor)} to execute mojo.
   */
  public <T> T getConfiguredMojo(MavenSession session, MojoExecution mojoExecution, Class<T> clazz)
      throws CoreException;

  /**
   * Releases resources used by Mojo acquired with {@link #getConfiguredMojo(MavenSession, MojoExecution, Class)}
   */
  void releaseMojo(Object mojo, MojoExecution mojoExecution) throws CoreException;

  /**
   * Gets class realm of the specified project.
   * 
   * @return The class realm of the specified project.
   */
  ClassLoader getProjectRealm(MavenProject project);

  // execution context

  /**
   * This is convenience method fully equivalent to
   * 
   * <pre>
   * IMavenExecutionContext context = createExecutionContext();
   * context.getExecutionRequest().setOffline(offline);
   * context.getExecutionRequest().setUpdateSnapshots(forceDependencyUpdate);
   * return context.execute(callable, monitor);
   * </pre>
   * 
   * @since 1.4
   */
  <V> V execute(boolean offline, boolean forceDependencyUpdate, ICallable<V> callable, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Either joins existing session or starts new session with default configuration and executes the callable in the
   * context of the session.
   * 
   * @since 1.4
   */
  <V> V execute(ICallable<V> callable, IProgressMonitor monitor) throws CoreException;

  /**
   * Creates and returns new, possibly nested, maven execution context.
   * 
   * @since 1.4
   */
  IMavenExecutionContext createExecutionContext() throws CoreException;

  /**
   * Returns execution context associated with the current thread or <code>null</code> if the current thread does not
   * have associated maven execution context.
   * 
   * @since 1.4
   */
  IMavenExecutionContext getExecutionContext();

  /**
   * Lookup a component from the embedded PlexusContainer.
   * @param clazz the requested role
   * @return The component instance requested.
   * @throws CoreException if the requested component is not available
   * 
   * @since 1.10
   */
  <T> T lookup(Class<T> clazz) throws CoreException;
}
