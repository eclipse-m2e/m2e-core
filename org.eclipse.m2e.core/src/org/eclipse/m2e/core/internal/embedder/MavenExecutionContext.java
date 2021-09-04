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

import static org.eclipse.m2e.core.internal.M2EUtils.copyProperties;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.session.scope.internal.SessionScope;

import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;


/**
 * @since 1.4
 */
public class MavenExecutionContext implements IMavenExecutionContext {

  private static final String CTX_PREFIX = MavenExecutionContext.class.getName();

  private static final String CTX_LOCALREPOSITORY = CTX_PREFIX + "/localRepository";

  private static final String CTX_MAVENSESSION = CTX_PREFIX + "/mavenSession";

  private static final String CTX_REPOSITORYSESSION = CTX_PREFIX + "/repositorySession";

  private static final ThreadLocal<Deque<MavenExecutionContext>> threadLocal = new ThreadLocal<>();

  private final MavenImpl maven;

  private MavenExecutionRequest request;

  // TODO maybe delegate to parent context
  private Map<String, Object> context;

  public MavenExecutionContext(MavenImpl maven) {
    this.maven = maven;
  }

  @Override
  public MavenExecutionRequest getExecutionRequest() throws CoreException {
    if(request != null && context != null) {
      return new ReadonlyMavenExecutionRequest(request);
    }
    if(request == null) {
      request = newExecutionRequest();
    }
    return request;
  }

  protected MavenExecutionRequest newExecutionRequest() throws CoreException {
    MavenExecutionRequest request = null;
    Deque<MavenExecutionContext> stack = threadLocal.get();
    if(stack != null && !stack.isEmpty()) {
      MavenExecutionRequest parent = stack.peek().request;
      if(parent == null) {
        throw new IllegalStateException(); //
      }
      request = DefaultMavenExecutionRequest.copy(parent);
    }
    if(request == null) {
      request = maven.createExecutionRequest();
    }

    return request;
  }

  @Override
  public <V> V execute(ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    return execute(null, callable, monitor);
  }

  @Override
  public <V> V execute(MavenProject project, ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    Deque<MavenExecutionContext> stack = threadLocal.get();
    if(stack == null) {
      stack = new ArrayDeque<>();
      threadLocal.set(stack);
    }
    final MavenExecutionContext parent = stack.peek();

    if(this == parent) {
      // shortcut the setup logic, this is nested invocation of the same context
      return executeBare(project, callable, monitor);
    }

    // remember original configuration to "pop" the session stack properly
    final MavenExecutionRequest origRequest = request;
    final Map<String, Object> origContext = context;

    if(request == null && parent != null) {
      this.request = parent.request;
      this.context = new HashMap<>(parent.context);
    } else {
      this.context = new HashMap<>();
      if(request == null) {
        request = newExecutionRequest();
      }
      maven.populateDefaults(request);
      populateSystemProperties(request);
      setValue(CTX_LOCALREPOSITORY, request.getLocalRepository());
      final FilterRepositorySystemSession repositorySession = maven.createRepositorySession(request);
      setValue(CTX_REPOSITORYSESSION, repositorySession);
      if(parent != null) {
        repositorySession.setData(parent.getRepositorySession().getData());
      }
      final MavenExecutionResult result = new DefaultMavenExecutionResult();
      setValue(CTX_MAVENSESSION, new MavenSession(maven.getPlexusContainer(), repositorySession, request, result));
    }

    final LegacySupport legacySupport = maven.lookup(LegacySupport.class);
    final MavenSession origLegacySession = legacySupport.getSession(); // TODO validate == origSession

    stack.push(this);

    final MavenSession session = getSession();
    legacySupport.setSession(session);
    final SessionScope sessionScope = maven.lookup(SessionScope.class);
    sessionScope.enter();
    sessionScope.seed(MavenSession.class, session);

    try {
      return executeBare(project, callable, monitor);
    } finally {
      sessionScope.exit();
      stack.pop();
      if(stack.isEmpty()) {
        threadLocal.set(null); // TODO decide if this is useful
      }
      legacySupport.setSession(origLegacySession);
      request = origRequest;
      context = origContext;
    }
  }

