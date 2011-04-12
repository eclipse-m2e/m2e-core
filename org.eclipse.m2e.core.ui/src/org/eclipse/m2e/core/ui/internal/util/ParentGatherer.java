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

package org.eclipse.m2e.core.ui.internal.util;

import java.util.LinkedList;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

/**
 * Helper class to get the parent chain given a pom 
 */
public class ParentGatherer {
  private MavenProject mavenProject;

  private IMavenProjectFacade projectFacade;

  public ParentGatherer(MavenProject leafProject, IMavenProjectFacade facade) {
    this.mavenProject = leafProject;
    this.projectFacade = facade;
  }

  /**
   * Return the list of parents for a give pom
   * @param monitor
   * @return list of {@link MavenProject} from the given project to its ultimate parent. 
   * The first entry is the given pom, the last one the ultimate parent.
   * @throws CoreException
   */
  public LinkedList<MavenProject> getParentHierarchy(IProgressMonitor monitor) throws CoreException {
    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    IMaven maven = MavenPlugin.getDefault().getMaven();
    IMavenProjectRegistry projectManager = MavenPlugin.getDefault().getMavenProjectRegistry();
    maven.detachFromSession(mavenProject);

    hierarchy.add(mavenProject);

    MavenProject project = mavenProject;
    while(project.getModel().getParent() != null) {
      if(monitor.isCanceled()) {
        return null;
      }
      MavenExecutionRequest request = projectManager.createExecutionRequest(projectFacade, monitor);
      project = maven.resolveParentProject(request, project, monitor);
      hierarchy.add(project);
    }
    return hierarchy;
  }
}
