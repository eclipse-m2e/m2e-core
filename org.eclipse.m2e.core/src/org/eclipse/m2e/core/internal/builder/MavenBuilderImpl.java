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

package org.eclipse.m2e.core.internal.builder;

import static org.eclipse.core.resources.IncrementalProjectBuilder.AUTO_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.builder.plexusbuildapi.AbstractEclipseBuildContext;
import org.eclipse.m2e.core.internal.builder.plexusbuildapi.AbstractEclipseBuildContext.Message;
import org.eclipse.m2e.core.internal.builder.plexusbuildapi.EclipseBuildContext;
import org.eclipse.m2e.core.internal.builder.plexusbuildapi.EclipseEmptyBuildContext;
import org.eclipse.m2e.core.internal.builder.plexusbuildapi.EclipseIncrementalBuildContext;
import org.eclipse.m2e.core.internal.embedder.MavenProjectMutableState;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant2;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class MavenBuilderImpl {
  private static Logger log = LoggerFactory.getLogger(MavenBuilderImpl.class);

  public static QualifiedName BUILD_CONTEXT_KEY = new QualifiedName(IMavenConstants.PLUGIN_ID, "BuildContext"); //$NON-NLS-1$

  private static final String BUILD_PARTICIPANT_ID_ATTR_NAME = "buildParticipantId";

  private final DeltaProvider deltaProvider;

  public MavenBuilderImpl(DeltaProvider deltaProvider) {
    this.deltaProvider = deltaProvider;
  }

  public MavenBuilderImpl() {
    this(new DeltaProvider() {
      public IResourceDelta getDelta(IProject project) {
        return null;
      }
    });
  }

  public Set<IProject> build(MavenSession session, IMavenProjectFacade projectFacade, int kind,
      Map<String, String> args, Map<MojoExecutionKey, List<AbstractBuildParticipant>> participants,
      IProgressMonitor monitor) throws CoreException {
    Collection<BuildDebugHook> debugHooks = MavenBuilder.getDebugHooks();

    Set<IProject> dependencies = new HashSet<IProject>();

    MavenProject mavenProject = projectFacade.getMavenProject();
    IProject project = projectFacade.getProject();

    IResourceDelta delta = getDeltaProvider().getDelta(project);

    @SuppressWarnings("unchecked")
    Map<String, Object> contextState = (Map<String, Object>) project.getSessionProperty(BUILD_CONTEXT_KEY);
    AbstractEclipseBuildContext buildContext;
    if(delta != null && contextState != null && (INCREMENTAL_BUILD == kind || AUTO_BUILD == kind)) {
      buildContext = new EclipseIncrementalBuildContext(delta, contextState);
    } else {
      contextState = new HashMap<String, Object>();
      project.setSessionProperty(BUILD_CONTEXT_KEY, contextState);
      if(AbstractBuildParticipant2.PRECONFIGURE_BUILD == kind) {
        buildContext = new EclipseEmptyBuildContext(project, contextState);
      } else {
        // must be full build
        buildContext = new EclipseBuildContext(project, contextState);
      }
    }

    debugBuildStart(debugHooks, projectFacade, kind, args, participants, delta, monitor);

    Map<Throwable, MojoExecutionKey> buildErrors = new LinkedHashMap<Throwable, MojoExecutionKey>();
    ThreadBuildContext.setThreadBuildContext(buildContext);
    MavenProjectMutableState snapshot = MavenProjectMutableState.takeSnapshot(mavenProject);
    try {
      for(Entry<MojoExecutionKey, List<AbstractBuildParticipant>> entry : participants.entrySet()) {
        MojoExecutionKey mojoExecutionKey = entry.getKey();
        for(InternalBuildParticipant participant : entry.getValue()) {
          Set<File> debugRefreshFiles = !debugHooks.isEmpty() ? new LinkedHashSet<File>(buildContext.getFiles()) : null;

          log.debug("Executing build participant {} for plugin execution {}", participant.getClass().getName(),
              mojoExecutionKey.toString());
          String stringMojoExecutionKey = mojoExecutionKey.getKeyString();
          buildContext.setCurrentBuildParticipantId(stringMojoExecutionKey + "-" + participant.getClass().getName());
          participant.setMavenProjectFacade(projectFacade);
          participant.setGetDeltaCallback(getDeltaProvider());
          participant.setSession(session);
          participant.setBuildContext(buildContext);
          if(participant instanceof InternalBuildParticipant2) {
            ((InternalBuildParticipant2) participant).setArgs(args);
          }
          long executionStartTime = System.currentTimeMillis();
          try {
            if(isApplicable(participant, kind, delta)) {
              Set<IProject> sub = participant.build(kind, monitor);
              if(sub != null) {
                dependencies.addAll(sub);
              }
            }
          } catch(Exception e) {
            log.debug("Exception in build participant {}", participant.getClass().getName(), e);
            buildErrors.put(e, mojoExecutionKey);
          } finally {
            log.debug("Finished executing build participant {} for plugin execution {} in {} ms", new Object[] {
                participant.getClass().getName(), mojoExecutionKey.toString(),
                System.currentTimeMillis() - executionStartTime});
            participant.setMavenProjectFacade(null);
            participant.setGetDeltaCallback(null);
            participant.setSession(null);
            participant.setBuildContext(null);
            if(participant instanceof InternalBuildParticipant2) {
              ((InternalBuildParticipant2) participant).setArgs(Collections.<String, String> emptyMap());
            }

            processMavenSessionErrors(session, mojoExecutionKey, buildErrors);
          }

          debugBuildParticipant(debugHooks, projectFacade, mojoExecutionKey, (AbstractBuildParticipant) participant,
              diff(debugRefreshFiles, buildContext.getFiles()), monitor);
        }
      }
    } catch(Exception e) {
      buildErrors.put(e, null);
    } finally {
      snapshot.restore(mavenProject);
      ThreadBuildContext.setThreadBuildContext(null);
    }

    // Refresh files modified by build participants/maven plugins
    refreshResources(project, buildContext, monitor);

    // Process errors and warnings
    MavenExecutionResult result = session.getResult();
    processBuildResults(project, mavenProject, result, buildContext, buildErrors);

    debugBuildEnd(debugHooks, projectFacade, buildContext, monitor);

    return dependencies;
  }

  private void debugBuildParticipant(Collection<BuildDebugHook> hooks, IMavenProjectFacade projectFacade,
      MojoExecutionKey mojoExecutionKey, AbstractBuildParticipant participant, Set<File> files, IProgressMonitor monitor) {
    for(BuildDebugHook hook : hooks) {
      hook.buildParticipant(projectFacade, mojoExecutionKey, participant, files, monitor);
    }
  }

  private Set<File> diff(Set<File> before, Set<File> after) {
    if(before == null) {
      return after;
    }
    Set<File> result = new LinkedHashSet<File>(after);
    result.removeAll(before);
    return result;
  }

  private void debugBuildStart(Collection<BuildDebugHook> hooks, IMavenProjectFacade projectFacade, int kind,
      Map<String, String> args, Map<MojoExecutionKey, List<AbstractBuildParticipant>> participants,
      IResourceDelta delta, IProgressMonitor monitor) {
    for(BuildDebugHook hook : hooks) {
      hook.buildStart(projectFacade, kind, args, participants, delta, monitor);
    }
  }

  private void debugBuildEnd(Collection<BuildDebugHook> hooks, IMavenProjectFacade projectFacade,
      AbstractEclipseBuildContext buildContext, IProgressMonitor monitor) {
    for(BuildDebugHook hook : hooks) {
    }
  }

  protected boolean isApplicable(InternalBuildParticipant participant, int kind, IResourceDelta delta) {
    return FULL_BUILD == kind || delta != null || participant.callOnEmptyDelta();
  }

  private void processMavenSessionErrors(MavenSession session, MojoExecutionKey mojoExecutionKey,
      Map<Throwable, MojoExecutionKey> buildErrors) {
    MavenExecutionResult result = session.getResult();
    if(result.hasExceptions()) {
      for(Throwable e : result.getExceptions()) {
        buildErrors.put(e, mojoExecutionKey);
      }
      result.getExceptions().clear();
    }
  }

  private void refreshResources(IProject project, AbstractEclipseBuildContext buildContext, IProgressMonitor monitor)
      throws CoreException {
    for(File file : buildContext.getFiles()) {
      IPath path = getProjectRelativePath(project, file);
      if(path == null) {
        log.debug("Could not get relative path for file: ", file.getAbsoluteFile());
        continue; // odd
      }

      IResource resource;
      if(!file.exists()) {
        resource = project.findMember(path);
      } else if(file.isDirectory()) {
        resource = project.getFolder(path);
      } else {
        resource = project.getFile(path);
      }
      if(resource != null) {
        workaroundBug368376(resource, monitor);
        resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      }
    }
  }

  void workaroundBug368376(IResource resource, IProgressMonitor monitor) throws CoreException {
    // refreshing a new file does not automatically refresh enclosing new folders
    // refreshLocal(IResource.DEPTH_ONE) on all out-of-sync parents seems to be the least expansive way to refresh
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=368376
    List<IContainer> parents = new LinkedList<IContainer>();
    for(IContainer parent = resource.getParent(); parent != null && !parent.isSynchronized(IResource.DEPTH_ZERO); parent = parent
        .getParent()) {
      parents.add(0, parent);
    }
    for(IContainer parent : parents) {
      parent.refreshLocal(IResource.DEPTH_ONE, monitor);
    }
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

  private void processBuildResults(IProject project, MavenProject mavenProject, MavenExecutionResult result,
      AbstractEclipseBuildContext buildContext, Map<Throwable, MojoExecutionKey> buildErrors) {
    IMavenMarkerManager markerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();

    // Remove obsolete markers for problems reported by build participants
    for(Entry<String, List<File>> entry : buildContext.getRemoveMessages().entrySet()) {
      String buildParticipantId = entry.getKey();
      for(File file : entry.getValue()) {
        deleteBuildParticipantMarkers(project, markerManager, file, buildParticipantId);
      }
    }

    // Create new markers for problems reported by build participants
    for(Entry<String, List<Message>> messageEntry : buildContext.getMessages().entrySet()) {
      String buildParticipantId = messageEntry.getKey();
      for(Message buildMessage : messageEntry.getValue()) {
        addBuildParticipantMarker(project, markerManager, buildMessage, buildParticipantId);

        if(buildMessage.cause != null && buildErrors.containsKey(buildMessage.cause)) {
          buildErrors.remove(buildMessage.cause);
        }
      }
    }

    // Create markers for the build errors linked to mojo/plugin executions
    for(Throwable error : buildErrors.keySet()) {
      MojoExecutionKey mojoExecutionKey = buildErrors.get(error);
      SourceLocation markerLocation;
      if(mojoExecutionKey != null) {
        markerLocation = SourceLocationHelper.findLocation(mavenProject, mojoExecutionKey);
      } else {
        markerLocation = new SourceLocation(1, 0, 0);
      }
      BuildProblemInfo problem = new BuildProblemInfo(error, mojoExecutionKey, markerLocation);
      markerManager.addErrorMarker(project.getFile(IMavenConstants.POM_FILE_NAME), IMavenConstants.MARKER_BUILD_ID,
          problem);
    }

    if(result.hasExceptions()) {
      markerManager.addMarkers(project.getFile(IMavenConstants.POM_FILE_NAME), IMavenConstants.MARKER_BUILD_ID, result);
    }
  }

  private void deleteBuildParticipantMarkers(IProject project, IMavenMarkerManager markerManager, File file,
      String buildParticipantId) {
    IPath path = getProjectRelativePath(project, file);
    IResource resource = null;
    if(path != null) {
      resource = project.findMember(path);
    }
    if(resource == null) {
      resource = project.getFile(IMavenConstants.POM_FILE_NAME);
    }
    try {
      markerManager.deleteMarkers(resource, IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
          BUILD_PARTICIPANT_ID_ATTR_NAME, buildParticipantId);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private void addBuildParticipantMarker(IProject project, IMavenMarkerManager markerManager, Message buildMessage,
      String buildParticipantId) {

    IPath path = getProjectRelativePath(project, buildMessage.file);
    IResource resource = null;
    if(path != null) {
      resource = project.findMember(path);
    }
    if(resource == null) {
      resource = project.getFile(IMavenConstants.POM_FILE_NAME);
    }
    int at = buildParticipantId.lastIndexOf('-');
    String pluginExecutionKey = buildParticipantId.substring(0, at);
    String message = buildMessage.message + " (" + pluginExecutionKey + ')'; //$NON-NLS-1$
    IMarker marker = markerManager.addMarker(resource, IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, message,
        buildMessage.line, buildMessage.severity);
    try {
      marker.setAttribute(BUILD_PARTICIPANT_ID_ATTR_NAME, buildParticipantId);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  public void clean(MavenSession session, IMavenProjectFacade projectFacade,
      Map<MojoExecutionKey, List<AbstractBuildParticipant>> participants, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = projectFacade.getMavenProject();
    IProject project = projectFacade.getProject();

    // TODO flush relevant caches

    project.setSessionProperty(BUILD_CONTEXT_KEY, null); // clean context state

    Map<Throwable, MojoExecutionKey> buildErrors = new LinkedHashMap<Throwable, MojoExecutionKey>();
    Map<String, Object> contextState = new HashMap<String, Object>();
    EclipseBuildContext buildContext = new EclipseBuildContext(project, contextState);
    ThreadBuildContext.setThreadBuildContext(buildContext);
    try {
      for(Entry<MojoExecutionKey, List<AbstractBuildParticipant>> entry : participants.entrySet()) {
        MojoExecutionKey mojoExecutionKey = entry.getKey();
        for(InternalBuildParticipant participant : entry.getValue()) {
          String stringMojoExecutionKey = mojoExecutionKey.getKeyString();
          buildContext.setCurrentBuildParticipantId(stringMojoExecutionKey + "-" + participant.getClass().getName());
          participant.setMavenProjectFacade(projectFacade);
          participant.setGetDeltaCallback(getDeltaProvider());
          participant.setSession(session);
          participant.setBuildContext(buildContext);
          try {
            participant.clean(monitor);
          } catch(Exception e) {
            log.debug("Exception in build participant", e);
            buildErrors.put(e, mojoExecutionKey);
          } finally {
            participant.setMavenProjectFacade(null);
            participant.setGetDeltaCallback(null);
            participant.setSession(null);
            participant.setBuildContext(null);

            processMavenSessionErrors(session, mojoExecutionKey, buildErrors);
          }
        }
      }
    } catch(Exception e) {
      buildErrors.put(e, null);
    } finally {
      ThreadBuildContext.setThreadBuildContext(null);
    }

    // Refresh files modified by build participants/maven plugins
    refreshResources(project, buildContext, monitor);

    MavenExecutionResult result = session.getResult();
    processBuildResults(project, mavenProject, result, buildContext, buildErrors);
  }

  DeltaProvider getDeltaProvider() {
    return deltaProvider;
  }
}
