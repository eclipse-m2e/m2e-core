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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.util.StringUtils;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.project.IArchetype;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IProjectCreationListener;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;


@Component(service = {ArchetypeGenerator.class})
public class ArchetypeGenerator {

  private static final Logger log = LoggerFactory.getLogger(ArchetypeGenerator.class);

  @Reference
  IMaven maven;

  @Reference
  IProjectConfigurationManager projectConfigurationManager;

  @Reference
  MavenModelManager mavenModelManager;

  /**
   * Creates project structure using Archetype and then imports created project(s)
   *
   * @return an unmodifiable list of created projects.
   * @since 1.8
   */
  public List<IProject> createArchetypeProjects(IPath location, IArchetype archetype, String groupId, String artifactId,
      String version, String javaPackage, Properties properties, ProjectImportConfiguration configuration,
      IProjectCreationListener listener, IProgressMonitor monitor) throws CoreException {
    return IMavenExecutionContext.getThreadContext().orElseGet(maven::createExecutionContext)
        .execute((context, m) -> createArchetypeProjects0(location, archetype, groupId,
        artifactId, version,
        javaPackage, properties, configuration, listener, m), monitor);
  }

  List<IProject> createArchetypeProjects0(IPath location, IArchetype archetype, String groupId, String artifactId,
      String version, String javaPackage, Properties properties, ProjectImportConfiguration configuration,
      IProjectCreationListener listener, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(NLS.bind(Messages.ProjectConfigurationManager_task_creating_project1, artifactId), 2);

    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

    monitor.subTask(NLS.bind(Messages.ProjectConfigurationManager_task_executing_archetype, archetype.getGroupId(),
        archetype.getArtifactId()));
    if(location == null) {
      // if the project should be created in the workspace, figure out the path
      location = workspaceRoot.getLocation();
    }

    List<IProject> createdProjects = new ArrayList<>();

    try {

      Artifact artifact = resolveArchetype(archetype, monitor);

      ArchetypeGenerationRequest request = new ArchetypeGenerationRequest() //
          .setTransferListener(((MavenImpl) maven).createTransferListener(monitor)) //
          .setArchetypeGroupId(artifact.getGroupId()) //
          .setArchetypeArtifactId(artifact.getArtifactId()) //
          .setArchetypeVersion(artifact.getVersion()) //
          .setArchetypeRepository(archetype.getRepository()) //
          .setGroupId(groupId) //
          .setArtifactId(artifactId) //
          .setVersion(version) //
          .setPackage(javaPackage) // the model does not have a package field
          .setLocalRepository(maven.getLocalRepository()) //
          .setRemoteArtifactRepositories(maven.getArtifactRepositories(true)).setProperties(properties)
          .setOutputDirectory(location.toPortableString());

      ArchetypeGenerationResult result = Adapters.adapt(archetype, org.apache.maven.archetype.ArchetypeManager.class)
          .generateProjectFromArchetype(request);

      Exception cause = result.getCause();
      if(cause != null) {
        String msg = NLS.bind(Messages.ProjectConfigurationManager_error_unable_archetype, archetype);
        log.error(msg, cause);
        throw new CoreException(Status.error(msg, cause));
      }
      monitor.worked(1);

      // XXX Archetyper don't allow to specify project folder
      String projectFolder = location.append(artifactId).toFile().getAbsolutePath();

      LocalProjectScanner scanner = new LocalProjectScanner(List.of(projectFolder), true, mavenModelManager);
      scanner.run(monitor);

      Set<MavenProjectInfo> projectSet = projectConfigurationManager.collectProjects(scanner.getProjects());

      List<IMavenProjectImportResult> importResults = projectConfigurationManager.importProjects(projectSet,
          configuration, listener, monitor);
      for(IMavenProjectImportResult r : importResults) {
        IProject p = r.getProject();
        if(p != null && p.exists()) {
          createdProjects.add(p);
        }
      }

      monitor.worked(1);
    } catch(CoreException e) {
      throw e;
    } catch(InterruptedException e) {
      throw new CoreException(Status.CANCEL_STATUS);
    } catch(Exception ex) {
      throw new CoreException(Status.error(Messages.ProjectConfigurationManager_error_failed, ex)); //$NON-NLS-1$
    }
    return Collections.unmodifiableList(createdProjects);
  }

  /**
   * Apparently, Archetype#generateProjectFromArchetype 2.0-alpha-4 does not attempt to resolve archetype from
   * configured remote repositories. To compensate, we populate local repo with archetype pom/jar.
   */
  private Artifact resolveArchetype(IArchetype a, IProgressMonitor monitor) throws CoreException {
    List<ArtifactRepository> repos = new ArrayList<>();
    repos.addAll(maven.getArtifactRepositories()); // see org.apache.maven.archetype.downloader.DefaultDownloader#download

    //MNGECLIPSE-1399 use archetype repository too, not just the default ones
    String artifactRemoteRepository = a.getRepository();

    try {

      if(StringUtils.isNotBlank(artifactRemoteRepository)) {
        ArtifactRepository archetypeRepository = maven.createArtifactRepository(a.getArtifactId() + "-repo", //$NON-NLS-1$
            a.getRepository().trim());
        repos.add(0, archetypeRepository);//If the archetype doesn't exist locally, this will be the first remote repo to be searched.
      }

      maven.resolve(a.getGroupId(), a.getArtifactId(), a.getVersion(), "pom", null, repos, monitor); //$NON-NLS-1$
      return maven.resolve(a.getGroupId(), a.getArtifactId(), a.getVersion(), "jar", null, repos, monitor); //$NON-NLS-1$
    } catch(CoreException e) {
      String msg = Messages.ProjectConfigurationManager_error_resolve + a.getGroupId() + ':'
          + Messages.ProjectConfigurationManager_error_resolve2;
      throw new CoreException(Status.error(msg, e));
    }
  }

}
