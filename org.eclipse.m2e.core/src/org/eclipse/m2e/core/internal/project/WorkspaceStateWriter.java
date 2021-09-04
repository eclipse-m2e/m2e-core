/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.project;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectManager;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.workspace.MutableWorkspaceState;


/**
 * Maintains map file of maven artifacts present in workspace.
 */
public class WorkspaceStateWriter implements IMavenProjectChangedListener {
  private static QualifiedName PPROP_EXTENSION = new QualifiedName(WorkspaceStateWriter.class.getName(), "extension"); //$NON-NLS-1$

  private static final Logger log = LoggerFactory.getLogger(WorkspaceStateWriter.class);

  private final MavenProjectManager projectManager;

  public WorkspaceStateWriter(MavenProjectManager projectManager) {
    this.projectManager = projectManager;
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    try {
      MutableWorkspaceState state = new MutableWorkspaceState();

      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      for(IMavenProjectFacade projectFacade : projectManager.getProjects()) {
        IProject project = projectFacade.getProject();
        if(!project.isAccessible()) {
          log.debug("Project registry contains closed project {}", project);
          // this is actually a bug somewhere in registry refresh logic, closed projects should not be there
          continue;
        }
        try {
          ArtifactKey artifact = projectFacade.getArtifactKey();
          IFile pomFile = projectFacade.getPom();
          IPath location = pomFile.getLocation();
          if(location != null) {
            File pom = location.toFile();
            if(pom.canRead()) {
              state.putPom(pom, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            }
          }
          IResource outputLocation = root.findMember(projectFacade.getOutputLocation());
          if(!"pom".equals(projectFacade.getPackaging()) && outputLocation != null && outputLocation.exists()) { //$NON-NLS-1$
            // three cases to consider
            // 1. facade has cached MavenProject instance, i.e. it was refreshed during this eclipse session
            // 2. project has persistent PPROP_EXTENSION
            // 3. neither cached MavenProject instance nor PPROP_EXTENSION are present
            String extension;
            MavenProject mavenProject = projectFacade.getMavenProject();
            if(mavenProject != null) {
              extension = getAndPersistArtifactExtension(project, mavenProject);
            } else {
              extension = project.getPersistentProperty(PPROP_EXTENSION);
            }
            if(extension == null && mavenProject == null) {
              // force loading of MavenProject
              extension = getAndPersistArtifactExtension(project, projectFacade.getMavenProject(monitor));
            }
            if(extension != null) {
              String classifier = artifact.getClassifier();
              if(classifier == null) {
                classifier = "";
              }
              state.putArtifact(outputLocation.getLocation().toFile(), artifact.getGroupId(), artifact.getArtifactId(),
                  extension, classifier, artifact.getVersion());
            } else {
              log.warn("Could not determine project {} main artifact extension.", project);
            }
          }
          // assume test output location gets attached as classified=tests
          IResource testOutputLocation = root.findMember(projectFacade.getTestOutputLocation());
          if(!"pom".equals(projectFacade.getPackaging()) && testOutputLocation != null && testOutputLocation.exists()) {
            state.putArtifact(testOutputLocation.getLocation().toFile(), artifact.getGroupId(),
                artifact.getArtifactId(), "jar", "tests", artifact.getVersion());
          }
        } catch(CoreException ex) {
          log.error("Error writing workspace state file", ex);
        }
      }

      state.store(projectManager.getWorkspaceStateFile());
    } catch(IOException ex) {
      log.error("Error writing workspace state file", ex);
    }
  }

  private String getAndPersistArtifactExtension(IProject project, MavenProject mavenProject) throws CoreException {
    String extension = mavenProject.getArtifact().getArtifactHandler().getExtension();
    project.setPersistentProperty(PPROP_EXTENSION, extension);
    return extension;
  }
}
