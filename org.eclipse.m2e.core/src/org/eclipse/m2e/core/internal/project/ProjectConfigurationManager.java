/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others.
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
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.util.StringUtils;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.AbstractRunnable;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IProjectCreationListener;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * import Maven projects update project configuration from Maven enable Maven nature create new project
 * 
 * @author igor
 */
public class ProjectConfigurationManager implements IProjectConfigurationManager, IMavenProjectChangedListener,
    IResourceChangeListener {
  /*package*/static final Logger log = LoggerFactory.getLogger(ProjectConfigurationManager.class);

  final ProjectRegistryManager projectManager;

  final MavenModelManager mavenModelManager;

  final IMavenMarkerManager mavenMarkerManager;

  final IMaven maven;

  final IMavenConfiguration mavenConfiguration;

  public ProjectConfigurationManager(IMaven maven, ProjectRegistryManager projectManager,
      MavenModelManager mavenModelManager, IMavenMarkerManager mavenMarkerManager,
      IMavenConfiguration mavenConfiguration) {
    this.projectManager = projectManager;
    this.mavenModelManager = mavenModelManager;
    this.mavenMarkerManager = mavenMarkerManager;
    this.maven = maven;
    this.mavenConfiguration = mavenConfiguration;
  }

  public List<IMavenProjectImportResult> importProjects(Collection<MavenProjectInfo> projectInfos,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    return importProjects(projectInfos, configuration, null, monitor);
  }

  public List<IMavenProjectImportResult> importProjects(final Collection<MavenProjectInfo> projectInfos,
      final ProjectImportConfiguration configuration, final IProjectCreationListener listener,
      final IProgressMonitor monitor) throws CoreException {

    final SubMonitor progress = SubMonitor.convert(monitor, Messages.ProjectConfigurationManager_task_importing, 100);

    // overall execution context to share repository session data and cache for all projects
    return maven.execute((context, monitor1) -> {
    long t1 = System.currentTimeMillis();
    ArrayList<IMavenProjectImportResult> result = new ArrayList<IMavenProjectImportResult>();
    int total = projectInfos.size();
    ArrayList<IProject> projects = new ArrayList<IProject>(total);
    int i = 0;

    List<IProject> existingProjects = findExistingProjectsToHideFrom();

    // first, create all projects with basic configuration
    for(MavenProjectInfo projectInfo : projectInfos) {
    long t11 = System.currentTimeMillis();
    if(monitor1.isCanceled()) {
      throw new OperationCanceledException();
    }

    SubMonitor subProgress = SubMonitor.convert(progress.newChild(10), projectInfos.size() * 100);
    IProject project = create(projectInfo, configuration, listener, subProgress.newChild(100));

    result.add(new MavenProjectImportResult(projectInfo, project));

    if(project != null) {
      projects.add(project);
      long importTime = System.currentTimeMillis() - t11;
      log.debug("Imported project {} ({}/{}) in {} ms", project.getName(), ++i, total, importTime);
    }
    }

    hideNestedProjectsFromParents(projects, existingProjects, monitor1);
    // then configure maven for all projects
    configureNewMavenProjects(projects, progress.newChild(90));

    long t2 = System.currentTimeMillis();
    log.info("Imported and configured {} project(s) in {} sec", total, ((t2 - t1) / 1000));

    return result;
   }, monitor);

  }

  private void setHidden(IResource resource) {
    try {
      resource.setHidden(true);
    } catch(Exception ex) {
      log.error("Failed to hide resource: "
          + (resource.getLocation() == null ? resource.getName() : resource.getLocation().toOSString()), ex);
    }
  }

  /*package*/void hideNestedProjectsFromParents(List<IProject> projects, List<IProject> existingProjects,
      IProgressMonitor monitor) {

    if(!MavenPlugin.getMavenConfiguration().isHideFoldersOfNestedProjects()) {
      return;
    }

    // Prevent child project folders from showing up in parent project folders.

    HashMap<File, IProject> projectFileMap = new HashMap<>();

    if(existingProjects != null) {
      for(IProject project : existingProjects) {
        if(project.getLocation() != null) {
          projectFileMap.put(project.getLocation().toFile(), project);
        }
      }
    }
    for(IProject project : projects) {
      if(project.getLocation() != null) {
        projectFileMap.put(project.getLocation().toFile(), project);
      }
    }

    if(monitor == null) {
      monitor = new NullProgressMonitor();
    }

    Set<IProject> refreshedProjects = new HashSet<>();

    for(IProject project : projects) {
      if(monitor.isCanceled()) {
        return;
      }
      if(project.getLocation() == null) {
        continue;
      }
      File projectFile = project.getLocation().toFile();
      IProject physicalParentProject = projectFileMap.get(projectFile.getParentFile());
      if(physicalParentProject == null) {
        continue;
      }
      //Only refresh parent when necessary, i.e. the first time
      if(!refreshedProjects.contains(physicalParentProject)) {
        try {
          physicalParentProject.refreshLocal(IResource.DEPTH_ONE, monitor);
        } catch(Exception e) {
          log.error("Failed to refresh " + physicalParentProject.getName(), e);
        } finally {
          refreshedProjects.add(physicalParentProject);
        }
      }
      IFolder folder = physicalParentProject.getFolder(projectFile.getName());
      if(folder.exists()) {
        setHidden(folder);
      }
    }
  }

  /*package*/void configureNewMavenProjects(final List<IProject> projects, IProgressMonitor monitor)
      throws CoreException {
    final SubMonitor progress = SubMonitor.convert(monitor, Messages.ProjectConfigurationManager_task_configuring, 100);

    //SubProgressMonitor sub = new SubProgressMonitor(monitor, projects.size()+1);

    // first, resolve maven dependencies for all projects
    Set<IFile> pomFiles = new LinkedHashSet<IFile>();
    for(IProject project : projects) {
      pomFiles.add(project.getFile(IMavenConstants.POM_FILE_NAME));
    }
    progress.subTask(Messages.ProjectConfigurationManager_task_refreshing);

    projectManager.refresh(pomFiles, progress.newChild(75));

    // TODO this emits project change events, which may be premature at this point

    //Creating maven facades 
    SubMonitor subProgress = SubMonitor.convert(progress.newChild(5), projects.size() * 100);
    List<IMavenProjectFacade> facades = new ArrayList<IMavenProjectFacade>(projects.size());
    for(IProject project : projects) {
      if(progress.isCanceled()) {
        throw new OperationCanceledException();
      }
      IMavenProjectFacade facade = projectManager.create(project, subProgress.newChild(100));
      if(facade != null) {
        facades.add(facade);
      }
    }

    //MNGECLIPSE-1028 : Sort projects by build order here, 
    //as dependent projects need to be configured before depending projects (in WTP integration for ex.)
    sortProjects(facades, progress.newChild(5));
    //Then, perform detailed project configuration
    subProgress = SubMonitor.convert(progress.newChild(15), facades.size() * 100);
    for(IMavenProjectFacade facade : facades) {
      if(progress.isCanceled()) {
        throw new OperationCanceledException();
      }
      progress.subTask(NLS.bind(Messages.ProjectConfigurationManager_task_updating, facade.getProject().getName()));
      MavenProject mavenProject = facade.getMavenProject(subProgress.newChild(5));
      ProjectConfigurationRequest request = new ProjectConfigurationRequest(facade, mavenProject);
      updateProjectConfiguration(request, subProgress.newChild(90));
    }
  }

  public void sortProjects(List<IMavenProjectFacade> facades, IProgressMonitor monitor) throws CoreException {
    HashMap<MavenProject, IMavenProjectFacade> mavenProjectToFacadeMap = new HashMap<MavenProject, IMavenProjectFacade>(
        facades.size());
    for(IMavenProjectFacade facade : facades) {
      mavenProjectToFacadeMap.put(facade.getMavenProject(monitor), facade);
    }
    facades.clear();
    for(MavenProject mavenProject : maven.getSortedProjects(new ArrayList<MavenProject>(mavenProjectToFacadeMap
        .keySet()))) {
      facades.add(mavenProjectToFacadeMap.get(mavenProject));
    }
  }

  public void updateProjectConfiguration(IProject project, IProgressMonitor monitor) throws CoreException {
    updateProjectConfiguration(new MavenUpdateRequest(project, mavenConfiguration.isOffline(), false), monitor);
  }

  // TODO deprecate this method
  public void updateProjectConfiguration(MavenUpdateRequest request, IProgressMonitor monitor) throws CoreException {
    // for now, only allow one project per request.

    if(request.getPomFiles().size() != 1) {
      throw new IllegalArgumentException();
    }

    Map<String, IStatus> updateStatus = updateProjectConfiguration(request, true, true, monitor);

    IStatus status = updateStatus.values().iterator().next(); // only one project

    if(!status.isOK()) {
      throw new CoreException(status);
    }
  }

  /**
   * Returns project name to update status map.
   * <p/>
   * TODO promote to API
   * 
   * @since 1.1
   */
  public Map<String, IStatus> updateProjectConfiguration(final MavenUpdateRequest request,
      final boolean updateConfiguration, final boolean cleanProjects, final IProgressMonitor monitor) {
    return updateProjectConfiguration(request, updateConfiguration, cleanProjects, false/*refreshFromLocal*/, monitor);
  }

  /**
   * Returns project name to update status map.
   * <p/>
   * TODO promote to API. TODO decide if workspace or other lock is required during execution of this method.
   * 
   * @since 1.4
   */
  public Map<String, IStatus> updateProjectConfiguration(final MavenUpdateRequest request,
      final boolean updateConfiguration, final boolean cleanProjects, final boolean refreshFromLocal,
      final IProgressMonitor monitor) {
    try {
      return maven.execute(request.isOffline(), request.isForceDependencyUpdate(),
          (context, monitor1) -> updateProjectConfiguration0(request.getPomFiles(), updateConfiguration, cleanProjects,
              refreshFromLocal, monitor1), monitor);
    } catch(CoreException ex) {
      Map<String, IStatus> result = new LinkedHashMap<String, IStatus>();
      for(IFile pomFile : request.getPomFiles()) {
        result.put(pomFile.getProject().getName(), ex.getStatus());
      }
      return result;
    }
  }

  /*package*/Map<String, IStatus> updateProjectConfiguration0(Collection<IFile> pomFiles, boolean updateConfiguration,
      boolean cleanProjects, boolean refreshFromLocal, IProgressMonitor monitor) {

    monitor.beginTask(Messages.ProjectConfigurationManager_task_updating_projects, pomFiles.size()
        * (1 + (updateConfiguration ? 1 : 0) + (cleanProjects ? 1 : 0) + (refreshFromLocal ? 1 : 0)));

    long l1 = System.currentTimeMillis();
    log.info("Update started"); //$NON-NLS-1$

    Map<IFile, IMavenProjectFacade> projects = new LinkedHashMap<IFile, IMavenProjectFacade>();

    //project names to the errors encountered when updating them
    Map<String, IStatus> updateStatus = new HashMap<String, IStatus>();

    List<IFile> pomsToRefresh = new ArrayList<IFile>();

    // refresh from local filesystem
    if(refreshFromLocal) {
      for(IFile pom : pomFiles) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        IProject project = pom.getProject();
        try {
          project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1,
              SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
          pomsToRefresh.add(pom);
        } catch(CoreException ex) {
          updateStatus.put(project.getName(), ex.getStatus());
        }
      }
    } else {
      pomsToRefresh.addAll(pomFiles);
    }

    // refresh projects and update all dependencies
    // this will ensure that project registry is up-to-date on GAV of all projects being updated
    // TODO this sends multiple update events, rework using low-level registry update methods
    try {
      projectManager.refresh(pomsToRefresh, new SubProgressMonitor(monitor, pomFiles.size()));

      for(IFile pom : pomsToRefresh) {
        IProject project = pom.getProject();
        IMavenProjectFacade facade = projectManager.getProject(project);
        if(facade != null) { // facade is null if pom.xml cannot be read
          projects.put(pom, facade);
        }
        updateStatus.put(project.getName(), Status.OK_STATUS);
      }
    } catch(CoreException ex) {
      // TODO per-project status
      for(IFile pom : pomsToRefresh) {
        IProject project = pom.getProject();
        updateStatus.put(project.getName(), ex.getStatus());
      }
    }

    // update project configuration
    if(updateConfiguration) {
      Iterator<Entry<IFile, IMavenProjectFacade>> iterator = projects.entrySet().iterator();
      while(iterator.hasNext()) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        IMavenProjectFacade facade = iterator.next().getValue();

        monitor.subTask(facade.getProject().getName());

        SubProgressMonitor submonitor = new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
        try {
          ProjectConfigurationRequest cfgRequest = new ProjectConfigurationRequest(facade,
              facade.getMavenProject(submonitor));
          updateProjectConfiguration(cfgRequest, submonitor);
        } catch(CoreException ex) {
          iterator.remove();
          updateStatus.put(facade.getProject().getName(), ex.getStatus());
        }
      }
    }

    // rebuild
    if(cleanProjects) {
      Iterator<Entry<IFile, IMavenProjectFacade>> iterator = projects.entrySet().iterator();
      while(iterator.hasNext()) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        IMavenProjectFacade facade = iterator.next().getValue();

        IProject project = facade.getProject();

        monitor.subTask(project.getName());

        SubProgressMonitor submonitor = new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
        try {
          // only rebuild projects that were successfully updated
          IStatus status = updateStatus.get(project.getName());
          if(status == null || status.isOK()) {
            project.build(IncrementalProjectBuilder.CLEAN_BUILD, submonitor);
            // TODO provide an option to build projects if the workspace is not autobuilding
          }
        } catch(CoreException ex) {
          iterator.remove();
          updateStatus.put(project.getName(), ex.getStatus());
        }
      }
    }

    long l2 = System.currentTimeMillis();
    log.info(NLS.bind("Update completed: {0} sec", ((l2 - l1) / 1000))); //$NON-NLS-1$

    return updateStatus;
  }

  private void updateProjectConfiguration(final ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    final IProject project = request.getProject();
    long start = System.currentTimeMillis();
    final IMavenProjectFacade mavenProjectFacade = request.getMavenProjectFacade();
    log.debug("Updating project configuration for {}.", mavenProjectFacade.toString()); //$NON-NLS-1$

    addMavenNature(project, monitor);

    // Configure project file encoding
    final MavenProject mavenProject = request.getMavenProject();
    Properties mavenProperties = mavenProject.getProperties();
    String sourceEncoding = mavenProperties.getProperty("project.build.sourceEncoding");
    log.debug("Setting encoding for project {}: {}", project.getName(), sourceEncoding); //$NON-NLS-1$
    project.setDefaultCharset(sourceEncoding, monitor);

    MavenExecutionContext executionContext = projectManager.createExecutionContext(mavenProjectFacade.getPom(),
        mavenProjectFacade.getResolverConfiguration());

    executionContext.execute(mavenProject, (context, monitor1) -> {
      ILifecycleMapping lifecycleMapping = getLifecycleMapping(mavenProjectFacade);

      if(lifecycleMapping != null) {
        mavenMarkerManager.deleteMarkers(mavenProjectFacade.getProject(), IMavenConstants.MARKER_CONFIGURATION_ID);

        lifecycleMapping.configure(request, monitor1);

        LifecycleMappingConfiguration.persist(request.getMavenProjectFacade(), monitor1);
      } else {
        log.debug("LifecycleMapping is null for project {}", mavenProjectFacade.toString()); //$NON-NLS-1$
      }
      return null;
    }, monitor);

    log.debug(
        "Updated project configuration for {} in {} ms.", mavenProjectFacade.toString(), System.currentTimeMillis() - start); //$NON-NLS-1$
  }

  public void enableMavenNature(final IProject project, final ResolverConfiguration configuration,
      final IProgressMonitor monitor) throws CoreException {
    monitor.subTask(Messages.ProjectConfigurationManager_task_enable_nature);
    maven.execute(new AbstractRunnable() {
      protected void run(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        enableBasicMavenNature(project, configuration, monitor);
        configureNewMavenProjects(Collections.singletonList(project), monitor);
      }
    }, monitor);
  }

  /*package*/void enableBasicMavenNature(IProject project, ResolverConfiguration configuration,
      IProgressMonitor monitor) throws CoreException {
    ResolverConfigurationIO.saveResolverConfiguration(project, configuration);

    // add maven nature even for projects without valid pom.xml file
    addMavenNature(project, monitor);
  }

  private void addMavenNature(IProject project, IProgressMonitor monitor) throws CoreException {
    if(!project.hasNature(IMavenConstants.NATURE_ID)) {
      IProjectDescription description = project.getDescription();
      String[] prevNatures = description.getNatureIds();
      String[] newNatures = new String[prevNatures.length + 1];
      System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
      newNatures[0] = IMavenConstants.NATURE_ID;
      description.setNatureIds(newNatures);
      project.setDescription(description, monitor);
    }
  }

  public void disableMavenNature(IProject project, IProgressMonitor monitor) throws CoreException {
    monitor.subTask(Messages.ProjectConfigurationManager_task_disable_nature);

    IMavenProjectFacade facade = projectManager.create(project, monitor);
    if(facade != null) {
      ILifecycleMapping lifecycleMapping = getLifecycleMapping(facade);
      if(lifecycleMapping != null) {
        ProjectConfigurationRequest request = new ProjectConfigurationRequest(facade, facade.getMavenProject(monitor));
        lifecycleMapping.unconfigure(request, monitor);
      }
    }

    // Delete all m2e markers
    project.deleteMarkers(IMavenConstants.MARKER_ID, true, IResource.DEPTH_INFINITE);

    // Remove the m2e nature
    IProjectDescription description = project.getDescription();
    ArrayList<String> newNatures = new ArrayList<String>();
    for(String natureId : description.getNatureIds()) {
      if(!IMavenConstants.NATURE_ID.equals(natureId)) {
        newNatures.add(natureId);
      }
    }
    description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));

    // Remove the m2e builder
    removeMavenBuilder(project, description, monitor);

    project.setDescription(description, monitor);

    // tell the projectManager to remove the project facade and notify MavenProjectChangeListeners 
    MavenPlugin.getMavenProjectRegistry().refresh(
        new MavenUpdateRequest(project, mavenConfiguration.isOffline(), false));
  }

  public boolean addMavenBuilder(IProject project, IProjectDescription description, IProgressMonitor monitor)
      throws CoreException {
    boolean setProjectDescription = false;
    if(description == null) {
      description = project.getDescription();
      setProjectDescription = true;
    }

    // ensure Maven builder is always the last one
    ICommand mavenBuilder = null;
    ArrayList<ICommand> newSpec = new ArrayList<ICommand>();
    int i = 0;
    for(ICommand command : description.getBuildSpec()) {
      if(isMavenBuilderCommand(project, command)) {
        mavenBuilder = command;
        if(i == description.getBuildSpec().length - 1) {
          // This is the maven builder command and it is the last one in the list - there is nothing to change
          return false;
        }
      } else {
        newSpec.add(command);
      }
      i++ ;
    }
    if(mavenBuilder == null) {
      mavenBuilder = description.newCommand();
      mavenBuilder.setBuilderName(IMavenConstants.BUILDER_ID);
    }
    newSpec.add(mavenBuilder);
    description.setBuildSpec(newSpec.toArray(new ICommand[newSpec.size()]));

    if(setProjectDescription) {
      project.setDescription(description, monitor);
    }
    return true;
  }

  public boolean removeMavenBuilder(IProject project, IProjectDescription description, IProgressMonitor monitor)
      throws CoreException {
    boolean setProjectDescription = false;
    if(description == null) {
      description = project.getDescription();
      setProjectDescription = true;
    }

    boolean foundMavenBuilder = false;
    ArrayList<ICommand> newSpec = new ArrayList<ICommand>();
    for(ICommand command : description.getBuildSpec()) {
      if(!isMavenBuilderCommand(project, command)) {
        newSpec.add(command);
      } else {
        foundMavenBuilder = true;
      }
    }
    if(!foundMavenBuilder) {
      return false;
    }
    description.setBuildSpec(newSpec.toArray(new ICommand[newSpec.size()]));

    if(setProjectDescription) {
      project.setDescription(description, monitor);
    }

    return true;
  }

  private boolean isMavenBuilderCommand(IProject project, ICommand command) {
    return IMavenConstants.BUILDER_ID.equals(command.getBuilderName());
  }

  // project creation

  public void createSimpleProject(IProject project, IPath location, Model model, String[] directories,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    createSimpleProject(project, location, model, directories, configuration, null, monitor);
  }

  /**
   * Creates simple Maven project
   * <p>
   * The following steps are executed in the given order:
   * <ul>
   * <li>Creates the workspace project</li>
   * <li>Adds project to working set</li>
   * <li>Creates the required folders</li>
   * <li>Creates the POM</li>
   * <li>Configures project</li>
   * </ul>
   * </p>
   */
  // XXX should use Maven plugin configurations instead of manually specifying folders
  public void createSimpleProject(IProject project, IPath location, Model model, String[] directories,
      ProjectImportConfiguration configuration, IProjectCreationListener listener, IProgressMonitor monitor)
          throws CoreException {
    String projectName = project.getName();
    monitor.beginTask(NLS.bind(Messages.ProjectConfigurationManager_task_creating, projectName), 5);

    monitor.subTask(Messages.ProjectConfigurationManager_task_creating_workspace);
    IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    description.setLocation(location);
    project.create(description, monitor);
    project.open(monitor);
    monitor.worked(1);

    monitor.subTask(Messages.ProjectConfigurationManager_task_creating_pom);
    IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);

    mavenModelManager.createMavenModel(pomFile, model);
    monitor.worked(1);

    monitor.subTask(Messages.ProjectConfigurationManager_task_creating_folders);
    for(int i = 0; i < directories.length; i++ ) {
      createFolder(project.getFolder(directories[i]), false);
    }
    monitor.worked(1);

    if(listener != null) {
      listener.projectCreated(project);
    }

    monitor.subTask(Messages.ProjectConfigurationManager_task_creating_project);
    enableMavenNature(project, configuration.getResolverConfiguration(), monitor);
    monitor.worked(1);

    IProject parent = findParentProject(model);
    if(parent != null) {
      hideNestedProjectsFromParents(Collections.singletonList(project), Collections.singletonList(parent), monitor);
      monitor.worked(1);
    }

  }

  private IProject findParentProject(Model model) {
    Parent parent = model.getParent();
    if(parent == null) {
      return null;
    }
    MavenProjectFacade parentProjectFacade = projectManager.getMavenProject(parent.getGroupId(),
        parent.getArtifactId(), parent.getVersion());
    return parentProjectFacade == null ? null : parentProjectFacade.getProject();
  }

  /**
   * Helper method which creates a folder and, recursively, all its parent folders.
   * 
   * @param folder The folder to create.
   * @param derived true if folder should be marked as derived
   * @throws CoreException if creating the given <code>folder</code> or any of its parents fails.
   */
  public static void createFolder(IFolder folder, boolean derived) throws CoreException {
    // Recurse until we find a parent folder which already exists.
    if(!folder.exists()) {
      IContainer parent = folder.getParent();
      // First, make sure that all parent folders exist.
      if(parent != null && !parent.exists()) {
        createFolder((IFolder) parent, false);
      }
      folder.create(true, true, null);
    }

    if(folder.isAccessible() && derived) {
      folder.setDerived(true);
    }
  }

  /**
   * Creates project structure using Archetype and then imports created project(s)
   * 
   * @deprecated use
   *             {@link #createArchetypeProjects(IPath, Archetype, String, String, String, String, Properties, ProjectImportConfiguration, IProgressMonitor)}
   */
  @Deprecated
  public void createArchetypeProject(IProject project, IPath location, Archetype archetype, String groupId,
      String artifactId, String version, String javaPackage, Properties properties,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    createArchetypeProjects(location, archetype, groupId, artifactId, version, javaPackage, properties, configuration,
        monitor);
  }

  /**
   * Creates project structure using Archetype and then imports created project(s)
   * 
   * @return an unmodifiable list of created projects.
   * @since 1.1
   */
  public List<IProject> createArchetypeProjects(IPath location, Archetype archetype, final String groupId,
      String artifactId, String version, String javaPackage, Properties properties,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    return createArchetypeProjects(location, archetype, groupId, artifactId, version, javaPackage, properties,
        configuration, null, monitor);
  }

  /**
   * Creates project structure using Archetype and then imports created project(s)
   * 
   * @return an unmodifiable list of created projects.
   * @since 1.8
   */
  public List<IProject> createArchetypeProjects(final IPath location, final Archetype archetype, final String groupId,
      final String artifactId, final String version, final String javaPackage, final Properties properties,
      final ProjectImportConfiguration configuration, final IProjectCreationListener listener,
      final IProgressMonitor monitor) throws CoreException {
    return maven.execute((context, monitor1) -> createArchetypeProjects0(location, archetype, groupId, artifactId, version, javaPackage, properties,
        configuration, listener, monitor1), monitor);
  }

  /*package*/List<IProject> createArchetypeProjects0(IPath location, Archetype archetype, String groupId,
      String artifactId, String version, String javaPackage, Properties properties,
      ProjectImportConfiguration configuration, IProjectCreationListener listener, IProgressMonitor monitor)
          throws CoreException {
    monitor.beginTask(NLS.bind(Messages.ProjectConfigurationManager_task_creating_project1, artifactId), 2);

    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

    monitor.subTask(NLS.bind(Messages.ProjectConfigurationManager_task_executing_archetype, archetype.getGroupId(),
        archetype.getArtifactId()));
    if(location == null) {
      // if the project should be created in the workspace, figure out the path
      location = workspaceRoot.getLocation();
    }

    List<IProject> createdProjects = new ArrayList<IProject>();

    try {

      Artifact artifact = resolveArchetype(archetype, monitor);

      ArchetypeGenerationRequest request = new ArchetypeGenerationRequest()
          //
          .setTransferListener(maven.createTransferListener(monitor))
          //
          .setArchetypeGroupId(artifact.getGroupId())
          //
          .setArchetypeArtifactId(artifact.getArtifactId())
          //
          .setArchetypeVersion(artifact.getVersion())
          //
          .setArchetypeRepository(archetype.getRepository())
          //
          .setGroupId(groupId)
          //
          .setArtifactId(artifactId)
          //
          .setVersion(version)
          //
          .setPackage(javaPackage)
          // the model does not have a package field
          .setLocalRepository(maven.getLocalRepository())
          //
          .setRemoteArtifactRepositories(maven.getArtifactRepositories(true)).setProperties(properties)
          .setOutputDirectory(location.toPortableString());

      ArchetypeGenerationResult result = getArchetyper().generateProjectFromArchetype(request);

      Exception cause = result.getCause();
      if(cause != null) {
        String msg = NLS.bind(Messages.ProjectConfigurationManager_error_unable_archetype, archetype.toString());
        log.error(msg, cause);
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, cause));
      }
      monitor.worked(1);

      // XXX Archetyper don't allow to specify project folder
      String projectFolder = location.append(artifactId).toFile().getAbsolutePath();

      LocalProjectScanner scanner = new LocalProjectScanner(workspaceRoot.getLocation().toFile(), //
          projectFolder, true, mavenModelManager);
      scanner.run(monitor);

      Set<MavenProjectInfo> projectSet = collectProjects(scanner.getProjects());

      List<IMavenProjectImportResult> importResults = importProjects(projectSet, configuration, listener, monitor);
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
      throw new CoreException(new Status(IStatus.ERROR,
          "org.eclipse.m2e", Messages.ProjectConfigurationManager_error_failed, ex)); //$NON-NLS-1$
    }

    return Collections.unmodifiableList(createdProjects);
  }

  /**
   * Apparently, Archetype#generateProjectFromArchetype 2.0-alpha-4 does not attempt to resolve archetype from
   * configured remote repositories. To compensate, we populate local repo with archetype pom/jar.
   */
  private Artifact resolveArchetype(Archetype a, IProgressMonitor monitor) throws CoreException {
    ArrayList<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
    repos.addAll(maven.getArtifactRepositories()); // see org.apache.maven.archetype.downloader.DefaultDownloader#download    

    //MNGECLIPSE-1399 use archetype repository too, not just the default ones
    String artifactRemoteRepository = a.getRepository();

    try {

      if(StringUtils.isNotBlank(artifactRemoteRepository)) {
        ArtifactRepository archetypeRepository = maven.createArtifactRepository(
            a.getArtifactId() + "-repo", a.getRepository().trim()); //$NON-NLS-1$
        repos.add(0, archetypeRepository);//If the archetype doesn't exist locally, this will be the first remote repo to be searched.
      }

      maven.resolve(a.getGroupId(), a.getArtifactId(), a.getVersion(), "pom", null, repos, monitor); //$NON-NLS-1$
      return maven.resolve(a.getGroupId(), a.getArtifactId(), a.getVersion(), "jar", null, repos, monitor); //$NON-NLS-1$
    } catch(CoreException e) {
      StringBuilder sb = new StringBuilder();
      sb.append(Messages.ProjectConfigurationManager_error_resolve).append(a.getGroupId()).append(':')
          .append(a.getArtifactId()).append(':').append(a.getVersion());
      sb.append(Messages.ProjectConfigurationManager_error_resolve2);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, sb.toString(), e));
    }
  }

  private org.apache.maven.archetype.ArchetypeManager getArchetyper() {
    return MavenPluginActivator.getDefault().getArchetypeManager().getArchetyper();
  }

  public Set<MavenProjectInfo> collectProjects(Collection<MavenProjectInfo> projects) {
    // TODO what does this do?
    return new LinkedHashSet<MavenProjectInfo>() {
      private static final long serialVersionUID = 1L;

      public Set<MavenProjectInfo> collectProjects(Collection<MavenProjectInfo> projects) {
        for(MavenProjectInfo projectInfo : projects) {
          log.info("Collecting project info " + projectInfo);
          add(projectInfo);
          collectProjects(projectInfo.getProjects());
        }
        return this;
      }
    }.collectProjects(projects);
  }

  public ISchedulingRule getRule() {
    return ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();
  }

  /*package*/IProject create(MavenProjectInfo projectInfo, ProjectImportConfiguration configuration,
      IProjectCreationListener listener, IProgressMonitor monitor) throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();

    File pomFile = projectInfo.getPomFile();
    Model model = projectInfo.getModel();
    if(model == null) {
      try (InputStream pomStream = Files.newInputStream(pomFile.toPath())) {
        model = maven.readModel(pomStream);
      } catch(IOException ex) {
        log.error(Messages.MavenImpl_error_read_pom, ex);
      }
      projectInfo.setModel(model);
    }

    String projectName = configuration.getProjectName(model);

    File projectDir = pomFile.getParentFile();
    String projectParent = projectDir.getParentFile().getAbsolutePath();

    if(projectInfo.getBasedirRename() == MavenProjectInfo.RENAME_REQUIRED) {
      File newProject = new File(projectDir.getParent(), projectName);
      if(!projectDir.equals(newProject)) {
        boolean renamed = projectDir.renameTo(newProject);
        if(!renamed) {
          StringBuilder msg = new StringBuilder();
          msg.append(NLS.bind(Messages.ProjectConfigurationManager_error_rename, projectDir.getAbsolutePath())).append(
              '.');
          if(newProject.exists()) {
            msg.append(NLS.bind(Messages.ProjectConfigurationManager_error_targetDir, newProject.getAbsolutePath()));
          }
          throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg.toString(), null));
        }
        projectInfo.setPomFile(getCanonicalPomFile(newProject));
        projectDir = newProject;
      }
    } else {
      if(projectParent.equals(root.getLocation().toFile().getAbsolutePath())) {
        // immediately under workspace root, project name must match filesystem directory name
        projectName = projectDir.getName();
      }
    }

    monitor.subTask(NLS.bind(Messages.ProjectConfigurationManager_task_importing2, projectName));

    IProject project = root.getProject(projectName);
    if(project.exists()) {
      log.error("Project " + projectName + " already exists");
      return null;
    }

    if(projectDir.equals(root.getLocation().toFile())) {
      log.error("Can't create project " + projectName + " at Workspace folder");
      return null;
    }

    if(projectParent.equals(root.getLocation().toFile().getAbsolutePath())) {
      project.create(monitor);
    } else {
      IProjectDescription description = workspace.newProjectDescription(projectName);
      description.setLocation(new Path(projectDir.getAbsolutePath()));
      project.create(description, monitor);
    }

    if(!project.isOpen()) {
      project.open(monitor);
    }

    if(listener != null) {
      listener.projectCreated(project);
    }

    ResolverConfiguration resolverConfiguration = configuration.getResolverConfiguration();
    enableBasicMavenNature(project, resolverConfiguration, monitor);

    // create empty/marker persistent configuration
    // 1 project with bad pom.xml gets imported in workspace
    // 1a empty/marker configuration is persisted here
    // 2 project's pom.xml gets fixed
    // 3 mavenProjectChanged below compares empty/marker with the real config and creates config marker
    LifecycleMappingConfiguration.persistEmpty(project);

    return project;
  }

  private File getCanonicalPomFile(File projectDir) throws CoreException {
    try {
      return new File(projectDir.getCanonicalFile(), IMavenConstants.POM_FILE_NAME);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
          NLS.bind(Messages.ProjectConfigurationManager_0, projectDir.getAbsolutePath()), null));
    }
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    for(MavenProjectChangedEvent event : events) {
      try {
        IMavenProjectFacade facade = event.getMavenProject();
        ILifecycleMapping lifecycleMapping = getLifecycleMapping(facade);
        if(lifecycleMapping != null) {
          for(AbstractProjectConfigurator configurator : lifecycleMapping.getProjectConfigurators(facade, monitor)) {
            //MNGECLIPSE-2004 : only send the relevant event to the configurator
            configurator.mavenProjectChanged(event, monitor);
          }
        }

        if(facade != null) {
          ProblemSeverity outOfDateSeverity = ProblemSeverity.get(mavenConfiguration.getOutOfDateProjectSeverity());
          mavenMarkerManager.deleteMarkers(facade.getProject(), IMavenConstants.MARKER_CONFIGURATION_ID);
          if(!ProblemSeverity.ignore.equals(outOfDateSeverity)) {
            LifecycleMappingConfiguration oldConfiguration = LifecycleMappingConfiguration.restore(facade, monitor);
            if(oldConfiguration != null
                && LifecycleMappingFactory.isLifecycleMappingChanged(facade, oldConfiguration, monitor)) {
              mavenMarkerManager.addMarker(facade.getProject(), IMavenConstants.MARKER_CONFIGURATION_ID,
                  Messages.ProjectConfigurationUpdateRequired, -1, outOfDateSeverity.getSeverity());
            }
          }
        } else {
          IMavenProjectFacade oldFacade = event.getOldMavenProject();
          if(oldFacade != null) {
            //LifecycleMappingConfiguration.remove(oldFacade);
            mavenMarkerManager.deleteMarkers(oldFacade.getPom(), IMavenConstants.MARKER_CONFIGURATION_ID);
          }
        }
      } catch(CoreException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  public ILifecycleMapping getLifecycleMapping(IMavenProjectFacade projectFacade) {
    if(projectFacade == null) {
      return null;
    }

    return LifecycleMappingFactory.getLifecycleMapping(projectFacade);
  }

  public void resourceChanged(IResourceChangeEvent event) {
    if(event.getType() == IResourceChangeEvent.PRE_DELETE && event.getResource() instanceof IProject) {
      LifecycleMappingConfiguration.remove((IProject) event.getResource());
    }
  }

  public ResolverConfiguration getResolverConfiguration(IProject project) {
    return ResolverConfigurationIO.readResolverConfiguration(project);
  }

  public boolean setResolverConfiguration(IProject project, ResolverConfiguration configuration) {
    return ResolverConfigurationIO.saveResolverConfiguration(project, configuration);
  }

  /**
   * Finds all existing Maven {@link IProject}s to hide from, if "hide folders of nested projects" preference is on,
   * else, returns an empty list.
   */
  List<IProject> findExistingProjectsToHideFrom() {
    if(!MavenPlugin.getMavenConfiguration().isHideFoldersOfNestedProjects()) {
      return Collections.emptyList();
    }
    IMavenProjectFacade[] existingFacades = projectManager.getProjects();
    if(existingFacades == null || existingFacades.length == 0) {
      return Collections.emptyList();
    }
    List<IProject> existingProjects = new ArrayList<>(existingFacades.length);
    for(IMavenProjectFacade f : existingFacades) {
      existingProjects.add(f.getProject());
    }
    return existingProjects;
  }
}
