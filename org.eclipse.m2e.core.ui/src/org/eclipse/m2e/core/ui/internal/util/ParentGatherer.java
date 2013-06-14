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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * Helper class to get the parent chain given a pom
 */
@SuppressWarnings("restriction")
public class ParentGatherer {
  final IMavenProjectFacade projectFacade;

  public ParentGatherer(IMavenProjectFacade facade) {
    this.projectFacade = facade;
  }

  /**
   * Return the list of parents for a give pom
   * 
   * @param monitor
   * @return list of {@link MavenProject} from the given project to its ultimate parent. The first entry is the given
   *         pom, the last one the ultimate parent.
   * @throws CoreException
   */
  public List<ParentHierarchyEntry> getParentHierarchy(final IProgressMonitor monitor) throws CoreException {
    final List<ParentHierarchyEntry> hierarchy = new ArrayList<ParentHierarchyEntry>();
    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();

    final IMaven maven = MavenPlugin.getMaven();

    final MavenProject mavenProject = projectFacade.getMavenProject(monitor);

    hierarchy.add(new ParentHierarchyEntry(mavenProject, projectFacade));

    projectManager.execute(projectFacade, new ICallable<Void>() {
      public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        MavenProject project = mavenProject;
        while(project.getModel().getParent() != null) {
          if(monitor.isCanceled()) {
            return null;
          }
          project = maven.resolveParentProject(project, monitor);
          IFile resource = M2EUtils.getPomFile(project); // resource is null if parent is not coming from workspace
          IMavenProjectFacade facade = resource != null ? MavenPlugin.getMavenProjectRegistry().getProject(
              resource.getProject()) : null;
          hierarchy.add(new ParentHierarchyEntry(project, facade));
        }
        return null;
      }
    }, monitor);

    return hierarchy;
  }
}
