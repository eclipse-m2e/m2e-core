/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenExecutionRequest;

import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;


/**
 * The only reason this class exists is to implement async refresh(MavenUpdateRequest) without introducing circular
 * dependency between ProjectRegistryRefreshJob and ProjectRegistryManager. Otherwise, all requests are forwarded to
 * ProjectRegistryManager as is.
 */
public class MavenProjectManager implements IMavenProjectRegistry {
  public static final String STATE_FILENAME = "workspacestate.properties"; //$NON-NLS-1$

  private final ProjectRegistryManager manager;

  private final ProjectRegistryRefreshJob mavenBackgroundJob;

  private final File workspaceStateFile;

  public MavenProjectManager(ProjectRegistryManager manager, ProjectRegistryRefreshJob mavenBackgroundJob,
      File stateLocation) {
    this.manager = manager;
    this.mavenBackgroundJob = mavenBackgroundJob;
    this.workspaceStateFile = new File(stateLocation, STATE_FILENAME);
  }

  // Maven projects    

  public void refresh(MavenUpdateRequest request) {
    mavenBackgroundJob.refresh(request);
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public void refresh(MavenUpdateRequest request, IProgressMonitor monitor) throws CoreException {
    manager.refresh(request, monitor);
  }

  public void refresh(Collection<IFile> pomFiles, IProgressMonitor monitor) throws CoreException {
    manager.refresh(pomFiles, monitor);
  }

  public void addMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    manager.addMavenProjectChangedListener(listener);
  }

  public void removeMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    manager.removeMavenProjectChangedListener(listener);
  }

  public IMavenProjectFacade create(IFile pom, boolean load, IProgressMonitor monitor) {
    return manager.create(pom, load, monitor);
  }

  public IMavenProjectFacade create(IProject project, IProgressMonitor monitor) {
    return manager.create(project, monitor);
  }

  public IMavenProjectFacade[] getProjects() {
    return manager.getProjects();
  }

  //XXX mkleint: this only returns a correct facade for the project's own pom.xml, if the POM file is nested, the result is wrong.
  public IMavenProjectFacade getProject(IProject project) {
    return manager.getProject(project);
  }

  //XXX mkleint: what happens when multiple workspace projects have the same coordinates?!?
  public IMavenProjectFacade getMavenProject(String groupId, String artifactId, String version) {
    return manager.getMavenProject(groupId, artifactId, version);
  }

  public File getWorkspaceStateFile() {
    return workspaceStateFile;
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public MavenExecutionRequest createExecutionRequest(IFile pom, ResolverConfiguration resolverConfiguration,
      IProgressMonitor monitor) throws CoreException {
    return manager.createExecutionRequest(pom, resolverConfiguration, monitor);
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public MavenExecutionRequest createExecutionRequest(IMavenProjectFacade project, IProgressMonitor monitor)
      throws CoreException {
    return createExecutionRequest(project.getPom(), project.getResolverConfiguration(), monitor);
  }

  public <V> V execute(final IMavenProjectFacade facade, final ICallable<V> callable, IProgressMonitor monitor)
      throws CoreException {
    MavenExecutionContext context = manager.createExecutionContext(facade.getPom(), facade.getResolverConfiguration());
    return context.execute(new ICallable<V>() {
      public V call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        return context.execute(facade.getMavenProject(monitor), callable, monitor);
      }
    }, monitor);
  }
}
