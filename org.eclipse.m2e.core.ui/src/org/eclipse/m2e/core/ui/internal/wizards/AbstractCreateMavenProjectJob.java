/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.IProgressConstants;

import org.eclipse.m2e.core.internal.jobs.MavenWorkspaceJob;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.ui.internal.actions.OpenMavenConsoleAction;


public abstract class AbstractCreateMavenProjectJob extends MavenWorkspaceJob {

  private List<IProject> createdProjects;

  /**
   * @since 1.8
   */
  public AbstractCreateMavenProjectJob(String name) {
    super(name);
  }

  @Override
  public final IStatus runInWorkspace(IProgressMonitor monitor) {
    setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
    createdProjects = null;
    AbstractCreateMavenProjectsOperation op = new AbstractCreateMavenProjectsOperation() {
      @Override
      protected List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException {
        return AbstractCreateMavenProjectJob.this.doCreateMavenProjects(monitor);
      }
    };
    try {
      op.run(monitor);
      List<IProject> projects = op.getCreatedProjects();
      if(projects != null) {
        createdProjects = Collections.unmodifiableList(projects);
      }
    } catch(InvocationTargetException e) {
      return AbstractCreateMavenProjectsOperation.toStatus(e);
    }
    return Status.OK_STATUS;
  }

  protected abstract List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException;

  protected static ArrayList<IProject> toProjects(List<IMavenProjectImportResult> results) {
    return AbstractCreateMavenProjectsOperation.toProjects(results);
  }

  /**
   * @return an unmodifiable list of created projects, or <code>null</code>
   * @since 1.6
   */
  public List<IProject> getCreatedProjects() {
    return createdProjects;
  }
}
