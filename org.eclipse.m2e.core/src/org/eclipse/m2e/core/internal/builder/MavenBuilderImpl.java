/*******************************************************************************
 * Copyright (c) 2010, 2021 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.builder;

import static org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.ExtensionReader;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.URLConnectionCaches;
import org.eclipse.m2e.core.internal.builder.BuildResultCollector.Message;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework.BuildContext;
import org.eclipse.m2e.core.internal.builder.IIncrementalBuildFramework.BuildDelta;
import org.eclipse.m2e.core.internal.builder.plexusbuildapi.AbstractEclipseBuildContext;
import org.eclipse.m2e.core.internal.builder.plexusbuildapi.EclipseResourceBuildDelta;
import org.eclipse.m2e.core.internal.builder.plexusbuildapi.PlexusBuildAPI;
import org.eclipse.m2e.core.internal.embedder.MavenProjectMutableState;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class MavenBuilderImpl {

  private static Logger log = LoggerFactory.getLogger(MavenBuilderImpl.class);

  public static final QualifiedName BUILD_CONTEXT_KEY = new QualifiedName(IMavenConstants.PLUGIN_ID, "BuildContext"); //$NON-NLS-1$

  private static final String BUILD_PARTICIPANT_ID_ATTR_NAME = "buildParticipantId";

  private final DeltaProvider deltaProvider;

  private final List<IIncrementalBuildFramework> incrementalBuildFrameworks;

  private final Map<IProject, ProjectBuildState> deltaState = new ConcurrentHashMap<>();

  private enum DeltaType {
    INCREMENTAL, IRRELEVANT, FULL_BUILD, UNKOWN;
  }

  public MavenBuilderImpl(DeltaProvider deltaProvider) {
    this.deltaProvider = deltaProvider;
    this.incrementalBuildFrameworks = loadIncrementalBuildFrameworks();
  }

  private List<IIncrementalBuildFramework> loadIncrementalBuildFrameworks() {
    List<IIncrementalBuildFramework> frameworks = new ArrayList<>();
    frameworks.add(new PlexusBuildAPI());
    frameworks.addAll(ExtensionReader.readIncrementalBuildFrameworks());
    return frameworks;
  }

  public MavenBuilderImpl() {
    this(project -> null);
  }

  public Set<IProject> build(MavenSession session, IMavenProjectFacade projectFacade, int kind,
      Map<String, String> args, Map<MojoExecutionKey, List<AbstractBuildParticipant>> participants,
      IProgressMonitor monitor) throws CoreException {

    // 442524 safety guard
    URLConnectionCaches.assertDisabled();

    Collection<BuildDebugHook> debugHooks = MavenBuilder.getDebugHooks();

    Set<IProject> dependencies = new HashSet<>();

    MavenProject mavenProject = projectFacade.getMavenProject();
    IProject project = projectFacade.getProject();

    DeltaProvider deltaProvider = getDeltaProvider();
    IResourceDelta delta = deltaProvider.getDelta(project);
    DeltaType deltaType = hasRelevantDelta(projectFacade, delta);
    if(deltaType == DeltaType.IRRELEVANT) {
      return Set.of(project);
    }
    ProjectBuildState buildState = deltaState.computeIfAbsent(project, ProjectBuildState::new);
    final BuildResultCollector participantResults = new BuildResultCollector();
    List<BuildContext> incrementalContexts = setupProjectBuildContext(project, kind, delta, participantResults,
        buildState, deltaType);

    debugBuildStart(debugHooks, projectFacade, kind, args, participants, delta, monitor);

    Map<Throwable, MojoExecutionKey> buildErrors = new LinkedHashMap<>();
    MavenProjectMutableState snapshot = MavenProjectMutableState.takeSnapshot(mavenProject);
    try {
      participants.forEach((mojoExecutionKey, buildParticipants) -> {
        for(InternalBuildParticipant participant : buildParticipants) {
          Set<File> debugRefreshFiles = !debugHooks.isEmpty() ? new LinkedHashSet<>(participantResults.getFiles())
              : null;
          log.debug("Executing build participant {} for plugin execution {}", participant.getClass().getName(),
              mojoExecutionKey);
          participantResults.setParticipantId(mojoExecutionKey.getKeyString() + "-" + participant.getClass().getName());
          participant.setMavenProjectFacade(projectFacade);
          participant.setGetDeltaCallback(deltaProvider);
          participant.setSession(session);
          BuildContext buildContext = incrementalContexts.get(0);
          if(buildContext instanceof org.sonatype.plexus.build.incremental.BuildContext incremental) {
            participant.setBuildContext(incremental);
          }
          if(participant instanceof InternalBuildParticipant2 participant2) {
            participant2.setArgs(args);
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
            log.debug("Finished executing build participant {} for plugin execution {} in {} ms",
                participant.getClass().getName(), mojoExecutionKey, System.currentTimeMillis() - executionStartTime);
            participant.setMavenProjectFacade(null);
            participant.setGetDeltaCallback(null);
            participant.setSession(null);
            participant.setBuildContext(null);
            if(participant instanceof InternalBuildParticipant2 participant2) {
              participant2.setArgs(Collections.<String, String> emptyMap());
            }

            processMavenSessionErrors(session, mojoExecutionKey, buildErrors);
          }

          debugBuildParticipant(debugHooks, projectFacade, mojoExecutionKey, (AbstractBuildParticipant) participant,
              diff(debugRefreshFiles, participantResults.getFiles()), monitor);
        }
      });
    } catch(Exception e) {
      log.debug("Unexpected build exception", e);
      buildErrors.put(e, null);
    } finally {
      snapshot.restore(mavenProject);
      for(IIncrementalBuildFramework.BuildContext context : incrementalContexts) {
        context.release();
      }
    }
    
    // Refresh files modified by build participants/maven plugins
    refreshResources(project, participantResults.getFiles(), monitor);

    // Process errors and warnings
    MavenExecutionResult result = session.getResult();
    processBuildResults(project, mavenProject, result, participantResults, buildErrors);
    if(buildErrors.isEmpty()) {
      //we only commit this when there are no errors so just in case a failure is cased by a changed file it is again queried afterwards
      buildState.commit();
    }
    return dependencies;
  }

  private DeltaType hasRelevantDelta(IMavenProjectFacade projectFacade, IResourceDelta resourceDelta)
      throws CoreException {
    if(resourceDelta == null) {
      return DeltaType.FULL_BUILD;
    }
    IProject project = projectFacade.getProject();
    IPath buildOutputLocation = projectFacade.getBuildOutputLocation();
    if(project == null || buildOutputLocation == null) {
      return DeltaType.UNKOWN;
    }

    Predicate<IPath> isOutput = toPrefixPredicate(projectFacade.getOutputLocation());
    Predicate<IPath> isTestOutput = toPrefixPredicate(projectFacade.getTestOutputLocation());
    Predicate<IPath> isOutputOrTestOutput = isOutput.or(isTestOutput);

    IPath projectPath = project.getFullPath();
    List<IPath> moduleLocations = projectFacade.getMavenProjectModules().stream()
        .map(module -> projectPath.append(module)).toList();
    AtomicReference<DeltaType> deltaType = new AtomicReference<>(DeltaType.IRRELEVANT);
    resourceDelta.accept(delta -> {
      IResource resource = delta.getResource();
      if(resource instanceof IFile) {
        IPath fullPath = delta.getFullPath();
        if(buildOutputLocation.isPrefixOf(fullPath)) {
          //anything in the build output is not interesting for a change as it is produced by the build
          // ... unless a classpath resource that existed before has been deleted, possibly by another builder
          if(isOutputOrTestOutput.test(fullPath) && !resource.exists()) {
            //in this case we should perform a full build as we can't know what mojo has placed data possible here...
            deltaType.set(DeltaType.FULL_BUILD);
            return false;
          }
          return true;
        }
        for(IPath modulePath : moduleLocations) {
          if(modulePath.isPrefixOf(fullPath)) {
            //this is a change in a child module so this one is not really affected and the child will be (possibly) build directly.
            return true;
          }
        }
        //anything else has changed, so mark this as relevant an leave the loop
        deltaType.set(DeltaType.INCREMENTAL);
        return false;
      }
      return true;
    });
    return deltaType.get();
  }

  private static Predicate<IPath> toPrefixPredicate(IPath location) {
    if(location == null) {
      return (p) -> false;
    }
    return (p) -> location.isPrefixOf(p);
  }

  private List<IIncrementalBuildFramework.BuildContext> setupProjectBuildContext(IProject project, int kind,
      IResourceDelta delta, IIncrementalBuildFramework.BuildResultCollector results, ProjectBuildState buildState,
      DeltaType deltaType)
      throws CoreException {
    List<IIncrementalBuildFramework.BuildContext> contexts = new ArrayList<>();

    BuildDelta buildDelta = deltaType == DeltaType.FULL_BUILD || delta == null ? null
        : new ProjectBuildStateDelta(buildState, new EclipseResourceBuildDelta(delta));
    for(IIncrementalBuildFramework framework : incrementalBuildFrameworks) {
      contexts.add(framework.setupProjectBuildContext(project, kind, buildDelta, results));
    }
    return contexts;
  }

  private void debugBuildParticipant(Collection<BuildDebugHook> hooks, IMavenProjectFacade projectFacade,
      MojoExecutionKey mojoExecutionKey, AbstractBuildParticipant participant, Set<File> files,
      IProgressMonitor monitor) {
    for(BuildDebugHook hook : hooks) {
      hook.buildParticipant(projectFacade, mojoExecutionKey, participant, files, monitor);
    }
  }

  private Set<File> diff(Set<File> before, Set<File> after) {
    if(before == null) {
      return after;
    }
    Set<File> result = new LinkedHashSet<>(after);
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

  protected boolean isApplicable(InternalBuildParticipant participant, int kind, IResourceDelta delta) {
    return FULL_BUILD == kind || delta != null || participant.callOnEmptyDelta();
  }

  private void processMavenSessionErrors(MavenSession session, MojoExecutionKey mojoExecutionKey,
      Map<Throwable, MojoExecutionKey> buildErrors) {
    MavenExecutionResult result = session.getResult();
    if(result.hasExceptions()) {
      for(Throwable e : result.getExceptions()) {
        log.debug("Exception during execution {}", mojoExecutionKey, e);
        buildErrors.put(e, mojoExecutionKey);
      }
      result.getExceptions().clear();
    }
  }

  private void refreshResources(IProject project, Collection<File> resources, IProgressMonitor monitor)
      throws CoreException {
    if(isAutoRefresh()) {
      //if autorefresh is on, resources will be refreshed automatically
      return;
    }
    //1st is to refresh all project resources, just to make sure if anything has changed during the build will become visible to eclipse
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    //2nd is to refresh all explicitly updated resources by named files...
    for(File file : resources) {
      IPath path = MavenProjectUtils.getProjectRelativePath(project, file.getAbsolutePath());
      if(path == null) {
        log.debug("Could not get relative path for file: {}", file.getAbsoluteFile());
        continue; // odd
      }

      IResource resource;
      if(path.isEmpty()) {
        resource = project;
      } else if(!file.exists()) {
        resource = project.findMember(path); // null if path does not exist in the workspace
      } else if(file.isDirectory()) {
        resource = project.getFolder(path);
      } else {
        resource = project.getFile(path);
      }
      if(resource != null) {
        resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        if(resource.exists()) {
          // the resource has changed for certain, make sure resource sends IResourceChangeEvent

          // eclipse uses file lastModified timestamp to detect resource changes
          // this can result in missing IResourceChangeEvent's under certain conditions
          // - two builds happen within filesystem resolution (1s on linux and osx, causes problems during unit tests)
          // - maven mojo deliberately keeps lastModified (unlikely, but theoretically possible)
          // @see org.eclipse.core.internal.localstore.RefreshLocalVisitor.visit(UnifiedTreeNode)
          resource.touch(monitor);
        }
      }
    }
  }

  @SuppressWarnings("deprecation")
  private boolean isAutoRefresh() {
    return ResourcesPlugin.getPlugin().getPluginPreferences().getBoolean(ResourcesPlugin.PREF_AUTO_REFRESH);
  }

  private void processBuildResults(IProject project, MavenProject mavenProject, MavenExecutionResult result,
      BuildResultCollector results, Map<Throwable, MojoExecutionKey> buildErrors) {
    IMavenMarkerManager markerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();

    // Remove obsolete markers for problems reported by build participants
    results.getRemoveMessages().forEach((buildParticipantId, files) -> {
      for(File file : files) {
        deleteBuildParticipantMarkers(project, markerManager, file, buildParticipantId);
      }
    });

    // Create new markers for problems reported by build participants
    results.getMessages().forEach((buildParticipantId, messages) -> {
      for(Message buildMessage : messages) {
        addBuildParticipantMarker(project, markerManager, buildMessage, buildParticipantId);

        if(buildMessage.cause() != null) {
          buildErrors.remove(buildMessage.cause());
        }
      }
    });

    // Create markers for the build errors linked to mojo/plugin executions
    buildErrors.forEach((error, mojoExecutionKey) -> {
      SourceLocation markerLocation;
      if(mojoExecutionKey != null) {
        markerLocation = SourceLocationHelper.findLocation(mavenProject, mojoExecutionKey);
      } else {
        markerLocation = new SourceLocation(1, 0, 0);
      }
      BuildProblemInfo problem = new BuildProblemInfo(error, mojoExecutionKey, markerLocation);
      markerManager.addErrorMarker(project.getFile(IMavenConstants.POM_FILE_NAME), IMavenConstants.MARKER_BUILD_ID,
          problem);
    });

    if(result.hasExceptions()) {
      markerManager.addMarkers(project.getFile(IMavenConstants.POM_FILE_NAME), IMavenConstants.MARKER_BUILD_ID, result);
    }
  }

  private void deleteBuildParticipantMarkers(IProject project, IMavenMarkerManager markerManager, File file,
      String buildParticipantId) {
    IResource resource = MavenProjectUtils.getProjectResource(project, file);
    if(resource == null) {
      resource = project.getFile(IMavenConstants.POM_FILE_NAME);
    }
    try {
      markerManager.deleteMarkers(resource, IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, BUILD_PARTICIPANT_ID_ATTR_NAME,
          buildParticipantId);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private void addBuildParticipantMarker(IProject project, IMavenMarkerManager markerManager, Message buildMessage,
      String buildParticipantId) {

    IResource resource = MavenProjectUtils.getProjectResource(project, buildMessage.file());
    if(resource == null) {
      resource = project.getFile(IMavenConstants.POM_FILE_NAME);
    }
    int at = buildParticipantId.lastIndexOf('-');
    String pluginExecutionKey = buildParticipantId.substring(0, at);
    String message = buildMessage.message() + " (" + pluginExecutionKey + ')'; //$NON-NLS-1$
    IMarker marker = markerManager.addMarker(resource, IMavenConstants.MARKER_BUILD_PARTICIPANT_ID, message,
        buildMessage.line(), buildMessage.severity());
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

    final BuildResultCollector participantResults = new BuildResultCollector();
    List<BuildContext> incrementalContexts = setupProjectBuildContext(project, IncrementalProjectBuilder.CLEAN_BUILD,
        null, participantResults, null, DeltaType.UNKOWN);

    Map<Throwable, MojoExecutionKey> buildErrors = new LinkedHashMap<>();
    try {
      participants.forEach((mojoExecutionKey, buildParticipants) -> {
        for(InternalBuildParticipant participant : buildParticipants) {
          participantResults.setParticipantId(mojoExecutionKey.getKeyString() + "-" + participant.getClass().getName());
          participant.setMavenProjectFacade(projectFacade);
          participant.setGetDeltaCallback(getDeltaProvider());
          participant.setSession(session);
          participant.setBuildContext((AbstractEclipseBuildContext) incrementalContexts.get(0));
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
      });
    } catch(Exception e) {
      buildErrors.put(e, null);
    } finally {
      for(IIncrementalBuildFramework.BuildContext context : incrementalContexts) {
        context.release();
      }
    }

    // Refresh files modified by build participants/maven plugins
    refreshResources(project, participantResults.getFiles(), monitor);

    MavenExecutionResult result = session.getResult();
    processBuildResults(project, mavenProject, result, participantResults, buildErrors);
  }

  DeltaProvider getDeltaProvider() {
    return deltaProvider;
  }

  private static final class ProjectBuildState {

    private long lastBuild;

    private IProject project;

    public ProjectBuildState(IProject project) {
      this.project = project;
    }

    public void commit() {
      this.lastBuild = System.currentTimeMillis();
    }

    @Override
    public String toString() {
      return "BuildState for " + project + " lat reccorded timestamp "
          + DateFormat.getDateTimeInstance().format(new Date(lastBuild));
    }
  }

  private static final class ProjectBuildStateDelta implements BuildDelta, IAdaptable {

    private ProjectBuildState buildState;

    private BuildDelta delegate;

    /**
     * @param buildState
     * @param eclipseResourceBuildDelta
     */
    public ProjectBuildStateDelta(ProjectBuildState buildState, BuildDelta delegate) {
      this.buildState = buildState;
      this.delegate = delegate;
    }

    @Override
    public boolean hasDelta(File file) {
      //first check the delegate...
      if(delegate != null && delegate.hasDelta(file)) {
        return true;
      }
      //... now perform additional checks
      if(file.isFile()) {
        long lastModified = file.lastModified();
        if(lastModified > buildState.lastBuild) {
          //if the file is modified after the last build timestamp we assume it was modified even though not part of the current delta!
          return true;
        }
      }
      return false;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
      return Adapters.adapt(delegate, adapter);
    }

  }
}
