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
 *******************************************************************************/

package org.eclipse.m2e.core.internal.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class MavenBuilder extends IncrementalProjectBuilder implements DeltaProvider {
  private static final Logger log = LoggerFactory.getLogger(MavenBuilder.class);

  final MavenBuilderImpl builder = new MavenBuilderImpl(this);

  private final ProjectRegistryManager projectManager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();

  private final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

  private final IMavenMarkerManager markerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();

  private abstract class BuildMethod<T> {

    public final T execute(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
      IProject project = getProject();
      boolean includeSubtypes = kind == FULL_BUILD || kind == CLEAN_BUILD;
      markerManager.deleteMarkers(project, includeSubtypes, IMavenConstants.MARKER_BUILD_ID);
      IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
      if(pomResource == null) {
        return null;
      }

      IProjectConfiguration resolverConfiguration = configurationManager.getProjectConfiguration(project);

      if(resolverConfiguration == null) {
        // TODO unit test me
        return null;
      }

      IMavenExecutionContext context = projectManager.createExecutionContext(pomResource, resolverConfiguration);

      return context.execute((context2, monitor2) -> {
        IMavenProjectFacade projectFacade = getProjectFacade(project, monitor2);

        if(projectFacade == null) {
          return null;
        }

        MavenProject mavenProject;
        try {
          // make sure projectFacade has MavenProject instance loaded
          mavenProject = projectFacade.getMavenProject(monitor2);
        } catch(CoreException ce) {
          //unable to read the project facade
          addErrorMarker(project, ce);
          return null;
        }

        return context2.execute(mavenProject, (context1, monitor1) -> {
          ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade);
          if(lifecycleMapping == null) {
            return null;
          }

          Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipantsByMojoExecutionKey = lifecycleMapping
              .getBuildParticipants(projectFacade, monitor1);

          return method(context1, projectFacade, buildParticipantsByMojoExecutionKey, kind, args, monitor1);
        }, monitor2);
      }, monitor);
    }

    abstract T method(IMavenExecutionContext context, IMavenProjectFacade projectFacade,
        Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipantsByMojoExecutionKey, int kind,
        Map<String, String> args, IProgressMonitor monitor) throws CoreException;

    void addErrorMarker(IProject project, Exception e) {
      String msg = e.getMessage();
      String rootCause = M2EUtils.getRootCauseMessage(e);
      if(msg != null && !msg.equals(rootCause)) {
        msg = msg + ": " + rootCause; //$NON-NLS-1$
      }

      markerManager.addMarker(project, IMavenConstants.MARKER_BUILD_ID, msg, 1, IMarker.SEVERITY_ERROR);
    }

    IMavenProjectFacade getProjectFacade(IProject project, IProgressMonitor monitor) throws CoreException {
      IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);

      // facade refresh should be forced whenever pom.xml has changed
      // there is no delta info for full builds
      // but these are usually forced from Project/Clean
      // so assume pom did not change
      boolean force = false;

      IResourceDelta delta = getDelta(project);
      if(delta != null) {
        delta = delta.findMember(pomResource.getFullPath());
        force = delta != null && delta.getKind() == IResourceDelta.CHANGED;
      }

      IMavenProjectFacade projectFacade = projectManager.getProject(project);

      if(force || projectFacade == null || projectFacade.isStale()) {
        projectManager.refresh(Collections.singleton(pomResource), monitor);
        projectFacade = projectManager.getProject(project);
        if(projectFacade == null) {
          // error marker should have been created
          return null;
        }
      }

      return projectFacade;
    }
  }

  private final BuildMethod<IProject[]> methodBuild = new BuildMethod<>() {
    @Override
    protected IProject[] method(IMavenExecutionContext context, IMavenProjectFacade projectFacade,
        Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipantsByMojoExecutionKey, int kind,
        Map<String, String> args, IProgressMonitor monitor) throws CoreException {

      Set<IProject> dependencies = builder.build(context.getSession(), projectFacade, kind, args,
          buildParticipantsByMojoExecutionKey, monitor);

      if(dependencies.isEmpty()) {
        return null;
      }

      return dependencies.toArray(new IProject[dependencies.size()]);
    }
  };

  private final BuildMethod<Void> methodClean = new BuildMethod<>() {
    @Override
    protected Void method(IMavenExecutionContext context, IMavenProjectFacade projectFacade,
        Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipantsByMojoExecutionKey, int kind,
        Map<String, String> args, IProgressMonitor monitor) throws CoreException {

      builder.clean(context.getSession(), projectFacade, buildParticipantsByMojoExecutionKey, monitor);

      return null;
    }
  };

  @Override
  protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
    log.debug("Building project {}", getProject().getName()); //$NON-NLS-1$
    long start = System.currentTimeMillis();
    try {
      return methodBuild.execute(kind, args, monitor);
    } finally {
      log.debug("Built project {} in {} ms", getProject().getName(), System.currentTimeMillis() - start); //$NON-NLS-1$
    }
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    log.debug("Cleaning project {}", getProject().getName()); //$NON-NLS-1$
    long start = System.currentTimeMillis();

    try {
      methodClean.execute(CLEAN_BUILD, Collections.emptyMap(), monitor);
    } finally {
      log.debug("Cleaned project {} in {} ms", getProject().getName(), System.currentTimeMillis() - start); //$NON-NLS-1$
    }
  }

  private static final List<BuildDebugHook> debugHooks = new ArrayList<>();

  public static void addDebugHook(BuildDebugHook hook) {
    synchronized(debugHooks) {
      if(debugHooks.stream().noneMatch(h -> h == hook)) {
        debugHooks.add(hook);
      }
    }
  }

  public static void removeDebugHook(BuildDebugHook hook) {
    synchronized(debugHooks) {
      debugHooks.removeIf(h -> h == hook);
    }
  }

  public static Collection<BuildDebugHook> getDebugHooks() {
    synchronized(debugHooks) {
      return new ArrayList<>(debugHooks);
    }
  }

  @Override
  public ISchedulingRule getRule(int kind, Map<String, String> args) {
    if(MavenPlugin.getMavenConfiguration().buildWithNullSchedulingRule()) {
      return null;
    }
    return super.getRule(kind, args);
  }
}
