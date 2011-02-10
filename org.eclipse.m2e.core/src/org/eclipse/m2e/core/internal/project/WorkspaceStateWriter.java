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

package org.eclipse.m2e.core.internal.project;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;

import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenProjectManager;

/**
 * Maintains map file of maven artifacts present in workspace.   
 */
public class WorkspaceStateWriter implements IMavenProjectChangedListener {
  private static final Logger log = LoggerFactory.getLogger(WorkspaceStateWriter.class);

  private MavenProjectManager projectManager;

  public WorkspaceStateWriter(MavenProjectManager projectManager) {
    this.projectManager = projectManager;
  }
  
  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    try {
      Properties state = new Properties();

      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      for(IMavenProjectFacade projectFacade : projectManager.getProjects()) {
        try {
          Artifact artifact = projectFacade.getMavenProject(monitor).getArtifact();
          IFile pomFile = projectFacade.getPom();
          IPath location = pomFile.getLocation();
          if(location != null) {
            File pom = location.toFile();
            if(pom.canRead()) {
              String key = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":pom:" + artifact.getBaseVersion(); //$NON-NLS-1$ //$NON-NLS-2$
              state.put(key, pom.getCanonicalPath());
            }
          }
          IResource outputLocation = root.findMember(projectFacade.getOutputLocation());
          if (!"pom".equals(artifact.getType()) && outputLocation != null && outputLocation.exists()) { //$NON-NLS-1$
            String key = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + artifact.getBaseVersion(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            state.put(key, outputLocation.getLocation().toFile().getCanonicalPath());
          }
        } catch (CoreException ex) {
          log.error("Error writing workspace state file", ex);
        }
      }

      OutputStream buf = new BufferedOutputStream(new FileOutputStream(projectManager.getWorkspaceStateFile()));
      try {
        state.store(buf, null);
      } finally {
        buf.close();
      }
    } catch(IOException ex) {
      log.error("Error writing workspace state file", ex);
    }
  }
}
