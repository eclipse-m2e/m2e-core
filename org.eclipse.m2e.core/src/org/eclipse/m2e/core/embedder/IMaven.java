/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Christoph Läubrich  - remove IMavenExecutionContext getExecutionContext()
 *                          - remove  IMaven.populateDefaults(MavenExecutionRequest)
 *******************************************************************************/

package org.eclipse.m2e.core.embedder;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
import org.apache.maven.wagon.proxy.ProxyInfo;

import org.eclipse.m2e.core.internal.IMavenToolbox;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


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
public interface IMaven extends IComponentLookup {

  // POM Model read/write operations

  /**
   * @deprecated use the {@link MavenModelManager} instead
   */
  @Deprecated(forRemoval = true)
  default Model readModel(InputStream in) throws CoreException {
    return IMavenToolbox.of(this).readModel(in);
  }

  default void writeModel(Model model, OutputStream out) throws CoreException {
    IMavenToolbox.of(this).writeModel(model, out);
  }

  // artifact resolution

  /**
   * Resolves specified artifact from specified remote repositories.
   *
   * @return Artifact resolved artifact
   * @throws CoreException if the artifact cannot be resolved.
   */
  Artifact resolve(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> artifactRepositories, IProgressMonitor monitor) throws CoreException;

  /**
   * Returns path of the specified artifact relative to repository baseDir. Can use used to access local repository
   * files bypassing maven resolution logic.
   */
  String getArtifactPath(ArtifactRepository repository, String groupId, String artifactId, String version,
      String type, String classifier) throws CoreException;

  /**
   * Returns true if the artifact does NOT exist in the local repository and known to be UNavailable from all specified
   * repositories.
   */
  boolean isUnavailable(String groupId, String artifactId, String version, String type, String classifier,
      List<ArtifactRepository> repositories) throws CoreException;

  // read MavenProject

  MavenProject readProject(File pomFile, IProgressMonitor monitor) throws CoreException;

  /**
   * @since 1.4
   * @see {@link #readMavenProjects(File, ProjectBuildingRequest)} to group requests and improve performance (RAM and
   *      CPU)
   * @deprecated this method should never have been API and is prone to errors, if you still need this method please
   *             contact the m2e team to provide better alternatives for your use-case
   */
  @Deprecated(forRemoval = true)
  MavenExecutionResult readMavenProject(File pomFile, ProjectBuildingRequest configuration) throws CoreException;

  /**
   * @since 1.10
   * @deprecated this method should never have been API and is prone to errors, if you still need this method please
   *             contact the m2e team to provide better alternatives for your use-case
   */
  @Deprecated(forRemoval = true)
  Map<File, MavenExecutionResult> readMavenProjects(Collection<File> pomFiles,
      ProjectBuildingRequest configuration)
      throws CoreException;

  /**
   * this method is a noop now
   */
  @Deprecated(forRemoval = true)
  void detachFromSession(MavenProject project) throws CoreException;

  // execution

  /**
   * @deprecated replaced with direct usage of {@link IMavenExecutionContext}.
   * @since 1.4
   */
  @Deprecated(forRemoval = true)
  default void execute(MavenProject project, MojoExecution execution, IProgressMonitor monitor) throws CoreException {
    IMavenExecutionContext.getThreadContext().orElseGet(this::createExecutionContext).execute(project, execution,
        monitor);
  }

  /**
   * @since 1.4
   * @Deprecated use {@link IMavenProjectFacade#calculateExecutionPlan(Collection, IProgressMonitor)} or
   *             {@link IMavenProjectFacade#setupExecutionPlan(Collection, IProgressMonitor)} instead
   */
  @Deprecated(forRemoval = true)
  MavenExecutionPlan calculateExecutionPlan(MavenProject project, List<String> goals, boolean setup,
      IProgressMonitor monitor) throws CoreException;

