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

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.ui.internal.actions.OpenMavenConsoleAction;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.progress.IProgressConstants;


public abstract class AbstactCreateMavenProjectJob extends WorkspaceJob {

  private final List<IWorkingSet> workingSets;

  public AbstactCreateMavenProjectJob(String name, List<IWorkingSet> workingSets) {
    super(name);
    this.workingSets = workingSets;
  }

  @Override
  public final IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
    AbstractCreateMavenProjectsOperation op = new AbstractCreateMavenProjectsOperation(workingSets) {
      @Override
      protected List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException {
        return doCreateMavenProjects(monitor);
      }
    };
    try {
      op.run(monitor);
    } catch(InvocationTargetException e) {
      return AbstractCreateMavenProjectsOperation.toStatus(e);
    } catch(InterruptedException e) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  protected abstract List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException;

  protected static ArrayList<IProject> toProjects(List<IMavenProjectImportResult> results) {
    return AbstractCreateMavenProjectsOperation.toProjects(results);
  }
  
}
