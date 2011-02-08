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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.IWorkingSet;


public class RefreshMavenModelsAction implements IWorkbenchWindowActionDelegate, IExecutableExtension {

  public static final String ID = "org.eclipse.m2e.refreshMavenModelsAction"; //$NON-NLS-1$

  public static final String ID_SNAPSHOTS = "org.eclipse.m2e.refreshMavenSnapshotsAction"; //$NON-NLS-1$
  
  private boolean updateSnapshots = false;

  private boolean offline = false;  // should respect global settings

  private IStructuredSelection selection;

  public RefreshMavenModelsAction() {
  }
  
  public RefreshMavenModelsAction(boolean updateSnapshots) {
    this.updateSnapshots = updateSnapshots;
  }
  
  // IExecutableExtension
  
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if("snapshots".equals(data)) { //$NON-NLS-1$
      this.updateSnapshots = true;
    }
  }
  
  // IWorkbenchWindowActionDelegate
  
  public void run(IAction action) {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    projectManager.refresh(new MavenUpdateRequest(getProjects(), offline, updateSnapshots));
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

  private IProject[] getProjects() {
    ArrayList<IProject> projectList = new ArrayList<IProject>();
    if(selection != null) {
      for(Iterator<?> it = selection.iterator(); it.hasNext();) {
        Object o = it.next();
        if(o instanceof IProject) {
          projectList.add((IProject) o);
        } else if(o instanceof IWorkingSet) {
          IWorkingSet workingSet = (IWorkingSet) o;
          for(IAdaptable adaptable : workingSet.getElements()) {
            IProject project = (IProject) adaptable.getAdapter(IProject.class);
            try {
              if(project != null && project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)) {
                projectList.add(project);
              }
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        }
      }
    }
    if(projectList.isEmpty()) {
      return ResourcesPlugin.getWorkspace().getRoot().getProjects();
    }
    return projectList.toArray(new IProject[projectList.size()]);
  }
  
}

