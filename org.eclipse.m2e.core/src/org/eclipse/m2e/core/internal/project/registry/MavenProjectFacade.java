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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.embedder.ArtifactRepositoryRef;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;

public class MavenProjectFacade implements IMavenProjectFacade, Serializable {

  private static final long serialVersionUID = -3648172776786224087L;

  public static final String PROP_LIFECYCLE_MAPPING = MavenProjectFacade.class.getName() + "/lifecycleMapping";

  public static final String PROP_CONFIGURATORS = MavenProjectFacade.class.getName() + "/configurators";

  private final ProjectRegistryManager manager;

  private final IFile pom;

  private final File pomFile;

  private transient MavenProject mavenProject;

  /**
   * Maps LIFECYCLE_* to corresponding mojo executions
   */
  private transient Map<String, List<MojoExecution>> executionPlans;

  private transient Map<MojoExecutionKey, MojoExecution> setupMojoExecutions;

  private transient Map<String, Object> sessionProperties;

  // XXX make final, there should be no need to change it
  private ResolverConfiguration resolverConfiguration;

  private final long[] timestamp = new long[ProjectRegistryManager.METADATA_PATH.size() + 1];

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
  private Set<ArtifactRef> artifacts; // dependencies are resolved after facade instance is created 
  
  // lifecycle mapping
  private String lifecycleMappingId;
  private Map<MojoExecutionKey, List<PluginExecutionMetadata>> mojoExecutionMapping;

