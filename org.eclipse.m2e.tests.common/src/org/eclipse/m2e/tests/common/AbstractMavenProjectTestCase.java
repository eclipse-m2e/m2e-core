/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
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

package org.eclipse.m2e.tests.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.composition.CycleDetectedInComponentGraphException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.wagon.Wagon;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.IArchetype;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IProjectCreationListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.jdt.MavenJdtPlugin;
import org.eclipse.m2e.jdt.internal.BuildPathManager;


@SuppressWarnings("restriction")
public abstract class AbstractMavenProjectTestCase {

  private static final boolean STOP_ON_FAILED_TEST = Boolean.getBoolean("m2e.stopOnFailedTest");

  public static final int DELETE_RETRY_COUNT = 10;

  public static final long DELETE_RETRY_DELAY = 6000L;

  protected static final IProgressMonitor monitor = new IProgressMonitor() {

    AtomicBoolean canceled = new AtomicBoolean();

    public void worked(int work) {
      driveEvents();
    }

    public void subTask(String name) {
      driveEvents();

    }

    public void setTaskName(String name) {
      driveEvents();

    }

    public void setCanceled(boolean value) {
      canceled.set(value);
    }

    public boolean isCanceled() {
      return canceled.get();
    }

    public void internalWorked(double work) {
      driveEvents();

    }

    public void done() {
      driveEvents();

    }

    public void beginTask(String name, int totalWork) {
      driveEvents();
    }
  };

  protected IWorkspace workspace;

  @Rule
  public TestName name = new TestName();

  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcher() {
    @Override
    protected void failed(Throwable e, Description description) {
      if(STOP_ON_FAILED_TEST) {
        AtomicBoolean wait = new AtomicBoolean(true);
        Shell shell = new Shell(Display.getCurrent());
        shell.setLayout(new FillLayout());
        Button button = new Button(shell, SWT.PUSH);
        button.setText("Shutdown test");

        SelectionListener.widgetSelectedAdapter(ev -> wait.set(false));
        shell.setVisible(true);
        shell.pack();
        while(wait.get() && !shell.isDisposed()) {
          driveEvents();
          Display.getCurrent().sleep();
        }
      }
    }

    @Override
    protected void succeeded(Description description) {
    }
  };

  protected File repo;

  protected ProjectRegistryRefreshJob projectRefreshJob;

  protected Job downloadSourcesJob;

  protected IMavenConfiguration mavenConfiguration;

  private String oldUserSettingsFile;

  @Before
  public void setUp() throws Exception {
    monitor.setCanceled(false);
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

    // Create .mvn folder to prevent Maven launches in tests from using the .mvn folder of this git repo.
    // Otherwise MavenLaunchDelegate.findMavenProjectBasedir() find this git repos .mvn folder and its content would interfer with the tests.
    Files.createDirectories(Path.of(workspace.getRoot().getLocationURI()).resolve(".mvn"));

    FilexWagon.reset();
    HttxWagon.reset();
    driveEvents();
  }

  protected void useSettings(String settings) throws IOException, CoreException {
    String userSettingsFile = new File(settings).getCanonicalFile().getAbsolutePath();
    mavenConfiguration.setUserSettingsFile(userSettingsFile);
  }

  @After
  public void tearDown() throws Exception {
    waitForJobsToComplete();
    driveEvents();
    if(!STOP_ON_FAILED_TEST) {
      WorkspaceHelpers.cleanWorkspace();
    }

    // Restore the user settings file location
    mavenConfiguration.setUserSettingsFile(oldUserSettingsFile);

    projectRefreshJob.wakeUp();
    setAutoBuilding(false);
    setAutomaticallyUpdateConfiguration(false);
    FilexWagon.reset();
    HttxWagon.reset();
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
      } catch(InterruptedException | OperationCanceledException e) {
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
    throw new CoreException(Status.error("Could not delete project", cause));
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
    try (InputStream is = new FileInputStream(pomResource)) {
      return createProject(projectName, is);
    } catch(IOException ex1) {
      throw new CoreException(Status.error(ex1.toString(), ex1));
    }
  }

