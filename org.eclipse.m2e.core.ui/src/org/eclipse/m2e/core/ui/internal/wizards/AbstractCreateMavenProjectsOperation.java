/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.ui.IWorkingSet;


public abstract class AbstractCreateMavenProjectsOperation implements IRunnableWithProgress {

  private final List<IWorkingSet> workingSets;

  public AbstractCreateMavenProjectsOperation(List<IWorkingSet> workingSets) {
    this.workingSets = workingSets;
  }

  protected abstract List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException;

  // PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(project, new IWorkingSet[] {workingSet});
  public static void addToWorkingSets(IProject project, List<IWorkingSet> workingSets) {
    if(workingSets != null && workingSets.size() > 0) {
      // IAdaptable[] elements = workingSet.adaptElements(new IAdaptable[] {project});
      // if(elements.length == 1) {
      for(IWorkingSet workingSet : workingSets) {
        if(workingSet != null) {
          IAdaptable[] oldElements = workingSet.getElements();
          IAdaptable[] newElements = new IAdaptable[oldElements.length + 1];
          System.arraycopy(oldElements, 0, newElements, 0, oldElements.length);
          newElements[oldElements.length] = project;
          workingSet.setElements(newElements);
        }
      }
    }
  }

  protected static ArrayList<IProject> toProjects(List<IMavenProjectImportResult> results) {
    ArrayList<IProject> projects = new ArrayList<IProject>();
    for(IMavenProjectImportResult result : results) {
      if(result.getProject() != null) {
        projects.add(result.getProject());
      }
    }
    return projects;
  }

  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();
    Job.getJobManager().beginRule(rule, monitor);
    try {
      List<IProject> projects;
      try {
        projects = doCreateMavenProjects(monitor);
        if(projects != null) {
          for(IProject project : projects) {
            addToWorkingSets(project, workingSets);
          }
        }
      } catch(CoreException e) {
        throw new InvocationTargetException(e);
      }
    } finally {
      Job.getJobManager().endRule(rule);
    }
  }

  public static IStatus toStatus(InvocationTargetException e) {
    Throwable t = e.getCause();
    if (t instanceof CoreException) {
      return ((CoreException) t).getStatus();
    }
    return new Status(IStatus.ERROR, M2EUIPluginActivator.PLUGIN_ID, t.getMessage(), t);
  }
}
