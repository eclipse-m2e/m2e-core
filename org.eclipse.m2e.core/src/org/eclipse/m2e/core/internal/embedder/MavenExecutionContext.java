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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Properties;

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

import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.transfer.TransferListener;

import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;


/**
 * @since 1.4
 */
public class MavenExecutionContext implements IMavenExecutionContext {
  private static final ThreadLocal<Deque<MavenExecutionContext>> context = new ThreadLocal<Deque<MavenExecutionContext>>();

  private final MavenImpl maven;

  private MavenExecutionRequest request;

  private ArtifactRepository localRepository;

  private FilterRepositorySystemSession repositorySession;

  private MavenSession mavenSession;

  public MavenExecutionContext(MavenImpl maven) {
    this.maven = maven;
  }

  public MavenExecutionRequest getExecutionRequest() throws CoreException {
    if(request != null && mavenSession != null) {
      return new ReadonlyMavenExecutionRequest(request);
    }
    if(request == null) {
      request = newExecutionRequest();
    }
    return request;
  }

  protected MavenExecutionRequest newExecutionRequest() throws CoreException {
    MavenExecutionRequest request = null;
    Deque<MavenExecutionContext> stack = context.get();
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

  public <V> V execute(ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    return execute(null, callable, monitor);
  }

  public <V> V execute(MavenProject project, ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    Deque<MavenExecutionContext> stack = context.get();
    if(stack == null) {
      stack = new ArrayDeque<MavenExecutionContext>();
      context.set(stack);
    }
    final MavenExecutionContext parent = stack.peek();

    if(this == parent) {
      // shortcut the setup logic, this is nested invocation of the same context
      return executeBare(project, callable, monitor);
    }

    // remember original configuration to "pop" the session stack properly
    final ArtifactRepository origlocalRepository = localRepository;
    final FilterRepositorySystemSession origRepositorySession = repositorySession;
    final MavenSession origMavenSession = mavenSession;
    final MavenExecutionRequest origRequest = request;

    if(request == null && parent != null) {
      this.request = parent.request;
      this.localRepository = parent.localRepository;
      this.repositorySession = parent.repositorySession;
      this.mavenSession = parent.mavenSession;
    } else {
      if(request == null) {
        request = newExecutionRequest();
      }
      maven.populateDefaults(request);
      populateSystemProperties(request);
      this.localRepository = request.getLocalRepository();
      this.repositorySession = maven.createRepositorySession(request);
      if(parent != null) {
        this.repositorySession.setData(parent.repositorySession.getData());
      }
      final MavenExecutionResult result = new DefaultMavenExecutionResult();
      this.mavenSession = new MavenSession(maven.getPlexusContainer(), repositorySession, request, result);
    }

    final LegacySupport legacySupport = maven.lookup(LegacySupport.class);
    final MavenSession origLegacySession = legacySupport.getSession(); // TODO validate == origSession

    stack.push(this);
    legacySupport.setSession(mavenSession);
    try {
      return executeBare(project, callable, monitor);
    } finally {
      stack.pop();
      if(stack.isEmpty()) {
        context.set(null); // TODO decide if this is useful
      }
      legacySupport.setSession(origLegacySession);
      mavenSession = origMavenSession;
      repositorySession = origRepositorySession;
      localRepository = origlocalRepository;
      request = origRequest;
    }
  }

  private <V> V executeBare(MavenProject project, ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    final TransferListener origTransferListener = repositorySession.setTransferListener(maven
        .createArtifactTransferListener(monitor));
    final MavenProject origProject = mavenSession.getCurrentProject();
    final List<MavenProject> origProjects = mavenSession.getProjects();
    try {
      if(project != null) {
        mavenSession.setCurrentProject(project);
        mavenSession.setProjects(Collections.singletonList(project));
      }
      return callable.call(this, monitor);
    } finally {
      repositorySession.setTransferListener(origTransferListener);
      if(project != null) {
        mavenSession.setCurrentProject(origProject);
        mavenSession.setProjects(origProjects != null ? origProjects : Collections.<MavenProject> emptyList());
      }
    }
  }

  public MavenSession getSession() {
    if(mavenSession == null) {
      throw new IllegalStateException();
    }
    return mavenSession;
  }

  public ArtifactRepository getLocalRepository() {
    if(mavenSession == null) {
      throw new IllegalStateException();
    }
    return localRepository;
  }

  public RepositorySystemSession getRepositorySession() {
    if(mavenSession == null) {
      throw new IllegalStateException();
    }
    return repositorySession;
  }

  public static MavenExecutionContext getThreadContext() {
    final Deque<MavenExecutionContext> stack = context.get();
    return stack != null ? stack.peek() : null;
  }

  public static void populateSystemProperties(MavenExecutionRequest request) {
    // temporary solution for https://issues.sonatype.org/browse/MNGECLIPSE-1607
    // oddly, there are no unit tests that fail if this is commented out
    Properties systemProperties = new Properties();
    EnvironmentUtils.addEnvVars(systemProperties);
    systemProperties.putAll(System.getProperties());
    request.setSystemProperties(systemProperties);
  }

  /*
   * <rant>Maven core does not provide good separation between session state, i.e. caches, settings, etc, and project
   * building configuration, i.e. if dependencies should be resolve, resolution leniency, etc. On top of that, there is
   * no easy way to create new populated ProjectBuildingRequest instances. Otherwise this method would not be
   * needed.</rant>
   */
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
    Deque<MavenExecutionContext> queue = context.get();
    context.set(null);
    return queue;
  }

  /**
   * Resumes Maven execution context suspended with {@link #suspend()}.
   * 
   * @see #resume(Deque)
   * @since 1.5
   */
  public static void resume(Deque<MavenExecutionContext> queue) {
    if(context.get() != null) {
      throw new IllegalStateException();
    }
    context.set(queue);
  }

}
