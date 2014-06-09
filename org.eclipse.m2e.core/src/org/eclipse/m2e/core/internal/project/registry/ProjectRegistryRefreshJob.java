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

package org.eclipse.m2e.core.internal.project.registry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenUpdateRequest;


public class ProjectRegistryRefreshJob extends Job implements IResourceChangeListener, IPreferenceChangeListener,
    IBackgroundProcessingQueue {
  private static final Logger log = LoggerFactory.getLogger(ProjectRegistryRefreshJob.class);

  private static final long SCHEDULE_DELAY = 1000L;

  private static final int DELTA_FLAGS = IResourceDelta.CONTENT | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
      | IResourceDelta.COPIED_FROM | IResourceDelta.REPLACED;

  private final Set<MavenUpdateRequest> queue = new LinkedHashSet<MavenUpdateRequest>();

  /*package*/final ProjectRegistryManager manager;

  private final IMavenConfiguration mavenConfiguration;

  public ProjectRegistryRefreshJob(ProjectRegistryManager manager, IMavenConfiguration mavenConfiguration) {
    super(Messages.ProjectRegistryRefreshJob_title);
    this.manager = manager;
    this.mavenConfiguration = mavenConfiguration;
  }

  public void refresh(MavenUpdateRequest updateRequest) {
    queue(updateRequest);
    schedule(SCHEDULE_DELAY);
  }

  // Job

  public IStatus run(final IProgressMonitor monitor) {
    monitor.beginTask(Messages.ProjectRegistryRefreshJob_task_refreshing, IProgressMonitor.UNKNOWN);
    final ArrayList<MavenUpdateRequest> requests;
    synchronized(this.queue) {
      requests = new ArrayList<MavenUpdateRequest>(this.queue);
      this.queue.clear();
    }

    try {
      final MutableProjectRegistry newState = manager.newMutableProjectRegistry();
      try {
        manager.getMaven().execute(new ICallable<Void>() {
          public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
            for(final MavenUpdateRequest request : requests) {
              if(monitor.isCanceled()) {
                throw new OperationCanceledException();
              }
              manager.getMaven().execute(request.isOffline(), request.isForceDependencyUpdate(), new ICallable<Void>() {
                public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
                  manager.refresh(newState, request.getPomFiles(), monitor);
                  return null;
                }
              }, monitor);
            }

            ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();
            getJobManager().beginRule(rule, monitor);
            try {
              manager.applyMutableProjectRegistry(newState, monitor);
            } finally {
              getJobManager().endRule(rule);
            }
            return null;
          }
        }, monitor);
      } finally {
        newState.close();
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    } catch(OperationCanceledException ex) {
      log.info("{} was canceled", getClass().getName());
    } catch(StaleMutableProjectRegistryException e) {
      synchronized(this.queue) {
        // must preserve order of requests here
        requests.addAll(this.queue);
        this.queue.clear();
        this.queue.addAll(requests);
        if(!this.queue.isEmpty()) {
          schedule(SCHEDULE_DELAY);
        }
      }
    } catch(Exception ex) {
      log.error(ex.getMessage(), ex);
    } finally {
      monitor.done();
    }

    return Status.OK_STATUS;
  }

  // IResourceChangeListener

  public void resourceChanged(IResourceChangeEvent event) {
    boolean offline = mavenConfiguration.isOffline();
    boolean forceDependencyUpdate = false;

    int type = event.getType();

    if(IResourceChangeEvent.PRE_CLOSE == type || IResourceChangeEvent.PRE_DELETE == type) {
      IProject project = (IProject) event.getResource();
      if(isMavenProject(project)) {
        queue(new MavenUpdateRequest(project, offline, forceDependencyUpdate));
      }
    } else {
      // if (IResourceChangeEvent.POST_CHANGE == type)
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      boolean autobuilding = workspace != null && workspace.isAutoBuilding();

      // MavenBuilder will synchronously read/refresh workspace Maven project state.
      // To avoid double-work and/or locking between MavenBuilder and background registry refresh job, we skip project
      // refresh when workspace is autobuilding.
      // We still refresh opened projects because workspace does not run build after project open event.

      IResourceDelta delta = event.getDelta(); // workspace delta
      IResourceDelta[] projectDeltas = delta.getAffectedChildren();
      Set<IProject> refreshProjects = new LinkedHashSet<IProject>();
      for(int i = 0; i < projectDeltas.length; i++ ) {
        IResourceDelta projectDelta = projectDeltas[i];
        IProject project = (IProject) projectDelta.getResource();
        if(!isMavenProject(project)) {
          continue;
        }

        //Bug 436679: queue update request only for reopened projects. For imported projects, delta.getKind() == IResourceDelta.ADDED
        if((projectDelta.getKind() == IResourceDelta.CHANGED && (projectDelta.getFlags() & IResourceDelta.OPEN) != 0)) {
          queue(new MavenUpdateRequest(project, offline, forceDependencyUpdate));
        } else if(!autobuilding && projectChanged(projectDelta)) {
          IMavenProjectFacade facade = manager.getProject(project);
          if(facade == null || facade.isStale()) {
            // facade is up-to-date for resource change events fired right after project import
            refreshProjects.add(project);
          }
        }
      }

      if(!refreshProjects.isEmpty()) {
        IProject[] projects = refreshProjects.toArray(new IProject[refreshProjects.size()]);
        MavenUpdateRequest updateRequest = new MavenUpdateRequest(projects, offline, forceDependencyUpdate);
        queue(updateRequest);
      }
    }

    synchronized(queue) {
      if(!queue.isEmpty()) {
        schedule(SCHEDULE_DELAY);
      }
    }
  }

  private boolean projectChanged(IResourceDelta projectDelta) {
    for(IPath path : ProjectRegistryManager.METADATA_PATH) {
      IResourceDelta delta = projectDelta.findMember(path);
      if(delta != null && isInterestingDelta(delta)) {
        return true;
      }
    }
    return false;
  }

  private void queue(MavenUpdateRequest updateRequest) {
    synchronized(queue) {
      queue.add(updateRequest);
      log.debug("Queued refresh request: {}", updateRequest.toString()); //$NON-NLS-1$
    }
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    boolean offline = mavenConfiguration.isOffline();
    boolean updateSnapshots = false;

    if(event.getSource() instanceof IProject) {
      queue(new MavenUpdateRequest((IProject) event.getSource(), offline, updateSnapshots));
    }
  }

  public boolean isEmpty() {
    synchronized(queue) {
      return queue.isEmpty();
    }
  }

  protected boolean isInterestingDelta(IResourceDelta delta) {
    return delta.getKind() == IResourceDelta.REMOVED || delta.getKind() == IResourceDelta.ADDED
        || (delta.getKind() == IResourceDelta.CHANGED && ((delta.getFlags() & DELTA_FLAGS) != 0));
  }

  private boolean isMavenProject(IProject project) {
    try {
      return project != null && project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
    return false;
  }
}
