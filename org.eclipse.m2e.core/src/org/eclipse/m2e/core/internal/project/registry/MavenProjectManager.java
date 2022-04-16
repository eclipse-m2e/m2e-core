/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenUpdateRequest;


/**
 * The only reason this class exists is to implement async refresh(MavenUpdateRequest) without introducing circular
 * dependency between ProjectRegistryRefreshJob and ProjectRegistryManager. Otherwise, all requests are forwarded to
 * ProjectRegistryManager as is.
 */
@Component(service = {IMavenProjectRegistry.class})
public class MavenProjectManager implements IMavenProjectRegistry {
  public static final String STATE_FILENAME = "workspacestate.properties"; //$NON-NLS-1$

  @Reference
  private ProjectRegistryManager manager;

  @Reference
  private ProjectRegistryRefreshJob mavenBackgroundJob;

  @Reference
  private IMavenConfiguration configuration;

  @Reference
  private IWorkspace workspace;

  private File workspaceStateFile;

  @Activate
  void init(BundleContext bundleContext) {
    IPath result = Platform.getStateLocation(bundleContext.getBundle());
    File bundleStateLocation = result.toFile();
    this.workspaceStateFile = new File(bundleStateLocation, STATE_FILENAME);
    boolean updateProjectsOnStartup = configuration.isUpdateProjectsOnStartup();
    if(updateProjectsOnStartup || manager.getProjects().length == 0) {
      refresh(new MavenUpdateRequest(workspace.getRoot().getProjects(), //
          configuration.isOffline() /*offline*/, false /* updateSnapshots */));
    }
  }

  // Maven projects

  @Override
  public void refresh(MavenUpdateRequest request) {
    mavenBackgroundJob.refresh(request);
  }

  @Override
  public void refresh(Collection<IFile> pomFiles, IProgressMonitor monitor) throws CoreException {
    manager.refresh(pomFiles, monitor);
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  @Override
  public void addMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    manager.addMavenProjectChangedListener(listener);
  }

  @Override
  public void removeMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    manager.removeMavenProjectChangedListener(listener);
  }

  @Override
  public IMavenProjectFacade create(IFile pom, boolean load, IProgressMonitor monitor) {
    return manager.create(pom, load, monitor);
  }

  @Override
  public IMavenProjectFacade create(IProject project, IProgressMonitor monitor) {
    return manager.create(project, monitor);
  }

  @Override
  public IMavenProjectFacade[] getProjects() {
    return manager.getProjects();
  }

  //XXX mkleint: this only returns a correct facade for the project's own pom.xml, if the POM file is nested, the result is wrong.
  @Override
  public IMavenProjectFacade getProject(IProject project) {
    return manager.getProject(project);
  }

  //XXX mkleint: what happens when multiple workspace projects have the same coordinates?!?
  @Override
  public IMavenProjectFacade getMavenProject(String groupId, String artifactId, String version) {
    return manager.getMavenProject(groupId, artifactId, version);
  }

  public File getWorkspaceStateFile() {
    //FIXME this is more a property of the WorkspaceStateWriter!
    return workspaceStateFile;
  }

  @Override
  public <V> V execute(final IMavenProjectFacade facade, final ICallable<V> callable, IProgressMonitor monitor)
      throws CoreException {
    IMavenExecutionContext context = manager.createExecutionContext(facade.getPom(), facade.getResolverConfiguration());
    return context.execute(
        (context1, monitor1) -> context1.execute(facade.getMavenProject(monitor1), callable, monitor1), monitor);
  }
}
