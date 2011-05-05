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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.MavenMetadataCache;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.repository.DelegatingLocalArtifactRepository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ILocalRepositoryListener;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.ExtensionReader;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.MarkerUtils;
import org.eclipse.m2e.core.internal.project.DependencyResolutionContext;
import org.eclipse.m2e.core.internal.project.IManagedCache;
import org.eclipse.m2e.core.internal.project.ResolverConfigurationIO;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;

/**
 * This class keeps track of all maven projects present in the workspace and
 * provides mapping between Maven and the workspace.
 */
public class ProjectRegistryManager {
  static final Logger log = LoggerFactory.getLogger(ProjectRegistryManager.class);

  static final String ARTIFACT_TYPE_POM = "pom"; //$NON-NLS-1$
  static final String ARTIFACT_TYPE_JAR = "jar"; //$NON-NLS-1$
  public static final String ARTIFACT_TYPE_JAVA_SOURCE = "java-source"; //$NON-NLS-1$
  public static final String ARTIFACT_TYPE_JAVADOC = "javadoc"; //$NON-NLS-1$

  public static final String LIFECYCLE_DEFAULT = "deploy";
  public static final String LIFECYCLE_CLEAN = "clean";
  public static final String LIFECYCLE_SITE = "site";

  /**
   * Path of project metadata files, relative to the project. These
   * files are used to determine if project dependencies need to be
   * updated.
   */
  public static final List<? extends IPath> METADATA_PATH = Arrays.asList( //
      new Path("pom.xml"), // //$NON-NLS-1$
      new Path(".project"), // //$NON-NLS-1$
      new Path(".classpath"), // //$NON-NLS-1$
      new Path(".settings/" + IMavenConstants.PLUGIN_ID + ".prefs")); // dirty trick! //$NON-NLS-1$ //$NON-NLS-2$

  private final ProjectRegistry projectRegistry;

  private final MavenImpl maven;

  private final IMavenMarkerManager markerManager;

  private final ProjectRegistryReader stateReader;

  private final Set<IMavenProjectChangedListener> projectChangeListeners = new LinkedHashSet<IMavenProjectChangedListener>();

  private volatile Thread syncRefreshThread;

  public ProjectRegistryManager(MavenImpl maven, File stateLocationDir, boolean readState,
      IMavenMarkerManager mavenMarkerManager) {
    this.markerManager = mavenMarkerManager;
    this.maven = maven;

    this.stateReader = new ProjectRegistryReader(stateLocationDir);

    ProjectRegistry state = readState && stateReader != null ? stateReader.readWorkspaceState(this) : null;
    this.projectRegistry = (state != null && state.isValid()) ? state : new ProjectRegistry();
  }
  
  /**
   * Creates or returns cached MavenProjectFacade for the given project.
   * 
   * This method will not block if called from IMavenProjectChangedListener#mavenProjectChanged
   */
  public MavenProjectFacade create(IProject project, IProgressMonitor monitor) {
    return create(getPom(project), false, monitor);
  }

  /**
   * Returns MavenProjectFacade corresponding to the pom.
   * 
   * This method first looks in the project cache, then attempts to load
   * the pom if the pom is not found in the cache. In the latter case,
   * workspace resolution is assumed to be enabled for the pom but the pom
   * will not be added to the cache.
   */
  public MavenProjectFacade create(IFile pom, boolean load, IProgressMonitor monitor) {
    if(pom == null) {
      return null;
    }

    // MavenProjectFacade projectFacade = (MavenProjectFacade) workspacePoms.get(pom.getFullPath());
    MavenProjectFacade projectFacade = projectRegistry.getProjectFacade(pom);
    if(projectFacade == null && load) {
      ResolverConfiguration configuration = ResolverConfigurationIO.readResolverConfiguration(pom.getProject());
      //this used to just pass in 'true' for 'offline'. when the local repo was removed or
      //corrupted, though, the project wouldn't load correctly
      IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();
      boolean isOffline = mavenConfiguration.isOffline();
      MavenExecutionResult executionResult = readProjectWithDependencies(projectRegistry, pom, configuration, //
          new MavenUpdateRequest(isOffline, false /* updateSnapshots */),
          monitor);
      MavenProject mavenProject = executionResult.getProject();
      if(mavenProject != null) {
        projectFacade = new MavenProjectFacade(this, pom, mavenProject, null, configuration);
      } else {
        List<Throwable> exceptions = executionResult.getExceptions();
        if (exceptions != null) {
          for(Throwable ex : exceptions) {
            String msg = "Failed to read Maven project: " + ex.getMessage();
            log.error(msg, ex);
          }
        }
      }
    }
    return projectFacade;
  }

