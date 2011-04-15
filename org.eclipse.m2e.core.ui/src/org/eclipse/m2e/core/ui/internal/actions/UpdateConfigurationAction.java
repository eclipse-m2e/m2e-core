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

package org.eclipse.m2e.core.ui.internal.actions;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.UpdateConfigurationJob;


public class UpdateConfigurationAction implements IObjectActionDelegate {
  private static final Logger log = LoggerFactory.getLogger(UpdateConfigurationAction.class);

  public static final String ID = "org.eclipse.m2e.updateConfigurationAction"; //$NON-NLS-1$

  private IStructuredSelection selection;

  public UpdateConfigurationAction() {
  }
  
  public UpdateConfigurationAction(Shell shell) {
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
    new UpdateConfigurationJob(projects.toArray(new IProject[projects.size()])).schedule();
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
              log.error(ex.getMessage(), ex);
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
