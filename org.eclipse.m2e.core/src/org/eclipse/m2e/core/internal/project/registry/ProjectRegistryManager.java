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
 *      Christoph Läubrich - M2Eclipse gets stuck in endless update loop
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginArtifactsCache;
import org.apache.maven.plugin.PluginRealmCache;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectRealmCache;
import org.apache.maven.project.artifact.MavenMetadataCache;
import org.apache.maven.repository.DelegatingLocalArtifactRepository;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.ILocalRepositoryListener;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.ExtensionReader;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.URLConnectionCaches;
import org.eclipse.m2e.core.internal.builder.MavenBuilder;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.MarkerUtils;
import org.eclipse.m2e.core.internal.project.DependencyResolutionContext;
import org.eclipse.m2e.core.internal.project.IManagedCache;
import org.eclipse.m2e.core.internal.project.ProjectProcessingTracker;
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
 * This class keeps track of all maven projects present in the workspace and provides mapping between Maven and the
 * workspace.
 */
@Component(service = ProjectRegistryManager.class)
public class ProjectRegistryManager implements ISaveParticipant {
  static final Logger log = LoggerFactory.getLogger(ProjectRegistryManager.class);

  static final String ARTIFACT_TYPE_POM = "pom"; //$NON-NLS-1$

  static final String ARTIFACT_TYPE_JAR = "jar"; //$NON-NLS-1$

  public static final String ARTIFACT_TYPE_JAVA_SOURCE = "java-source"; //$NON-NLS-1$

  public static final String ARTIFACT_TYPE_JAVADOC = "javadoc"; //$NON-NLS-1$

  public static final String LIFECYCLE_DEFAULT = "deploy";

  public static final String LIFECYCLE_CLEAN = "clean";

  public static final String LIFECYCLE_SITE = "site";

  /**
   * Path of project metadata files, relative to the project. These files are used to determine if project dependencies
   * need to be updated.
   */
  public static final List<IPath> METADATA_PATH = List.of( //
      new Path("pom.xml"), // //$NON-NLS-1$
      new Path(".settings/" + IMavenConstants.PLUGIN_ID + ".prefs")); // dirty trick! //$NON-NLS-1$ //$NON-NLS-2$

  private static final String CTX_MAVENPROJECTS = ProjectRegistryManager.class.getName() + "/mavenProjects";

  private ProjectRegistry projectRegistry;

  @Reference
  /*package*/ IMaven maven;

  @Reference
  /*package*/IMavenMarkerManager markerManager;

  @Reference
  private IMavenConfiguration configuration;

  @Reference
  private ProjectRegistryReader stateReader;

  private final Set<IMavenProjectChangedListener> projectChangeListeners = new LinkedHashSet<>();

  private volatile Thread syncRefreshThread;

  /**
   * Backwards compatibility with clients that request setup MojoExecution outside of {@link MavenBuilder} execution.
   */
  private final Map<MavenProjectFacade, MavenProject> legacyMavenProjects = new IdentityHashMap<>();

  private final Map<MavenProjectFacade, MavenProject> mavenProjectCache;

  /**
   * @noreference For tests only
   */
  Consumer<Map<MavenProjectFacade, MavenProject>> addContextProjectListener;

  public ProjectRegistryManager() {
    this.mavenProjectCache = createProjectCache();
  }

  @Activate
  void init() {
    //TODO this should really happen in the background!
    ProjectRegistry state;
    if(configuration.isUpdateProjectsOnStartup()) {
      state = null;
    } else {
      state = stateReader.readWorkspaceState(this);
    }
    this.projectRegistry = (state != null && state.isValid()) ? state : new ProjectRegistry();

  }

  /**
   * Creates or returns cached MavenProjectFacade for the given project. This method will not block if called from
   * IMavenProjectChangedListener#mavenProjectChanged
   */
  public MavenProjectFacade create(IProject project, IProgressMonitor monitor) {
    return create(getPom(project), false, monitor);
  }

