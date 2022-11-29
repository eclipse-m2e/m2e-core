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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import org.codehaus.plexus.util.FileUtils;

import org.eclipse.m2e.core.embedder.MavenModelManager;
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
   * Creates project structure(s) using the given {@link IArchetype}. These projects are not imported automatically so
   * {@link IProjectConfigurationManager#importProjects(Collection, org.eclipse.m2e.core.project.ProjectImportConfiguration, IProgressMonitor)
   * IProjectConfigurationManager.importProjects()} must be called if they are to be imported into the workspace.
   * <p>
   * May block during execution. Pumps the UI event loop if called from the UI thread. Cancellation from progress
   * monitor is honored and signaled in the return value.
   * <p>
   * Equivalent to
   * {@link #createArchetypeProjects(IPath, IArchetype, String, String, String, String, Map, boolean, IProgressMonitor)}
   * with <code>interactive = false</code>
   * 
   * @param location where to place them, may be {@code null} for the workspace root
   * @param archetype archetype
   * @param groupId groupid
   * @param artifactId artifactid
   * @param version version
   * @param javaPackage java package
   * @param properties initial properties (some properties will be set/overriden by this call)
   * @param monitor monitor for progress and cancellation, may be {@code null}
   * @return a list of created projects.
   * @throws CoreException to signal an error or a cancellation (if the contained status is CANCEL)
   * @since 1.8
   */
  public Collection<MavenProjectInfo> createArchetypeProjects(IPath location, IArchetype archetype, String groupId,
      String artifactId, String version, String javaPackage, Map<String, String> properties, IProgressMonitor monitor)
      throws CoreException {
    return createArchetypeProjects(location, archetype, groupId, artifactId, version, javaPackage, properties, false,
        monitor);
  }

  /**
   * Creates project structure(s) using the given {@link IArchetype}. These projects are not imported automatically so
   * {@link IProjectConfigurationManager#importProjects(Collection, org.eclipse.m2e.core.project.ProjectImportConfiguration, IProgressMonitor)
   * IProjectConfigurationManager.importProjects()} must be called if they are to be imported into the workspace.
   * <p>
   * May block during execution. Pumps the UI event loop if called from the UI thread. Cancellation from progress
   * monitor is honored and signaled in the return value.
   * 
   * @param location where to place them, may be {@code null} for the workspace root
   * @param archetype archetype
   * @param groupId groupid
   * @param artifactId artifactid
   * @param version version
   * @param javaPackage java package
   * @param properties initial properties (some properties will be set/overriden by this call)
   * @param interactive see {@link IMavenLauncher#runMaven(File, String, Map, boolean)}
   * @param monitor monitor for progress and cancellation, may be {@code null}
   * @return a list of created projects.
   * @throws CoreException to signal an error or a cancellation (if the contained status is CANCEL)
   * @since 1.8
   */
  public Collection<MavenProjectInfo> createArchetypeProjects(IPath location, IArchetype archetype, String groupId,
      String artifactId, String version, String javaPackage, Map<String, String> properties, boolean interactive,
      IProgressMonitor monitor) throws CoreException {
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
    Map<String, String> userProperties = new LinkedHashMap<>(properties);
    userProperties.put("archetypeGroupId", archetype.getGroupId());
    userProperties.put("archetypeArtifactId", archetype.getArtifactId());
    userProperties.put("archetypeVersion", archetype.getVersion());
    userProperties.put("groupId", groupId);
    userProperties.put("artifactId", artifactId);
    userProperties.put("version", version);
    userProperties.put("package", javaPackage);
    userProperties.put("outputDirectory", basedir.getAbsolutePath());
    String projectFolder = location.append(artifactId).toFile().getAbsolutePath();

    CompletableFuture<?> mavenRun = null;
    File[] workingDir = new File[1];
    try (var workingDirCleaner = createEmptyWorkingDirectory(workingDir)) {
      String goals = "-U " + ArchetypePlugin.ARCHETYPE_PREFIX + ":generate";
      mavenRun = mavenLauncher.runMaven(workingDir[0], goals, userProperties, interactive);
      subMonitor.worked(1);
      Display current = Display.getCurrent();
      while(!mavenRun.isDone()) {
        if(current != null) {
          while(!current.isDisposed() && current.readAndDispatch()) {
            //loop to process events
          }
        }
        Thread.onSpinWait();
        subMonitor.checkCanceled(); // check for cancellation when UI thread is idle only (if on UI thread)
      }
      mavenRun.get(); //wait for maven build to complete...
      subMonitor.worked(1);
      LocalProjectScanner scanner = new LocalProjectScanner(List.of(projectFolder), true, mavenModelManager);
      try {
        scanner.run(subMonitor.split(1));
      } catch(InterruptedException e) {
        return List.of();
      }
      return projectConfigurationManager.collectProjects(scanner.getProjects());
    } catch(InterruptedException | CancellationException | OperationCanceledException ex) {
      // ensure cancellations that might not have originated from the Future itself, request it to cancel  
      if(mavenRun != null) {
        mavenRun.cancel(true);
      }
      // in all cases, for API compatibility, do not throw OCE, and instead throw a CoreException with CANCEL status
      throw new CoreException(Status.CANCEL_STATUS);
    } catch(ExecutionException | IOException ex) {
      if(ex.getCause() instanceof CoreException coreException) {
        throw coreException;
      }
      throw new CoreException(Status.error(Messages.ProjectConfigurationManager_error_failed, ex)); //$NON-NLS-1$
    }
  }

  private static Closeable createEmptyWorkingDirectory(File[] workingDir) throws IOException {
    Path tempWorkingDir = Files.createTempDirectory("m2e-archetypeGenerator");
    Files.createDirectories(tempWorkingDir.resolve(".mvn")); // Ensure tempWorkingDir is used as maven.multiModuleProjectDirectory
    workingDir[0] = tempWorkingDir.toFile();
    return () -> FileUtils.deleteDirectory(workingDir[0]);
  }

}
