/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.ui.internal.archetype;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.launch.IMavenLauncher;
import org.eclipse.m2e.core.project.IArchetype;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;


@Component(service = {ArchetypeGenerator.class})
@SuppressWarnings("restriction")
public class ArchetypeGenerator {

  @Reference
  IProjectConfigurationManager projectConfigurationManager;

  @Reference
  MavenModelManager mavenModelManager;

  @Reference
  IMavenLauncher mavenLauncher;

  /**
   * Creates project structure using Archetype and then imports created project(s)
   *
   * @return a list of created projects.
   * @since 1.8
   */
  public Collection<MavenProjectInfo> createArchetypeProjects(IPath location, IArchetype archetype, String groupId,
      String artifactId, String version, String javaPackage, Properties properties, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor,
        NLS.bind(Messages.ProjectConfigurationManager_task_creating_project1, artifactId), 3);
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

    subMonitor.subTask(NLS.bind(Messages.ProjectConfigurationManager_task_executing_archetype, archetype.getGroupId(),
        archetype.getArtifactId()));
    if(location == null) {
      // if the project should be created in the workspace, figure out the path
      location = workspaceRoot.getLocation();
    }
    File basedir = location.toFile();
    if(basedir == null || (!basedir.mkdirs() && !basedir.isDirectory())) {
      throw new CoreException(Status.error(Messages.ProjectConfigurationManager_error_failed));
    }
    //See https://maven.apache.org/archetype/maven-archetype-plugin/generate-mojo.html
    Properties userProperties = new Properties(properties);
    userProperties.setProperty("archetypeGroupId", archetype.getGroupId());
    userProperties.setProperty("archetypeArtifactId", archetype.getArtifactId());
    userProperties.setProperty("archetypeVersion", archetype.getVersion());
    String repository = archetype.getRepository();
    if(repository != null) {
      userProperties.setProperty("archetypeRepository", repository);
    }
    userProperties.setProperty("groupId", groupId);
    userProperties.setProperty("artifactId", artifactId);
    userProperties.setProperty("version", version);
    userProperties.setProperty("package", javaPackage);
    userProperties.setProperty("outputDirectory", basedir.getAbsolutePath());
    String projectFolder = location.append(artifactId).toFile().getAbsolutePath();
    File emptyPom = getEmptyPom(basedir);
    try {
      String goals = "-U archetype:generate";
      if(emptyPom != null) {
        goals += " -f " + emptyPom.getAbsolutePath();
      }
      CompletableFuture<?> maven = mavenLauncher.runMaven(basedir, goals, userProperties);
      subMonitor.worked(1);
      Display current = Display.getCurrent();
      while(!maven.isDone()) {
        if(current != null) {
          while(!current.isDisposed() && current.readAndDispatch()) {
            //loop to process events
          }
        }
        Thread.onSpinWait();
      }
      maven.get(); //wait for maven build to complete...
      subMonitor.worked(1);
      LocalProjectScanner scanner = new LocalProjectScanner(List.of(projectFolder), true, mavenModelManager);
      try {
        scanner.run(subMonitor.split(1));
      } catch(InterruptedException e) {
        return List.of();
      }
      return projectConfigurationManager.collectProjects(scanner.getProjects());
    } catch(InterruptedException | CancellationException ex) {
      throw new CoreException(Status.CANCEL_STATUS);
    } catch(ExecutionException ex) {
      if(ex.getCause() instanceof CoreException coreException) {
        throw coreException;
      }
      throw new CoreException(Status.error(Messages.ProjectConfigurationManager_error_failed, ex)); //$NON-NLS-1$
    } finally {
      if(emptyPom != null) {
        emptyPom.delete();
      }
    }
  }

  private File getEmptyPom(File basedir) {
    if(new File(basedir, IMavenConstants.POM_FILE_NAME).isFile()) {
      try {
        File tempFile = File.createTempFile("pom", ".xml", basedir);
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(),
            "<project><modelVersion>4.0.0</modelVersion><groupId>empty</groupId><artifactId>empty</artifactId><version>1</version><name>Generating archetype</name></project>",
            StandardCharsets.UTF_8);
        return tempFile;
      } catch(IOException ex) {
      }
    }
    return null;
  }

}
