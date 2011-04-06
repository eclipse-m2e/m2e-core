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

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

/**
 * Maven 3.0-SNAPSHOT runtime loaded from the Eclipse Workspace
 * 
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 */
public class MavenWorkspaceRuntime implements MavenRuntime {

  private static final ArtifactKey MAVEN_DISTRIBUTION = new ArtifactKey("org.apache.maven", "apache-maven", "3.0", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

  private static final ArtifactKey PLEXUS_CLASSWORLDS = new ArtifactKey("org.codehaus.plexus", "plexus-classworlds", null, null); //$NON-NLS-1$ //$NON-NLS-2$

  private static final String MAVEN_EXECUTOR_CLASS = "org.apache.maven.cli.MavenCli"; //$NON-NLS-1$

  private static final String PLEXUS_CLASSWORLD_NAME = "plexus.core"; //$NON-NLS-1$

  private IMavenProjectRegistry projectManager;

  public MavenWorkspaceRuntime(IMavenProjectRegistry projectManager) {
    this.projectManager = projectManager;
  }
  
  public String getLocation() {
    return MavenRuntimeManager.WORKSPACE;
  }
  
  public String getSettings() {
    return null;
  }

  public boolean isEditable() {
    return false;
  }

  public boolean isAvailable() {
    return getMavenDistribution() != null;
  }

  private IMavenProjectFacade getMavenDistribution() {
    for (IMavenProjectFacade facade : projectManager.getProjects()) {
      ArtifactKey artifactKey = facade.getArtifactKey();
      if (MAVEN_DISTRIBUTION.getGroupId().equals(artifactKey.getGroupId()) //
            && MAVEN_DISTRIBUTION.getArtifactId().equals(artifactKey.getArtifactId())//
            && artifactKey.getVersion().startsWith(MAVEN_DISTRIBUTION.getVersion())) {
        return facade;
      }
    }
    return null;
  }

  public void createLauncherConfiguration(IMavenLauncherConfiguration collector, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade maven = getMavenDistribution();
    if (maven != null) {
      MavenProject mavenProject = maven.getMavenProject(monitor);

      collector.setMainType(MAVEN_EXECUTOR_CLASS, PLEXUS_CLASSWORLD_NAME);

      collector.addRealm(PLEXUS_CLASSWORLD_NAME);

      Set<Artifact> artifacts = mavenProject.getArtifacts();
      
      Artifact launcherArtifact = null;

      for (Artifact artifact : artifacts) {
        if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
          continue;
        }

        if (PLEXUS_CLASSWORLDS.getGroupId().equals(artifact.getGroupId()) && PLEXUS_CLASSWORLDS.getArtifactId().equals(artifact.getArtifactId())) {
          launcherArtifact = artifact;
          continue;
        }

        addArtifact(collector, artifact);
      }
      
      if (launcherArtifact != null) {
        collector.addRealm(IMavenLauncherConfiguration.LAUNCHER_REALM);
        addArtifact(collector, launcherArtifact);
      }
    }

    // XXX throw something at the caller! 
  }

  private void addArtifact(IMavenLauncherConfiguration collector, Artifact artifact) throws CoreException {
    IMavenProjectFacade facade = projectManager.getMavenProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());

    if (facade != null) {
      collector.addProjectEntry(facade);
    } else {
      File file = artifact.getFile();
      if (file != null) {
        collector.addArchiveEntry(file.getAbsolutePath());
      }
    }
  }

  public String toString() {
    return "Workspace (" + getVersion() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  public String getVersion() {
    IMavenProjectFacade maven = getMavenDistribution();
    if (maven != null) {
      return maven.getArtifactKey().getVersion();
    }
    return MAVEN_DISTRIBUTION.getVersion();
  }

}

