/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Mickael Istria (Red Hat Inc.) - Group operations to save CPU & RAM
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue;
import org.eclipse.m2e.core.project.MavenUpdateRequest;


public class ProjectRegistryRefreshJob extends Job implements IResourceChangeListener, IPreferenceChangeListener,
    IBackgroundProcessingQueue {
  private static final Logger log = LoggerFactory.getLogger(ProjectRegistryRefreshJob.class);

  private static final long SCHEDULE_DELAY = 1000L;

  private final Set<MavenUpdateRequest> queue = new LinkedHashSet<>();

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

  @Override
  public IStatus run(final IProgressMonitor monitor) {
    monitor.beginTask(Messages.ProjectRegistryRefreshJob_task_refreshing, IProgressMonitor.UNKNOWN);
    final ArrayList<MavenUpdateRequest> requests;
    synchronized(this.queue) {
      requests = new ArrayList<>(this.queue);
      this.queue.clear();
    }

    try {
      final MutableProjectRegistry newState = manager.newMutableProjectRegistry();
      try {
        manager.getMaven().execute((context, theMonitor) -> {
          // group requests
          Set<IFile> offlineForceDependencyUpdate = new HashSet<>();
          Set<IFile> offlineNotForceDependencyUpdate = new HashSet<>();
          Set<IFile> notOfflineForceDependencyUpdate = new HashSet<>();
          Set<IFile> notOfflineNotForceDependencyUpdate = new HashSet<>();
          for(MavenUpdateRequest request : requests) {
            if(theMonitor.isCanceled()) {
              throw new OperationCanceledException();
            }
            if(request.isOffline() && request.isForceDependencyUpdate()) {
              offlineForceDependencyUpdate.addAll(request.getPomFiles());
            } else if(request.isOffline() && !request.isForceDependencyUpdate()) {
              offlineNotForceDependencyUpdate.addAll(request.getPomFiles());
            } else if(!request.isOffline() && request.isForceDependencyUpdate()) {
              notOfflineForceDependencyUpdate.addAll(request.getPomFiles());
            } else if(!request.isOffline() && !request.isForceDependencyUpdate()) {
              notOfflineNotForceDependencyUpdate.addAll(request.getPomFiles());
            }
          }
          // process requests
          // true * true
          if(theMonitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          if(!offlineForceDependencyUpdate.isEmpty()) {
            manager.getMaven().execute(true, true, (aContext, aMonitor) -> {
              manager.refresh(newState, offlineForceDependencyUpdate, aMonitor);
              return null;
            }, theMonitor);
          }
          // true*false
          if(theMonitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          if(!offlineNotForceDependencyUpdate.isEmpty()) {
            manager.getMaven().execute(true, false, (aContext, aMonitor) -> {
              manager.refresh(newState, offlineNotForceDependencyUpdate, aMonitor);
              return null;
            }, theMonitor);
          }
          // false*true
          if(theMonitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          if(!notOfflineForceDependencyUpdate.isEmpty()) {
            manager.getMaven().execute(false, true, (aContext, aMonitor) -> {
              manager.refresh(newState, notOfflineForceDependencyUpdate, aMonitor);
              return null;
            }, theMonitor);
          }
          // false*false
          if(theMonitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          if(!notOfflineNotForceDependencyUpdate.isEmpty()) {
            manager.getMaven().execute(false, false, (aContext, aMonitor) -> {
              manager.refresh(newState, notOfflineNotForceDependencyUpdate, aMonitor);
              return null;
            }, theMonitor);
          }

          ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();
          getJobManager().beginRule(rule, monitor);
          try {
            manager.applyMutableProjectRegistry(newState, monitor);
          } finally {
            getJobManager().endRule(rule);
          }
          return null;
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

  @Override
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
      // MavenBuilder will synchronously read/refresh workspace Maven project state.
      // We still refresh opened projects because workspace does not run build after project open event.

      IResourceDelta delta = event.getDelta(); // workspace delta
      IResourceDelta[] projectDeltas = delta.getAffectedChildren();
      for(IResourceDelta projectDelta : projectDeltas) {
        IProject project = (IProject) projectDelta.getResource();
        if(!isMavenProject(project)) {
          continue;
        }
        //Bug 436679: queue update request only for reopened projects.
        //Imported projects (delta.getKind() == IResourceDelta.ADDED) will be taken care of by the builder.
        if((projectDelta.getKind() == IResourceDelta.CHANGED && (projectDelta.getFlags() & IResourceDelta.OPEN) != 0)) {
          queue(new MavenUpdateRequest(project, offline, forceDependencyUpdate));
        }
      }
    }

    synchronized(queue) {
      if(!queue.isEmpty()) {
        schedule(SCHEDULE_DELAY);
      }
    }
  }

  private void queue(MavenUpdateRequest updateRequest) {
    synchronized(queue) {
      queue.add(updateRequest);
      log.debug("Queued refresh request: {}", updateRequest.toString()); //$NON-NLS-1$
    }
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent event) {
    boolean offline = mavenConfiguration.isOffline();
    boolean updateSnapshots = false;

    if(event.getSource() instanceof IProject) {
      queue(new MavenUpdateRequest((IProject) event.getSource(), offline, updateSnapshots));
    }
  }

  @Override
  public boolean isEmpty() {
    synchronized(queue) {
      return queue.isEmpty();
    }
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