  IFile getPom(IProject project) {
    if (project == null || !project.isAccessible()) {
      // XXX sensible handling
      return null;
    }
    return project.getFile(IMavenConstants.POM_FILE_NAME);
  }

  /**
   * Removes specified poms from the cache.
   * Adds dependent poms to pomSet but does not directly refresh dependent poms.
   * Recursively removes all nested modules if appropriate.
   * 
   * @return a {@link Set} of {@link IFile} affected poms
   */
  public Set<IFile> remove(MutableProjectRegistry state, Set<IFile> poms, boolean force) {
    Set<IFile> pomSet = new LinkedHashSet<IFile>();
    for (Iterator<IFile> it = poms.iterator(); it.hasNext(); ) {
      IFile pom = it.next();
      MavenProjectFacade facade = state.getProjectFacade(pom);
      if (force || facade == null || facade.isStale()) {
        pomSet.addAll(remove(state, pom));
      }
    }
    return pomSet;
  }
  
  /**
   * Removes the pom from the cache. 
   * Adds dependent poms to pomSet but does not directly refresh dependent poms.
   * Recursively removes all nested modules if appropriate.
   * 
   * @return a {@link Set} of {@link IFile} affected poms
   */
  public Set<IFile> remove(MutableProjectRegistry state, IFile pom) {
    MavenProjectFacade facade = state.getProjectFacade(pom);
    ArtifactKey mavenProject = facade != null ? facade.getArtifactKey() : null;

    flushCaches(pom, facade);

    if (mavenProject == null) {
      state.removeProject(pom, null);
      return Collections.emptySet();
    }

    Set<IFile> pomSet = new LinkedHashSet<IFile>();

    pomSet.addAll(state.getDependents(MavenCapability.createMavenArtifact(mavenProject), false));
    pomSet.addAll(state.getDependents(MavenCapability.createMavenParent(mavenProject), false)); // TODO check packaging
    state.removeProject(pom, mavenProject);

    pomSet.addAll(refreshWorkspaceModules(state, pom, mavenProject));

    pomSet.remove(pom);
    
    return pomSet;
  }

  private void flushCaches(IFile pom, MavenProjectFacade facade) {
    ArtifactKey key = null;
    MavenProject project = null;
    
    if (facade != null) {
      key = facade.getArtifactKey();
      project = facade.getMavenProject();
    }
    try {
      IManagedCache cache = (IManagedCache) maven.getPlexusContainer().lookup(MavenMetadataCache.class);
      cache.removeProject(pom, key);
    } catch(ComponentLookupException ex) {
      // can't really happen
    } catch(CoreException ex) {
      // can't really happen
    }
    if (project != null) {
      ((MavenImpl) getMaven()).releaseExtensionsRealm(project);
    }
  }

