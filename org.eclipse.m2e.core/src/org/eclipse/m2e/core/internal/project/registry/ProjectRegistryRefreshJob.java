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
import java.util.List;
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

import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenUpdateRequest;

public class ProjectRegistryRefreshJob extends Job implements IResourceChangeListener, IPreferenceChangeListener, IBackgroundProcessingQueue {
  private static final Logger log = LoggerFactory.getLogger(ProjectRegistryRefreshJob.class);

  private static final long SCHEDULE_DELAY = 1000L;

  private static final int DELTA_FLAGS = IResourceDelta.CONTENT | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
  | IResourceDelta.COPIED_FROM | IResourceDelta.REPLACED;
  
  private final List<MavenUpdateRequest> queue = new ArrayList<MavenUpdateRequest>();

  private final ProjectRegistryManager manager;
  
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
  
  public IStatus run(IProgressMonitor monitor) {
    monitor.beginTask(Messages.ProjectRegistryRefreshJob_task_refreshing, IProgressMonitor.UNKNOWN);
    ArrayList<MavenUpdateRequest> requests;
    synchronized(this.queue) {
      requests = new ArrayList<MavenUpdateRequest>(this.queue);
      this.queue.clear();
    }

    try {
      MutableProjectRegistry newState = manager.newMutableProjectRegistry();
      try {
        for (MavenUpdateRequest request : requests) {
          if(monitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          manager.refresh(newState, request, monitor);
        }
  
        ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();
        getJobManager().beginRule(rule, monitor);
        try {
          manager.applyMutableProjectRegistry(newState, monitor);
        } finally {
          getJobManager().endRule(rule);
        }
      } finally {
        newState.close();
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    } catch(OperationCanceledException ex) {
      log.info("{} was canceled", getClass().getName());
    } catch (StaleMutableProjectRegistryException e) {
      synchronized(this.queue) {
        this.queue.addAll(0, requests);
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
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    if (workspace != null && workspace.isAutoBuilding()) {
      return;
    }
    boolean offline = mavenConfiguration.isOffline();  
    boolean forceDependencyUpdate = false;

    int type = event.getType();

    if(IResourceChangeEvent.PRE_CLOSE == type || IResourceChangeEvent.PRE_DELETE == type) {
      queue(new MavenUpdateRequest((IProject) event.getResource(), offline, forceDependencyUpdate));
    } else {
      // if (IResourceChangeEvent.POST_CHANGE == type)
      IResourceDelta delta = event.getDelta(); // workspace delta
      IResourceDelta[] projectDeltas = delta.getAffectedChildren();
      Set<IProject> refreshProjects = new LinkedHashSet<IProject>();
      for(int i = 0; i < projectDeltas.length; i++ ) {
        if(projectChanged(projectDeltas[i])) {
          IProject project = (IProject) projectDeltas[i].getResource();
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

    if (event.getSource() instanceof IProject) {
      queue(new MavenUpdateRequest((IProject) event.getSource(), offline, updateSnapshots));
    }
  }

  public boolean isEmpty() {
    synchronized(queue) {
      return queue.isEmpty();
    }
  }

  protected boolean isInterestingDelta(IResourceDelta delta) {
    return delta.getKind() == IResourceDelta.REMOVED 
        || delta.getKind() == IResourceDelta.ADDED
        || (delta.getKind() == IResourceDelta.CHANGED && ((delta.getFlags() & DELTA_FLAGS) != 0));
  }
}
