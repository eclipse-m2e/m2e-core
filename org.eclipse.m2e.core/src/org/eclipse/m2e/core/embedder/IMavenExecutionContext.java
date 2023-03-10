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

package org.eclipse.m2e.core.embedder;

import java.io.File;
import java.util.Optional;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.PlexusContainerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * Maven execution context. Encapsulates maven execution request, maven session and related maven session state objects.
 * Instances can be either in configuration or execution states. In configuration state, {@link #getExecutionRequest()}
 * can be used to customize maven execution configuration parameters. Context enters execution state during
 * {@link #execute(..., IProgressMonitor)} invocation. During transition from configuration state to execution state,
 * maven session is created and the the context is associated with the current thread. Maven session instance and
 * related objects can be accessed through their corresponding context getXXX methods.
 * <p>
 * Maven execution contexts can be nested, i.e. new context can be created and entered from a thread that already has
 * associated maven execution context. By default nested contexts inherit all configuration from immediate outer
 * context, but can be customized via {@link #getExecutionRequest()}. Outer context is suspended during execution of
 * nested context's {@link #execute(ICallable, IProgressMonitor)} method.
 * <p>
 * Typical usage
 *
 * <pre>
 * IMavenExecutionContext context = mavenProjectFacade.createExecutionContext();
 *
 * MavenProject project = context.execute(new ICallable&lt;MavenProject&gt;() {
 *   public MavenProject call(IMavenExecutionContext context, IProgressMonitor monitor) {
 *     return maven.readMavenProject(pom, monitor);
 *   }
 * }, monitor);
 * </pre>
 * <p>
 * Maven execution context instances are not thread safe and cannot be used on other threads.
 *
 * @see ICallable
 * @see IMaven#createExecutionContext()
 * @see IMavenProjectRegistry#execute(org.eclipse.m2e.core.project.IMavenProjectFacade, ICallable, IProgressMonitor)
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 1.4
 */
public interface IMavenExecutionContext {

  /**
   * Can be called outside of {@link #execute(ICallable, IProgressMonitor)} to configure Maven session parameters. For
   * nested contexts, invocation of this method triggers creation of new nested session. When called during
   * {@link #execute(MavenProject, ICallable, IProgressMonitor)}, only getter request methods are allowed and all
   * request setter or modifier will through {@link IllegalStateException}.
   *
   * @since 1.4
   */
  MavenExecutionRequest getExecutionRequest() throws CoreException;

  /**
   * @since 1.4
   */
  <V> V execute(ICallable<V> callable, IProgressMonitor monitor) throws CoreException;

  /**
   * @since 1.4
   */
  <V> V execute(MavenProject project, ICallable<V> callable, IProgressMonitor monitor) throws CoreException;

  /**
   * @since 1.4
   */
  void execute(MavenProject project, MojoExecution execution, IProgressMonitor monitor) throws CoreException;

  /**
   * Execute the given {@link MavenExecutionRequest} in the context of m2eclipse and return the result, this could be
   * seen as an embedded run of the maven cli, at least it tries to replicate as much as possible from that.
   * 
   * @param request a {@link MavenExecutionRequest}
   * @return the result of the execution
   */
  MavenExecutionResult execute(MavenExecutionRequest request);

  /**
   * @throws IllegalStateException if called outside of {@link #execute(MavenProject,ICallable, IProgressMonitor)}
   * @since 1.4
   */
  MavenSession getSession();

  /**
   * @throws IllegalStateException if called outside of {@link #execute(MavenProject,ICallable, IProgressMonitor)}
   * @since 1.4
   */
  ArtifactRepository getLocalRepository();

  /**
   * @throws IllegalStateException if called outside of {@link #execute(MavenProject, ICallable, IProgressMonitor)}
   * @since 1.4
   */
  RepositorySystemSession getRepositorySession();

  /**
   * @since 1.4
   */
  ProjectBuildingRequest newProjectBuildingRequest();

  /**
   * @return the {@link IComponentLookup} to use for this context
   * @throws IllegalStateException if called outside of any execute call
   */
  IComponentLookup getComponentLookup();

  /**
   * Get the current thread context.
   * <p>
   * To trigger a new execution, it's often better to create a new project-specific context with
   * {@link IMavenProjectFacade#createExecutionContext()} than to blindly reuse current thread context, as we do not
   * know whether the current thread context maps the current project properly.
   * </p>
   * 
   * @return the IMavenExecutionContext already in use for current thread, or {@link Optional#empty()} if absent.
   * @since 2.0
   */
  static Optional<IMavenExecutionContext> getThreadContext() {
    return Optional.ofNullable(MavenExecutionContext.getThreadContext());
  }

  /**
   * Creates a detached execution context for the given base directory.
   * 
   * @param baseDir the basedir for what an execution context should be created
   * @return
   * @throws CoreException if creation failed
   */
  static IMavenExecutionContext of(File baseDir) throws CoreException {
    PlexusContainerManager containerManager = MavenPlugin.getMaven().lookup(PlexusContainerManager.class);
    try {
      //basedir scoped executions has a basedir but not a project supplier
      return new MavenExecutionContext(containerManager.getComponentLookup(baseDir), baseDir, null);
    } catch(Exception ex) {
      throw new CoreException(
          Status.error("Acquire container for basedir " + baseDir.getAbsolutePath() + " failed!", ex));
    }

  }


}