  /**
   * This method acquires workspace root's lock and sends project change events.
   * It is meant for synchronous registry updates.
   */
  public void refresh(MavenUpdateRequest request, IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, Messages.ProjectRegistryManager_task_refreshing, 100);
    ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();
    Job.getJobManager().beginRule(rule, progress);
    try {
      syncRefreshThread = Thread.currentThread();

      MutableProjectRegistry newState = newMutableProjectRegistry();
      try {
        refresh(newState, request, progress.newChild(95));
  
        applyMutableProjectRegistry(newState, progress.newChild(5));
      } finally {
        newState.close();
      }
    } finally {
      syncRefreshThread = null;
      Job.getJobManager().endRule(rule);
    }
  }

  void refresh(final MutableProjectRegistry newState, MavenUpdateRequest updateRequest, IProgressMonitor monitor)
      throws CoreException {
    log.debug("Refreshing: {}", updateRequest.toString()); //$NON-NLS-1$

    MavenExecutionRequest executionRequest = getMaven().createExecutionRequest(monitor);

    final DependencyResolutionContext context = new DependencyResolutionContext(updateRequest, executionRequest);

    // safety net -- do not force refresh of the same installed/resolved artifact more than once 
    final Set<ArtifactKey> installedArtifacts = new HashSet<ArtifactKey>();

    ILocalRepositoryListener listener = new ILocalRepositoryListener() {
      public void artifactInstalled(File repositoryBasedir, ArtifactKey baseArtifact, ArtifactKey artifact,
          File artifactFile) {
        if(artifactFile == null) {
          // resolution error
          return;
        }
        // TODO remove=false?
        Set<IFile> refresh = new LinkedHashSet<IFile>();
        if(installedArtifacts.add(artifact)) {
          refresh.addAll(newState.getDependents(MavenCapability.createMavenParent(artifact), true));
          refresh.addAll(newState.getDependents(MavenCapability.createMavenArtifact(artifact), true));
        }
        if(installedArtifacts.add(baseArtifact)) {
          refresh.addAll(newState.getDependents(MavenCapability.createMavenParent(baseArtifact), true));
          refresh.addAll(newState.getDependents(MavenCapability.createMavenArtifact(baseArtifact), true));
        }
        if (!refresh.isEmpty()) {
          log.debug("Automatic refresh. artifact={}/{}. projects={}", new Object[] {baseArtifact, artifact, refresh});
          context.forcePomFiles(refresh);
        }
      }
    };

    maven.addLocalRepositoryListener(listener);
    try {
      refresh(newState, context, monitor);
    } finally {
      maven.removeLocalRepositoryListener(listener);
    }

    log.debug("Refreshed: {}", updateRequest.toString()); //$NON-NLS-1$
  }

  private void refresh(MutableProjectRegistry newState, DependencyResolutionContext context, IProgressMonitor monitor)
      throws CoreException {
    Set<IFile> secondPhaseBacklog = new LinkedHashSet<IFile>();

    Map<IFile, Set<Capability>> originalCapabilities = new HashMap<IFile, Set<Capability>>();
    Map<IFile, Set<RequiredCapability>> originalRequirements = new HashMap<IFile, Set<RequiredCapability>>();

    // phase 1: build projects without dependencies and populate workspace with known projects
    while(!context.isEmpty()) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      if(newState.isStale() || (syncRefreshThread != null && syncRefreshThread != Thread.currentThread())) {
        throw new StaleMutableProjectRegistryException();
      }

      IFile pom = context.pop();

      monitor.subTask(NLS.bind(Messages.ProjectRegistryManager_task_project, pom.getProject().getName()));
      MavenProjectFacade newFacade = null;
      if(pom.isAccessible() && pom.getProject().hasNature(IMavenConstants.NATURE_ID)) {
        MavenProjectFacade oldFacade = newState.getProjectFacade(pom);

        flushCaches(pom, oldFacade);

        if(oldFacade != null) {
          // refresh old child modules
          MavenCapability mavenParentCapability = MavenCapability.createMavenParent(oldFacade.getArtifactKey());
          context.forcePomFiles(newState.getDependents(mavenParentCapability, true));
        }

        newFacade = readMavenProject(pom, context, newState, monitor);
      } else {
        // refresh children of deleted/closed parent
        MavenProjectFacade oldFacade = newState.getProjectFacade(pom);
        if(oldFacade != null) {
          MavenCapability mavenParentCapability = MavenCapability.createMavenParent(oldFacade.getArtifactKey());
          context.forcePomFiles(newState.getDependents(mavenParentCapability, true));
        }
      }

      newState.setProject(pom, newFacade);

      if(newFacade != null) {
        // refresh new child modules
        MavenCapability mavenParentCapability = MavenCapability.createMavenParent(newFacade.getArtifactKey());
        context.forcePomFiles(newState.getDependents(mavenParentCapability, true));

        Set<Capability> capabilities = new LinkedHashSet<Capability>();
        capabilities.add(mavenParentCapability);
        capabilities.add(MavenCapability.createMavenArtifact(newFacade.getArtifactKey()));
        Set<Capability> oldCapabilities = newState.setCapabilities(pom, capabilities);
        if(!originalCapabilities.containsKey(pom)) {
          originalCapabilities.put(pom, oldCapabilities);
        }

        Set<RequiredCapability> requirements = new LinkedHashSet<RequiredCapability>();
        DefaultMavenDependencyResolver.addParentRequirements(requirements, newFacade.getMavenProject());
        Set<RequiredCapability> oldRequirements = newState.setRequirements(pom, requirements);
        if(!originalRequirements.containsKey(pom)) {
          originalRequirements.put(pom, oldRequirements);
        }
      }

      // at this point project facade and project capabilities/requirements are inconsistent in the state
      // this will be reconciled during the second phase

      secondPhaseBacklog.add(pom);
    }

    context.forcePomFiles(secondPhaseBacklog);

    // phase 2: resolve project dependencies
    Set<IFile> secondPhaseProcessed = new HashSet<IFile>();
    while(!context.isEmpty()) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      if(newState.isStale() || (syncRefreshThread != null && syncRefreshThread != Thread.currentThread())) {
        throw new StaleMutableProjectRegistryException();
      }

      IFile pom = context.pop();

      if(!secondPhaseProcessed.add(pom)) {
        // because workspace contents is fully known at this point, each project needs to be resolved at most once 
        continue;
      }
      
      MavenProjectFacade newFacade = null;
      if(pom.isAccessible() && pom.getProject().hasNature(IMavenConstants.NATURE_ID)) {
        newFacade = newState.getProjectFacade(pom);
      }
      if(newFacade != null) {
        if(newFacade.getMavenProject() == null) {
          // facade from workspace state that has not been refreshed yet 
          newFacade = readMavenProject(pom, context, newState, monitor);
        } else {
          // loose any session state
          newFacade = new MavenProjectFacade(newFacade);
        }
      }

      Set<Capability> capabilities = null;
      Set<RequiredCapability> requirements = null;
      if(newFacade != null) {
        monitor.subTask(NLS.bind(Messages.ProjectRegistryManager_task_project, newFacade.getProject().getName()));

        setupLifecycleMapping(newState, context, monitor, newFacade);

        capabilities = new LinkedHashSet<Capability>();
        requirements = new LinkedHashSet<RequiredCapability>();

        Capability mavenParentCapability = MavenCapability.createMavenParent(newFacade.getArtifactKey());

        // maven projects always have these capabilities
        capabilities.add(MavenCapability.createMavenArtifact(newFacade.getArtifactKey()));
        capabilities.add(mavenParentCapability); // TODO consider packaging

        // maven projects always have these requirements
        DefaultMavenDependencyResolver.addParentRequirements(requirements, newFacade.getMavenProject());

        AbstractMavenDependencyResolver resolver = getMavenDependencyResolver(newFacade, monitor);
        resolver.setContextProjectRegistry(newState);
        try {
          MavenExecutionRequest mavenRequest = getConfiguredExecutionRequest(context, newState, newFacade.getPom(),
              newFacade.getResolverConfiguration());
          mavenRequest.getProjectBuildingRequest().setProject(newFacade.getMavenProject());
          mavenRequest.getProjectBuildingRequest().setResolveDependencies(true);
          resolver.resolveProjectDependencies(newFacade, mavenRequest, capabilities, requirements, monitor);
        } finally {
          resolver.setContextProjectRegistry(null);
        }

        newState.setProject(pom, newFacade);

        newFacade.setMavenProjectArtifacts();
      } else {
        if(pom.isAccessible() && pom.getProject().hasNature(IMavenConstants.NATURE_ID)) {
          try {
            // MNGECLIPSE-605 embedder is not able to resolve the project due to missing configuration in the parent
            Model model = getMaven().readModel(pom.getLocation().toFile());
            if(model != null && model.getParent() != null) {
              Parent parent = model.getParent();
              if(parent.getGroupId() != null && parent.getArtifactId() != null && parent.getVersion() != null) {
                ArtifactKey parentKey = new ArtifactKey(parent.getGroupId(), parent.getArtifactId(),
                    parent.getVersion(), null);
                requirements = new HashSet<RequiredCapability>();
                requirements.add(MavenRequiredCapability.createMavenParent(parentKey));
              }
            }
          } catch(Exception e) {
            // we've tried our best, there is nothing else we can do
            log.error(e.getMessage(), e);
          }
        }
      }

      Set<Capability> oldCapabilities = newState.setCapabilities(pom, capabilities);
      if(originalCapabilities.containsKey(pom)) {
        oldCapabilities = originalCapabilities.get(pom);
      }
      // if our capabilities changed, recalculate everyone who depends on new/changed/removed capabilities
      Set<Capability> changedCapabilities = diff(oldCapabilities, capabilities);
      for(Capability capability : changedCapabilities) {
        context.forcePomFiles(newState.getDependents(capability, true));
      }

      Set<RequiredCapability> oldRequirements = newState.setRequirements(pom, requirements);
      if(originalRequirements.containsKey(pom)) {
        oldRequirements = originalRequirements.get(pom);
      }
      // if our dependencies changed, recalculate everyone who depends on us
      // this is needed to deal with transitive dependency resolution in maven
      if(oldCapabilities != null && hasDiff(oldRequirements, requirements)) {
        for(Capability capability : oldCapabilities) {
          context.forcePomFiles(newState.getDependents(capability.getVersionlessKey(), true));
        }
      }

      monitor.worked(1);
    }
  }

  private void setupLifecycleMapping(MutableProjectRegistry newState, DependencyResolutionContext context,
      IProgressMonitor monitor, MavenProjectFacade newFacade) throws CoreException {
    MavenExecutionRequest mavenRequest = getConfiguredExecutionRequest(context, newState, newFacade.getPom(),
        newFacade.getResolverConfiguration());

    LifecycleMappingResult mappingResult = LifecycleMappingFactory.calculateLifecycleMapping(mavenRequest, newFacade,
        monitor);

    newFacade.setLifecycleMappingId(mappingResult.getLifecycleMappingId());
    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping = mappingResult
        .getMojoExecutionMapping();
    if(mojoExecutionMapping != null) {
      detachMappingSources(mojoExecutionMapping);
    }
    newFacade.setMojoExecutionMapping(mojoExecutionMapping);

    // XXX reconcile with corresponding LifecycleMappingFactory methods
    newFacade.setSessionProperty(MavenProjectFacade.PROP_LIFECYCLE_MAPPING, mappingResult.getLifecycleMapping());
    newFacade.setSessionProperty(MavenProjectFacade.PROP_CONFIGURATORS, mappingResult.getProjectConfigurators());

    markerManager.deleteMarkers(newFacade.getPom(), IMavenConstants.MARKER_LIFECYCLEMAPPING_ID);
    if(mappingResult.hasProblems()) {
      markerManager.addErrorMarkers(newFacade.getPom(), IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
          mappingResult.getProblems());
    }
  }

  private void detachMappingSources(Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mapping) {
    for(List<IPluginExecutionMetadata> executions : mapping.values()) {
      if(executions != null) {
        for(IPluginExecutionMetadata execution : executions) {
          ((PluginExecutionMetadata) execution).setSource(null);
        }
      }
    }
  }

  static <T> Set<T> diff(Set<T> a, Set<T> b) {
    if(a == null || a.isEmpty()) {
      if(b == null || b.isEmpty()) {
        return Collections.emptySet();
      }
      return b;
    }
    if(b == null || b.isEmpty()) {
      return a;
    }
    Set<T> result = new HashSet<T>();
    Set<T> t;
    
    t = new HashSet<T>(a); t.removeAll(b); result.addAll(t);
    t = new HashSet<T>(b); t.removeAll(a); result.addAll(t);

    return result;
  }

  static <T> boolean hasDiff(Set<T> a, Set<T> b) {
    if(a == null || a.isEmpty()) {
      return b != null && !b.isEmpty();
    }

    if(b == null || b.isEmpty()) {
      return true;
    }

    if(a.size() != b.size()) {
      return true;
    }

    Iterator<T> oldIter = a.iterator();
    Iterator<T> iter = b.iterator();

    while(oldIter.hasNext()) {
      T oldRequirement = oldIter.next();
      T requirement = iter.next();
      if (!oldRequirement.equals(requirement)) {
        return true;
      }
    }
    return false;
  }

  private AbstractMavenDependencyResolver getMavenDependencyResolver(MavenProjectFacade newFacade, IProgressMonitor monitor) throws CoreException {
    ILifecycleMapping lifecycleMapping = LifecycleMappingFactory.getLifecycleMapping(newFacade);

    if (lifecycleMapping instanceof ILifecycleMapping2) {
      AbstractMavenDependencyResolver resolver = ((ILifecycleMapping2) lifecycleMapping).getDependencyResolver(monitor);
      resolver.setManager(this);
      return resolver;
    }

    return new DefaultMavenDependencyResolver(this, markerManager);
  }

  protected MavenExecutionRequest getConfiguredExecutionRequest(DependencyResolutionContext context,
      IProjectRegistry state, IFile pom, ResolverConfiguration resolverConfiguration) throws CoreException {
    MavenExecutionRequest mavenRequest = DefaultMavenExecutionRequest.copy(context.getExecutionRequest());
    mavenRequest.setStartTime(context.getExecutionRequest().getStartTime());
    configureExecutionRequest(mavenRequest, state, pom, resolverConfiguration);
    getMaven().populateDefaults(mavenRequest);
    mavenRequest.setOffline(context.getRequest().isOffline());
    mavenRequest.setUpdateSnapshots(context.getRequest().isForceDependencyUpdate());
    return mavenRequest;
  }

  private MavenProjectFacade readMavenProject(IFile pom, DependencyResolutionContext context,
      MutableProjectRegistry state, IProgressMonitor monitor) throws CoreException {
    markerManager.deleteMarkers(pom, IMavenConstants.MARKER_POM_LOADING_ID);

    ResolverConfiguration resolverConfiguration = ResolverConfigurationIO.readResolverConfiguration(pom.getProject());

    MavenProject mavenProject = null;
    MavenExecutionResult mavenResult = null;
    if (pom.isAccessible()) {
        MavenExecutionRequest mavenRequest = getConfiguredExecutionRequest(context, state, pom, resolverConfiguration);
        mavenResult = getMaven().readProject(mavenRequest, monitor);
        mavenProject = mavenResult.getProject();
    }

    MarkerUtils.addEditorHintMarkers(markerManager, pom, mavenProject, IMavenConstants.MARKER_POM_LOADING_ID);
    if (mavenProject == null) {
      markerManager.addMarkers(pom, IMavenConstants.MARKER_POM_LOADING_ID, mavenResult);
      return null;
    }

    // don't cache maven session
    getMaven().detachFromSession(mavenProject);

    Map<String, List<MojoExecution>> executionPlans = calculateExecutionPlans(context, state, pom, mavenProject,
        resolverConfiguration, monitor);

    // create and return new project facade
    MavenProjectFacade mavenProjectFacade = new MavenProjectFacade(ProjectRegistryManager.this, pom, mavenProject,
        executionPlans, resolverConfiguration);

    return mavenProjectFacade;
  }

  private Map<String, List<MojoExecution>> calculateExecutionPlans(DependencyResolutionContext context,
      IProjectRegistry state, IFile pom, MavenProject mavenProject, ResolverConfiguration resolverConfiguration,
      IProgressMonitor monitor) {
    Map<String, List<MojoExecution>> executionPlans = new LinkedHashMap<String, List<MojoExecution>>();
    executionPlans.put(LIFECYCLE_CLEAN,
        calculateExecutionPlan(context, state, pom, mavenProject, resolverConfiguration, LIFECYCLE_CLEAN, monitor));
    executionPlans.put(LIFECYCLE_DEFAULT,
        calculateExecutionPlan(context, state, pom, mavenProject, resolverConfiguration, LIFECYCLE_DEFAULT, monitor));
    executionPlans.put(LIFECYCLE_SITE,
        calculateExecutionPlan(context, state, pom, mavenProject, resolverConfiguration, LIFECYCLE_SITE, monitor));
    return executionPlans;
  }

  private List<MojoExecution> calculateExecutionPlan(DependencyResolutionContext context, IProjectRegistry state,
      IFile pom, MavenProject mavenProject, ResolverConfiguration resolverConfiguration, String lifecycle,
      IProgressMonitor monitor) {
    List<MojoExecution> mojoExecutions = null;
    try {
      MavenExecutionRequest mavenRequest = getConfiguredExecutionRequest(context, state, pom, resolverConfiguration);
      MavenSession session = maven.createSession(mavenRequest, mavenProject);
      MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(session, mavenProject, Arrays.asList(lifecycle),
          false, monitor);
      mojoExecutions = executionPlan.getMojoExecutions();
    } catch(CoreException e) {
      markerManager.addErrorMarkers(pom, IMavenConstants.MARKER_POM_LOADING_ID, e);
    }
    return mojoExecutions;
  }

  public IFile getModulePom(IFile pom, String moduleName) {
    return pom.getParent().getFile(new Path(moduleName).append(IMavenConstants.POM_FILE_NAME));
  }

  private Set<IFile> refreshWorkspaceModules(MutableProjectRegistry state, IFile pom, ArtifactKey mavenProject) {
    if (mavenProject == null) {
      return Collections.emptySet();
    }

    return state.removeWorkspaceModules(pom, mavenProject);
  }

  public void addMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    synchronized (projectChangeListeners) {
      projectChangeListeners.add(listener);
    }
  }

  public void removeMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    if(listener == null) {
      return;
    }
    synchronized (projectChangeListeners) {
      projectChangeListeners.remove(listener);
    }
  }

  public void notifyProjectChangeListeners(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
    if(events.size() > 0) {
      MavenProjectChangedEvent[] eventsArray = events.toArray(new MavenProjectChangedEvent[events.size()]);
      ArrayList<IMavenProjectChangedListener> listeners = new ArrayList<IMavenProjectChangedListener>();
      synchronized(this.projectChangeListeners) {
        listeners.addAll(this.projectChangeListeners);
      }
      listeners.addAll(ExtensionReader.readProjectChangedEventListenerExtentions());
      for(IMavenProjectChangedListener listener : listeners) {
        listener.mavenProjectChanged(eventsArray, monitor);
      }
    }
  }

  public MavenProjectFacade getMavenProject(String groupId, String artifactId, String version) {
    return projectRegistry.getProjectFacade(groupId, artifactId, version);
  }

  MavenExecutionResult readProjectWithDependencies(IFile pomFile, ResolverConfiguration resolverConfiguration,
      MavenUpdateRequest updateRequest, IProgressMonitor monitor) {
    return readProjectWithDependencies(projectRegistry, pomFile, resolverConfiguration, updateRequest, monitor);
  }

  private MavenExecutionResult readProjectWithDependencies(IProjectRegistry state, IFile pomFile, ResolverConfiguration resolverConfiguration,
      MavenUpdateRequest updateRequest, IProgressMonitor monitor) {

    try {
      MavenExecutionRequest request = createExecutionRequest(state, pomFile, resolverConfiguration, monitor);
      getMaven().populateDefaults(request);
      request.setOffline(updateRequest.isOffline());
      request.getProjectBuildingRequest().setResolveDependencies(true);
      return getMaven().readProject(request, monitor);
    } catch(CoreException ex) {
      DefaultMavenExecutionResult result = new DefaultMavenExecutionResult();
      result.addException(ex);
      return result;
    }

  }

  public IMavenProjectFacade[] getProjects() {
    return projectRegistry.getProjects();
  }

  public IMavenProjectFacade getProject(IProject project) {
    return projectRegistry.getProjectFacade(getPom(project));
  }

  /**
   * Context
   */
  static class Context {
    final IProjectRegistry state;

    final ResolverConfiguration resolverConfiguration;

    final IFile pom;

    Context(IProjectRegistry state, ResolverConfiguration resolverConfiguration, IFile pom) {
      this.state = state;
      this.resolverConfiguration = resolverConfiguration;
      this.pom = pom;
    }
  }

  public MavenExecutionRequest createExecutionRequest(IFile pom, ResolverConfiguration resolverConfiguration, IProgressMonitor monitor) throws CoreException {
    return createExecutionRequest(projectRegistry, pom, resolverConfiguration, monitor);
  }

  private MavenExecutionRequest createExecutionRequest(IProjectRegistry state, IFile pom, ResolverConfiguration resolverConfiguration, IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest request = getMaven().createExecutionRequest(monitor);

    return configureExecutionRequest(request, state, pom, resolverConfiguration);
  }

  private MavenExecutionRequest configureExecutionRequest(MavenExecutionRequest request, IProjectRegistry state,
      IFile pom, ResolverConfiguration resolverConfiguration) throws CoreException {
    request.setPom(pom.getLocation().toFile());

    request.addActiveProfiles(resolverConfiguration.getActiveProfileList());

    // temporary solution for https://issues.sonatype.org/browse/MNGECLIPSE-1607
    Properties systemProperties = new Properties();
    EnvironmentUtils.addEnvVars(systemProperties);
    systemProperties.putAll(System.getProperties());
    request.setSystemProperties(systemProperties);

    // eclipse workspace repository implements both workspace dependency resolution
    // and inter-module dependency resolution for multi-module projects.

    request.setLocalRepository(getMaven().getLocalRepository());
    request.setWorkspaceReader(getWorkspaceReader(state, pom, resolverConfiguration));

    return request;
  }

  private EclipseWorkspaceArtifactRepository getWorkspaceReader(IProjectRegistry state, IFile pom,
      ResolverConfiguration resolverConfiguration) {
    Context context = new Context(state, resolverConfiguration, pom);
    EclipseWorkspaceArtifactRepository workspaceReader = new EclipseWorkspaceArtifactRepository(context);
    return workspaceReader;
  }

  public MavenArtifactRepository getWorkspaceLocalRepository() throws CoreException {
    ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
    resolverConfiguration.setResolveWorkspaceProjects(true);
    EclipseWorkspaceArtifactRepository workspaceReader = getWorkspaceReader(projectRegistry, null,
        resolverConfiguration);

    DelegatingLocalArtifactRepository localRepo = new DelegatingLocalArtifactRepository(getMaven().getLocalRepository());
    localRepo.setIdeWorkspace(workspaceReader);

    return localRepo;
  }

  MutableProjectRegistry newMutableProjectRegistry() {
    return new MutableProjectRegistry(projectRegistry);
  }

  /**
   * Applies mutable project registry to the primary project registry and
   * and corresponding MavenProjectChangedEvent's to all registered 
   * IMavenProjectChangedListener's.
   * 
   * This method must be called from a thread holding workspace root's lock.
   * 
   * @throws StaleMutableProjectRegistryException if primary project registry
   *    was modified after mutable registry has been created
   */
  void applyMutableProjectRegistry(MutableProjectRegistry newState, IProgressMonitor monitor) {
    List<MavenProjectChangedEvent> events = projectRegistry.apply(newState);
    //stateReader.writeWorkspaceState(projectRegistry);
    notifyProjectChangeListeners(events, monitor);
  }
  
  public void writeWorkspaceState() {
    if(stateReader != null && projectRegistry != null) {
      stateReader.writeWorkspaceState(projectRegistry);
    }
  }

  IMaven getMaven() {
    return maven;
  }

  public MojoExecution setupMojoExecution(MavenProjectFacade projectFacade, MojoExecution mojoExecution,
      IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest request = createExecutionRequest(projectFacade.getPom(),
        projectFacade.getResolverConfiguration(), monitor);
    MavenSession session = maven.createSession(request, projectFacade.getMavenProject());
    return maven.setupMojoExecution(session, projectFacade.getMavenProject(), mojoExecution);
  }

  public Map<String, List<MojoExecution>> calculateExecutionPlans(MavenProjectFacade projectFacade, IProgressMonitor monitor)
      throws CoreException {
    boolean offline = MavenPlugin.getMavenConfiguration().isOffline();
    MavenUpdateRequest request = new MavenUpdateRequest(offline, false /*updateSnapshots*/);
    MavenExecutionRequest executionRequest = createExecutionRequest(projectFacade.getPom(),
        projectFacade.getResolverConfiguration(), monitor);
    DependencyResolutionContext context = new DependencyResolutionContext(request, executionRequest);
    return calculateExecutionPlans(context, projectRegistry, projectFacade.getPom(),
        projectFacade.getMavenProject(monitor), projectFacade.getResolverConfiguration(), monitor);
  }
}
