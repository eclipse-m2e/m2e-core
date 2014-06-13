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

package org.eclipse.m2e.jdt.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * MavenClasspathContainerInitializer
 * 
 * @author Eugene Kuleshov
 */
public class MavenClasspathContainerInitializer extends ClasspathContainerInitializer {
  private static final Logger log = LoggerFactory.getLogger(MavenClasspathContainerInitializer.class);

  public void initialize(IPath containerPath, IJavaProject project) {
    if(MavenClasspathHelpers.isMaven2ClasspathContainer(containerPath)) {
      try {
        IClasspathContainer mavenContainer = getBuildPathManager().getSavedContainer(project.getProject());
        if(mavenContainer != null) {
          JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
              new IClasspathContainer[] {mavenContainer}, new NullProgressMonitor());
          return;
        }
      } catch(CoreException ex) {
        log.error("Exception initializing classpath container " + containerPath.toString(), ex);
      }
    }
  }

  public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
    return true;
  }

  public void requestClasspathContainerUpdate(IPath containerPath, final IJavaProject project,
      final IClasspathContainer containerSuggestion) {
    // one job per request. assumption that users are not going to change hundreds of containers simultaneously.
    new Job(Messages.MavenClasspathContainerInitializer_job_name) {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          getBuildPathManager().persistAttachedSourcesAndJavadoc(project, containerSuggestion, monitor);
        } catch(CoreException ex) {
          log.error(ex.getMessage(), ex);
          return new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0,
              Messages.MavenClasspathContainerInitializer_error_cannot_persist, ex);
        }
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  BuildPathManager getBuildPathManager() {
    return (BuildPathManager) MavenJdtPlugin.getDefault().getBuildpathManager();
  }

  IMavenProjectRegistry getMavenProjectManager() {
    return MavenPlugin.getMavenProjectRegistry();
  }
}