  private <V> V executeBare(MavenProject project, ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    final MavenSession mavenSession = getSession();
    final FilterRepositorySystemSession repositorySession = getRepositorySession();
    final TransferListener origTransferListener = repositorySession.setTransferListener(maven
        .createArtifactTransferListener(monitor));
    final MavenProject origProject = mavenSession.getCurrentProject();
    final List<MavenProject> origProjects = mavenSession.getProjects();
    final ClassLoader origTCCL = Thread.currentThread().getContextClassLoader();
    try {
      if(project != null) {
        mavenSession.setCurrentProject(project);
        mavenSession.setProjects(Collections.singletonList(project));
      }
      return callable.call(this, monitor);
    } finally {
      Thread.currentThread().setContextClassLoader(origTCCL);
      repositorySession.setTransferListener(origTransferListener);
      if(project != null) {
        mavenSession.setCurrentProject(origProject);
        mavenSession.setProjects(origProjects != null ? origProjects : Collections.<MavenProject> emptyList());
      }
    }
  }

  @Override
  public MavenSession getSession() {
    if(context == null) {
      throw new IllegalStateException();
    }
    return getValue(CTX_MAVENSESSION);
  }

  @Override
  public ArtifactRepository getLocalRepository() {
    if(context == null) {
      throw new IllegalStateException();
    }
    return getValue(CTX_LOCALREPOSITORY);
  }

  @Override
  public FilterRepositorySystemSession getRepositorySession() {
    if(context == null) {
      throw new IllegalStateException();
    }
    return getValue(CTX_REPOSITORYSESSION);
  }

  public static MavenExecutionContext getThreadContext() {
    return getThreadContext(true);
  }

  /**
   * @since 1.5
   */
  public static MavenExecutionContext getThreadContext(boolean innermost) {
    final Deque<MavenExecutionContext> stack = threadLocal.get();
    return stack != null ? (innermost ? stack.peekFirst() : stack.peekLast()) : null;
  }

  public static void populateSystemProperties(MavenExecutionRequest request) {
    // temporary solution for https://issues.sonatype.org/browse/MNGECLIPSE-1607
    // oddly, there are no unit tests that fail if this is commented out
    if(request.getSystemProperties() == null || request.getSystemProperties().isEmpty()) {
      Properties systemProperties = new Properties();
      EnvironmentUtils.addEnvVars(systemProperties);
      copyProperties(systemProperties, System.getProperties());
      MavenProperties.setProperties(systemProperties);
      request.setSystemProperties(systemProperties);
    }
  }

  /*
   * <rant>Maven core does not provide good separation between session state, i.e. caches, settings, etc, and project
   * building configuration, i.e. if dependencies should be resolve, resolution leniency, etc. On top of that, there is
   * no easy way to create new populated ProjectBuildingRequest instances. Otherwise this method would not be
   * needed.</rant>
   */
  @Override
  public ProjectBuildingRequest newProjectBuildingRequest() {
    DefaultProjectBuildingRequest projectBuildingRequest = new DefaultProjectBuildingRequest();
    projectBuildingRequest.setLocalRepository(getLocalRepository());
    projectBuildingRequest.setRepositorySession(getRepositorySession());
    projectBuildingRequest.setSystemProperties(request.getSystemProperties());
    projectBuildingRequest.setUserProperties(request.getUserProperties());
    projectBuildingRequest.setRemoteRepositories(request.getRemoteRepositories());
    projectBuildingRequest.setPluginArtifactRepositories(request.getPluginArtifactRepositories());
    projectBuildingRequest.setActiveProfileIds(request.getActiveProfiles());
    projectBuildingRequest.setInactiveProfileIds(request.getInactiveProfiles());
    projectBuildingRequest.setProfiles(request.getProfiles());
    projectBuildingRequest.setProcessPlugins(true);
    projectBuildingRequest.setBuildStartTime(request.getStartTime());
    return projectBuildingRequest;
  }

  /**
   * Suspends current Maven execution context, if any. Returns suspended context or {@code null} if there was no context
   * associated with the current thread.
   *
   * @see #resume(Deque)
   * @since 1.5
   */
  public static Deque<MavenExecutionContext> suspend() {
    Deque<MavenExecutionContext> queue = threadLocal.get();
    threadLocal.set(null);
    return queue;
  }

  /**
   * Resumes Maven execution context suspended with {@link #suspend()}.
   *
   * @see #resume(Deque)
   * @since 1.5
   */
  public static void resume(Deque<MavenExecutionContext> queue) {
    if(threadLocal.get() != null) {
      throw new IllegalStateException();
    }
    threadLocal.set(queue);
  }

  /**
   * @since 1.5
   */
  @SuppressWarnings("unchecked")
  public <T> T getValue(String key) {
    return (T) context.get(key);
  }

  /**
   * @since 1.5
   */
  public <T> void setValue(String key, T value) {
    context.put(key, value);
  }
}
