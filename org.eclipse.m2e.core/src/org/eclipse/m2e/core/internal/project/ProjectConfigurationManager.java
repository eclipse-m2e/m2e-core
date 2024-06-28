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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.IMavenToolbox;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.AbstractRunnable;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.embedder.PlexusContainerManager;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IProjectCreationListener;
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
@Component(service = {IProjectConfigurationManager.class, IMavenProjectChangedListener.class,
    IResourceChangeListener.class}, property = "event.mask:Integer=" + IResourceChangeEvent.PRE_DELETE)
public class ProjectConfigurationManager
    implements IProjectConfigurationManager, IMavenProjectChangedListener, IResourceChangeListener {
  private static final Logger log = LoggerFactory.getLogger(ProjectConfigurationManager.class);

  @Reference
  ProjectRegistryManager projectManager;

  @Reference
  MavenModelManager mavenModelManager;

  @Reference
  IMavenMarkerManager mavenMarkerManager;

  @Reference
  IMaven maven;

  @Reference
  IMavenConfiguration mavenConfiguration;

  @Reference
  PlexusContainerManager containerManager;

  @Override
  public List<IMavenProjectImportResult> importProjects(Collection<MavenProjectInfo> projectInfos,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    return importProjects(projectInfos, configuration, null, monitor);
  }

  @Override
  public List<IMavenProjectImportResult> importProjects(Collection<MavenProjectInfo> projectInfos,
      ProjectImportConfiguration configuration, IProjectCreationListener listener, IProgressMonitor monitor)
      throws CoreException {

    // overall execution context to share repository session data and cache for all projects
    return IMavenExecutionContext.getThreadContext().orElseGet(maven::createExecutionContext).execute((context, m) -> {
      SubMonitor progress = SubMonitor.convert(m, Messages.ProjectConfigurationManager_task_importing, 100);
      long t1 = System.currentTimeMillis();
      List<IMavenProjectImportResult> result = new ArrayList<>();
      int total = projectInfos.size();
      List<IProject> projects = new ArrayList<>(total);
      int i = 0;

      List<IProject> existingProjects = findExistingProjectsToHideFrom();

      // first, create all projects with basic configuration
      SubMonitor subProgress = SubMonitor.convert(progress.split(10), projectInfos.size());
      for(MavenProjectInfo projectInfo : projectInfos) {
        long t11 = System.currentTimeMillis();
        IProject project = create(projectInfo, configuration, listener, subProgress.split(1));

        result.add(new MavenProjectImportResult(projectInfo, project));

        if(project != null) {
          projects.add(project);
          long importTime = System.currentTimeMillis() - t11;
          log.debug("Imported project {} ({}/{}) in {} ms", project.getName(), ++i, total, importTime);
        }
      }

      hideNestedProjectsFromParents(projects, existingProjects, m);
      // then configure maven for all projects
      configureNewMavenProjects(projects, progress.split(90));

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

  void hideNestedProjectsFromParents(List<IProject> projects, List<IProject> existingProjects,
      IProgressMonitor monitor) {

    if(!MavenPlugin.getMavenConfiguration().isHideFoldersOfNestedProjects()) {
      return;
    }

    // Prevent child project folders from showing up in parent project folders.

    Map<File, IProject> projectFileMap = new HashMap<>();

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

  private void configureNewMavenProjects(List<IProject> projects, IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, Messages.ProjectConfigurationManager_task_configuring, 100);

    // first, resolve maven dependencies for all projects
    Set<IFile> pomFiles = new LinkedHashSet<>();
    for(IProject project : projects) {
      File baseDir = project.getLocation().toFile();
      IMavenToolbox.of(containerManager.getComponentLookup(baseDir)).locatePom(baseDir).ifPresent(pomFile -> {
        pomFiles.add(project.getFile(pomFile.getName()));
      });
    }
    progress.subTask(Messages.ProjectConfigurationManager_task_refreshing);

    projectManager.refresh(pomFiles, progress.split(75));

    // TODO this emits project change events, which may be premature at this point

    //Creating maven facades
    SubMonitor subProgress = SubMonitor.convert(progress.split(5), projects.size());
    List<IMavenProjectFacade> facades = new ArrayList<>(projects.size());
    for(IProject project : projects) {
      IMavenProjectFacade facade = projectManager.create(project, subProgress.split(1));
      if(facade != null) {
        facades.add(facade);
      }
    }

    //MNGECLIPSE-1028 : Sort projects by build order here,
    //as dependent projects need to be configured before depending projects (in WTP integration for ex.)
    sortProjects(facades, progress.split(5));
    //Then, perform detailed project configuration
    subProgress = SubMonitor.convert(progress.split(15), facades.size() * 10);
    for(IMavenProjectFacade facade : facades) {
      progress.subTask(NLS.bind(Messages.ProjectConfigurationManager_task_updating, facade.getProject().getName()));
      MavenProject mavenProject = facade.getMavenProject(subProgress.split(1));
      ProjectConfigurationRequest request = new ProjectConfigurationRequest(facade, mavenProject);
      updateProjectConfiguration(request, subProgress.split(9));
    }
  }

  public void sortProjects(List<IMavenProjectFacade> facades, IProgressMonitor monitor) throws CoreException {
    Map<MavenProject, IMavenProjectFacade> mavenProjectToFacadeMap = new HashMap<>(facades.size());
    for(IMavenProjectFacade facade : facades) {
      mavenProjectToFacadeMap.put(facade.getMavenProject(monitor), facade);
    }
    facades.clear();
    for(MavenProject mavenProject : maven.getSortedProjects(new ArrayList<>(mavenProjectToFacadeMap.keySet()))) {
      facades.add(mavenProjectToFacadeMap.get(mavenProject));
    }
  }

  @Override
  public void updateProjectConfiguration(IProject project, IProgressMonitor monitor) throws CoreException {
    updateProjectConfiguration(new MavenUpdateRequest(project, mavenConfiguration.isOffline(), false), monitor);
  }

  // TODO deprecate this method
  @Override
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
  public Map<String, IStatus> updateProjectConfiguration(MavenUpdateRequest request, boolean updateConfiguration,
      boolean cleanProjects, IProgressMonitor monitor) {
    return updateProjectConfiguration(request, updateConfiguration, cleanProjects, false/*refreshFromLocal*/, monitor);
  }

  /**
   * Returns project name to update status map.
   * <p/>
   * TODO promote to API. TODO decide if workspace or other lock is required during execution of this method.
   *
   * @since 1.4
   */
  public Map<String, IStatus> updateProjectConfiguration(MavenUpdateRequest request, boolean updateConfiguration,
      boolean cleanProjects, boolean refreshFromLocal, IProgressMonitor monitor) {
    try {
      return MavenImpl.execute(maven, request.isOffline(), request.isForceDependencyUpdate(),
          (context, m) -> updateProjectConfiguration0(request.getPomFiles(), updateConfiguration, cleanProjects,
              refreshFromLocal, m),
          monitor);
    } catch(CoreException ex) {
      Map<String, IStatus> result = new LinkedHashMap<>();
      for(IFile pomFile : request.getPomFiles()) {
        result.put(pomFile.getProject().getName(), ex.getStatus());
      }
      return result;
    }
  }

  private Map<String, IStatus> updateProjectConfiguration0(Collection<IFile> pomFiles, boolean updateConfiguration,
      boolean cleanProjects, boolean refreshFromLocal, IProgressMonitor m) {

    SubMonitor monitor = SubMonitor.convert(m, Messages.ProjectConfigurationManager_task_updating_projects,
        pomFiles.size() * (1 + (updateConfiguration ? 1 : 0) + (cleanProjects ? 1 : 0) + (refreshFromLocal ? 1 : 0)));

    long start = System.currentTimeMillis();
    log.info("Update started"); //$NON-NLS-1$

    Map<IFile, IMavenProjectFacade> projects = new LinkedHashMap<>();

    //project names to the errors encountered when updating them
    Map<String, IStatus> updateStatus = new HashMap<>();

    List<IFile> pomsToRefresh = new ArrayList<>();

    // refresh from local filesystem
    if(refreshFromLocal) {
      for(IFile pom : pomFiles) {
        IProject project = pom.getProject();
        try {
          project.refreshLocal(IResource.DEPTH_INFINITE, monitor.split(1, SubMonitor.SUPPRESS_SUBTASK));
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
    long refreshLocal = System.currentTimeMillis();
    try {
      Map<IFile, IStatus> refresh = projectManager.refresh(pomsToRefresh, monitor.split(pomFiles.size()));

      for(IFile pom : pomsToRefresh) {
        IProject project = pom.getProject();
        IStatus status = refresh.get(pom);
        if(status != null && !status.isOK()) {
          updateStatus.put(project.getName(), status);
          continue;
        }
        IMavenProjectFacade facade = projectManager.getProject(project);
        if(facade != null) { // facade is null if pom.xml cannot be read
          projects.put(pom, facade);
        }
        updateStatus.put(project.getName(), Status.OK_STATUS);
      }
    } catch(CoreException ex) {
      for(IFile pom : pomsToRefresh) {
        IProject project = pom.getProject();
        updateStatus.put(project.getName(), ex.getStatus());
      }
    }
    long refreshProjects = System.currentTimeMillis();

    // update project configuration
    if(updateConfiguration) {

      projects.values().removeIf(facade -> {
        String projectName = facade.getProject().getName();
        IStatus status = updateStatus.get(projectName);
        if(status != null && !status.isOK()) {
          updateStatus.put(projectName, status);
          return true;
        }
        monitor.subTask(projectName);
        try {
          SubMonitor submonitor = monitor.split(1, SubMonitor.SUPPRESS_SUBTASK);
          ProjectConfigurationRequest cfgRequest = new ProjectConfigurationRequest(facade,
              facade.getMavenProject(submonitor));
          updateProjectConfiguration(cfgRequest, submonitor);
        } catch(CoreException ex) {
          updateStatus.put(projectName, ex.getStatus());
          return true;
        }
        return false;
      });

    }
    long updateConfig = System.currentTimeMillis();

    // rebuild
    if(cleanProjects) {
      projects.values().removeIf(facade -> {
        monitor.checkCanceled();
        IProject project = facade.getProject();
        monitor.subTask(project.getName());

        try {
          // only rebuild projects that were successfully updated
          IStatus status = updateStatus.get(project.getName());
          if(status == null || status.isOK()) {
            project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor.split(1, SubMonitor.SUPPRESS_SUBTASK));
            // TODO provide an option to build projects if the workspace is not autobuilding
          }
        } catch(CoreException ex) {
          updateStatus.put(project.getName(), ex.getStatus());
          return true;
        }
        return false;
      });
    }

    long clean = System.currentTimeMillis();
    log.info("Update completed for {} poms: {}, {}, {} ", pomFiles.size(), //$NON-NLS-1$
        printDelta("local refresh", start, refreshLocal), printDelta("refresh facades", refreshLocal, refreshProjects),
        printDelta("update config", refreshProjects, updateConfig),
        printDelta("clean projects config", updateConfig, clean),
        printDelta("total", start, clean));

    return updateStatus;
  }

  /**
   * @param start
   * @param refreshLocal
   * @return
   */
  private String printDelta(String type, long start, long end) {
    double delay = (end - start) / 1000;
    return String.format("%s takes %.2f sec", type, delay);
  }

  private void updateProjectConfiguration(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    IProject project = request.mavenProjectFacade().getProject();
    long start = System.currentTimeMillis();
    IMavenProjectFacade mavenProjectFacade = request.mavenProjectFacade();
    log.debug("Updating project configuration for {}.", mavenProjectFacade); //$NON-NLS-1$

    addMavenNature(project, monitor);

    // Configure project file encoding
    MavenProject mavenProject = request.mavenProject();
    Properties mavenProperties = mavenProject.getProperties();
    String sourceEncoding = mavenProperties.getProperty("project.build.sourceEncoding");
    if(!Objects.equals(project.getDefaultCharset(false), sourceEncoding)) {
      log.debug("Setting encoding for project {}: {}", project.getName(), sourceEncoding); //$NON-NLS-1$
      project.setDefaultCharset(sourceEncoding, monitor);
    }

    IMavenExecutionContext executionContext = projectManager.createExecutionContext(mavenProjectFacade.getPom(),
        mavenProjectFacade.getConfiguration());

    executionContext.execute(mavenProject, (context, m) -> {
      ILifecycleMapping lifecycleMapping = getLifecycleMapping(mavenProjectFacade);

      if(lifecycleMapping != null) {
        mavenMarkerManager.deleteMarkers(mavenProjectFacade.getProject(), IMavenConstants.MARKER_CONFIGURATION_ID);

        lifecycleMapping.configure(request, m);

        LifecycleMappingConfiguration.persist(request.mavenProjectFacade(), m);
      } else {
        log.debug("LifecycleMapping is null for project {}", mavenProjectFacade); //$NON-NLS-1$
      }
      return null;
    }, monitor);

    log.debug("Updated project configuration for {} in {} ms.", mavenProjectFacade, System.currentTimeMillis() - start); //$NON-NLS-1$
  }

  @Override
  public void enableMavenNature(IProject project, IProjectConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    monitor.subTask(Messages.ProjectConfigurationManager_task_enable_nature);
    IMavenExecutionContext.getThreadContext().orElseGet(maven::createExecutionContext).execute(new AbstractRunnable() {
      @Override
      protected void run(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        enableBasicMavenNature(project, configuration, monitor);
        configureNewMavenProjects(Collections.singletonList(project), monitor);
      }
    }, monitor);
  }

  void enableBasicMavenNature(IProject project, IProjectConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
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

  @Override
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
    List<String> newNatures = new ArrayList<>();
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
    MavenPlugin.getMavenProjectRegistry()
        .refresh(new MavenUpdateRequest(project, mavenConfiguration.isOffline(), false));
  }

  @Override
  public boolean addMavenBuilder(IProject project, IProjectDescription description, IProgressMonitor monitor)
      throws CoreException {
    boolean setProjectDescription = false;
    if(description == null) {
      description = project.getDescription();
      setProjectDescription = true;
    }

    // ensure Maven builder is always the last one
    ICommand mavenBuilder = null;
    List<ICommand> newSpec = new ArrayList<>();
    int i = 0;
    for(ICommand command : description.getBuildSpec()) {
      if(isMavenBuilderCommand(command)) {
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
    description.setBuildSpec(newSpec.toArray(ICommand[]::new));

    if(setProjectDescription) {
      project.setDescription(description, monitor);
    }
    return true;
  }

  @Override
  public boolean removeMavenBuilder(IProject project, IProjectDescription description, IProgressMonitor monitor)
      throws CoreException {
    boolean setProjectDescription = false;
    if(description == null) {
      description = project.getDescription();
      setProjectDescription = true;
    }

    boolean foundMavenBuilder = false;
    List<ICommand> newSpec = new ArrayList<>();
    for(ICommand command : description.getBuildSpec()) {
      if(!isMavenBuilderCommand(command)) {
        newSpec.add(command);
      } else {
        foundMavenBuilder = true;
      }
    }
    if(!foundMavenBuilder) {
      return false;
    }
    description.setBuildSpec(newSpec.toArray(ICommand[]::new));

    if(setProjectDescription) {
      project.setDescription(description, monitor);
    }

    return true;
  }

  private boolean isMavenBuilderCommand(ICommand command) {
    return IMavenConstants.BUILDER_ID.equals(command.getBuilderName());
  }

  // project creation

  @Override
  public void createSimpleProject(IProject project, IPath location, Model model, List<String> directories,
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
  @Override
  public void createSimpleProject(IProject project, IPath location, Model model, List<String> directories,
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
    for(String element : directories) {
      createFolder(project.getFolder(element), false);
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
    MavenProjectFacade parentProjectFacade = projectManager.getMavenProject(parent.getGroupId(), parent.getArtifactId(),
        parent.getVersion());
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
      folder.setDerived(true, new NullProgressMonitor());
    }
  }

  @Override
  public Set<MavenProjectInfo> collectProjects(Collection<MavenProjectInfo> projects) {
    // TODO what does this do?
    return new LinkedHashSet<MavenProjectInfo>() {
      private static final long serialVersionUID = 1L;

      public Set<MavenProjectInfo> collectProjects(Collection<MavenProjectInfo> projects) {
        for(MavenProjectInfo projectInfo : projects) {
          log.info("Collecting project info {}", projectInfo);
          add(projectInfo);
          collectProjects(projectInfo.getProjects());
        }
        return this;
      }
    }.collectProjects(projects);
  }

  @Override
  public ISchedulingRule getRule() {
    return ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();
  }

  IProject create(MavenProjectInfo projectInfo, ProjectImportConfiguration configuration,
      IProjectCreationListener listener, IProgressMonitor monitor) throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();

    File pomFile = projectInfo.getPomFile();
    Model model = projectInfo.getModel();
    if(model == null) {
      try (InputStream pomStream = Files.newInputStream(pomFile.toPath())) {
        model = IMavenToolbox.of(maven).readModel(pomStream);
      } catch(IOException ex) {
        log.error(Messages.MavenImpl_error_read_pom, ex);
      }
      projectInfo.setModel(model);
    }
    if(model == null) {
      throw new IllegalStateException("Failed to load model of project " + projectInfo);
    }
    model.setPomFile(pomFile);
    String projectName = getProjectName(configuration, model);

    File projectDir = pomFile.getParentFile();
    String projectParent = projectDir.getParentFile().getAbsolutePath();

    if(projectInfo.getBasedirRename() == MavenProjectInfo.RENAME_REQUIRED) {
      File newProject = new File(projectDir.getParent(), projectName);
      if(!projectDir.equals(newProject)) {
        boolean renamed = projectDir.renameTo(newProject);
        if(!renamed) {
          StringBuilder msg = new StringBuilder();
          msg.append(NLS.bind(Messages.ProjectConfigurationManager_error_rename, projectDir.getAbsolutePath()))
              .append('.');
          if(newProject.exists()) {
            msg.append(NLS.bind(Messages.ProjectConfigurationManager_error_targetDir, newProject.getAbsolutePath()));
          }
          throw new CoreException(Status.error(msg.toString()));
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
      log.error("Project {} already exists", projectName);
      return null;
    }

    if(projectDir.equals(root.getLocation().toFile())) {
      log.error("Can't create project {} at Workspace folder", projectName);
      return null;
    }

    if(projectParent.equals(root.getLocation().toFile().getAbsolutePath())) {
      project.create(monitor);
    } else {
      IProjectDescription description = workspace.newProjectDescription(projectName);
      description.setLocation(IPath.fromOSString(projectDir.getAbsolutePath()));
      project.create(description, monitor);
    }

    if(!project.isOpen()) {
      project.open(monitor);
    }

    if(listener != null) {
      listener.projectCreated(project);
    }

    IProjectConfiguration resolverConfiguration = configuration.getResolverConfiguration();
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
      throw new CoreException(
          Status.error(NLS.bind(Messages.ProjectConfigurationManager_0, projectDir.getAbsolutePath())));
    }
  }

  @Override
  public void mavenProjectChanged(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
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
          IProject project = facade.getProject();
          mavenMarkerManager.deleteMarkers(project, IMavenConstants.MARKER_CONFIGURATION_ID);
          if(!ProblemSeverity.ignore.equals(outOfDateSeverity)) {
            LifecycleMappingConfiguration oldConfiguration = LifecycleMappingConfiguration.restore(facade, monitor);
            if(oldConfiguration != null
                && LifecycleMappingFactory.isLifecycleMappingChanged(facade, oldConfiguration, monitor)) {
              if(!ResolverConfigurationIO.isAutomaticallyUpdateConfiguration(project)) {
                outOfDateSeverity = ProblemSeverity.info;
              }
              mavenMarkerManager.addMarker(project, IMavenConstants.MARKER_CONFIGURATION_ID,
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

  @Override
  public ILifecycleMapping getLifecycleMapping(IMavenProjectFacade projectFacade) {
    if(projectFacade == null) {
      return null;
    }

    return LifecycleMappingFactory.getLifecycleMapping(projectFacade);
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if(event.getType() == IResourceChangeEvent.PRE_DELETE && event.getResource() instanceof IProject project) {
      LifecycleMappingConfiguration.remove(project);
    }
  }

  @Override
  public ResolverConfiguration getResolverConfiguration(IProject project) {
    IProjectConfiguration cfg = ResolverConfigurationIO.readResolverConfiguration(project);
    if(cfg instanceof ResolverConfiguration) {
      return (ResolverConfiguration) cfg;
    }
    return new ResolverConfiguration(cfg);
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.IProjectConfigurationManager#getProjectConfiguration(org.eclipse.core.resources.IProject)
   */
  @Override
  public IProjectConfiguration getProjectConfiguration(IProject project) {
    return ResolverConfigurationIO.readResolverConfiguration(project);
  }

  @Override
  public boolean setResolverConfiguration(IProject project, ResolverConfiguration configuration) {
    return ResolverConfigurationIO.saveResolverConfiguration(project, configuration);
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.IProjectConfigurationManager#updateProjectConfiguration(org.eclipse.core.resources.IProject, org.eclipse.m2e.core.project.IProjectConfiguration, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public IStatus updateProjectConfiguration(IProject project, IProjectConfiguration configuration,
      IProgressMonitor monitor) {
    if(ResolverConfigurationIO.saveResolverConfiguration(project, configuration)) {
      try {
        updateProjectConfiguration(project, monitor);
      } catch(CoreException ex) {
        return ex.getStatus();
      }
    }
    return Status.CANCEL_STATUS;
  }

  /**
   * Finds all existing Maven {@link IProject}s to hide from, if "hide folders of nested projects" preference is on,
   * else, returns an empty list.
   */
  List<IProject> findExistingProjectsToHideFrom() {
    if(!MavenPlugin.getMavenConfiguration().isHideFoldersOfNestedProjects()) {
      return Collections.emptyList();
    }
    List<MavenProjectFacade> existingFacades = projectManager.getProjects();
    if(existingFacades == null || existingFacades.isEmpty()) {
      return Collections.emptyList();
    }
    return existingFacades.stream().map(IMavenProjectFacade::getProject).toList();
  }

  private static final String GROUP_ID = "[groupId]"; //$NON-NLS-1$

  private static final String ARTIFACT_ID = "[artifactId]"; //$NON-NLS-1$

  private static final String VERSION = "[version]"; //$NON-NLS-1$

  private static final String NAME = "[name]"; //$NON-NLS-1$ 

  /** Calculates the project name for the given model. */
  public static String getProjectName(ProjectImportConfiguration importConfig, Model model) {
    // XXX should use resolved MavenProject or Model
    //TODO: This method does not take into account MavenProjectInfo.basedirRename
    if(importConfig.getProjectNameTemplate().isEmpty()) {
      String cleanProjectNameComponent = cleanProjectNameComponent(model.getArtifactId(), false);
      if(cleanProjectNameComponent != null && !cleanProjectNameComponent.isEmpty()) {
        return cleanProjectNameComponent;
      }
      return model.getPomFile().getParentFile().getName();
    }

    String artifactId = model.getArtifactId();
    String groupId = model.getGroupId();
    if(groupId == null && model.getParent() != null) {
      groupId = model.getParent().getGroupId();
    }
    String version = model.getVersion();
    if(version == null && model.getParent() != null) {
      version = model.getParent().getVersion();
    }
    String name = model.getName();
    if(name == null || name.trim().isEmpty()) {
      name = artifactId;
    }

    // XXX needs MavenProjectManager update to resolve groupId and version
    return importConfig.getProjectNameTemplate().replace(GROUP_ID, cleanProjectNameComponent(groupId, true))
        .replace(ARTIFACT_ID, cleanProjectNameComponent(artifactId, true))
        .replace(NAME, cleanProjectNameComponent(name, true))
        .replace(VERSION, version == null ? "" : cleanProjectNameComponent(version, true)); //$NON-NLS-1$
  }

  private static final String cleanProjectNameComponent(String value, boolean quote) {
    // remove property placeholders
    value = value.replaceAll("\\$\\{[^\\}]++\\}", ""); //$NON-NLS-1$ $NON-NLS-2$
    if(quote) {
      value = Matcher.quoteReplacement(value);
    }
    return value;
  }

}
