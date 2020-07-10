/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.embedder.ArtifactRepositoryRef;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
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

  private final ResolverConfiguration resolverConfiguration;

  private final long[] timestamp;

  // cached values from mavenProject
  private final ArtifactKey artifactKey;

  private final List<String> modules;

  private final String packaging;

  private final IPath[] resourceLocations;

  private final IPath[] testResourceLocations;

  private final IPath[] compileSourceLocations;

  private final IPath[] testCompileSourceLocations;

  private final IPath outputLocation;

  private final IPath testOutputLocation;

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
      ResolverConfiguration resolverConfiguration) {
    this.manager = manager;
    this.pom = pom;
    this.pomFile = ProjectRegistryManager.toJavaIoFile(pom);
    this.resolverConfiguration = resolverConfiguration;

    this.artifactKey = new ArtifactKey(mavenProject.getArtifact());
    this.packaging = mavenProject.getPackaging();
    this.modules = mavenProject.getModules();

    this.resourceLocations = MavenProjectUtils.getResourceLocations(getProject(), mavenProject.getResources());
    this.testResourceLocations = MavenProjectUtils.getResourceLocations(getProject(), mavenProject.getTestResources());
    this.compileSourceLocations = MavenProjectUtils.getSourceLocations(getProject(),
        mavenProject.getCompileSourceRoots());
    this.testCompileSourceLocations = MavenProjectUtils.getSourceLocations(getProject(),
        mavenProject.getTestCompileSourceRoots());

    IPath fullPath = getProject().getFullPath();

    IPath path = getProjectRelativePath(mavenProject.getBuild().getOutputDirectory());
    this.outputLocation = (path != null) ? fullPath.append(path) : null;

    path = getProjectRelativePath(mavenProject.getBuild().getTestOutputDirectory());
    this.testOutputLocation = path != null ? fullPath.append(path) : null;

    this.artifactRepositories = new LinkedHashSet<ArtifactRepositoryRef>();
    for(ArtifactRepository repository : mavenProject.getRemoteArtifactRepositories()) {
      this.artifactRepositories.add(new ArtifactRepositoryRef(repository));
    }

    this.pluginArtifactRepositories = new LinkedHashSet<ArtifactRepositoryRef>();
    for(ArtifactRepository repository : mavenProject.getPluginArtifactRepositories()) {
      this.pluginArtifactRepositories.add(new ArtifactRepositoryRef(repository));
    }

    timestamp = new long[ProjectRegistryManager.METADATA_PATH.size() + 1];
    IProject project = getProject();
    int i = 0;
    for(IPath metadataPath : ProjectRegistryManager.METADATA_PATH) {
      timestamp[i] = getModificationStamp(project.getFile(metadataPath));
      i++ ;
    }
    timestamp[timestamp.length - 1] = getModificationStamp(pom);
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
    this.modules = new ArrayList<String>(other.modules);

    this.resourceLocations = arrayCopy(other.resourceLocations);
    this.testResourceLocations = arrayCopy(other.testResourceLocations);
    this.compileSourceLocations = arrayCopy(other.compileSourceLocations);
    this.testCompileSourceLocations = arrayCopy(other.testCompileSourceLocations);

    this.outputLocation = other.outputLocation;

    this.testOutputLocation = other.testOutputLocation;

    this.artifactRepositories = new LinkedHashSet<ArtifactRepositoryRef>(other.artifactRepositories);

    this.pluginArtifactRepositories = new LinkedHashSet<ArtifactRepositoryRef>(other.pluginArtifactRepositories);

    this.timestamp = Arrays.copyOf(other.timestamp, other.timestamp.length);
  }

  private static <T> T[] arrayCopy(T[] a) {
    return Arrays.copyOf(a, a.length);
  }

  /**
   * Returns project relative paths of resource directories
   */
  public IPath[] getResourceLocations() {
    return resourceLocations;
  }

  /**
   * Returns project relative paths of test resource directories
   */
  public IPath[] getTestResourceLocations() {
    return testResourceLocations;
  }

  public IPath[] getCompileSourceLocations() {
    return compileSourceLocations;
  }

  public IPath[] getTestCompileSourceLocations() {
    return testCompileSourceLocations;
  }

  /**
   * Returns project resource for given file system location or null the location is outside of project.
   * 
   * @param resourceLocation absolute file system location
   * @return IPath the full, absolute workspace path resourceLocation
   */
  public IPath getProjectRelativePath(String resourceLocation) {
    return MavenProjectUtils.getProjectRelativePath(getProject(), resourceLocation);
  }

  /**
   * Returns the full, absolute path of this project maven build output directory relative to the workspace or null if
   * maven build output directory cannot be determined or outside of the workspace.
   */
  public IPath getOutputLocation() {
    return outputLocation;
  }

  /**
   * Returns the full, absolute path of this project maven build test output directory relative to the workspace or null
   * if maven build output directory cannot be determined or outside of the workspace.
   */
  public IPath getTestOutputLocation() {
    return testOutputLocation;
  }

  public IPath getFullPath() {
    return getProject().getFullPath();
  }

  /**
   * Lazy load and cache MavenProject instance
   */
  public MavenProject getMavenProject(IProgressMonitor monitor) throws CoreException {
    return manager.getMavenProject(this, monitor);
  }

  public MavenProject getMavenProject() {
    return manager.getMavenProject(this);
  }

  public String getPackaging() {
    return packaging;
  }

  public IProject getProject() {
    return pom.getProject();
  }

  public IFile getPom() {
    return pom;
  }

  public File getPomFile() {
    return pomFile;
  }

  /**
   * Returns the full, absolute path of the given file relative to the workspace. Returns null if the file does not
   * exist or is not a member of this project.
   */
  public IPath getFullPath(File file) {
    return MavenProjectUtils.getFullPath(getProject(), file);
  }

  public List<String> getMavenProjectModules() {
    return modules;
  }

  public Set<ArtifactRef> getMavenProjectArtifacts() {
    return artifacts;
  }

  void setMavenProjectArtifacts(MavenProject mavenProject) {
    this.artifacts = Collections.unmodifiableSet(ArtifactRef.fromArtifact(mavenProject.getArtifacts()));
  }

  public ResolverConfiguration getResolverConfiguration() {
    return resolverConfiguration;
  }

  /**
   * @return true if maven project needs to be re-read from disk
   */
  public boolean isStale() {
    IProject project = getProject();
    int i = 0;
    for(IPath path : ProjectRegistryManager.METADATA_PATH) {
      if(timestamp[i] != getModificationStamp(project.getFile(path))) {
        return true;
      }
      i++ ;
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

  public ArtifactKey getArtifactKey() {
    return artifactKey;
  }

  public synchronized void setSessionProperty(String key, Object value) {
    if(sessionProperties == null) {
      sessionProperties = new HashMap<String, Object>();
    }
    if(value != null) {
      sessionProperties.put(key, value);
    } else {
      sessionProperties.remove(key);
    }
  }

  public synchronized Object getSessionProperty(String key) {
    return sessionProperties != null ? sessionProperties.get(key) : null;
  }

  public Set<ArtifactRepositoryRef> getArtifactRepositoryRefs() {
    return artifactRepositories;
  }

  public Set<ArtifactRepositoryRef> getPluginArtifactRepositoryRefs() {
    return pluginArtifactRepositories;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getProject().toString()).append(": ").append(getArtifactKey().toString());
    return sb.toString();
  }

  public String getLifecycleMappingId() {
    return lifecycleMappingId;
  }

  public void setLifecycleMappingId(String lifecycleMappingId) {
    this.lifecycleMappingId = lifecycleMappingId;
  }

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
      executionPlans = manager.calculateExecutionPlans(pom, mavenProject, monitor);
      mavenProject.setContextValue(CTX_EXECUTION_PLANS, executionPlans);
    }
    return executionPlans;
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
      executionPlans = new LinkedHashMap<MojoExecutionKey, MojoExecution>();
      mavenProject.setContextValue(CTX_SETUP_EXECUTIONS, executionPlans);
    }
    return executionPlans;
  }

  public MojoExecution getMojoExecution(MojoExecutionKey mojoExecutionKey, IProgressMonitor monitor)
      throws CoreException {
    Map<MojoExecutionKey, MojoExecution> setupMojoExecutions = getSetupMojoExecutions(monitor);
    MojoExecution execution = setupMojoExecutions.get(mojoExecutionKey);
    if(execution == null) {
      for(MojoExecution _execution : getMojoExecutions(monitor)) {
        if(mojoExecutionKey.match(_execution)) {
          execution = manager.setupMojoExecution(this, _execution, monitor);
          break;
        }
      }
      putSetupMojoExecution(setupMojoExecutions, mojoExecutionKey, execution);
    }
    return execution;
  }

  private void putSetupMojoExecution(Map<MojoExecutionKey, MojoExecution> setupMojoExecutions,
      MojoExecutionKey mojoExecutionKey, MojoExecution execution) {
    if(execution != null) {
      setupMojoExecutions.put(mojoExecutionKey, execution);
    }
  }

  public List<MojoExecution> getMojoExecutions(String groupId, String artifactId, IProgressMonitor monitor,
      String... goals) throws CoreException {
    List<MojoExecution> result = new ArrayList<MojoExecution>();
    List<MojoExecution> _executions = getMojoExecutions(monitor);
    if(_executions != null) {
      for(MojoExecution _execution : _executions) {
        if(groupId.equals(_execution.getGroupId()) && artifactId.equals(_execution.getArtifactId())
            && contains(goals, _execution.getGoal())) {
          MojoExecutionKey _key = new MojoExecutionKey(_execution);
          Map<MojoExecutionKey, MojoExecution> setupMojoExecutions = getSetupMojoExecutions(monitor);
          MojoExecution execution = setupMojoExecutions.get(_key);
          if(execution == null) {
            execution = manager.setupMojoExecution(this, _execution, monitor);
            putSetupMojoExecution(setupMojoExecutions, _key, execution);
          }
          result.add(execution);
        }
      }
    }
    return result;
  }

  private static boolean contains(String[] goals, String goal) {
    for(int i = 0; i < goals.length; i++ ) {
      if(goals[i].equals(goal)) {
        return true;
      }
    }
    return false;
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
    if(executionPlans == null) {
      return null;
    }
    List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();
    for(List<MojoExecution> executionPlan : executionPlans.values()) {
      if(executionPlan != null) { // null if execution plan could not be calculated
        mojoExecutions.addAll(executionPlan);
      }
    }
    return mojoExecutions;
  }

  public List<MojoExecution> getExecutionPlan(String lifecycle, IProgressMonitor monitor) throws CoreException {
    Map<String, List<MojoExecution>> executionPlans = getExecutionPlans(monitor);
    return executionPlans != null ? executionPlans.get(lifecycle) : null;
  }
}
