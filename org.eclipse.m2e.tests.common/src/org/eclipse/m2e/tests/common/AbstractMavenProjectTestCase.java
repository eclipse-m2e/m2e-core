/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.common;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.wagon.Wagon;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.embedder.AbstractRunnable;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectCreationListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.MavenJdtPlugin;
import org.eclipse.m2e.jdt.internal.BuildPathManager;


@SuppressWarnings("restriction")
public abstract class AbstractMavenProjectTestCase extends TestCase {

  public static final int DELETE_RETRY_COUNT = 10;

  public static final long DELETE_RETRY_DELAY = 6000L;

  protected static final IProgressMonitor monitor = new NullProgressMonitor();

  protected IWorkspace workspace;

  protected File repo;

  protected ProjectRegistryRefreshJob projectRefreshJob;

  protected Job downloadSourcesJob;

  protected IMavenConfiguration mavenConfiguration;

  private String oldUserSettingsFile;

  protected void setUp() throws Exception {
    System.out.println("TEST-SETUP: " + getName());

    super.setUp();

    workspace = ResourcesPlugin.getWorkspace();
    mavenConfiguration = MavenPlugin.getMavenConfiguration();
    setAutoBuilding(false);
    setAutomaticallyUpdateConfiguration(false);

    // lets not assume we've got subversion in the target platform 
    Hashtable<String, String> options = JavaCore.getOptions();
    options.put(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, ".svn/");
    JavaCore.setOptions(options);

    projectRefreshJob = MavenPluginActivator.getDefault().getProjectManagerRefreshJob();
    projectRefreshJob.sleep();

    downloadSourcesJob = ((BuildPathManager) MavenJdtPlugin.getDefault().getBuildpathManager()).getDownloadSourcesJob();
    downloadSourcesJob.sleep();

    oldUserSettingsFile = mavenConfiguration.getUserSettingsFile();
    File settings = new File("settings.xml").getCanonicalFile();
    if(settings.canRead()) {
      String userSettingsFile = settings.getAbsolutePath();
      mavenConfiguration.setUserSettingsFile(userSettingsFile);
    }

    ArtifactRepository localRepository = MavenPlugin.getMaven().getLocalRepository();
    if(localRepository != null) {
      repo = new File(localRepository.getBasedir());
    } else {
      fail("Cannot determine local repository path");
    }

    // make sure all tests use default metadata by default
    LifecycleMappingFactory.setUseDefaultLifecycleMappingMetadataSource(true);
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(null);

    WorkspaceHelpers.cleanWorkspace();
    FilexWagon.setRequestFailPattern(null);
    FilexWagon.setRequestFilterPattern(null, true);
  }

  protected void tearDown() throws Exception {
    try {
      waitForJobsToComplete();
      WorkspaceHelpers.cleanWorkspace();

      // Restore the user settings file location
      mavenConfiguration.setUserSettingsFile(oldUserSettingsFile);

      projectRefreshJob.wakeUp();
      setAutoBuilding(false);
      setAutomaticallyUpdateConfiguration(false);
      FilexWagon.reset();
    } finally {
      super.tearDown();
    }
  }

  /**
   * @since 1.6.0
   */
  protected void setAutoBuilding(boolean autobuilding) throws CoreException {
    IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(autobuilding);
    workspace.setDescription(description);
  }

  /**
   * @since 1.6.0
   */
  protected boolean isAutoBuilding() {
    IWorkspaceDescription description = workspace.getDescription();
    return description.isAutoBuilding();
  }

  /**
   * @since 1.6.0
   */
  protected void setAutomaticallyUpdateConfiguration(boolean update) {
    ((MavenConfigurationImpl) mavenConfiguration).setAutomaticallyUpdateConfiguration(update);
  }

