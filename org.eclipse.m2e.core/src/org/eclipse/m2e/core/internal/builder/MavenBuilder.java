/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.builder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class MavenBuilder extends IncrementalProjectBuilder implements DeltaProvider {
  private static Logger log = LoggerFactory.getLogger(MavenBuilder.class);

  private MavenBuilderImpl builder = new MavenBuilderImpl(this);

  /*
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
    IProject project = getProject();
    log.debug("Building project {}", project.getName()); //$NON-NLS-1$
    long start = System.currentTimeMillis();

    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
    IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();
    IMavenMarkerManager markerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();

    markerManager.deleteMarkers(project, kind == FULL_BUILD, IMavenConstants.MARKER_BUILD_ID);

    if(!project.hasNature(IMavenConstants.NATURE_ID)) {
      return null;
    }

    IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
    if(pomResource == null) {
      log.error("Project {} does not have pom.xml", project.getName());
      return null;
    }

    IResourceDelta delta = getDelta(project);

    IMavenProjectFacade projectFacade = projectManager.create(getProject(), monitor);

    if(delta == null || projectFacade == null || projectFacade.isStale()) {
      MavenUpdateRequest updateRequest = new MavenUpdateRequest(project, mavenConfiguration.isOffline() /*offline*/,
          false /*updateSnapshots*/);
      projectManager.refresh(updateRequest, monitor);
      projectFacade = projectManager.create(project, monitor);
      if(projectFacade == null) {
        // error marker should have been created
        return null;
      }
    }

    MavenProject mavenProject = null;
    try {
      mavenProject = projectFacade.getMavenProject(monitor);
    } catch(CoreException ce) {
      //unable to read the project facade
      addErrorMarker(project, ce);
      monitor.done();
      return null;
    }

    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade);
    if(lifecycleMapping == null) {
      return null;
    }
    Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipantsByMojoExecutionKey = lifecycleMapping
        .getBuildParticipants(projectFacade, monitor);

    IMaven maven = MavenPlugin.getMaven();
    MavenExecutionRequest request = projectManager.createExecutionRequest(pomResource,
        projectFacade.getResolverConfiguration(), monitor);
    MavenSession session = maven.createSession(request, mavenProject);

    Set<IProject> dependencies = builder.build(session, projectFacade, kind, args, buildParticipantsByMojoExecutionKey,
        monitor);

    log.debug("Built project {} in {} ms", project.getName(), System.currentTimeMillis() - start); //$NON-NLS-1$
    if(dependencies.isEmpty()) {
      return null;
    }
    return dependencies.toArray(new IProject[dependencies.size()]);
  }

  protected void clean(IProgressMonitor monitor) throws CoreException {
    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

    IProject project = getProject();
    IMavenMarkerManager markerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();
    markerManager.deleteMarkers(project, IMavenConstants.MARKER_BUILD_ID);

    if(!project.hasNature(IMavenConstants.NATURE_ID)) {
      return;
    }
    IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
    if(pomResource == null) {
      return;
    }

    IMavenProjectFacade projectFacade = projectManager.create(getProject(), monitor);
    if(projectFacade == null) {
      return;
    }

    // 380096 make sure facade.getMavenProject() is not null
    if(projectFacade.getMavenProject(monitor) == null) {
      return;
    }

    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade);
    if(lifecycleMapping == null) {
      return;
    }
    Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipantsByMojoExecutionKey = lifecycleMapping
        .getBuildParticipants(projectFacade, monitor);

    MavenProject mavenProject = null;
    try {
      mavenProject = projectFacade.getMavenProject(monitor);
    } catch(CoreException ce) {
      //the pom cannot be read. don't fill the log full of junk, just add an error marker
      addErrorMarker(project, ce);
      return;
    }

    IMaven maven = MavenPlugin.getMaven();
    MavenExecutionRequest request = projectManager.createExecutionRequest(pomResource,
        projectFacade.getResolverConfiguration(), monitor);
    MavenSession session = maven.createSession(request, mavenProject);

    builder.clean(session, projectFacade, buildParticipantsByMojoExecutionKey, monitor);
  }

  private void addErrorMarker(IProject project, Exception e) {
    String msg = e.getMessage();
    String rootCause = M2EUtils.getRootCauseMessage(e);
    if(!e.equals(msg)) {
      msg = msg + ": " + rootCause; //$NON-NLS-1$
    }

    IMavenMarkerManager markerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();
    markerManager.addMarker(project, IMavenConstants.MARKER_BUILD_ID, msg, 1, IMarker.SEVERITY_ERROR);
  }
}
