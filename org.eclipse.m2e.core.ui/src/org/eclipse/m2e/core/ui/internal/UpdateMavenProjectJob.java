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

package org.eclipse.m2e.core.ui.internal;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.ui.internal.actions.OpenMavenConsoleAction;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;


public class UpdateMavenProjectJob extends WorkspaceJob {
  private static final Logger log = LoggerFactory.getLogger(UpdateMavenProjectJob.class);

  private final IProject[] projects;

  private final boolean offline;

  private final boolean forceUpdateDependencies;

  private final boolean updateConfiguration;

  private final boolean rebuild;

  public UpdateMavenProjectJob(IProject[] projects) {
    this(projects, MavenPlugin.getMavenConfiguration().isOffline(), false /*forceUpdateDependencies*/,
        true /*updateConfiguration*/, true /*rebuild*/);
  }

  public UpdateMavenProjectJob(IProject[] projects, boolean offline, boolean forceUpdateDependencies,
      boolean updateConfiguration, boolean rebuild) {

    super(Messages.UpdateSourcesAction_job_update_conf);

    this.projects = projects;
    this.offline = offline;
    this.forceUpdateDependencies = forceUpdateDependencies;
    this.updateConfiguration = updateConfiguration;
    this.rebuild = rebuild;

    setRule(MavenPlugin.getProjectConfigurationManager().getRule());
  }

  public IStatus runInWorkspace(IProgressMonitor monitor) {
    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
    IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();
    boolean autoBuilding = ResourcesPlugin.getWorkspace().isAutoBuilding();

    setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
    monitor.beginTask(getName(), projects.length);

    long l1 = System.currentTimeMillis();
    log.info("Update started"); //$NON-NLS-1$

    MultiStatus status = null;
    //project names to the errors encountered when updating them
    Map<String, Throwable> updateErrors = new HashMap<String, Throwable>();

    for(IProject project : projects) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      monitor.subTask(project.getName());
      SubProgressMonitor submonitor = new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);

      try {
        MavenUpdateRequest request = new MavenUpdateRequest(project, offline, forceUpdateDependencies);
        if(updateConfiguration) {
          configurationManager.updateProjectConfiguration(request, submonitor);
        } else {
          projectRegistry.refresh(request, submonitor);
        }
        // only rebuild projects that were successfully updated
        if(rebuild) {
          project.build(IncrementalProjectBuilder.CLEAN_BUILD, submonitor);
          if(autoBuilding) {
            // TODO this is not enough, in most cases we need to re-run the build several times
            project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, submonitor);
          }
        }
      } catch(CoreException ex) {
        if(status == null) {
          status = new MultiStatus(M2EUIPluginActivator.PLUGIN_ID, IStatus.ERROR, //
              Messages.UpdateSourcesAction_error_cannot_update, null);
        }
        status.add(ex.getStatus());
        updateErrors.put(project.getName(), ex);
      } catch(IllegalArgumentException e) {
        status = new MultiStatus(M2EUIPluginActivator.PLUGIN_ID, IStatus.ERROR, //
            Messages.UpdateSourcesAction_error_cannot_update, null);
        updateErrors.put(project.getName(), e);
      }
    }

    if(updateErrors.size() > 0) {
      handleErrors(updateErrors);
    }
    long l2 = System.currentTimeMillis();
    log.info(NLS.bind("Update completed: {0} sec", ((l2 - l1) / 1000))); //$NON-NLS-1$

    return status != null ? status : Status.OK_STATUS;
  }

  private void handleErrors(final Map<String, Throwable> updateErrors) {
    final Display display = Display.getDefault();
    if(display != null) {
      display.asyncExec(new Runnable() {
        public void run() {
          M2EUIUtils.showErrorsForProjectsDialog(display.getActiveShell(), Messages.UpdateSourcesAction_error_title,
              Messages.UpdateSourcesAction_error_message, updateErrors);
        }
      });
    }
  }
}
