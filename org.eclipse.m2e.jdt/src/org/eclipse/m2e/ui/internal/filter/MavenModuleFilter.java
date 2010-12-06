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

package org.eclipse.m2e.ui.internal.filter;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;

/**
 * MavenModuleFilter
 *
 * @author Eugene Kuleshov
 */
public class MavenModuleFilter extends ViewerFilter {

  public boolean select(Viewer viewer, Object parentElement, Object element) {
    if(element instanceof IFolder) {
      IFolder folder = (IFolder) element;
      IProject project = folder.getProject();
      try {
        if(project.hasNature(IMavenConstants.NATURE_ID)) {
          MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();

          IMavenProjectFacade projectFacade = projectManager.create(project, null);
          if(projectFacade != null) {
            // XXX implement corner cases
            // modules have ".." in the path
            // modules have more then one segment in the path
            // modules not imported in workspace
            MavenProject mavenProject = projectFacade.getMavenProject(null);
            IPath folderPath = folder.getFullPath();

            // workspace-relative path sans project name
            String folderName = folderPath.removeFirstSegments(1).toPortableString();

            if(mavenProject.getModules().contains(folderName)) {
              return false;
            }
          }
        }
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
      
    }
    return true;
  }

}

