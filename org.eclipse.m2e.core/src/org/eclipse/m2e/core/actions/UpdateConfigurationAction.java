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

package org.eclipse.m2e.core.actions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.progress.IProgressConstants;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.util.M2EUtils;


public class UpdateConfigurationAction implements IObjectActionDelegate {

  public static final String ID = "org.eclipse.m2e.updateConfigurationAction"; //$NON-NLS-1$

  private IStructuredSelection selection;

  private Shell shell;

  public UpdateConfigurationAction(Shell shell) {
    this.shell = shell;
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  public void run(IAction action) {
    final Set<IProject> projects = getProjects();
    final MavenPlugin plugin = MavenPlugin.getDefault();
    WorkspaceJob job = new WorkspaceJob(Messages.UpdateSourcesAction_job_update_conf) {
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
        monitor.beginTask(getName(), projects.size());

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
                  projectFacade.getResolverConfiguration(), //
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
          M2EUtils.showErrorsForProjectsDialog(shell, Messages.UpdateSourcesAction_error_title,
              Messages.UpdateSourcesAction_error_message, updateErrors);
        }
        long l2 = System.currentTimeMillis();
        console.logMessage(NLS.bind("Update completed: {0} sec", ((l2 - l1) / 1000)));

        return status != null ? status : Status.OK_STATUS;
      }
    };
    // We need to grab workspace lock because IJavaProject.setRawClasspath() needs it.
    job.setRule(plugin.getProjectConfigurationManager().getRule());
    job.schedule();
  }

  private Set<IProject> getProjects() {
    Set<IProject> projects = new LinkedHashSet<IProject>();
    if(selection != null) {
      for(Iterator<?> it = selection.iterator(); it.hasNext();) {
        Object element = it.next();
        if(element instanceof IProject) {
          projects.add((IProject) element);
        } else if(element instanceof IWorkingSet) {
          IWorkingSet workingSet = (IWorkingSet) element;
          for(IAdaptable adaptable : workingSet.getElements()) {
            IProject project = (IProject) adaptable.getAdapter(IProject.class);
            try {
              if(project != null && project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)) {
                projects.add(project);
              }
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        } else if(element instanceof IAdaptable) {
          IProject project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
          if(project != null) {
            projects.add(project);
          }
        }
      }
    }
    return projects;
  }

}