  /**
   * Synchronously refreshes maven project state.
   * 
   * @see IMavenProjectRegistry#refresh(Collection, IProgressMonitor)
   * @since 1.6.0
   */
  protected void refreshMavenProject(IProject project) {
    MavenPlugin.getMavenProjectRegistry().refresh(new MavenUpdateRequest(project, false, false));
  }

  protected void deleteProject(String projectName) throws CoreException, InterruptedException {
    IProject project = workspace.getRoot().getProject(projectName);

    deleteProject(project);
  }

  protected void deleteProject(IProject project) throws InterruptedException, CoreException {
    Exception cause = null;
    for(int i = 0; i < DELETE_RETRY_COUNT; i++ ) {
      try {
        doDeleteProject(project);
      } catch(InterruptedException e) {
        throw e;
      } catch(OperationCanceledException e) {
        throw e;
      } catch(Exception e) {
        cause = e;
        Thread.sleep(DELETE_RETRY_DELAY);
        continue;
      }

      // all clear
      return;
    }

    // must be a timeout
    throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, "Could not delete project", cause));
  }

  private void doDeleteProject(final IProject project) throws CoreException, InterruptedException {
    waitForJobsToComplete(monitor);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        if(project.exists()) {
          deleteMember(".classpath", project, monitor);
          deleteMember(".project", project, monitor);
          project.delete(false, true, monitor);
        }
      }

      private void deleteMember(String name, final IProject project, IProgressMonitor monitor) throws CoreException {
        IResource member = project.findMember(name);
        if(member != null && member.exists()) {
          member.delete(true, monitor);
        }
      }
    }, new NullProgressMonitor());
  }

  protected IProject createProject(String projectName, final String pomResource) throws CoreException {
    final IProject project = workspace.getRoot().getProject(projectName);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        project.create(monitor);

        if(!project.isOpen()) {
          project.open(monitor);
        }

        IFile pomFile = project.getFile("pom.xml");
        if(!pomFile.exists()) {
          InputStream is = null;
          try {
            is = new FileInputStream(pomResource);
            pomFile.create(is, true, monitor);
          } catch(FileNotFoundException ex) {
            throw new CoreException(new Status(IStatus.ERROR, "", 0, ex.toString(), ex));
          } finally {
            IOUtil.close(is);
          }
        }
      }
    }, null);

    return project;
  }

  protected IProject createExisting(String projectName, String projectLocation) throws IOException, CoreException {
    File dir = new File(workspace.getRoot().getLocation().toFile(), projectName);
    copyDir(new File(projectLocation), dir);

    final IProject project = workspace.getRoot().getProject(projectName);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        if(!project.exists()) {
          IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());
          projectDescription.setLocation(null);
          project.create(projectDescription, monitor);
          project.open(IResource.NONE, monitor);
        } else {
          project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
      }
    }, null);

    // emulate behavior when autobuild was not honored by ProjectRegistryRefreshJob
    if(!isAutoBuilding()) {
      refreshMavenProject(project);
    }

    return project;
  }

  /**
   * Import a test project into the Eclipse workspace
   * 
   * @param pomLocation - a relative location of the pom file for the project to import
   * @return created project
   */
  protected IProject importProject(String pomLocation) throws IOException, CoreException {
    return importProject(pomLocation, new ResolverConfiguration());
  }

  /**
   * Import a test project into the Eclipse workspace
   * 
   * @param pomLocation   - a relative location of the pom file for the project to import
   * @param configuration - a resolver configuration to be used to configure imported project
   * @return created project
   */
  protected IProject importProject(String pomLocation, ResolverConfiguration configuration)
      throws IOException, CoreException {
    return importProject(pomLocation, configuration, null);
  }

  /**
   * Import a test project into the Eclipse workspace
   * 
   * @param pomLocation   - a relative location of the pom file for the project to import
   * @param configuration - a resolver configuration to be used to configure imported project
   * @param listener      - listener which will get notified of the raw project creation
   * @return created project
   */
  protected IProject importProject(String pomLocation, ResolverConfiguration configuration,
      IProjectCreationListener listener) throws IOException, CoreException {
    File pomFile = new File(pomLocation);
    return importProjects(pomFile.getParentFile().getCanonicalPath(), new String[] {pomFile.getName()}, configuration,
        false, listener)[0];
  }

  /**
   * Import test projects into the Eclipse workspace
   * 
   * @param basedir       - a base directory for all projects to import
   * @param pomNames      - a relative locations of the pom files for the projects to import
   * @param configuration - a resolver configuration to be used to configure imported projects
   * @return created projects
   */
  protected IProject[] importProjects(String basedir, String[] pomNames, ResolverConfiguration configuration)
      throws IOException, CoreException {
    return importProjects(basedir, pomNames, configuration, false);
  }

  /**
   * Import test projects into the Eclipse workspace
   * 
   * @param basedir       - a base directory for all projects to import
   * @param pomNames      - a relative locations of the pom files for the projects to import
   * @param configuration - a resolver configuration to be used to configure imported projects
   * @return created projects
   */
  protected IProject[] importProjects(String basedir, String[] pomNames, ResolverConfiguration configuration,
      boolean skipSanityCheck) throws IOException, CoreException {
    return importProjects(basedir, pomNames, configuration, skipSanityCheck, null);
  }

  protected IProject[] importProjects(String basedir, String[] pomNames, ResolverConfiguration configuration,
      boolean skipSanityCheck, IProjectCreationListener listener) throws IOException, CoreException {
    MavenModelManager mavenModelManager = MavenPlugin.getMavenModelManager();
    IWorkspaceRoot root = workspace.getRoot();

    File src = new File(basedir);
    File dst = new File(root.getLocation().toFile(), src.getName());
    copyDir(src, dst);

    final ArrayList<MavenProjectInfo> projectInfos = new ArrayList<MavenProjectInfo>();
    for(String pomName : pomNames) {
      File pomFile = new File(dst, pomName);
      Model model = mavenModelManager.readMavenModel(pomFile);
      MavenProjectInfo projectInfo = new MavenProjectInfo(pomName, pomFile, model, null);
      setBasedirRename(projectInfo);
      projectInfos.add(projectInfo);
    }

    final ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(configuration);

    final ArrayList<IMavenProjectImportResult> importResults = new ArrayList<IMavenProjectImportResult>();

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        importResults.addAll(MavenPlugin.getProjectConfigurationManager().importProjects(projectInfos,
            importConfiguration, listener, monitor));
      }
    }, MavenPlugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    IProject[] projects = new IProject[projectInfos.size()];
    for(int i = 0; i < projectInfos.size(); i++ ) {
      IMavenProjectImportResult importResult = importResults.get(i);
      assertSame(projectInfos.get(i), importResult.getMavenProjectInfo());
      projects[i] = importResult.getProject();
      assertNotNull("Failed to import project " + projectInfos, projects[i]);

      /*
       * Sanity check: make sure they were all imported
       */
      if(!skipSanityCheck) {
        Model model = projectInfos.get(i).getModel();
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(projects[i], monitor);
        if(facade == null) {
          fail("Project " + model.getGroupId() + "-" + model.getArtifactId() + "-" + model.getVersion()
              + " was not imported. Errors: "
              + WorkspaceHelpers.toString(WorkspaceHelpers.findErrorMarkers(projects[i])));
        }
      }
    }

    return projects;
  }

  private void setBasedirRename(MavenProjectInfo projectInfo) throws IOException {
    File workspaceRoot = workspace.getRoot().getLocation().toFile();
    File basedir = projectInfo.getPomFile().getParentFile().getCanonicalFile();

    projectInfo.setBasedirRename(
        basedir.getParentFile().equals(workspaceRoot) ? MavenProjectInfo.RENAME_REQUIRED : MavenProjectInfo.RENAME_NO);
  }

  protected IProject importProject(String projectName, String projectLocation, ResolverConfiguration configuration)
      throws IOException, CoreException {
    ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(configuration);
    importConfiguration.setProjectNameTemplate(projectName);
    return importProject(projectName, projectLocation, importConfiguration);
  }

  protected IProject importProject(String projectName, String projectLocation,
      final ProjectImportConfiguration importConfiguration) throws IOException, CoreException {
    File dir = new File(workspace.getRoot().getLocation().toFile(), projectName);
    copyDir(new File(projectLocation), dir);

    File pomFile = new File(dir, IMavenConstants.POM_FILE_NAME);
    Model model = MavenPlugin.getMavenModelManager().readMavenModel(pomFile);
    final MavenProjectInfo projectInfo = new MavenProjectInfo(projectName, pomFile, model, null);
    setBasedirRename(projectInfo);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        MavenPlugin.getProjectConfigurationManager().importProjects(Collections.singleton(projectInfo),
            importConfiguration, monitor);
        IProject project = workspace.getRoot().getProject(importConfiguration.getProjectName(projectInfo.getModel()));
        assertNotNull("Failed to import project " + projectInfo, project);
      }
    }, MavenPlugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    return workspace.getRoot().getProject(projectName);
  }

  protected void waitForJobsToComplete() throws InterruptedException, CoreException {
    waitForJobsToComplete(monitor);
  }

  public static void waitForJobsToComplete(IProgressMonitor monitor) throws InterruptedException, CoreException {
    JobHelpers.waitForJobsToComplete(monitor);
  }

  protected IClasspathEntry[] getMavenContainerEntries(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    return container.getClasspathEntries();
  }

  protected static String toString(IMarker[] markers) {
    return WorkspaceHelpers.toString(markers);
  }

  protected static String toString(List<IMarker> markers) {
    return WorkspaceHelpers.toString(markers);
  }

  protected void copyContent(IProject project, String from, String to) throws Exception {
    copyContent(project, project.getFile(from).getContents(), to, true);
  }

  protected void copyContent(IProject project, String from, String to, boolean waitForJobsToComplete) throws Exception {
    copyContent(project, project.getFile(from).getContents(), to, waitForJobsToComplete);
  }

  protected void copyContent(IProject project, File from, String to) throws Exception {
    copyContent(project, new FileInputStream(from), to, true);
  }

  /**
   * closes contents stream
   */
  private void copyContent(IProject project, InputStream contents, String to, boolean waitForJobsToComplete)
      throws CoreException, IOException, InterruptedException {
    IFile file;
    try {
      file = project.getFile(to);
      if(!file.exists()) {
        file.create(contents, IResource.FORCE, monitor);
      } else {
        file.setContents(contents, IResource.FORCE, monitor);
      }
    } finally {
      contents.close();
    }
    if(waitForJobsToComplete) {
      // emulate behavior when autobuild was not honored by ProjectRegistryRefreshJob
      if(!isAutoBuilding() && file.getParent().getType() == IResource.PROJECT && file.getName().equals("pom.xml")) {
        refreshMavenProject(project);
      }
      waitForJobsToComplete();
    }
  }

  public static void copyDir(File src, File dst) throws IOException {
    FileHelpers.copyDir(src, dst);
  }

  public static void copyDir(File src, File dst, FileFilter filter) throws IOException {
    FileHelpers.copyDir(src, dst, filter);
  }

  protected static List<IMarker> findErrorMarkers(IProject project) throws CoreException {
    return WorkspaceHelpers.findErrorMarkers(project);
  }

  protected static List<IMarker> findMarkers(IProject project, int targetSeverity) throws CoreException {
    return WorkspaceHelpers.findMarkers(project, targetSeverity);
  }

  protected static void assertNoErrors(IProject project) throws CoreException {
    WorkspaceHelpers.assertNoErrors(project);
  }

  /**
   * Returns a set of projects that were affected by specified collection of events
   * 
   * @since 1.6.0
   */
  protected static Set<IProject> getProjectsFromEvents(Collection<MavenProjectChangedEvent> events) {
    Set<IProject> projects = new HashSet<IProject>();
    for(MavenProjectChangedEvent event : events) {
      projects.add(event.getSource().getProject());
    }
    return projects;
  }

  /**
   * Assert that provided list <b>only</b> contains specified expected items.
   * 
   * @since 1.6.0
   */
  @SafeVarargs
  protected static <T> void assertContainsOnly(Set<? extends T> actual, T... expected) {
    Set<T> expectedSet = new HashSet<T>();
    for(T item : expected) {
      expectedSet.add(item);
    }
    assertEquals(expectedSet, actual);
  }

  protected void injectFilexWagon() throws Exception {
    PlexusContainer container = ((MavenImpl) MavenPlugin.getMaven()).getPlexusContainer();
    if(container.getContainerRealm().getResource(FilexWagon.class.getName().replace('.', '/') + ".class") == null) {
      container.getContainerRealm().importFrom(FilexWagon.class.getClassLoader(), FilexWagon.class.getName());
      ComponentDescriptor<Wagon> descriptor = new ComponentDescriptor<Wagon>();
      descriptor.setRealm(container.getContainerRealm());
      descriptor.setRoleClass(Wagon.class);
      descriptor.setImplementationClass(FilexWagon.class);
      descriptor.setRoleHint("filex");
      descriptor.setInstantiationStrategy("per-lookup");
      container.addComponentDescriptor(descriptor);
    }
  }

  /**
   * Nullifies all transient IMavenProjectFacade fields, which should have roughly the same effect as writing it to
   * workspace state and reading it back after workspace restart.
   */
  protected void deserializeFromWorkspaceState(final IMavenProjectFacade projectFacade) throws IllegalAccessException {
    // pretend it was deserialized from workspace state
    for(Field field : projectFacade.getClass().getDeclaredFields()) {
      if(Modifier.isTransient(field.getModifiers())) {
        field.setAccessible(true);
        field.set(projectFacade, null);
      }
    }
    MavenPluginActivator.getDefault().getMavenProjectManagerImpl().putMavenProject((MavenProjectFacade) projectFacade,
        null);
  }

  @Override
  public void runTest() throws Throwable {
    if(!requiresMavenExecutionContext()) {
      super.runTest();
    } else {
      @SuppressWarnings("serial")
      class WrappedThrowable extends RuntimeException {
        public WrappedThrowable(Throwable ex) {
          super(ex);
        }
      }
      try {
        MavenPlugin.getMaven().execute(new AbstractRunnable() {
          @SuppressWarnings("synthetic-access")
          protected void run(IMavenExecutionContext context, IProgressMonitor monitor) {
            try {
              AbstractMavenProjectTestCase.super.runTest();
            } catch(Throwable ex) {
              throw new WrappedThrowable(ex);
            }
          }
        }, monitor);
      } catch(WrappedThrowable e) {
        throw e.getCause();
      }
    }
  }

  private boolean requiresMavenExecutionContext() {
    String fName = getName();
    if(fName != null) {
      try {
        Method runMethod = getClass().getMethod(fName, (Class[]) null);
        RequireMavenExecutionContext ann = runMethod.getAnnotation(RequireMavenExecutionContext.class);
        if(ann != null) {
          return ann.require();
        }
      } catch(SecurityException ex) {
        // fall through
      } catch(NoSuchMethodException ex) {
        // fall through
      }
    }

    Class<?> clazz = getClass();
    do {
      RequireMavenExecutionContext ann = clazz.getAnnotation(RequireMavenExecutionContext.class);
      if(ann != null) {
        return ann.require();
      }
      clazz = clazz.getSuperclass();
    } while(clazz != null);

    return false;
  }

}