  public MavenProjectFacade(ProjectRegistryManager manager, IFile pom, MavenProject mavenProject,
      Map<String, List<MojoExecution>> executionPlans, ResolverConfiguration resolverConfiguration) {
    this.manager = manager;
    this.pom = pom;
    IPath location = pom.getLocation();
    this.pomFile = location == null ? null : location.toFile(); // save pom file
    this.resolverConfiguration = resolverConfiguration;

    this.mavenProject = mavenProject;
    this.executionPlans = executionPlans;

    this.artifactKey = new ArtifactKey(mavenProject.getArtifact());
    this.packaging = mavenProject.getPackaging();
    this.modules = mavenProject.getModules();

    this.resourceLocations = MavenProjectUtils.getResourceLocations(getProject(), mavenProject.getResources());
    this.testResourceLocations = MavenProjectUtils.getResourceLocations(getProject(), mavenProject.getTestResources());
    this.compileSourceLocations = MavenProjectUtils.getSourceLocations(getProject(), mavenProject.getCompileSourceRoots());
    this.testCompileSourceLocations = MavenProjectUtils.getSourceLocations(getProject(),mavenProject.getTestCompileSourceRoots());

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

    setMavenProjectArtifacts();

    updateTimestamp();
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
  public synchronized MavenProject getMavenProject(IProgressMonitor monitor) throws CoreException {
    if (mavenProject == null) {
      //this used to just pass in 'true' for 'offline'. when the local repo was removed or
      //corrupted, though, the project wouldn't load correctly
      IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
      boolean isOffline = mavenConfiguration.isOffline();
      MavenExecutionResult result = manager.readProjectWithDependencies(pom, resolverConfiguration, //
          new MavenUpdateRequest(isOffline, false /* updateSnapshots */), monitor);
      mavenProject = result.getProject();
      if (mavenProject == null) {
        MultiStatus status = new MultiStatus(IMavenConstants.PLUGIN_ID, 0, Messages.MavenProjectFacade_error, null);
        List<Throwable> exceptions = result.getExceptions();
        for (Throwable e : exceptions) {
          status.add(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0, e.getMessage(), e));
        }
        throw new CoreException(status);
      }
    }
    return mavenProject;
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

  void setMavenProjectArtifacts() {
    this.artifacts = ArtifactRef.fromArtifact(mavenProject.getArtifacts());
  }

  public ResolverConfiguration getResolverConfiguration() {
    return resolverConfiguration;
  }

  public void setResolverConfiguration(ResolverConfiguration configuration) {
    resolverConfiguration = configuration;
  }

  /**
   * @return true if maven project needs to be re-read from disk  
   */
  public boolean isStale() {
    IProject project = getProject();
    int i = 0;
    for(IPath path : ProjectRegistryManager.METADATA_PATH) {
      if (timestamp[i] != getModificationStamp(project.getFile(path))) {
        return true;
      }
      i++;
    }
    return timestamp[timestamp.length - 1] != getModificationStamp(pom);
  }

  private void updateTimestamp() {
    IProject project = getProject();
    int i = 0;
    for(IPath path : ProjectRegistryManager.METADATA_PATH) {
      timestamp[i] = getModificationStamp(project.getFile(path)); 
      i++;
    }
    timestamp[timestamp.length - 1] = getModificationStamp(pom);
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

  public MavenProject getMavenProject() {
    return mavenProject;
  }

  public synchronized void setSessionProperty(String key, Object value) {
    if (sessionProperties == null) {
      sessionProperties = new HashMap<String, Object>();
    }
    if (value != null) {
      sessionProperties.put(key, value);
    } else {
      sessionProperties.remove(key);
    }
  }

  public synchronized Object getSessionProperty(String key) {
    return sessionProperties != null? sessionProperties.get(key): null;
  }

  public Set<ArtifactRepositoryRef> getArtifactRepositoryRefs() {
    return artifactRepositories;
  }

  public Set<ArtifactRepositoryRef> getPluginArtifactRepositoryRefs() {
    return pluginArtifactRepositories;
  }

  public String toString() {
    if(mavenProject == null) {
      return "Maven Project: null";
    }
    return mavenProject.toString();
  }

  public String getLifecycleMappingId() {
    return lifecycleMappingId;
  }

  public Map<MojoExecutionKey, List<PluginExecutionMetadata>> getMojoExecutionMapping() {
    return mojoExecutionMapping;
  }

  public synchronized MojoExecution getMojoExecution(MojoExecutionKey mojoExecutionKey, IProgressMonitor monitor)
      throws CoreException {
    MojoExecution execution = setupMojoExecutions != null ? setupMojoExecutions.get(mojoExecutionKey) : null;
    if(execution == null) {
      for(MojoExecution _execution : getMojoExecutions(monitor)) {
        if(mojoExecutionKey.match(_execution)) {
          execution = manager.setupMojoExecution(this, _execution, monitor);
          break;
        }
      }
      putSetupMojoExecution(mojoExecutionKey, execution);
    }
    return execution;
  }

  private void putSetupMojoExecution(MojoExecutionKey mojoExecutionKey, MojoExecution execution) {
    if(execution != null) {
      if(setupMojoExecutions == null) {
        setupMojoExecutions = new HashMap<MojoExecutionKey, MojoExecution>();
      }
      setupMojoExecutions.put(mojoExecutionKey, execution);
    }
  }

  public synchronized List<MojoExecution> getMojoExecutions(String groupId, String artifactId, IProgressMonitor monitor,
      String... goals) throws CoreException {
    List<MojoExecution> result = new ArrayList<MojoExecution>();
    List<MojoExecution> _executions = getMojoExecutions(monitor);
    if(_executions != null) {
      for(MojoExecution _execution : _executions) {
        if(groupId.equals(_execution.getGroupId()) && artifactId.equals(_execution.getArtifactId())
            && contains(goals, _execution.getGoal())) {
          MojoExecutionKey _key = new MojoExecutionKey(_execution);
          MojoExecution execution = setupMojoExecutions != null ? setupMojoExecutions.get(_key) : null;
          if(execution == null) {
            execution = manager.setupMojoExecution(this, _execution, monitor);
            putSetupMojoExecution(_key, execution);
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

  public void setLifecycleMappingId(String lifecycleMappingId) {
    this.lifecycleMappingId = lifecycleMappingId;
  }

  public void setMojoExecutionMapping(Map<MojoExecutionKey, List<PluginExecutionMetadata>> mojoExecutionMapping) {
    this.mojoExecutionMapping = mojoExecutionMapping;
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

  private synchronized Map<String, List<MojoExecution>> getExecutionPlans(IProgressMonitor monitor)
      throws CoreException {
    if(executionPlans == null) {
      if(monitor == null) {
        return null;
      }
      executionPlans = manager.calculateExecutionPlans(this, monitor);
    }
    return executionPlans;
  }
}
