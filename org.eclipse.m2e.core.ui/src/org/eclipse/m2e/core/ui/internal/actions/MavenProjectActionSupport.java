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

package org.eclipse.m2e.core.ui.internal.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.m2e.core.internal.IMavenConstants;


public abstract class MavenProjectActionSupport extends MavenActionSupport implements IWorkbenchWindowActionDelegate {

  private static final Logger log = LoggerFactory.getLogger(MavenProjectActionSupport.class);

  protected IProject[] getProjects() {
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
              log.error(ex.getMessage(), ex);
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

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

}