  protected IProject createProject(String projectName, InputStream pomContent) throws CoreException {
    final IProject project = workspace.getRoot().getProject(projectName);

    workspace.run(m -> {
      project.create(m);

      if(!project.isOpen()) {
        project.open(m);
      }

      IFile pomFile = project.getFile("pom.xml");
      if(!pomFile.exists()) {
        try {
          pomFile.create(pomContent, true, m);
        } catch(CoreException ex) {
          throw new CoreException(Status.error(ex.toString(), ex));
        }
      }
    }, null);

    return project;
  }

  protected IProject createExisting(String projectName, String projectLocation) throws IOException, CoreException {
    return createExisting(projectName, projectLocation, false);
  }

  protected IProject createExisting(String projectName, String projectLocation, boolean addNature)
      throws IOException, CoreException {
    File dir = new File(workspace.getRoot().getLocation().toFile(), projectName);
    if(dir.isFile()) {
      dir = dir.getParentFile();
    }
    copyDir(new File(projectLocation), dir);

    final IProject project = workspace.getRoot().getProject(projectName);

    workspace.run(m -> {
      if(!project.exists()) {
        IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());
        if(addNature) {
          projectDescription.setNatureIds(new String[] {IMavenConstants.NATURE_ID});
        }
        projectDescription.setLocation(null);
        project.create(projectDescription, m);
        project.open(IResource.NONE, m);
      } else {
        project.refreshLocal(IResource.DEPTH_INFINITE, m);
      }
      ensureDefaultCharset(project, m);
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
   * @param pomLocation - a relative location of the pom file for the project to import
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
   * @param pomLocation - a relative location of the pom file for the project to import
   * @param configuration - a resolver configuration to be used to configure imported project
   * @param listener - listener which will get notified of the raw project creation
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
   * @param basedir - a base directory for all projects to import
   * @param pomNames - a relative locations of the pom files for the projects to import
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
   * @param basedir - a base directory for all projects to import
   * @param pomNames - a relative locations of the pom files for the projects to import
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
    File src = new File(basedir);
    File dst = new File(workspace.getRoot().getLocation().toFile(), src.getName());
    copyDir(src, dst);

    final List<MavenProjectInfo> projectInfos = new ArrayList<>();
    for(String pomName : pomNames) {
      File pomFile = new File(dst, pomName);
      Model model = mavenModelManager.readMavenModel(pomFile);
      MavenProjectInfo projectInfo = new MavenProjectInfo(pomName, pomFile, model, null);
      setBasedirRename(projectInfo);
      projectInfos.add(projectInfo);
    }

    ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(configuration);
    List<IMavenProjectImportResult> importResults = new ArrayList<>();
    IProjectConfigurationManager configManager = MavenPlugin.getProjectConfigurationManager();
    workspace.run(m -> {
      importResults.addAll(configManager.importProjects(projectInfos, importConfiguration, listener, m));
    }, configManager.getRule(), IWorkspace.AVOID_UPDATE, monitor);

    IProject[] projects = new IProject[projectInfos.size()];
    for(int i = 0; i < projectInfos.size(); i++ ) {
      IMavenProjectImportResult importResult = importResults.get(i);
      assertSame(projectInfos.get(i), importResult.getMavenProjectInfo());
      projects[i] = importResult.getProject();
      ensureDefaultCharset(projects[i], monitor);
      assertNotNull("Failed to import project " + projectInfos, projects[i]);

      /*
       * Sanity check: make sure they were all imported
       */
      if(!skipSanityCheck) {
        Model model = projectInfos.get(i).getModel();
        IMavenProjectRegistry mavenProjectRegistry = MavenPlugin.getMavenProjectRegistry();
        IMavenProjectFacade facade = mavenProjectRegistry.create(projects[i], monitor);
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

  private static void ensureDefaultCharset(IProject project, IProgressMonitor monitor) throws CoreException {
    if(project.getDefaultCharset(false) == null) {
      project.setDefaultCharset("UTF-8", monitor);
    }
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

    workspace.run(m -> {
      MavenPlugin.getProjectConfigurationManager().importProjects(Collections.singleton(projectInfo),
          importConfiguration, m);
      IProject project = workspace.getRoot()
          .getProject(ProjectConfigurationManager.getProjectName(importConfiguration, projectInfo.getModel()));
      assertNotNull("Failed to import project " + projectInfo, project);
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

  protected static <T> T withDefaultLifecycleMapping(PluginExecutionAction defaultAction, Callable<T> action)
      throws Exception {
    IMavenConfiguration configuration = MavenPlugin.getMavenConfiguration();
    PluginExecutionAction oldvalue = configuration.getDefaultMojoExecutionAction();
    try {
      configuration.setDefaultMojoExecutionAction(defaultAction);
      return action.call();
    } finally {
      configuration.setDefaultMojoExecutionAction(oldvalue);
    }
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
      if(!isAutoBuilding() && file.getParent().getType() == IResource.PROJECT && "pom.xml".equals(file.getName())) {
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

  protected static void assertMavenNature(IProject project) throws CoreException {
    assertTrue("project " + project.getName() + " has no maven nature", project.hasNature(IMavenConstants.NATURE_ID));
  }

  /**
   * Returns a set of projects that were affected by specified collection of events
   *
   * @since 1.6.0
   */
  protected static Set<IProject> getProjectsFromEvents(Collection<MavenProjectChangedEvent> events) {
    Set<IProject> projects = new HashSet<>();
    for(MavenProjectChangedEvent event : events) {
      projects.add(event.getSource().getProject());
    }
    return projects;
  }

  protected void injectRedirectingWagons() throws Exception {
    injectWagon(FilexWagon.class, FilexWagon.PROTOCOL);
    injectWagon(HttxWagon.class, HttxWagon.PROTOCOL);
  }

  private void injectWagon(Class<? extends Wagon> wagonClass, String wagonRoleHint)
      throws CoreException, CycleDetectedInComponentGraphException {
    PlexusContainer container = MavenPlugin.getMaven().lookup(PlexusContainer.class);
    if(container.getContainerRealm().getResource(wagonClass.getName().replace('.', '/') + ".class") == null) {
      container.getContainerRealm().importFrom(wagonClass.getClassLoader(), wagonClass.getName());
      ComponentDescriptor<Wagon> descriptor = new ComponentDescriptor<>();
      descriptor.setRealm(container.getContainerRealm());
      descriptor.setRoleClass(Wagon.class);
      descriptor.setImplementationClass(wagonClass);
      descriptor.setRoleHint(wagonRoleHint);
      descriptor.setInstantiationStrategy("per-lookup");
      container.addComponentDescriptor(descriptor);
    }
  }

  protected Collection<IProject> createProjectsFromArchetype(final String projectName, final IArchetype archetype,
      final IPath location) throws CoreException {
    return createProjectsFromArchetype(projectName, archetype, Map.of(), location);
  }

  protected Collection<IProject> createProjectsFromArchetype(final String projectName, final IArchetype archetype,
      Map<String, String> properties, final IPath location) throws CoreException {
    List<IProject> eclipseProjects = new ArrayList<>();
    workspace.run((IWorkspaceRunnable) m -> {
      Collection<MavenProjectInfo> projects = M2EUIPluginActivator.getDefault().getArchetypePlugin().getGenerator()
          .createArchetypeProjects(location, archetype, //
              projectName, projectName, "0.0.1-SNAPSHOT", "test", //
              properties, monitor);
      MavenPlugin.getProjectConfigurationManager()
          .importProjects(projects, new ProjectImportConfiguration(), null, monitor).stream()
          .filter(r -> r.getProject() != null && r.getProject().exists()).map(r -> r.getProject())
          .forEach(eclipseProjects::add);

    }, MavenPlugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);
    try {
      waitForJobsToComplete();
    } catch(InterruptedException e) {
      fail("Interrupted");
    }
    return eclipseProjects;
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

  public static Set<IFile> getPomFiles(IProject... projects) {
    return Stream.of(projects).map(p -> p.getFile(IMavenConstants.POM_FILE_NAME)).collect(Collectors.toSet());
  }

  public static void driveEvents() {
    Display current = Display.getCurrent();
    if(current != null) {
      while(current.readAndDispatch()) {
        //process all events...
      }
    }
  }

}
