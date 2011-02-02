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

package org.eclipse.m2e.core.jobs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.IProgressConstants;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.OpenMavenConsoleAction;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.util.M2EUtils;


public class UpdateConfigurationJob extends WorkspaceJob {

  private IProject[] projects;

  private MavenPlugin plugin;

  private Shell shell;

  public UpdateConfigurationJob(MavenPlugin plugin, IProject[] projects) {
    this(plugin, projects, null);
  }

  public UpdateConfigurationJob(MavenPlugin plugin, IProject[] projects, Shell shell) {
    this(plugin, shell);
    this.projects = projects;
  }

  private UpdateConfigurationJob(MavenPlugin plugin, Shell shell) {
    super(Messages.UpdateSourcesAction_job_update_conf);
    this.plugin = plugin;
    this.shell = shell;
    setRule(this.plugin.getProjectConfigurationManager().getRule());
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
   */
  public IStatus runInWorkspace(IProgressMonitor monitor) {
    setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
    monitor.beginTask(getName(), projects.length);

    MavenConsole console = plugin.getConsole();

    long l1 = System.currentTimeMillis();
    console.logMessage("Update started");

    MultiStatus status = null;
    //project names to the errors encountered when updating them
    Map<String, Throwable> updateErrors = new HashMap<String, Throwable>();

    for(IProject project : projects) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      monitor.subTask(project.getName());
      IMavenProjectFacade projectFacade = plugin.getMavenProjectManager().create(project, monitor);
      if(projectFacade != null) {
      try {
          plugin.getProjectConfigurationManager().updateProjectConfiguration(project, //
              new SubProgressMonitor(monitor, 1));
      } catch(CoreException ex) {
        if(status == null) {
          status = new MultiStatus(IMavenConstants.PLUGIN_ID, IStatus.ERROR, //
              Messages.UpdateSourcesAction_error_cannot_update, null);
        }
        status.add(ex.getStatus());
        updateErrors.put(project.getName(), ex);
      } catch(IllegalArgumentException e) {
        status = new MultiStatus(IMavenConstants.PLUGIN_ID, IStatus.ERROR, //
            Messages.UpdateSourcesAction_error_cannot_update, null);
        updateErrors.put(project.getName(), e);
      }
    }
    }
    if(updateErrors.size() > 0) {
      handleErrors(updateErrors);
    }
    long l2 = System.currentTimeMillis();
    console.logMessage(NLS.bind("Update completed: {0} sec", ((l2 - l1) / 1000)));

    return status != null ? status : Status.OK_STATUS;
  }

  private void handleErrors(final Map<String, Throwable> updateErrors) {
    final Display display = Display.getDefault();
    if(display != null) {
      display.asyncExec(new Runnable() {

        public void run() {
          M2EUtils.showErrorsForProjectsDialog(shell != null ? shell : display.getActiveShell(),
              Messages.UpdateSourcesAction_error_title, Messages.UpdateSourcesAction_error_message, updateErrors);
        }
      });
    }
  }
}