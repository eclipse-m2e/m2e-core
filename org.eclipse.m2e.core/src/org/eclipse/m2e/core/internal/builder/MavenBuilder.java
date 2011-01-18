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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.builder.AbstractEclipseBuildContext;
import org.eclipse.m2e.core.builder.AbstractEclipseBuildContext.Message;
import org.eclipse.m2e.core.builder.EclipseBuildContext;
import org.eclipse.m2e.core.builder.EclipseIncrementalBuildContext;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.project.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.util.M2EUtils;


public class MavenBuilder extends IncrementalProjectBuilder {
  private static Logger log = LoggerFactory.getLogger(MavenBuilder.class);

  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/builder")); //$NON-NLS-1$

  public static QualifiedName BUILD_CONTEXT_KEY = new QualifiedName(IMavenConstants.PLUGIN_ID, "BuildContext"); //$NON-NLS-1$

  private static final String BUILD_PARTICIPANT_ID_ATTR_NAME = "buildParticipantId";

  static interface GetDeltaCallback {
    public IResourceDelta getDelta(IProject project);
  }

  private GetDeltaCallback getDeltaCallback = new GetDeltaCallback() {
    public IResourceDelta getDelta(IProject project) {
      return MavenBuilder.this.getDelta(project);
    }
  };

  /*
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  @SuppressWarnings("unchecked")
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenConsole console = plugin.getConsole();
    MavenProjectManager projectManager = plugin.getMavenProjectManager();
    IProjectConfigurationManager configurationManager = plugin.getProjectConfigurationManager();
    IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
    IMavenMarkerManager markerManager = plugin.getMavenMarkerManager();
    
    IProject project = getProject();
    markerManager.deleteMarkers(project, kind == FULL_BUILD, IMavenConstants.MARKER_BUILD_ID);

    if(!project.hasNature(IMavenConstants.NATURE_ID)) {
      return null;
    }

    IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
    if(pomResource == null) {
      console.logError("Project " + project.getName() + " does not have pom.xml");
      return null;
    }

    IMavenProjectFacade projectFacade = projectManager.create(getProject(), monitor);
    if(projectFacade == null) {
      // XXX is this really possible? should we warn the user?
      return null;
    }

    if(projectFacade.isStale()) {
      MavenUpdateRequest updateRequest = new MavenUpdateRequest(project, mavenConfiguration.isOffline() /*offline*/,
          false /*updateSnapshots*/);
      projectManager.refresh(updateRequest, monitor);
      IMavenProjectFacade facade = projectManager.create(project, monitor);
      if(facade == null) {
        // error marker should have been created
        return null;
      }
    }

    MavenProject mavenProject = null;
    try {
      mavenProject = projectFacade.getMavenProject(monitor);
    } catch(CoreException ce) {
      //unable to read the project facade
      addErrorMarker(ce);
      monitor.done();
      return null;
    }

    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade, monitor);
    if(lifecycleMapping == null || !projectFacade.hasValidConfiguration()) {
      return null;
    }

    Set<IProject> dependencies = new HashSet<IProject>();

    IMaven maven = MavenPlugin.getDefault().getMaven();
    MavenExecutionRequest request = projectManager.createExecutionRequest(pomResource,
        projectFacade.getResolverConfiguration(), monitor);
    MavenSession session = maven.createSession(request, mavenProject);

    IResourceDelta delta = getDelta(project);
    Map<String, Object> contextState = (Map<String, Object>) project.getSessionProperty(BUILD_CONTEXT_KEY);
    AbstractEclipseBuildContext buildContext;
    if(contextState != null && (INCREMENTAL_BUILD == kind || AUTO_BUILD == kind)) {
      buildContext = new EclipseIncrementalBuildContext(delta, contextState);
    } else {
      // must be full build
      contextState = new HashMap<String, Object>();
      project.setSessionProperty(BUILD_CONTEXT_KEY, contextState);
      buildContext = new EclipseBuildContext(project, contextState);
    }

    List<Throwable> buildErrors = new ArrayList<Throwable>();
    ThreadBuildContext.setThreadBuildContext(buildContext);
    try {
      Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipantsByMojoExecutionKey = lifecycleMapping
          .getBuildParticipants(monitor);
      for(Entry<MojoExecutionKey, List<AbstractBuildParticipant>> entry : buildParticipantsByMojoExecutionKey
          .entrySet()) {
        for(InternalBuildParticipant participant : entry.getValue()) {
          String stringMojoExecutionKey = entry.getKey().getKeyString();
          buildContext.setCurrentBuildParticipantId(stringMojoExecutionKey + "-" + participant.getClass().getName());
          participant.setMavenProjectFacade(projectFacade);
          participant.setGetDeltaCallback(getDeltaCallback);
          participant.setSession(session);
          participant.setBuildContext(buildContext);
          try {
            if(FULL_BUILD == kind || delta != null || participant.callOnEmptyDelta()) {
              Set<IProject> sub = participant.build(kind, monitor);
              if(sub != null) {
                dependencies.addAll(sub);
              }
            }
          } catch(Exception e) {
            log.debug("Exception in build participant", e);
            buildErrors.add(e);
          } finally {
            participant.setMavenProjectFacade(null);
            participant.setGetDeltaCallback(null);
            participant.setSession(null);
            participant.setBuildContext(null);
          }
        }
      }
    } catch(CoreException e) {
      buildErrors.add(e);
    } finally {
      ThreadBuildContext.setThreadBuildContext(null);
    }

    for(File file : buildContext.getFiles()) {
      IPath path = getProjectRelativePath(project, file);
      if(path == null) {
        continue; // odd
      }

      if(!file.exists()) {
        IResource resource = project.findMember(path);
        if(resource != null) {
          resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
      } else if(file.isDirectory()) {
        IFolder ifolder = project.getFolder(path);
        ifolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      } else {
        IFile ifile = project.getFile(path);
        ifile.refreshLocal(IResource.DEPTH_ZERO, monitor);
      }
    }

    MavenExecutionResult result = session.getResult();
    for(Throwable buildError : buildErrors) {
      result.addException(buildError);
    }
    processBuildResults(result, buildContext);

    if(dependencies.isEmpty()) {
      return null;
    }
    return dependencies.toArray(new IProject[dependencies.size()]);
  }

  private void processBuildResults(MavenExecutionResult result, AbstractEclipseBuildContext buildContext) {
    MavenPlugin plugin = MavenPlugin.getDefault();
    IMavenMarkerManager markerManager = plugin.getMavenMarkerManager();

    for(Entry<String, List<File>> entry : buildContext.getRemoveErrors().entrySet()) {
      String buildParticipantId = entry.getKey();
      for(File file : entry.getValue()) {
        deleteBuildParticipantMarkers(markerManager, file, IMarker.SEVERITY_ERROR, buildParticipantId);
      }
    }
    for(Entry<String, List<File>> entry : buildContext.getRemoveWarnings().entrySet()) {
      String buildParticipantId = entry.getKey();
      for(File file : entry.getValue()) {
        deleteBuildParticipantMarkers(markerManager, file, IMarker.SEVERITY_WARNING, buildParticipantId);
      }
    }

    for(Entry<String, List<Message>> messageEntry : buildContext.getErrors().entrySet()) {
      String buildParticipantId = messageEntry.getKey();
      for(Message buildMessage : messageEntry.getValue()) {
        addBuildParticipantMarker(markerManager, buildMessage, IMarker.SEVERITY_ERROR, buildParticipantId);

        if(buildMessage.cause != null && result.hasExceptions()) {
          result.getExceptions().remove(buildMessage.cause);
        }
      }
    }
    for(Entry<String, List<Message>> messageEntry : buildContext.getWarnings().entrySet()) {
      String buildParticipantId = messageEntry.getKey();
      for(Message buildMessage : messageEntry.getValue()) {
        addBuildParticipantMarker(markerManager, buildMessage, IMarker.SEVERITY_WARNING, buildParticipantId);

        if(buildMessage.cause != null && result.hasExceptions()) {
          result.getExceptions().remove(buildMessage.cause);
        }
      }
    }

    if(result.hasExceptions()) {
      IProject project = getProject();
      markerManager.addMarkers(project.getFile(IMavenConstants.POM_FILE_NAME), IMavenConstants.MARKER_BUILD_ID, result);
    }
  }

  private void deleteBuildParticipantMarkers(IMavenMarkerManager markerManager, File file, int severity,
      String buildParticipantId) {
    IProject project = getProject();

    IPath path = getProjectRelativePath(getProject(), file);
    IResource resource = null;
    if(path != null) {
      resource = project.findMember(path);
    }
    if(resource == null) {
      resource = project.getFile(IMavenConstants.POM_FILE_NAME);
    }
    try {
      markerManager.deleteMarkers(resource, IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, severity,
          BUILD_PARTICIPANT_ID_ATTR_NAME, buildParticipantId);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private void addBuildParticipantMarker(IMavenMarkerManager markerManager, Message buildMessage, int severity,
      String buildParticipantId) {
    IProject project = getProject();

    IPath path = getProjectRelativePath(getProject(), buildMessage.file);
    IResource resource = null;
    if(path != null) {
      resource = project.findMember(path);
    }
    if(resource == null) {
      resource = project.getFile(IMavenConstants.POM_FILE_NAME);
    }
    IMarker marker = markerManager.addMarker(resource, IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        buildMessage.message, buildMessage.line, severity);
    try {
      marker.setAttribute(BUILD_PARTICIPANT_ID_ATTR_NAME, buildParticipantId);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private void addErrorMarker(Exception e) {
    String msg = e.getMessage();
    String rootCause = M2EUtils.getRootCauseMessage(e);
    if(!e.equals(msg)){
      msg = msg+": "+rootCause; //$NON-NLS-1$
    }

    MavenPlugin plugin = MavenPlugin.getDefault();
    IMavenMarkerManager markerManager = plugin.getMavenMarkerManager();
    markerManager.addMarker(getProject(), IMavenConstants.MARKER_BUILD_ID, msg, 1, IMarker.SEVERITY_ERROR);
  }

  public static IPath getProjectRelativePath(IProject project, File file) {
    if(project == null || file == null) {
      return null;
    }

    IPath projectPath = project.getLocation();
    if(projectPath == null) {
      return null;
    }

    IPath filePath = new Path(file.getAbsolutePath());
    if(!projectPath.isPrefixOf(filePath)) {
      return null;
    }

    return filePath.removeFirstSegments(projectPath.segmentCount());
  }

  protected void clean(IProgressMonitor monitor) throws CoreException{
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenProjectManager projectManager = plugin.getMavenProjectManager();
    IProjectConfigurationManager configurationManager = plugin.getProjectConfigurationManager();

    IProject project = getProject();
    IMavenMarkerManager markerManager = plugin.getMavenMarkerManager();
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

    IMaven maven = MavenPlugin.getDefault().getMaven();

    // TODO flush relevant caches

    project.setSessionProperty(BUILD_CONTEXT_KEY, null); // clean context state

    MavenExecutionRequest request = projectManager.createExecutionRequest(pomResource,
        projectFacade.getResolverConfiguration(), monitor);
    MavenSession session = null;
    try {
      session = maven.createSession(request, projectFacade.getMavenProject(monitor));
    } catch(CoreException ce) {
      //the pom cannot be read. don't fill the log full of junk, just add an error marker
      addErrorMarker(ce);
      return;
    }
    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade, monitor);

    if(lifecycleMapping == null) {
      return;
    }

    List<Throwable> buildErrors = new ArrayList<Throwable>();
    Map<String, Object> contextState = new HashMap<String, Object>();
    EclipseBuildContext buildContext = new EclipseBuildContext(project, contextState);
    ThreadBuildContext.setThreadBuildContext(buildContext);
    try {
      Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipantsByMojoExecutionKey = lifecycleMapping
          .getBuildParticipants(monitor);
      for(Entry<MojoExecutionKey, List<AbstractBuildParticipant>> entry : buildParticipantsByMojoExecutionKey
          .entrySet()) {
        for(InternalBuildParticipant participant : entry.getValue()) {
          String stringMojoExecutionKey = entry.getKey().getKeyString();
          buildContext.setCurrentBuildParticipantId(stringMojoExecutionKey + "-" + participant.getClass().getName());
          participant.setMavenProjectFacade(projectFacade);
          participant.setGetDeltaCallback(getDeltaCallback);
          participant.setSession(session);
          participant.setBuildContext(buildContext);
          try {
            participant.clean(monitor);
          } catch(Exception e) {
            log.debug("Exception in build participant", e);
            buildErrors.add(e);
          } finally {
            participant.setMavenProjectFacade(null);
            participant.setGetDeltaCallback(null);
            participant.setSession(null);
            participant.setBuildContext(null);
          }
        }
      }
    } catch(CoreException e) {
      buildErrors.add(e);
    } finally {
      ThreadBuildContext.setThreadBuildContext(null);
    }

    MavenExecutionResult result = session.getResult();
    for(Throwable buildError : buildErrors) {
      result.addException(buildError);
    }
    processBuildResults(result, buildContext);
  }
}
