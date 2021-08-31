/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - Return created projects
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.WorkingSets;


public abstract class AbstractCreateMavenProjectsOperation implements IRunnableWithProgress {

  @Deprecated
  private List<IWorkingSet> workingSets;

  /**
   * @since 1.8
   */
  public AbstractCreateMavenProjectsOperation() {
  }

  /**
   * A no-arg constructor should be used along with a {@link MavenProjectWorkspaceAssigner} instead.
   */
  @Deprecated
  public AbstractCreateMavenProjectsOperation(List<IWorkingSet> workingSets) {
    this.workingSets = workingSets;
  }

  private List<IProject> createdProjects;

  protected abstract List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException;

  protected static ArrayList<IProject> toProjects(List<IMavenProjectImportResult> results) {
    ArrayList<IProject> projects = new ArrayList<>();
    for(IMavenProjectImportResult result : results) {
      if(result.getProject() != null) {
        projects.add(result.getProject());
      }
    }
    return projects;
  }

  @Override
  public void run(IProgressMonitor monitor) throws InvocationTargetException {
    ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();
    Job.getJobManager().beginRule(rule, monitor);
    try {
      try {
        this.createdProjects = doCreateMavenProjects(monitor);
        if(workingSets != null) {
          WorkingSets.addToWorkingSets(createdProjects, workingSets);
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
    if(t instanceof CoreException) {
      return ((CoreException) t).getStatus();
    }
    return new Status(IStatus.ERROR, M2EUIPluginActivator.PLUGIN_ID, t.getMessage(), t);
  }

  /**
   * Returns a list of {@link IProject}s created by this operation.
   *
   * @since 1.5.0
   */
  public List<IProject> getCreatedProjects() {
    return createdProjects;
  }

}
