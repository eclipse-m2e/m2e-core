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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
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

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class MavenBuilder extends IncrementalProjectBuilder implements DeltaProvider {
  /*package*/static Logger log = LoggerFactory.getLogger(MavenBuilder.class);

  /*package*/final MavenBuilderImpl builder = new MavenBuilderImpl(this);

  /*package*/final MavenImpl maven = MavenPluginActivator.getDefault().getMaven();

  /*package*/final IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();

  /*package*/final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

  /*package*/final IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();

  /*package*/final IMavenMarkerManager markerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();

  /*
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected IProject[] build(final int kind, final Map<String, String> args, final IProgressMonitor monitor)
      throws CoreException {
    final IProject project = getProject();
    log.debug("Building project {}", project.getName()); //$NON-NLS-1$
    final long start = System.currentTimeMillis();

    markerManager.deleteMarkers(project, kind == FULL_BUILD, IMavenConstants.MARKER_BUILD_ID);

    if(!project.hasNature(IMavenConstants.NATURE_ID)) {
      return null;
    }

    final IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
    if(pomResource == null) {
      log.error("Project {} does not have pom.xml", project.getName());
      return null;
    }

    return maven.execute(new ICallable<IProject[]>() {
      public IProject[] call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        final IMavenProjectFacade projectFacade = getProjectFacade(pomResource, project, monitor);

        if(projectFacade == null) {
          // TODO unit test me
          return null;
        }

        return projectManager.execute(projectFacade, new ICallable<IProject[]>() {
          public IProject[] call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
            try {
              // make sure projectFacade has MavenProject instance loaded 
              projectFacade.getMavenProject(monitor);
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

            Set<IProject> dependencies = builder.build(context.getSession(), projectFacade, kind, args,
                buildParticipantsByMojoExecutionKey, monitor);

            log.debug("Built project {} in {} ms", project.getName(), System.currentTimeMillis() - start); //$NON-NLS-1$
            if(dependencies.isEmpty()) {
              return null;
            }
            return dependencies.toArray(new IProject[dependencies.size()]);
          }
        }, monitor);
      }
    }, monitor);

  }

  protected void clean(final IProgressMonitor monitor) throws CoreException {
    final IProject project = getProject();

    markerManager.deleteMarkers(project, IMavenConstants.MARKER_BUILD_ID);

    if(!project.hasNature(IMavenConstants.NATURE_ID)) {
      return;
    }

    final IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
    if(pomResource == null) {
      return;
    }

    maven.execute(new ICallable<Void>() {
      public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        final IMavenProjectFacade projectFacade = projectManager.create(getProject(), monitor);

        if(projectFacade == null) {
          return null;
        }

        return projectManager.execute(projectFacade, new ICallable<Void>() {
          public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {

            // 380096 make sure facade.getMavenProject() is not null
            if(projectFacade.getMavenProject(monitor) == null) {
              return null;
            }

            ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade);
            if(lifecycleMapping == null) {
              return null;
            }

            Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipantsByMojoExecutionKey = lifecycleMapping
                .getBuildParticipants(projectFacade, monitor);

            MavenProject mavenProject = null;
            try {
              mavenProject = projectFacade.getMavenProject(monitor);
            } catch(CoreException ce) {
              //the pom cannot be read. don't fill the log full of junk, just add an error marker
              addErrorMarker(project, ce);
              return null;
            }

            builder.clean(context.getSession(), projectFacade, buildParticipantsByMojoExecutionKey, monitor);
            return null;
          }
        }, monitor);
      }
    }, monitor);
  }

  /*package*/void addErrorMarker(IProject project, Exception e) {
    String msg = e.getMessage();
    String rootCause = M2EUtils.getRootCauseMessage(e);
    if(!e.equals(msg)) {
      msg = msg + ": " + rootCause; //$NON-NLS-1$
    }

    IMavenMarkerManager markerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();
    markerManager.addMarker(project, IMavenConstants.MARKER_BUILD_ID, msg, 1, IMarker.SEVERITY_ERROR);
  }

  /*package*/IMavenProjectFacade getProjectFacade(final IFile pomResource, final IProject project,
      final IProgressMonitor monitor) throws CoreException {

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

    IMavenProjectFacade projectFacade = projectManager.create(getProject(), monitor);

    if(force || projectFacade == null || projectFacade.isStale()) {
      projectManager.refresh(Collections.singleton(pomResource), monitor);
      projectFacade = projectManager.create(project, monitor);
      if(projectFacade == null) {
        // error marker should have been created
        return null;
      }
    }

    return projectFacade;
  }

  private static final List<BuildDebugHook> debugHooks = new ArrayList<BuildDebugHook>();

  public static void addDebugHook(BuildDebugHook hook) {
    synchronized(debugHooks) {
      for(BuildDebugHook other : debugHooks) {
        if(other == hook) {
          return;
        }
      }
      debugHooks.add(hook);
    }
  }

  public static void removeDebugHook(BuildDebugHook hook) {
    synchronized(debugHooks) {
      ListIterator<BuildDebugHook> iter = debugHooks.listIterator();
      while(iter.hasNext()) {
        if(iter.next() == hook) {
          iter.remove();
          break;
        }
      }
    }
  }

  public static Collection<BuildDebugHook> getDebugHooks() {
    synchronized(debugHooks) {
      return new ArrayList<BuildDebugHook>(debugHooks);
    }
  }
}
