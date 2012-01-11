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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.eclipse.core.resources.IMarker;
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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
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
 * import Maven projects
 * update project configuration from Maven 
 * enable Maven nature
 * create new project
 *
 * @author igor
 */
public class ProjectConfigurationManager implements IProjectConfigurationManager, IMavenProjectChangedListener,
    IResourceChangeListener {
  private static final Logger log = LoggerFactory.getLogger(ProjectConfigurationManager.class);

  final ProjectRegistryManager projectManager;

  final MavenModelManager mavenModelManager;

  final IMavenMarkerManager mavenMarkerManager;

  final IMaven maven;

  final IMavenConfiguration mavenConfiguration;

  public ProjectConfigurationManager(IMaven maven, ProjectRegistryManager projectManager,
      MavenModelManager mavenModelManager, IMavenMarkerManager mavenMarkerManager, IMavenConfiguration mavenConfiguration) {
    this.projectManager = projectManager;
    this.mavenModelManager = mavenModelManager;
    this.mavenMarkerManager = mavenMarkerManager;
    this.maven = maven;
    this.mavenConfiguration = mavenConfiguration;
  }

  public List<IMavenProjectImportResult> importProjects(Collection<MavenProjectInfo> projectInfos, ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    long t1 = System.currentTimeMillis();
    
    SubMonitor progress = SubMonitor.convert(monitor, Messages.ProjectConfigurationManager_task_importing, 100);

    ArrayList<IMavenProjectImportResult> result = new ArrayList<IMavenProjectImportResult>();
    ArrayList<IProject> projects = new ArrayList<IProject>();

    SubMonitor subProgress =
      SubMonitor.convert( progress.newChild( 10 ), projectInfos.size() * 100 );

    // first, create all projects with basic configuration
    for(MavenProjectInfo projectInfo : projectInfos) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      IProject project = create(projectInfo, configuration, subProgress.newChild(100));

      result.add(new MavenProjectImportResult(projectInfo, project));

      if (project != null) {
        projects.add(project);
      }
    }

    hideNestedProjectsFromParents(projects);
    // then configure maven for all projects
    configureNewMavenProject(projects, progress.newChild(90));

    long t2 = System.currentTimeMillis();
    log.info("Project import completed " + ((t2 - t1) / 1000) + " sec");

    return result;
  }

  private void setHidden(IResource resource) {
    try {
      resource.setHidden(true);
    } catch (Exception ex) {
      log.error("Failed to hide resource; " + resource.getLocation().toOSString(), ex);
    }
  }
  
  private void hideNestedProjectsFromParents(List<IProject> projects) {

    if(!MavenPlugin.getMavenConfiguration().isHideFoldersOfNestedProjects()) {
      return;
    }

    // Prevent child project folders from showing up in parent project folders.

    HashMap<File, IProject> projectFileMap = new HashMap<File, IProject>();

    for(IProject project : projects) {
      projectFileMap.put(project.getLocation().toFile(), project);
    }
    for(IProject project : projects) {
      File projectFile = project.getLocation().toFile();
      IProject physicalParentProject = projectFileMap.get(projectFile.getParentFile());
      if(physicalParentProject == null) {
        continue;
      }
      IFolder folder = physicalParentProject.getFolder(projectFile.getName());
      if(folder.exists()) {
        setHidden(folder);
      }
    }
  }
  
  private void configureNewMavenProject(List<IProject> projects, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, Messages.ProjectConfigurationManager_task_configuring, 100);

    //SubProgressMonitor sub = new SubProgressMonitor(monitor, projects.size()+1);
    
    // first, resolve maven dependencies for all projects
    MavenUpdateRequest updateRequest = new MavenUpdateRequest(mavenConfiguration.isOffline(), false);
    for(IProject project : projects) {
      updateRequest.addPomFile(project);
    }
    progress.subTask(Messages.ProjectConfigurationManager_task_refreshing);
    projectManager.refresh(updateRequest, progress.newChild(75));

    // TODO this emits project change events, which may be premature at this point

    //Creating maven facades 
    SubMonitor subProgress = SubMonitor.convert(progress.newChild(5), projects.size() * 100);
    List<IMavenProjectFacade> facades = new ArrayList<IMavenProjectFacade>(projects.size());
    for(IProject project : projects) {
      if(progress.isCanceled()) {
        throw new OperationCanceledException();
      } 
      IMavenProjectFacade facade = projectManager.create(project, subProgress.newChild(100));
      if (facade != null) {
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
      MavenSession mavenSession = createMavenSession(facade, subProgress.newChild(5));
      ProjectConfigurationRequest request = new ProjectConfigurationRequest(facade, mavenProject, mavenSession);
      updateProjectConfiguration(request, subProgress.newChild(90));
    }
  }

  private MavenSession createMavenSession(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest request = projectManager.createExecutionRequest(facade.getPom(), facade.getResolverConfiguration(), monitor);
    return maven.createSession(request, facade.getMavenProject(monitor));
  }

  public void sortProjects(List<IMavenProjectFacade> facades, IProgressMonitor monitor) throws CoreException {
      HashMap<MavenProject, IMavenProjectFacade> mavenProjectToFacadeMap = new HashMap<MavenProject, IMavenProjectFacade>(facades.size());
      for(IMavenProjectFacade facade:facades) {
        mavenProjectToFacadeMap.put(facade.getMavenProject(monitor), facade);
      }
      facades.clear();
      for(MavenProject mavenProject: maven.getSortedProjects(new ArrayList<MavenProject>(mavenProjectToFacadeMap.keySet()))) {
        facades.add(mavenProjectToFacadeMap.get(mavenProject));
      }
  }

  /**
   * A compatibility proxy stub
   */
  private static interface A {
    public IAdaptable[] adaptElements(IAdaptable[] objects);
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
   * Returns project name to update status map
   * 
   * @since 1.1
   * 
   * TODO promote to API
   */
  public Map<String, IStatus> updateProjectConfiguration(MavenUpdateRequest request, boolean updateConfiguration,
      boolean cleanProjects, IProgressMonitor monitor) {

    monitor.beginTask(Messages.ProjectConfigurationManager_task_updating_projects, request.getPomFiles().size()
        * (1 + (updateConfiguration ? 1 : 0) + (cleanProjects ? 1 : 0)));

    long l1 = System.currentTimeMillis();
    log.info("Update started"); //$NON-NLS-1$

    Map<IFile, IMavenProjectFacade> projects = new LinkedHashMap<IFile, IMavenProjectFacade>();

    //project names to the errors encountered when updating them
    Map<String, IStatus> updateStatus = new HashMap<String, IStatus>();

    // update all dependencies first
    // this will ensure that project registry is up-to-date on GAV of all projects being updated
    // TODO this sends multiple update events, rework using low-level registry update methods
    for(IFile pom : request.getPomFiles()) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      IProject project = pom.getProject();

      monitor.subTask(project.getName());
      
      try {
        SubProgressMonitor submonitor = new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
        projectManager.refresh(new MavenUpdateRequest(project, request.isOffline(), request.isForceDependencyUpdate()),
            submonitor);
        IMavenProjectFacade facade = projectManager.getProject(project);
        if(facade != null) { // facade is null if pom.xml cannot be read
          projects.put(pom, facade);
        }
        updateStatus.put(project.getName(), Status.OK_STATUS);
      } catch(CoreException ex) {
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
              facade.getMavenProject(submonitor), createMavenSession(facade, submonitor));
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
          if(!updateStatus.containsKey(project.getName())) {
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

  private void updateProjectConfiguration(ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    IProject project = request.getProject();
    long start = System.currentTimeMillis();
    IMavenProjectFacade mavenProjectFacade = request.getMavenProjectFacade();
    log.debug("Updating project configuration for {}.", mavenProjectFacade.toString()); //$NON-NLS-1$

    addMavenNature(project, monitor);

    // Configure project file encoding
    MavenProject mavenProject = request.getMavenProject();
    Properties mavenProperties = mavenProject.getProperties();
    String sourceEncoding = mavenProperties.getProperty("project.build.sourceEncoding");
    log.debug("Setting encoding for project {}: {}", project.getName(), sourceEncoding); //$NON-NLS-1$
    project.setDefaultCharset(sourceEncoding, monitor);
    
    ILifecycleMapping lifecycleMapping = getLifecycleMapping(mavenProjectFacade);

    if(lifecycleMapping != null) {
      mavenMarkerManager.deleteMarkers(mavenProjectFacade.getProject(), IMavenConstants.MARKER_CONFIGURATION_ID);

      lifecycleMapping.configure(request, monitor);

      LifecycleMappingConfiguration.persist(request.getMavenProjectFacade(), monitor);
    } else {
      log.debug("LifecycleMapping is null for project {}", mavenProjectFacade.toString()); //$NON-NLS-1$
    }

    log.debug(
        "Updated project configuration for {} in {} ms.", mavenProjectFacade.toString(), System.currentTimeMillis() - start); //$NON-NLS-1$
  }

  public void enableMavenNature(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    monitor.subTask(Messages.ProjectConfigurationManager_task_enable_nature);
    enableBasicMavenNature(project, configuration, monitor);

    ArrayList<IProject> projects = new ArrayList<IProject>();
    projects.add(project);
    configureNewMavenProject(projects, monitor);
  }

  private void enableBasicMavenNature(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    ResolverConfigurationIO.saveResolverConfiguration(project, configuration);

    // add maven nature even for projects without valid pom.xml file
    addMavenNature(project, monitor);
  }

  private void addMavenNature(IProject project, IProgressMonitor monitor) throws CoreException {
    if (!project.hasNature(IMavenConstants.NATURE_ID)) {
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
    if(facade!=null) {
      ILifecycleMapping lifecycleMapping = getLifecycleMapping(facade);
      if(lifecycleMapping != null) {
        ProjectConfigurationRequest request = new ProjectConfigurationRequest(facade, facade.getMavenProject(monitor),
            createMavenSession(facade, monitor));
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

    project.setDescription(description, null);
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
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    String projectName = project.getName();
    monitor.beginTask(NLS.bind(Messages.ProjectConfigurationManager_task_creating, projectName), 5);

    monitor.subTask(Messages.ProjectConfigurationManager_task_creating_workspace);
    IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    description.setLocation(location);
    project.create(description, monitor);
    project.open(monitor);
    monitor.worked(1);
    
    hideNestedProjectsFromParents(Collections.singletonList(project));

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

    monitor.subTask(Messages.ProjectConfigurationManager_task_creating_project);
    enableMavenNature(project, configuration.getResolverConfiguration(), monitor);
    monitor.worked(1);
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
   * Creates project structure using Archetype and then imports created project
   */
  public void createArchetypeProject(IProject project, IPath location, Archetype archetype, String groupId,
      String artifactId, String version, String javaPackage, Properties properties,
      ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(NLS.bind(Messages.ProjectConfigurationManager_task_creating_project1, project.getName()), 2);

    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
  
    monitor.subTask(NLS.bind(Messages.ProjectConfigurationManager_task_executing_archetype, archetype.getGroupId(), archetype.getArtifactId()));
    if(location == null) {
      // if the project should be created in the workspace, figure out the path
      location = workspaceRoot.getLocation();
    }

    try {
      
      
      
      Artifact artifact = resolveArchetype(archetype, monitor);
      
      ArchetypeGenerationRequest request = new ArchetypeGenerationRequest() //
          .setTransferListener(maven.createTransferListener(monitor)) //
          .setArchetypeGroupId(artifact.getGroupId()) //
          .setArchetypeArtifactId(artifact.getArtifactId()) //
          .setArchetypeVersion(artifact.getVersion()) //
          .setArchetypeRepository(archetype.getRepository()) //
          .setGroupId(groupId) //
          .setArtifactId(artifactId) //
          .setVersion(version) //
          .setPackage(javaPackage) // the model does not have a package field
          .setLocalRepository(maven.getLocalRepository()) //
          .setRemoteArtifactRepositories(maven.getArtifactRepositories(true))
          .setProperties(properties).setOutputDirectory(location.toPortableString());

      MavenSession session = maven.createSession(maven.createExecutionRequest(monitor), null);

      MavenSession oldSession = MavenPluginActivator.getDefault().setSession(session);

      ArchetypeGenerationResult result;
      try {
        result = getArchetyper().generateProjectFromArchetype(request);
      } finally {
        MavenPluginActivator.getDefault().setSession(oldSession);
      }

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
      
      importProjects(projectSet, configuration, monitor);
      
      monitor.worked(1);
    } catch (CoreException e) {
      throw e;
    } catch (InterruptedException e) {
      throw new CoreException(Status.CANCEL_STATUS);
    } catch (Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.m2e", Messages.ProjectConfigurationManager_error_failed, ex)); //$NON-NLS-1$
    }
  }

  /**
   * Apparently, Archetype#generateProjectFromArchetype 2.0-alpha-4 does not attempt to resolve archetype
   * from configured remote repositories. To compensate, we populate local repo with archetype pom/jar.
   */
  private Artifact resolveArchetype(Archetype a, IProgressMonitor monitor) throws CoreException {
    ArrayList<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
    repos.addAll(maven.getArtifactRepositories()); // see org.apache.maven.archetype.downloader.DefaultDownloader#download    

    //MNGECLIPSE-1399 use archetype repository too, not just the default ones
    String artifactRemoteRepository = a.getRepository();

    try {
    
      if (StringUtils.isBlank(artifactRemoteRepository)){
        
        IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();
        if (!mavenConfiguration.isOffline()){
          //Try to find the repository from remote catalog if needed
          final ArchetypeManager archetypeManager = MavenPluginActivator.getDefault().getArchetypeManager();
          RemoteCatalogFactory factory = archetypeManager.findParentCatalogFactory(a, RemoteCatalogFactory.class);
          if (factory != null) {
             //Grab the computed remote repository url
              artifactRemoteRepository = factory.getRepositoryUrl();
              a.setRepository(artifactRemoteRepository);//Hopefully will prevent further lookups for the same archetype
          }
        }
      }
  
      if (StringUtils.isNotBlank(artifactRemoteRepository)) {
        ArtifactRepository archetypeRepository = maven.createArtifactRepository("archetype", a.getRepository().trim()); //$NON-NLS-1$
        repos.add(0,archetypeRepository);//If the archetype doesn't exist locally, this will be the first remote repo to be searched.
      }
    
      maven.resolve(a.getGroupId(), a.getArtifactId(),a.getVersion(), "pom", null, repos, monitor); //$NON-NLS-1$
      return maven.resolve(a.getGroupId(), a.getArtifactId(),a.getVersion(), "jar", null, repos, monitor); //$NON-NLS-1$
    } catch (CoreException e) {
      StringBuilder sb = new StringBuilder();
      sb.append(Messages.ProjectConfigurationManager_error_resolve).append(a.getGroupId()).append(':').append(a.getArtifactId()).append(':').append(a.getVersion());
      sb.append(Messages.ProjectConfigurationManager_error_resolve2);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, sb.toString(), e));
    }
  }

  private org.apache.maven.archetype.Archetype getArchetyper() {
    return MavenPluginActivator.getDefault().getArchetype();
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

  private IProject create(MavenProjectInfo projectInfo, ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    
    File pomFile = projectInfo.getPomFile(); 
    Model model = projectInfo.getModel();
    if(model == null) {
      model = maven.readModel(pomFile);
      projectInfo.setModel(model);
    }

    String projectName = configuration.getProjectName(model);

    File projectDir = pomFile.getParentFile();
    String projectParent = projectDir.getParentFile().getAbsolutePath();

    if (projectInfo.getBasedirRename() == MavenProjectInfo.RENAME_REQUIRED) {
      File newProject = new File(projectDir.getParent(), projectName);
      if(!projectDir.equals(newProject)) {
        boolean renamed = projectDir.renameTo(newProject);
        if(!renamed) {
          StringBuilder msg = new StringBuilder();
          msg.append(NLS.bind(Messages.ProjectConfigurationManager_error_rename, projectDir.getAbsolutePath())).append('.');
          if (newProject.exists()) {
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
          LifecycleMappingConfiguration oldConfiguration = LifecycleMappingConfiguration.restore(facade, monitor);
          if(oldConfiguration != null
              && LifecycleMappingFactory.isLifecycleMappingChanged(facade, oldConfiguration, monitor)) {
            mavenMarkerManager.addMarker(facade.getProject(), IMavenConstants.MARKER_CONFIGURATION_ID,
                Messages.ProjectConfigurationUpdateRequired, -1, IMarker.SEVERITY_ERROR);
          }
        } else {
          IMavenProjectFacade oldFacade = event.getOldMavenProject();
          if(oldFacade != null) {
            //LifecycleMappingConfiguration.remove(oldFacade);
            mavenMarkerManager.deleteMarkers(oldFacade.getPom(), IMavenConstants.MARKER_CONFIGURATION_ID);
          }
        }
      } catch (CoreException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  public ILifecycleMapping getLifecycleMapping(IMavenProjectFacade projectFacade) throws CoreException {
    if (projectFacade==null) {
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
}