  /**
   * @since 1.4
   * @deprecated only used internally
   */
  @Deprecated(forRemoval = true)
  MojoExecution setupMojoExecution(MavenProject project, MojoExecution execution, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Resolves a configuration parameter from the given {@code mojoExecution}. It coerces from String to the given type
   * and considers expressions and default values.
   * 
   * @param <T>
   * @param project the Maven project
   * @param mojoExecution the mojo execution from which to retrieve the configuration value
   * @param parameter the name of the parameter (may be nested with separating {@code .})
   * @param asType the type to coerce to
   * @param monitor the progress monitor
   * @return the parameter value or {@code null} if the parameter with the given name was not found
   * @throws CoreException
   * @since 1.4
   */
  <T> T getMojoParameterValue(MavenProject project, MojoExecution mojoExecution, String parameter,
      Class<T> asType, IProgressMonitor monitor) throws CoreException;

  /**
   * @since 1.4
   */
  <T> T getMojoParameterValue(MavenProject project, String parameter, Class<T> type, Plugin plugin,
      ConfigurationContainer configuration, String goal, IProgressMonitor monitor) throws CoreException;

  // configuration

  /**
   * TODO should we expose Settings or provide access to servers and proxies instead?
   */
  Settings getSettings() throws CoreException;

  Settings getSettings(MavenSettingsLocations locations) throws CoreException;

  String getLocalRepositoryPath();

  ArtifactRepository getLocalRepository() throws CoreException;

  ArtifactRepository createArtifactRepository(String id, String url) throws CoreException;

  /**
   * Convenience method, fully equivalent to getArtifactRepositories(true)
   */
  List<ArtifactRepository> getArtifactRepositories() throws CoreException;

  /**
   * Returns list of remote artifact repositories configured in settings.xml. Only profiles active by default are
   * considered when calculating the list. If injectSettings=true, mirrors, authentication and proxy info will be
   * injected. If injectSettings=false, raw repository definition will be used.
   */
  List<ArtifactRepository> getArtifactRepositories(boolean injectSettings) throws CoreException;

  List<ArtifactRepository> getPluginArtifactRepositories() throws CoreException;

  List<ArtifactRepository> getPluginArtifactRepositories(boolean injectSettings) throws CoreException;

  /**
   * @deprecated use {@link #getSettings(MavenSettingsLocations)} instead
   */
  @Deprecated(forRemoval = true)
  Settings buildSettings(String globalSettings, String userSettings) throws CoreException;

  void writeSettings(Settings settings, OutputStream out) throws CoreException;

  List<SettingsProblem> validateSettings(String settings);

  List<Mirror> getMirrors() throws CoreException;

  Mirror getMirror(ArtifactRepository repo) throws CoreException;

  void reloadSettings() throws CoreException;

  Server decryptPassword(Server server) throws CoreException;

  /** @provisional */
  void addLocalRepositoryListener(ILocalRepositoryListener listener);

  /** @provisional */
  void removeLocalRepositoryListener(ILocalRepositoryListener listener);

  ProxyInfo getProxyInfo(String protocol) throws CoreException;

  /**
   * Sort projects by build order
   */
  List<MavenProject> getSortedProjects(List<MavenProject> projects) throws CoreException;

  String resolvePluginVersion(String groupId, String artifactId, MavenSession session) throws CoreException;

  /**
   * Returns new mojo instances configured according to provided mojoExecution. Caller must release returned mojo with
   * {@link #releaseMojo(Object, MojoExecution)}. This method is intended to allow introspection of mojo configuration
   * parameters, use {@link #execute(MavenSession, MojoExecution, IProgressMonitor)} to execute mojo.
   */
  <T> T getConfiguredMojo(MavenSession session, MojoExecution mojoExecution, Class<T> clazz)
      throws CoreException;

  /**
   * Releases resources used by Mojo acquired with {@link #getConfiguredMojo(MavenSession, MojoExecution, Class)}
   */
  void releaseMojo(Object mojo, MojoExecution mojoExecution) throws CoreException;

  /**
   * Gets class realm of the specified project.
   * 
   * @deprecated this method do not return the projects realm (what could be accessed by
   *             {@link MavenProject#getClassRealm()}. Use {@link IMavenProjectFacade#createExecutionContext()} if you
   *             want to execute/lookup components inside the projects realm!
   * @return The class realm of the specified project.
   */
  @Deprecated(forRemoval = true)
  ClassLoader getProjectRealm(MavenProject project);

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
   * @deprecated should be replaced with the fully equivalent code mentioned in this javadoc, e.g. inside a private util
   *             method
   * @since 1.4
   */
  @Deprecated(forRemoval = true)
  default <V> V execute(boolean offline, boolean forceDependencyUpdate, ICallable<V> callable, IProgressMonitor monitor)
      throws CoreException {
    return MavenImpl.execute(this, offline, forceDependencyUpdate, callable, monitor);
  }

  /**
   * Either joins existing session or starts new session with default configuration and executes the callable in the
   * context of the session.
   *
   * @deprecated replaced with direct usage of {@link IMavenExecutionContext}.
   * @since 1.4
   */
  @Deprecated(forRemoval = true)
  default <V> V execute(ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    return IMavenExecutionContext.getThreadContext().orElseGet(this::createExecutionContext).execute(callable, monitor);
  }

  /**
   * Creates and returns new, possibly nested, maven execution context for this Maven embedder.
   * <p>
   * <b>IMPORTANT:</b> When in the context of a particular project, it's usually better to use
   * {@link IMavenProjectFacade#createExecutionContext()} which will include more project-specific configuration and
   * will lead to more accurate and consistent results compared to Maven CLI commands.
   * </p>
   *
   * @since 1.4
   * @see IMavenProjectFacade#createExecutionContext()
   */
  IMavenExecutionContext createExecutionContext();

}
