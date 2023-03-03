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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.embedder.ArtifactRepositoryRef;
import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.IMavenToolbox;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.IMavenPlexusContainer;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.PlexusContainerManager;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class MavenProjectFacade implements IMavenProjectFacade, Serializable {

  private static final long serialVersionUID = -3648172776786224087L;

  private static final String CTX_EXECUTION_PLANS = MavenProjectFacade.class.getName() + "/executionPlans";

  private static final String CTX_SETUP_EXECUTIONS = MavenProjectFacade.class.getName() + "/setupExecutions";

  public static final String PROP_LIFECYCLE_MAPPING = MavenProjectFacade.class.getName() + "/lifecycleMapping";

  public static final String PROP_CONFIGURATORS = MavenProjectFacade.class.getName() + "/configurators";

  private final ProjectRegistryManager manager;

  private final IFile pom;

  private final File pomFile;

  private final IProjectConfiguration resolverConfiguration;

  private final long[] timestamp;

  // cached values from mavenProject
  private final ArtifactKey artifactKey;

  private final List<String> modules;

  private final String packaging;

  private final List<IPath> resourceLocations;

  private final List<IPath> testResourceLocations;

  private final List<IPath> compileSourceLocations;

  private final List<IPath> testCompileSourceLocations;

  private IPath buildOutputLocation;

  private final IPath outputLocation;

  private final IPath testOutputLocation;

  private final String finalName;

  private final Set<ArtifactRepositoryRef> artifactRepositories;

  private final Set<ArtifactRepositoryRef> pluginArtifactRepositories;

  // TODO make final
  private Set<ArtifactRef> artifacts; // dependencies are resolved after facade instance is created

  // lifecycle mapping

  // TODO make final
  private String lifecycleMappingId;

  // TODO make final
  private Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping;

  private transient Map<String, Object> sessionProperties;

  public MavenProjectFacade(ProjectRegistryManager manager, IFile pom, MavenProject mavenProject,
      IProjectConfiguration resolverConfiguration) {
    this.manager = manager;
    this.pom = pom;
    this.pomFile = ProjectRegistryManager.toJavaIoFile(pom);
    //TODO we currently always use the computed directory here 
    // but https://github.com/eclipse-m2e/m2e-core/issues/904 will add support for a user to specify a custom root directory
    // and then we should really inherit this from the configuration!
    this.resolverConfiguration = new MavenProjectConfiguration(resolverConfiguration);

    this.artifactKey = new ArtifactKey(mavenProject.getArtifact());
    this.packaging = mavenProject.getPackaging();
    this.modules = List.copyOf(mavenProject.getModules());

    this.resourceLocations = MavenProjectUtils.getResourceLocations(getProject(), mavenProject.getResources());
    this.testResourceLocations = MavenProjectUtils.getResourceLocations(getProject(), mavenProject.getTestResources());
    this.compileSourceLocations = MavenProjectUtils.getSourceLocations(getProject(),
        mavenProject.getCompileSourceRoots());
    this.testCompileSourceLocations = MavenProjectUtils.getSourceLocations(getProject(),
        mavenProject.getTestCompileSourceRoots());

    IPath fullPath = getProject().getFullPath();

    IPath path = getProjectRelativePath(mavenProject.getBuild().getOutputDirectory());
    this.outputLocation = (path != null) ? fullPath.append(path) : null;
    path = getProjectRelativePath(mavenProject.getBuild().getDirectory());
    this.buildOutputLocation = (path != null) ? fullPath.append(path) : null;

    path = getProjectRelativePath(mavenProject.getBuild().getTestOutputDirectory());
    this.testOutputLocation = path != null ? fullPath.append(path) : null;

    this.finalName = mavenProject.getBuild().getFinalName();

    this.artifactRepositories = toRepositoryReferences(mavenProject.getRemoteArtifactRepositories());
    this.pluginArtifactRepositories = toRepositoryReferences(mavenProject.getPluginArtifactRepositories());

    timestamp = new long[ProjectRegistryManager.METADATA_PATH.size() + 1];
    IProject project = getProject();
    int i = 0;
    for(IPath metadataPath : ProjectRegistryManager.METADATA_PATH) {
      timestamp[i] = getModificationStamp(project.getFile(metadataPath));
      i++ ;
    }
    timestamp[timestamp.length - 1] = getModificationStamp(pom);
  }

  private Set<ArtifactRepositoryRef> toRepositoryReferences(List<ArtifactRepository> artifactRepositories) {
    return artifactRepositories.stream().map(r -> {
      String username = r.getAuthentication() != null ? r.getAuthentication().getUsername() : null;
      return new ArtifactRepositoryRef(r.getId(), r.getUrl(), username);
    }).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Copy constructor. Does NOT preserve session state.
   */
  public MavenProjectFacade(MavenProjectFacade other) {
    this.manager = other.manager;
    this.pom = other.pom;
    this.pomFile = other.pomFile;
    this.resolverConfiguration = other.resolverConfiguration;

    this.artifactKey = other.artifactKey;
    this.packaging = other.packaging;
    this.modules = other.modules;

    this.resourceLocations = List.copyOf(other.resourceLocations);
    this.testResourceLocations = List.copyOf(other.testResourceLocations);
    this.compileSourceLocations = List.copyOf(other.compileSourceLocations);
    this.testCompileSourceLocations = List.copyOf(other.testCompileSourceLocations);

    this.outputLocation = other.outputLocation;
    this.buildOutputLocation = other.buildOutputLocation;
    this.testOutputLocation = other.testOutputLocation;
    this.finalName = other.finalName;

    this.artifactRepositories = new LinkedHashSet<>(other.artifactRepositories);

    this.pluginArtifactRepositories = new LinkedHashSet<>(other.pluginArtifactRepositories);

    this.timestamp = Arrays.copyOf(other.timestamp, other.timestamp.length);
  }

  /**
   * Returns project relative paths of resource directories
   */
  @Override
  public List<IPath> getResourceLocations() {
    return resourceLocations;
  }

  /**
   * Returns project relative paths of test resource directories
   */
  @Override
  public List<IPath> getTestResourceLocations() {
    return testResourceLocations;
  }

  @Override
  public List<IPath> getCompileSourceLocations() {
    return compileSourceLocations;
  }

  @Override
  public List<IPath> getTestCompileSourceLocations() {
    return testCompileSourceLocations;
  }

  /**
   * Returns project resource for given file system location or null the location is outside of project.
   *
   * @param resourceLocation absolute file system location
   * @return IPath the full, absolute workspace path resourceLocation
   */
  @Override
  public IPath getProjectRelativePath(String resourceLocation) {
    return MavenProjectUtils.getProjectRelativePath(getProject(), resourceLocation);
  }

  @Override
  public IPath getBuildOutputLocation() {
    return buildOutputLocation;
  }

  /**
   * Returns the full, absolute path of this project maven build output directory relative to the workspace or null if
   * maven build output directory cannot be determined or outside of the workspace.
   */
  @Override
  public IPath getOutputLocation() {
    return outputLocation;
  }

  /**
   * Returns the full, absolute path of this project maven build test output directory relative to the workspace or null
   * if maven build output directory cannot be determined or outside of the workspace.
   */
  @Override
  public IPath getTestOutputLocation() {
    return testOutputLocation;
  }

  @Override
  public String getFinalName() {
    return finalName;
  }

  @Override
  public IPath getFullPath() {
    return getProject().getFullPath();
  }

  /**
   * Lazy load and cache MavenProject instance
   */
  @Override
  public MavenProject getMavenProject(IProgressMonitor monitor) throws CoreException {
    return manager.getMavenProject(this, monitor);
  }

  @Override
  public MavenProject getMavenProject() {
    return manager.getMavenProject(this);
  }

  @Override
  public String getPackaging() {
    return packaging;
  }

  @Override
  public IProject getProject() {
    return pom.getProject();
  }

  @Override
  public IFile getPom() {
    return pom;
  }

  @Override
  public File getPomFile() {
    return pomFile;
  }

  public File getBaseDir() {
    if(pomFile != null) {
      return pomFile.isDirectory() ? pomFile : pomFile.getParentFile();
    }
    return null;
  }

  /**
   * Returns the full, absolute path of the given file relative to the workspace. Returns null if the file does not
   * exist or is not a member of this project.
   */
  @Override
  public IPath getFullPath(File file) {
    return MavenProjectUtils.getFullPath(getProject(), file);
  }

  @Override
  public List<String> getMavenProjectModules() {
    return modules;
  }

  @Override
  public Set<ArtifactRef> getMavenProjectArtifacts() {
    return artifacts;
  }

  void setMavenProjectArtifacts(MavenProject mavenProject) {
    Set<ArtifactRef> collect = mavenProject.getArtifacts().stream()
        .map(a -> new ArtifactRef(new ArtifactKey(a), a.getScope()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    this.artifacts = Collections.unmodifiableSet(collect);
  }

  @Override
  public ResolverConfiguration getResolverConfiguration() {
    return new ResolverConfiguration(resolverConfiguration);
  }

  @Override
  public IProjectConfiguration getConfiguration() {
    return resolverConfiguration;
  }

  /**
   * @return true if maven project needs to be re-read from disk
   */
  @Override
  public boolean isStale() {
    IProject project = getProject();
    int i = 0;
    for(IPath path : ProjectRegistryManager.METADATA_PATH) {
      if(timestamp[i++ ] != getModificationStamp(project.getFile(path))) {
        return true;
      }
    }
    return false;
  }

  private static long getModificationStamp(IFile file) {
    /*
     * this implementation misses update in the following scenario
     *
     * 1. two files, A and B, with different content were created with same localTimeStamp
     * 2. original A was deleted and B moved to A
     *
     * See also https://bugs.eclipse.org/bugs/show_bug.cgi?id=160728
     */
    return file.getLocalTimeStamp() + file.getModificationStamp();
  }

  @Override
  public ArtifactKey getArtifactKey() {
    return artifactKey;
  }

  @Override
  public synchronized void setSessionProperty(String key, Object value) {
    if(sessionProperties == null) {
      sessionProperties = new HashMap<>();
    }
    if(value != null) {
      sessionProperties.put(key, value);
    } else {
      sessionProperties.remove(key);
    }
  }

  @Override
  public synchronized Object getSessionProperty(String key) {
    return sessionProperties != null ? sessionProperties.get(key) : null;
  }

  @Override
  public Set<ArtifactRepositoryRef> getArtifactRepositoryRefs() {
    return artifactRepositories;
  }

  @Override
  public Set<ArtifactRepositoryRef> getPluginArtifactRepositoryRefs() {
    return pluginArtifactRepositories;
  }

  @Override
  public String toString() {
    return getProject() + ": " + getArtifactKey();
  }

  @Override
  public String getLifecycleMappingId() {
    return lifecycleMappingId;
  }

  public void setLifecycleMappingId(String lifecycleMappingId) {
    this.lifecycleMappingId = lifecycleMappingId;
  }

  @Override
  public Map<MojoExecutionKey, List<IPluginExecutionMetadata>> getMojoExecutionMapping() {
    return mojoExecutionMapping;
  }

  public void setMojoExecutionMapping(Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping) {
    this.mojoExecutionMapping = mojoExecutionMapping;
  }

  // mojo executions

  /**
   * Maps LIFECYCLE_* to corresponding mojo executions. The mojo executions are not fully setup and cannot be executed
   * directly.
   */
  private Map<String, List<MojoExecution>> getExecutionPlans(IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = getMavenProject(monitor);
    Map<String, List<MojoExecution>> executionPlans = getContextValue(mavenProject, CTX_EXECUTION_PLANS);
    if(executionPlans == null) {
      executionPlans = calculateExecutionPlans(mavenProject, monitor);
      mavenProject.setContextValue(CTX_EXECUTION_PLANS, executionPlans);
    }
    return executionPlans;
  }

  private Map<String, List<MojoExecution>> calculateExecutionPlans(MavenProject mavenProject,
      IProgressMonitor monitor) {
    //TODO is there a way to lookup them instead of hardocde them here?
    Map<String, List<MojoExecution>> executionPlans = new LinkedHashMap<>();
    executionPlans.put(ProjectRegistryManager.LIFECYCLE_CLEAN,
        calculateExecutionPlan(mavenProject, List.of(ProjectRegistryManager.LIFECYCLE_CLEAN), false, monitor)
            .getMojoExecutions());
    executionPlans.put(ProjectRegistryManager.LIFECYCLE_DEFAULT,
        calculateExecutionPlan(mavenProject, List.of(ProjectRegistryManager.LIFECYCLE_DEFAULT), false, monitor)
            .getMojoExecutions());
    executionPlans.put(ProjectRegistryManager.LIFECYCLE_SITE,
        calculateExecutionPlan(mavenProject, List.of(ProjectRegistryManager.LIFECYCLE_SITE), false, monitor)
            .getMojoExecutions());
    return executionPlans;
  }

  @Override
  public MavenExecutionPlan calculateExecutionPlan(Collection<String> tasks, IProgressMonitor monitor) {
    return calculateExecutionPlan(getMavenProject(), tasks, false, monitor);
  }

  @Override
  public MavenExecutionPlan setupExecutionPlan(Collection<String> tasks, IProgressMonitor monitor) {
    return calculateExecutionPlan(getMavenProject(), tasks, true, monitor);
  }

  private MavenExecutionPlan calculateExecutionPlan(MavenProject mavenProject, Collection<String> tasks, boolean setup,
      IProgressMonitor monitor) {
    try {
      return createExecutionContext().execute(mavenProject, (ctx, mon) -> {
        IMavenToolbox toolbox = IMavenToolbox.of(ctx);
        return toolbox.calculateExecutionPlan(tasks, setup);
      }, monitor);
    } catch(CoreException e) {
      manager.getMarkerManager().addErrorMarkers(pom, IMavenConstants.MARKER_POM_LOADING_ID, e);
    }
    return new MavenExecutionPlan(List.of(), new DefaultLifecycles());
  }

  @SuppressWarnings("unchecked")
  private static <T> T getContextValue(MavenProject mavenProject, String key) {
    // XXX this is not thread safe, but needs to be fixed in maven, I can't fix this properly from m2e end
    return (T) mavenProject.getContextValue(key);
  }

  /**
   * Returns cache of fully setup MojoExecution instances. Lifespan of the cache is linked to the lifespan of
   * MavenProject instance. The cache is initially empty and it is up to the callers to populate and maintain it.
   */
  private Map<MojoExecutionKey, MojoExecution> getSetupMojoExecutions(IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = getMavenProject(monitor);
    Map<MojoExecutionKey, MojoExecution> executionPlans = getContextValue(mavenProject, CTX_SETUP_EXECUTIONS);
    if(executionPlans == null) {
      executionPlans = new LinkedHashMap<>();
      mavenProject.setContextValue(CTX_SETUP_EXECUTIONS, executionPlans);
    }
    return executionPlans;
  }

  @Override
  public MojoExecution getMojoExecution(MojoExecutionKey mojoExecutionKey, IProgressMonitor monitor)
      throws CoreException {
    Map<MojoExecutionKey, MojoExecution> setupMojoExecutions = getSetupMojoExecutions(monitor);
    MojoExecution execution = setupMojoExecutions.get(mojoExecutionKey);
    if(execution == null) {
      for(MojoExecution mojoExecution : getMojoExecutions(monitor)) {
        if(match(mojoExecutionKey, mojoExecution)) {
          execution = setupMojoExecution(mojoExecution, monitor);
          setupMojoExecutions.put(mojoExecutionKey, execution);
          break;
        }
      }
    }
    return execution;
  }

  private MojoExecution setupMojoExecution(MojoExecution mojoExecution, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = getMavenProject(monitor);
    MojoExecution clone = new MojoExecution(mojoExecution.getPlugin(), mojoExecution.getGoal(),
        mojoExecution.getExecutionId());
    clone.setMojoDescriptor(mojoExecution.getMojoDescriptor());
    if(mojoExecution.getConfiguration() != null) {
      clone.setConfiguration(new Xpp3Dom(mojoExecution.getConfiguration()));
    }
    clone.setLifecyclePhase(mojoExecution.getLifecyclePhase());
    createExecutionContext().execute(mavenProject, (ctx, mon) -> {
      LifecycleExecutionPlanCalculator executionPlanCalculator = ctx.getComponentLookup()
          .lookup(LifecycleExecutionPlanCalculator.class);
      try {
        executionPlanCalculator.setupMojoExecution(ctx.getSession(), mavenProject, clone);
      } catch(Exception ex) {
        throw new CoreException(Status.error(NLS.bind(Messages.MavenImpl_error_calc_build_plan, ex.getMessage()), ex));
      }
      return null;
    }, monitor);
    return clone;
  }

  private boolean match(MojoExecutionKey key, MojoExecution mojoExecution) {
    if(mojoExecution == null) {
      return false;
    }
    return key.groupId().equals(mojoExecution.getGroupId()) && key.artifactId().equals(mojoExecution.getArtifactId())
        && key.version().equals(mojoExecution.getVersion()) && key.goal().equals(mojoExecution.getGoal())
        && key.executionId().equals(mojoExecution.getExecutionId());
  }

  @Override
  public List<MojoExecution> getMojoExecutions(String groupId, String artifactId, IProgressMonitor monitor,
      String... goals) throws CoreException {
    List<MojoExecution> result = new ArrayList<>();
    Set<String> consideredGoals = Set.of(goals);
    Map<MojoExecutionKey, MojoExecution> setupMojoExecutions = getSetupMojoExecutions(monitor);
    for(MojoExecution mojoExecution : getMojoExecutions(monitor)) {
      if(groupId.equals(mojoExecution.getGroupId()) && artifactId.equals(mojoExecution.getArtifactId())
          && consideredGoals.contains(mojoExecution.getGoal())) {
        MojoExecutionKey key = new MojoExecutionKey(mojoExecution);
        MojoExecution execution = setupMojoExecutions.get(key);
        if(execution == null) {
          execution = setupMojoExecution(mojoExecution, monitor);
          setupMojoExecutions.put(key, execution);
        }
        result.add(execution);
      }
    }
    return result;
  }

  /**
   * Returns cached list of MojoExecutions bound to project's clean, default and site lifecycles. Returned
   * MojoExecutions are not fully setup and {@link IMaven#setupMojoExecution(MavenSession, MavenProject, MojoExecution)}
   * is required to execute and/or query mojo parameters. Similarly to {@link #getMavenProject()}, return value is null
   * after workspace restart.
   */
  public List<MojoExecution> getMojoExecutions() {
    try {
      return getMojoExecutions(null);
    } catch(CoreException ex) {
      return null;
    }
  }

  /**
   * Returns list of MojoExecutions bound to project's clean, default and site lifecycles. Returned MojoExecutions are
   * not fully setup and {@link IMaven#setupMojoExecution(MavenSession, MavenProject, MojoExecution)} is required to
   * execute and/or query mojo parameters.
   */
  public List<MojoExecution> getMojoExecutions(IProgressMonitor monitor) throws CoreException {
    Map<String, List<MojoExecution>> executionPlans = getExecutionPlans(monitor);
    // contains null values if execution plan could not be calculated
    return executionPlans.values().stream().filter(Objects::nonNull).flatMap(List::stream).toList();
  }

  public List<MojoExecution> getExecutionPlan(String lifecycle, IProgressMonitor monitor) throws CoreException {
    return getExecutionPlans(monitor).get(lifecycle);
  }

  @Override
  public IMavenExecutionContext createExecutionContext() {
    File multiModuleProjectDirectory = getConfiguration().getMultiModuleProjectDirectory();
    PlexusContainerManager containerManager = manager.getContainerManager();
    IMavenPlexusContainer container;
    try {
      container = containerManager.aquire(multiModuleProjectDirectory);
    } catch(Exception ex) {
      throw new RuntimeException("Acquire container failed!", ex);
    }
    MavenProject mavenProject = tryGetMavenProject();
    if(mavenProject == null) {
      return new MavenExecutionContext(container.getComponentLookup(), getBaseDir(), multiModuleProjectDirectory, null);
    }
    return new MavenExecutionContext(
        PlexusContainerManager.wrap(container.getContainer(), mavenProject.getClassRealm()), getBaseDir(),
        multiModuleProjectDirectory, ctx -> mavenProject);
  }

  private MavenProject tryGetMavenProject() {
    try {
      return manager.getMavenProject(this, null);
    } catch(CoreException ex) {
      //TODO can we handle this more graceful? E.g. one error condition is that project dependencies can not be resolved, maybe we can try to load the project without dependencies then?
      manager.getMarkerManager().addErrorMarkers(pom, IMavenConstants.MARKER_POM_LOADING_ID, ex);
      return null;
    }
  }

  @Override
  public IComponentLookup getComponentLookup() {
    return manager.getContainerManager().getComponentLookup(getConfiguration().getMultiModuleProjectDirectory());
  }

  private static final class MavenProjectConfiguration implements IProjectConfiguration, Serializable {

    private final File multiModuleProjectDirectory;

    private final String mappingId;

    private final Map<String, String> properties;

    private final boolean resolveWorkspace;

    private final String profiles;

    private final Map<String, String> userProperties;

    private List<String> activeProfiles;

    private List<String> inactiveProfiles;

    private MavenProjectConfiguration(IProjectConfiguration baseConfiguration) {
      if(baseConfiguration == null) {
        //we should really forbid this but some test seem to pass null!
        baseConfiguration = new ResolverConfiguration();
      }
      this.multiModuleProjectDirectory = baseConfiguration.getMultiModuleProjectDirectory();
      this.mappingId = baseConfiguration.getLifecycleMappingId();
      this.properties = Map.copyOf(baseConfiguration.getConfigurationProperties());
      this.userProperties = Map.copyOf(baseConfiguration.getUserProperties());
      this.resolveWorkspace = baseConfiguration.isResolveWorkspaceProjects();
      this.profiles = baseConfiguration.getSelectedProfiles();
      this.activeProfiles = List.copyOf(baseConfiguration.getActiveProfileList());
      this.inactiveProfiles = List.copyOf(baseConfiguration.getInactiveProfileList());
    }

    @Override
    public Map<String, String> getConfigurationProperties() {
      return properties;
    }

    @Override
    public Map<String, String> getUserProperties() {
      return userProperties;
    }

    @Override
    public boolean isResolveWorkspaceProjects() {
      return resolveWorkspace;
    }

    @Override
    public String getSelectedProfiles() {
      return profiles;
    }

    @Override
    public String getLifecycleMappingId() {
      return mappingId;
    }

    @Override
    public File getMultiModuleProjectDirectory() {
      return multiModuleProjectDirectory;
    }

    @Override
    public int hashCode() {
      return IProjectConfiguration.contentsHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }
      if(obj instanceof IProjectConfiguration other) {
        return IProjectConfiguration.contentsEquals(this, other);
      }
      return false;
    }

    @Override
    public List<String> getActiveProfileList() {
      return activeProfiles;
    }

    @Override
    public List<String> getInactiveProfileList() {
      return inactiveProfiles;
    }

  }


}