  /**
   * Returns MavenProjectFacade corresponding to the pom. This method first looks in the project cache, then attempts to
   * load the pom if the pom is not found in the cache. In the latter case, workspace resolution is assumed to be
   * enabled for the pom but the pom will not be added to the cache.
   */
  public MavenProjectFacade create(IFile pom, boolean load, IProgressMonitor monitor) {
    if(pom == null) {
      return null;
    }

    // MavenProjectFacade projectFacade = (MavenProjectFacade) workspacePoms.get(pom.getFullPath());
    MavenProjectFacade projectFacade = projectRegistry.getProjectFacade(pom);
    if(projectFacade == null && load) {
      ResolverConfiguration configuration = ResolverConfigurationIO.readResolverConfiguration(pom.getProject());
      MavenExecutionResult executionResult = readProjectsWithDependencies(projectRegistry,
          Collections.singletonList(pom), configuration, monitor).values().iterator().next();
      MavenProject mavenProject = executionResult.getProject();
      if(mavenProject != null && mavenProject.getArtifact() != null) {
        projectFacade = new MavenProjectFacade(this, pom, mavenProject, configuration);
      } else {
        List<Throwable> exceptions = executionResult.getExceptions();
        if(exceptions != null) {
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
    if(project == null || !project.isAccessible()) {
      // XXX sensible handling
      return null;
    }
    return project.getFile(IMavenConstants.POM_FILE_NAME);
  }

  /**
   * Removes specified poms from the cache. Adds dependent poms to pomSet but does not directly refresh dependent poms.
   * Recursively removes all nested modules if appropriate.
   *
   * @return a {@link Set} of {@link IFile} affected poms
   */
  public Set<IFile> remove(MutableProjectRegistry state, Set<IFile> poms, boolean force) {
    Set<IFile> pomSet = new LinkedHashSet<>();
    for(IFile pom : poms) {
      MavenProjectFacade facade = state.getProjectFacade(pom);
      if(force || facade == null || facade.isStale()) {
        pomSet.addAll(remove(state, pom));
      }
    }
    return pomSet;
  }

  /**
   * Removes the pom from the cache. Adds dependent poms to pomSet but does not directly refresh dependent poms.
   * Recursively removes all nested modules if appropriate.
   *
   * @return a {@link Set} of {@link IFile} affected poms
   */
  public Set<IFile> remove(MutableProjectRegistry state, IFile pom) {
    MavenProjectFacade facade = state.getProjectFacade(pom);
    ArtifactKey mavenProject = facade != null ? facade.getArtifactKey() : null;

    flushCaches(state, pom, facade, false);

    if(mavenProject == null) {
      state.removeProject(pom, null);
      return Collections.emptySet();
    }

    Set<IFile> pomSet = new LinkedHashSet<>();

    pomSet.addAll(state.getDependents(MavenCapability.createMavenArtifact(mavenProject), false));
    pomSet.addAll(state.getDependents(MavenCapability.createMavenParent(mavenProject), false)); // TODO check packaging
    state.removeProject(pom, mavenProject);

    pomSet.addAll(refreshWorkspaceModules(state, pom, mavenProject));

    pomSet.remove(pom);

    return pomSet;
  }

  /**
   * @deprecated this method does not properly join {@link IMavenExecutionContext}, use
   *             {@link #refresh(Set, IProgressMonitor)} instead.
   */
  @Deprecated
  public void refresh(final MavenUpdateRequest request, final IProgressMonitor monitor) throws CoreException {
    getMaven().execute(request.isOffline(), request.isForceDependencyUpdate(), (context, pm) -> {
      refresh(request.getPomFiles(), monitor);
      return null;
    }, monitor);
  }

  private boolean isForceDependencyUpdate() throws CoreException {
    IMavenExecutionContext context = maven.getExecutionContext();
    return context != null && context.getExecutionRequest().isUpdateSnapshots();
  }

  /**
   * This method acquires workspace root's lock and sends project change events. It is meant for synchronous registry
   * updates.
   *
   * @since 1.4
   */
  public void refresh(Collection<IFile> pomFiles, IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, Messages.ProjectRegistryManager_task_refreshing, 100);
    ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();
    Job.getJobManager().beginRule(rule, progress);
    syncRefreshThread = Thread.currentThread();
    try (MutableProjectRegistry newState = newMutableProjectRegistry()) {
      refresh(newState, pomFiles, progress.newChild(95));

      applyMutableProjectRegistry(newState, progress.newChild(5));
    } finally {
      syncRefreshThread = null;
      Job.getJobManager().endRule(rule);
    }
  }

  void refresh(MutableProjectRegistry newState, Collection<IFile> pomFiles, IProgressMonitor monitor)
      throws CoreException {
    log.debug("Refreshing: {}", pomFiles); //$NON-NLS-1$

    // 442524 safety guard
    URLConnectionCaches.assertDisabled();

    DependencyResolutionContext context = new DependencyResolutionContext(pomFiles);

    // safety net -- do not force refresh of the same installed/resolved artifact more than once
    Set<ArtifactKey> installedArtifacts = new HashSet<>();

    ILocalRepositoryListener listener = (repositoryBasedir, baseArtifact, artifact, artifactFile) -> {
      if(artifactFile == null) {
        // resolution error
        return;
      }
      // TODO remove=false?
      Set<IFile> refresh = new LinkedHashSet<>();
      if(installedArtifacts.add(artifact)) {
        refresh.addAll(newState.getVersionedDependents(MavenCapability.createMavenParent(artifact), true));
        refresh.addAll(newState.getVersionedDependents(MavenCapability.createMavenArtifact(artifact), true));
        refresh.addAll(newState.getVersionedDependents(MavenCapability.createMavenArtifactImport(artifact), true));
      }
      if(installedArtifacts.add(baseArtifact)) {
        refresh.addAll(newState.getVersionedDependents(MavenCapability.createMavenParent(baseArtifact), true));
        refresh.addAll(newState.getVersionedDependents(MavenCapability.createMavenArtifact(baseArtifact), true));
        refresh.addAll(newState.getVersionedDependents(MavenCapability.createMavenArtifactImport(baseArtifact), true));
      }
      if(!refresh.isEmpty()) {
        log.debug("Automatic refresh. artifact={}/{}. projects={}", baseArtifact, artifact, refresh);
        context.forcePomFiles(refresh);
      }
    };

    maven.addLocalRepositoryListener(listener);
    try {
      refresh(newState, context, monitor);
    } finally {
      maven.removeLocalRepositoryListener(listener);
    }

    log.debug("Refreshed: {}", pomFiles); //$NON-NLS-1$
  }

  private void refresh(MutableProjectRegistry newState, DependencyResolutionContext context, IProgressMonitor monitor)
      throws CoreException {
    Set<IFile> allProcessedPoms = new HashSet<>();
    Set<IFile> allNewFacades = new HashSet<>();

    Map<IFile, Set<Capability>> originalCapabilities = new HashMap<>();
    Map<IFile, Set<RequiredCapability>> originalRequirements = new HashMap<>();

    // phase 1: build projects without dependencies and populate workspace with known projects
    while(!context.isEmpty()) { // context may be augmented, so we need to keep processing
      List<IFile> toReadPomFiles = new ArrayList<>();
      while(!context.isEmpty()) { // Group build of all current context
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        if(newState.isStale() || (syncRefreshThread != null && syncRefreshThread != Thread.currentThread())) {
          throw new StaleMutableProjectRegistryException();
        }

        IFile pom = context.pop();
        if(allNewFacades.contains(pom)) {
          // pom was already in context and successfully read
          continue;
        }
        allProcessedPoms.add(pom);

        monitor.subTask(NLS.bind(Messages.ProjectRegistryManager_task_project, pom.getProject().getName()));
        MavenProjectFacade oldFacade = newState.getProjectFacade(pom);

        context.forcePomFiles(flushCaches(newState, pom, oldFacade, isForceDependencyUpdate()));
        if(oldFacade != null) {
          putMavenProject(oldFacade, null); // maintain maven project cache
        }
        if(pom.isAccessible() && pom.getProject().hasNature(IMavenConstants.NATURE_ID)) {
          toReadPomFiles.add(pom);
          if(oldFacade != null) {
            // refresh old child modules
            MavenCapability mavenParentCapability = MavenCapability.createMavenParent(oldFacade.getArtifactKey());
            context.forcePomFiles(newState.getVersionedDependents(mavenParentCapability, true));

            // refresh projects that import dependencyManagement from this one
            MavenCapability mavenArtifactImportCapability = MavenCapability
                .createMavenArtifactImport(oldFacade.getArtifactKey());
            context.forcePomFiles(newState.getVersionedDependents(mavenArtifactImportCapability, true));
          }
        } else {
          newState.setProject(pom, null); // discard closed/deleted pom in workspace
          // refresh children of deleted/closed parent
          if(oldFacade != null) {
            MavenCapability mavenParentCapability = MavenCapability.createMavenParent(oldFacade.getArtifactKey());
            context.forcePomFiles(newState.getDependents(mavenParentCapability, true));

            MavenCapability mavenArtifactImportCapability = MavenCapability
                .createMavenArtifactImport(oldFacade.getArtifactKey());
            context.forcePomFiles(newState.getVersionedDependents(mavenArtifactImportCapability, true));
          }
        }
      }
      Map<IFile, MavenProjectFacade> newFacades = readMavenProjectFacades(toReadPomFiles, newState, monitor);
      for(Entry<IFile, MavenProjectFacade> entry : newFacades.entrySet()) {
        IFile pom = entry.getKey();
        MavenProjectFacade newFacade = entry.getValue();
        newState.setProject(pom, newFacade);
        if(newFacade != null) {
          // refresh new child modules
          MavenCapability mavenParentCapability = MavenCapability.createMavenParent(newFacade.getArtifactKey());
          context.forcePomFiles(newState.getVersionedDependents(mavenParentCapability, true));

          // refresh projects that import dependencyManagement from this one
          MavenCapability mavenArtifactImportCapability = MavenCapability
              .createMavenArtifactImport(newFacade.getArtifactKey());
          context.forcePomFiles(newState.getVersionedDependents(mavenArtifactImportCapability, true));

          Set<Capability> capabilities = new LinkedHashSet<>();
          capabilities.add(mavenParentCapability);
          capabilities.add(MavenCapability.createMavenArtifact(newFacade.getArtifactKey()));
          Set<Capability> oldCapabilities = newState.setCapabilities(pom, capabilities);
          if(!originalCapabilities.containsKey(pom)) {
            originalCapabilities.put(pom, oldCapabilities);
          }

          MavenProject mavenProject = getMavenProject(newFacade);
          Set<RequiredCapability> requirements = new LinkedHashSet<>();
          DefaultMavenDependencyResolver.addProjectStructureRequirements(requirements, mavenProject);
          Set<RequiredCapability> oldRequirements = newState.setRequirements(pom, requirements);
          if(!originalRequirements.containsKey(pom)) {
            originalRequirements.put(pom, oldRequirements);
          }
        }
      }
      allNewFacades.addAll(newFacades.keySet());
      List<IFile> erroneousPoms = new ArrayList<>(toReadPomFiles);
      erroneousPoms.removeAll(newFacades.keySet());
      erroneousPoms.forEach(pom -> newState.setProject(pom, null));
      if(!newFacades.isEmpty()) { // progress did happen
        // push files that could't be read back into context for a second pass.
        // This can help if some read files are parent of other ones in the same
        // request -> the child wouldn't be read immediately, but would be in a
        // 2nd pass once parent was read.
        context.forcePomFiles(new HashSet<>(erroneousPoms));
      }
    }

    context.forcePomFiles(allProcessedPoms);

    // phase 2: resolve project dependencies
    ProjectProcessingTracker tracker = new ProjectProcessingTracker(context);
    do {
      while(!context.isEmpty()) {
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        if(newState.isStale() || (syncRefreshThread != null && syncRefreshThread != Thread.currentThread())) {
          throw new StaleMutableProjectRegistryException();
        }

        IFile pom = context.pop();
        if(tracker.shouldProcess(pom)) {

          MavenProjectFacade newFacade = null;
          if(pom.isAccessible() && pom.getProject().hasNature(IMavenConstants.NATURE_ID)) {
            newFacade = newState.getProjectFacade(pom);
          }
          if(newFacade != null) {
            MavenProject mavenProject = getMavenProject(newFacade);
            if(!allProcessedPoms.contains(newFacade.getPom())) {
              // facade from workspace state that has not been refreshed yet
              newFacade = readMavenProjectFacades(Collections.singletonList(pom), newState, monitor).get(pom);
            } else {
              // recreate facade instance to trigger project changed event
              // this is only necessary for facades that are refreshed because their dependencies changed
              // but this is relatively cheap, so all facades are recreated here
              putMavenProject(newFacade, null);
              newFacade = new MavenProjectFacade(newFacade);
              putMavenProject(newFacade, mavenProject);
            }
          }

          if(newFacade != null) {
            MavenProjectFacade facade = newFacade;
            ResolverConfiguration resolverConfiguration = facade.getResolverConfiguration();
            createExecutionContext(newState, pom, resolverConfiguration).execute(getMavenProject(newFacade),
                (executionContext, pm) -> {
                  refreshPhase2(newState, context, originalCapabilities, originalRequirements, pom, facade, pm);
                  return null;
                }, monitor);
          } else {
            refreshPhase2(newState, context, originalCapabilities, originalRequirements, pom, newFacade, monitor);
          }

          monitor.worked(1);
        }
      }
    } while(tracker.needsImprovement());
  }

  void refreshPhase2(MutableProjectRegistry newState, DependencyResolutionContext context,
      Map<IFile, Set<Capability>> originalCapabilities, Map<IFile, Set<RequiredCapability>> originalRequirements,
      IFile pom, MavenProjectFacade newFacade, IProgressMonitor monitor) throws CoreException {
    Set<Capability> capabilities = null;
    Set<RequiredCapability> requirements = null;
    if(newFacade != null) {
      monitor.subTask(NLS.bind(Messages.ProjectRegistryManager_task_project, newFacade.getProject().getName()));

      setupLifecycleMapping(newState, monitor, newFacade);

      capabilities = new LinkedHashSet<>();
      requirements = new LinkedHashSet<>();

      Capability mavenParentCapability = MavenCapability.createMavenParent(newFacade.getArtifactKey());

      // maven projects always have these capabilities
      capabilities.add(MavenCapability.createMavenArtifact(newFacade.getArtifactKey()));
      capabilities.add(mavenParentCapability); // TODO consider packaging

      // maven projects always have these requirements
      DefaultMavenDependencyResolver.addProjectStructureRequirements(requirements, getMavenProject(newFacade));

      AbstractMavenDependencyResolver resolver = getMavenDependencyResolver(newFacade, monitor);
      resolver.setContextProjectRegistry(newState);
      try {
        resolver.resolveProjectDependencies(newFacade, capabilities, requirements, monitor);
      } finally {
        resolver.setContextProjectRegistry(null);
      }

      newState.setProject(pom, newFacade);

      newFacade.setMavenProjectArtifacts(getMavenProject(newFacade));
    } else {
      if(pom.isAccessible() && pom.getProject().hasNature(IMavenConstants.NATURE_ID)) {
        try (InputStream is = pom.getContents()) {
          // MNGECLIPSE-605 embedder is not able to resolve the project due to missing configuration in the parent
          Model model = getMaven().readModel(is);
          if(model != null && model.getParent() != null) {
            Parent parent = model.getParent();
            if(parent.getGroupId() != null && parent.getArtifactId() != null && parent.getVersion() != null) {
              ArtifactKey parentKey = new ArtifactKey(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(),
                  null);
              requirements = new HashSet<>();
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

    // refresh versioned dependents if only parent capability has changed
    boolean versionedCapabilitiesOnly = true;
    for(Capability capability : changedCapabilities) {
      if(MavenCapability.NS_MAVEN_ARTIFACT.equals(capability.getVersionlessKey().getNamespace())) {
        versionedCapabilitiesOnly = false;
        break;
      }
    }
    for(Capability capability : changedCapabilities) {
      context.forcePomFiles(versionedCapabilitiesOnly ? newState.getVersionedDependents(capability, true)
          : newState.getDependents(capability, true));
    }

    Set<RequiredCapability> oldRequirements = newState.setRequirements(pom, requirements);
    if(originalRequirements.containsKey(pom)) {
      oldRequirements = originalRequirements.get(pom);
    }
    // if our dependencies changed, recalculate everyone who depends on us
    // this is needed to deal with transitive dependency resolution in maven
    if(oldCapabilities != null && hasDiff(oldRequirements, requirements)) {
      for(Capability capability : oldCapabilities) {
        context.forcePomFiles(newState.getVersionedDependents(capability, true));
      }
    }
  }

  private void setupLifecycleMapping(MutableProjectRegistry newState, IProgressMonitor monitor,
      MavenProjectFacade newFacade) throws CoreException {
    LifecycleMappingResult mappingResult = LifecycleMappingFactory.calculateLifecycleMapping(getMavenProject(newFacade),
        newFacade.getMojoExecutions(), newFacade.getResolverConfiguration().getLifecycleMappingId(), monitor);

    newFacade.setLifecycleMappingId(mappingResult.getLifecycleMappingId());
    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping = mappingResult
        .getMojoExecutionMapping();
    if(mojoExecutionMapping != null) {
      detachMappingSources(mojoExecutionMapping);
    }
    newFacade.setMojoExecutionMapping(mojoExecutionMapping);

    // XXX reconcile with corresponding LifecycleMappingFactory methods
    newFacade.setSessionProperty(MavenProjectFacade.PROP_LIFECYCLE_MAPPING, mappingResult.getLifecycleMapping());
    LifecycleMappingFactory.setProjectConfigurators(newFacade, mappingResult);

    markerManager.deleteMarkers(newFacade.getPom(), IMavenConstants.MARKER_LIFECYCLEMAPPING_ID);
    if(mappingResult.hasProblems()) {
      markerManager.addErrorMarkers(newFacade.getPom(), IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
          mappingResult.getProblems());
    }
  }

  private void detachMappingSources(Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mapping) {
    for(List<IPluginExecutionMetadata> executions : mapping.values()) {
      if(executions != null) {
        ListIterator<IPluginExecutionMetadata> iterator = executions.listIterator();
        while(iterator.hasNext()) {
          PluginExecutionMetadata execution = (PluginExecutionMetadata) iterator.next();
          execution = execution.clone();
          execution.setSource(null);
          iterator.set(execution);
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
    Set<T> result = new HashSet<>();
    Set<T> t;

    t = new HashSet<>(a);
    t.removeAll(b);
    result.addAll(t);
    t = new HashSet<>(b);
    t.removeAll(a);
    result.addAll(t);

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
      if(!oldRequirement.equals(requirement)) {
        return true;
      }
    }
    return false;
  }

  private AbstractMavenDependencyResolver getMavenDependencyResolver(MavenProjectFacade newFacade,
      IProgressMonitor monitor) {
    ILifecycleMapping lifecycleMapping = LifecycleMappingFactory.getLifecycleMapping(newFacade);

    if(lifecycleMapping instanceof ILifecycleMapping2) {
      AbstractMavenDependencyResolver resolver = ((ILifecycleMapping2) lifecycleMapping).getDependencyResolver(monitor);
      resolver.setManager(this);
      return resolver;
    }

    return new DefaultMavenDependencyResolver(this, markerManager);
  }

  private Map<IFile, MavenProjectFacade> readMavenProjectFacades(Collection<IFile> poms, MutableProjectRegistry state,
      IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, poms.size());
    for(IFile pom : poms) {
      markerManager.deleteMarkers(pom, IMavenConstants.MARKER_POM_LOADING_ID);
    }

    Map<IFile, ResolverConfiguration> resolverConfigurations = new HashMap<>(poms.size(), 1.f);
    Map<ResolverConfiguration, Collection<IFile>> groupsToImport = poms.stream().collect(Collectors.groupingBy(pom -> {
      subMonitor.checkCanceled();
      ResolverConfiguration resolverConfiguration = ResolverConfigurationIO.readResolverConfiguration(pom.getProject());
      resolverConfigurations.put(pom, resolverConfiguration);
      return resolverConfiguration;
    }, LinkedHashMap::new, Collectors.toCollection(LinkedHashSet::new)));

    Map<IFile, MavenProjectFacade> result = new HashMap<>(poms.size(), 1.f);
    for(Entry<ResolverConfiguration, Collection<IFile>> entry : groupsToImport.entrySet()) {
      ResolverConfiguration resolverConfiguration = entry.getKey();
      Collection<IFile> pomFiles = entry.getValue();
      result.putAll(execute(state, poms.size() == 1 ? pomFiles.iterator().next() : null, resolverConfiguration,
          (executionContext, pm) -> {
            Map<File, MavenExecutionResult> mavenResults = getMaven().readMavenProjects(pomFiles.stream()
                .filter(IFile::isAccessible).map(ProjectRegistryManager::toJavaIoFile).collect(Collectors.toList()),
                executionContext.newProjectBuildingRequest());

            Map<IFile, MavenProjectFacade> facades = new HashMap<>(mavenResults.size(), 1.f);
            for(IFile pom : pomFiles) {
              if(!pom.isAccessible()) {
                continue;
              }
              MavenExecutionResult mavenResult = mavenResults.get(ProjectRegistryManager.toJavaIoFile(pom));
              MavenProject mavenProject = mavenResult.getProject();
              MarkerUtils.addEditorHintMarkers(markerManager, pom, mavenProject, IMavenConstants.MARKER_POM_LOADING_ID);
              markerManager.addMarkers(pom, IMavenConstants.MARKER_POM_LOADING_ID, mavenResult);
              if(mavenProject != null && mavenProject.getArtifact() != null) {
                MavenProjectFacade mavenProjectFacade = new MavenProjectFacade(ProjectRegistryManager.this, pom,
                    mavenProject, resolverConfiguration);
                putMavenProject(mavenProjectFacade, mavenProject); // maintain maven project cache
                facades.put(pom, mavenProjectFacade);
              }
            }

            return facades;
          }, subMonitor.split(pomFiles.size())));
    }
    return result;
  }

  /*package*/Map<String, List<MojoExecution>> calculateExecutionPlans(IFile pom, MavenProject mavenProject,
      IProgressMonitor monitor) {
    Map<String, List<MojoExecution>> executionPlans = new LinkedHashMap<>();
    executionPlans.put(LIFECYCLE_CLEAN, calculateExecutionPlan(pom, mavenProject, LIFECYCLE_CLEAN, monitor));
    executionPlans.put(LIFECYCLE_DEFAULT, calculateExecutionPlan(pom, mavenProject, LIFECYCLE_DEFAULT, monitor));
    executionPlans.put(LIFECYCLE_SITE, calculateExecutionPlan(pom, mavenProject, LIFECYCLE_SITE, monitor));
    return executionPlans;
  }

  private List<MojoExecution> calculateExecutionPlan(IFile pom, MavenProject mavenProject, String lifecycle,
      IProgressMonitor monitor) {
    List<MojoExecution> mojoExecutions = null;
    try {
      MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(mavenProject, Arrays.asList(lifecycle), false,
          monitor);
      return executionPlan.getMojoExecutions();
    } catch(CoreException e) {
      markerManager.addErrorMarkers(pom, IMavenConstants.MARKER_POM_LOADING_ID, e);
    }
    return mojoExecutions;
  }

  public IFile getModulePom(IFile pom, String moduleName) {
    return pom.getParent().getFile(new Path(moduleName).append(IMavenConstants.POM_FILE_NAME));
  }

  private Set<IFile> refreshWorkspaceModules(MutableProjectRegistry state, IFile pom, ArtifactKey mavenProject) {
    if(mavenProject == null) {
      return Collections.emptySet();
    }

    return state.removeWorkspaceModules(pom, mavenProject);
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void addMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    synchronized(projectChangeListeners) {
      projectChangeListeners.add(listener);
    }
  }

  public void removeMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    if(listener == null) {
      return;
    }
    synchronized(projectChangeListeners) {
      projectChangeListeners.remove(listener);
    }
  }

  public void notifyProjectChangeListeners(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
    if(events.size() > 0) {
      MavenProjectChangedEvent[] eventsArray = events.toArray(new MavenProjectChangedEvent[events.size()]);
      ArrayList<IMavenProjectChangedListener> listeners = new ArrayList<>();
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

  private MavenProject readProjectWithDependencies(IFile pomFile, ResolverConfiguration resolverConfiguration,
      IProgressMonitor monitor) {
    Map<File, MavenExecutionResult> results = readProjectsWithDependencies(projectRegistry,
        Collections.singletonList(pomFile), resolverConfiguration, monitor);
    if(results.size() != 1) {
      throw new IllegalStateException("Results should contain one entry.");
    }
    MavenExecutionResult result = results.values().iterator().next();
    MavenProject mavenProject = result.getProject();
    if(mavenProject != null && !hasError(result)) {
      return mavenProject;
    }
    MultiStatus status = new MultiStatus(IMavenConstants.PLUGIN_ID, 0, Messages.MavenProjectFacade_error);
    for(Throwable e : result.getExceptions()) {
      status.add(Status.error(e.getMessage(), e));
    }
    throw new IllegalStateException(new CoreException(status).fillInStackTrace());
  }

  private static boolean hasError(MavenExecutionResult mavenExecutionResult) {
    return mavenExecutionResult.getExceptions().stream().anyMatch(ex -> !(ex instanceof ProjectBuildingException))
        || mavenExecutionResult.getExceptions().stream()//
            .map(ProjectBuildingException.class::cast)//
            .flatMap(ex -> ex.getResults().stream())//
            .flatMap(result -> result.getProblems().stream())//
            .map(ModelProblem::getSeverity)//
            .anyMatch(severity -> severity != Severity.WARNING);
  }

  Map<File, MavenExecutionResult> readProjectsWithDependencies(IProjectRegistry state, Collection<IFile> pomFiles,
      ResolverConfiguration resolverConfiguration, IProgressMonitor monitor) {
    try {
      return execute(state, pomFiles.size() == 1 ? pomFiles.iterator().next() : null, resolverConfiguration,
          (context, aMonitor) -> {
            ProjectBuildingRequest configuration = context.newProjectBuildingRequest();
            configuration.setResolveDependencies(true);
            return getMaven().readMavenProjects(pomFiles.stream().map(ProjectRegistryManager::toJavaIoFile)
                .filter(Objects::nonNull).collect(Collectors.toList()), configuration);
          }, monitor);
    } catch(CoreException ex) {
      MavenExecutionResult result = new DefaultMavenExecutionResult();
      result.addException(ex);
      return pomFiles.stream().filter(IResource::isAccessible).map(ProjectRegistryManager::toJavaIoFile)
          .filter(Objects::nonNull).collect(HashMap::new, (map, pomFile) -> map.put(pomFile, result),
              (container, toFold) -> container.putAll(toFold));
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

  /**
   * @deprecated This method does not properly join {@link IMavenExecutionContext}
   */
  @Deprecated
  public MavenExecutionRequest createExecutionRequest(IFile pom, ResolverConfiguration resolverConfiguration,
      IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest request = getMaven().createExecutionRequest(monitor);
    configureExecutionRequest(request, projectRegistry, pom, resolverConfiguration);
    MavenExecutionContext.populateSystemProperties(request);
    return request;
  }

  /*package*/MavenExecutionRequest configureExecutionRequest(MavenExecutionRequest request, IProjectRegistry state,
      IFile pom, ResolverConfiguration resolverConfiguration) throws CoreException {
    if(pom != null) {
      request.setPom(ProjectRegistryManager.toJavaIoFile(pom));
    }

    request.addActiveProfiles(resolverConfiguration.getActiveProfileList());
    request.addInactiveProfiles(resolverConfiguration.getInactiveProfileList());

    Properties p = request.getUserProperties();
    Properties addProperties = resolverConfiguration.getProperties();
    if(addProperties != null) {
      if(p == null) {
        p = new Properties();
      }
      p.putAll(addProperties);
    }

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

    DelegatingLocalArtifactRepository localRepo = new DelegatingLocalArtifactRepository(
        getMaven().getLocalRepository());
    localRepo.setIdeWorkspace(workspaceReader);

    return localRepo;
  }

  MutableProjectRegistry newMutableProjectRegistry() {
    return new MutableProjectRegistry(projectRegistry);
  }

  /**
   * Applies mutable project registry to the primary project registry and and corresponding MavenProjectChangedEvent's
   * to all registered IMavenProjectChangedListener's. This method must be called from a thread holding workspace root's
   * lock.
   *
   * @throws StaleMutableProjectRegistryException if primary project registry was modified after mutable registry has
   *           been created
   */
  void applyMutableProjectRegistry(MutableProjectRegistry newState, IProgressMonitor monitor) throws CoreException {
    // don't cache maven sessions
    for(MavenProjectFacade facade : newState.getProjects()) {
      MavenProject mavenProject = getMavenProject(facade);
      if(mavenProject != null) {
        getMaven().detachFromSession(mavenProject);
      }
    }
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

  /*package*/MojoExecution setupMojoExecution(MavenProjectFacade projectFacade, MojoExecution mojoExecution,
      IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = null;
    if(MavenExecutionContext.getThreadContext() == null) {
      // the intent of this code is to provide backwards compatibility for clients that request setup MojoExecution
      // outside of MavenBuilder context. Setup MojoExecutions are specific to MavenProject instances, so keep
      // MavenProject until the facade is discarded
      synchronized(legacyMavenProjects) {
        mavenProject = legacyMavenProjects.get(projectFacade);
        if(mavenProject == null) {
          mavenProject = getMavenProject(projectFacade, monitor);
          legacyMavenProjects.put(projectFacade, mavenProject);
        }
      }
    }
    if(mavenProject == null) {
      mavenProject = getMavenProject(projectFacade, monitor);
    }
    return maven.setupMojoExecution(mavenProject, mojoExecution, monitor);
  }

  private <V> V execute(IProjectRegistry state, IFile pom, ResolverConfiguration resolverConfiguration,
      ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    return createExecutionContext(state, pom, resolverConfiguration).execute(callable, monitor);
  }

  private IMavenExecutionContext createExecutionContext(IProjectRegistry state, IFile pom,
      ResolverConfiguration resolverConfiguration) throws CoreException {
    IMavenExecutionContext context = maven.createExecutionContext();
    configureExecutionRequest(context.getExecutionRequest(), state, pom, resolverConfiguration);
    return context;
  }

  public IMavenExecutionContext createExecutionContext(IFile pom, ResolverConfiguration resolverConfiguration)
      throws CoreException {
    return createExecutionContext(projectRegistry, pom, resolverConfiguration);
  }

  /**
   * There are three MavenProjectFacade-to-MavenProject maps.
   * <ul>
   * <li>Each MavenExecutionContext has "context project map" that guarantees consistent facade-project association for
   * entire lifespan of the context. In other words, calling facade.getMavenProject multiple times from within the same
   * maven execution scope is guaranteed to return the same MavenProject instance.</li>
   * <li>Global "project cache", that is meant to improve performance during incremental workspace builds. The project
   * cache is small and cached values are discarded and reloaded as needed.</li>
   * <li>Global "legacy support project map" provides support for legacy, i.e. pre m2e 1.4, extensions that setup
   * MojoExecution instances outside of maven execution scope. Legacy support project map entries are not discarded
   * until their corresponding facade instances are discarded.</li>
   * </ul>
   */

  MavenProject getMavenProject(MavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject;
    synchronized(legacyMavenProjects) {
      mavenProject = legacyMavenProjects.get(facade);
    }
    if(mavenProject != null) {
      return mavenProject;
    }
    Map<MavenProjectFacade, MavenProject> mavenProjects = getContextProjects();
    try {
      return mavenProjects.computeIfAbsent(facade, fac -> mavenProjectCache.computeIfAbsent(fac,
          f -> readProjectWithDependencies(f.getPom(), f.getResolverConfiguration(), monitor)));
    } catch(RuntimeException ex) { // thrown by method called in lambda to carry CoreException
      Throwable cause = ex.getCause();
      if(cause instanceof CoreException) {
        throw (CoreException) cause;
      }
      throw new CoreException(Status.error("Unexpected internal Error occured", ex)); // this really should never happen
    }
  }

  MavenProject getMavenProject(MavenProjectFacade facade) {
    MavenProject mavenProject;
    synchronized(legacyMavenProjects) {
      mavenProject = legacyMavenProjects.get(facade);
    }
    if(mavenProject == null) {
      mavenProject = mavenProjectCache.get(facade);
      if(mavenProject != null) {
        putMavenProject(facade, mavenProject);
      }
    }
    if(mavenProject == null) {
      mavenProject = getContextProjects().get(facade);
    }
    return mavenProject;
  }

  /**
   * @noreference public for test purposes only
   */
  public void putMavenProject(MavenProjectFacade facade, MavenProject mavenProject) {
    Map<MavenProjectFacade, MavenProject> mavenProjects = getContextProjects();
    if(mavenProject != null) {
      mavenProjects.put(facade, mavenProject);
      if(this.addContextProjectListener != null) {
        this.addContextProjectListener.accept(mavenProjects);
      }
    } else {
      mavenProjects.remove(facade);
      synchronized(legacyMavenProjects) {
        legacyMavenProjects.remove(facade);
      }
    }
  }

  /**
   * Do not modify this map directly, use {@link #putMavenProject(MavenProjectFacade, MavenProject)}
   *
   * @return
   */
  private Map<MavenProjectFacade, MavenProject> getContextProjects() {
    Map<MavenProjectFacade, MavenProject> projects = null;
    MavenExecutionContext context = MavenExecutionContext.getThreadContext(false);
    if(context != null) {
      projects = context.getValue(CTX_MAVENPROJECTS);
      if(projects == null) {
        projects = new IdentityHashMap<>();
        context.setValue(CTX_MAVENPROJECTS, projects);
      }
    }
    return projects != null ? projects : new IdentityHashMap<>();
  }

  private Map<MavenProjectFacade, MavenProject> createProjectCache() {
    int maxCacheSize = 5;
    return Collections.synchronizedMap(new LinkedHashMap<>(maxCacheSize * 4 / 3 + 1, 0.75f, true) {
      private static final long serialVersionUID = 8022606648487974598L;

      @Override
      protected boolean removeEldestEntry(java.util.Map.Entry<MavenProjectFacade, MavenProject> eldest) {
        if(size() > maxCacheSize) {
          // there is currently no good way to determine if MavenProject instance is still being used or not
          // for now assume that cache entries removed from project cache can only be referenced by context map
          MavenProjectFacade facade = eldest.getKey();
          Map<MavenProjectFacade, MavenProject> contextProjects = getContextProjects();
          if(!contextProjects.containsKey(facade)) {
            flushMavenCaches(facade.getPomFile(), facade.getArtifactKey(), false);
          }
          return true;
        }
        return false;
      }
    });
  }

  private Set<IFile> flushCaches(MutableProjectRegistry newState, IFile pom, MavenProjectFacade facade,
      boolean forceDependencyUpdate) {
    if(facade != null) {
      ArtifactKey key = facade.getArtifactKey();
      mavenProjectCache.remove(facade);
      Set<IFile> ifiles = new HashSet<>();
      for(File file : flushMavenCaches(facade.getPomFile(), key, forceDependencyUpdate)) {
        MavenProjectFacade affected = projectRegistry.getProjectFacade(file);
        if(affected != null) {
          ifiles.add(affected.getPom());
        }
      }
      return ifiles;
    }

    return Collections.emptySet();
  }

  /**
   * Flushes caches maintained by Maven core.
   */
  private Set<File> flushMavenCaches(File pom, ArtifactKey key, boolean force) {
    Set<File> affected = new HashSet<>();
    affected.addAll(flushMavenCache(ProjectRealmCache.class, pom, key, force));
    affected.addAll(flushMavenCache(ExtensionRealmCache.class, pom, key, force));
    affected.addAll(flushMavenCache(PluginRealmCache.class, pom, key, force));
    affected.addAll(flushMavenCache(MavenMetadataCache.class, pom, key, force));
    affected.addAll(flushMavenCache(PluginArtifactsCache.class, pom, key, force));
    return affected;
  }

  private Set<File> flushMavenCache(Class<?> clazz, File pom, ArtifactKey key, boolean force) {
    try {
      IManagedCache cache = (IManagedCache) maven.lookup(clazz);
      return cache.removeProject(pom, key, force);
    } catch(CoreException ex) {
      // can't really happen
    }
    return Collections.emptySet();
  }

  static File toJavaIoFile(IFile file) {
    IPath path = file.getLocation();
    if(path == null) {
      return getRemoteFile(file);
    }
    return path.toFile();
  }

  private static File getRemoteFile(IFile file) {
    try {
      URI fileLocation = file.getLocationURI();
      org.eclipse.core.filesystem.IFileStore fileStore = EFS.getStore(fileLocation);
      return fileStore.toLocalFile(EFS.CACHE, null);
    } catch(CoreException ex) {
      log.warn("Failed to create local file representation of " + file);
      return null;
    }
  }

  @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
  void setWorkspace(IWorkspace workspace) throws CoreException {
    //TODO better be declarative see https://github.com/eclipse-platform/eclipse.platform.resources/issues/55
    workspace.addSaveParticipant(IMavenConstants.PLUGIN_ID, this);
  }

  void unsetWorkspace(IWorkspace workspace) {
    workspace.removeSaveParticipant(IMavenConstants.PLUGIN_ID);
  }

  @Override
  public void doneSaving(ISaveContext context) {

  }

  @Override
  public void prepareToSave(ISaveContext context) {

  }

  @Override
  public void rollback(ISaveContext context) {

  }

  @Override
  public void saving(ISaveContext context) {
    writeWorkspaceState();
  }
}
