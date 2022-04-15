/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.internal;

import java.io.File;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.repository.legacy.WagonManager;

import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.index.filter.ArtifactFilterManager;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.MavenMarkerManager;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.internal.project.WorkspaceClassifierResolverManager;
import org.eclipse.m2e.core.internal.project.WorkspaceStateWriter;
import org.eclipse.m2e.core.internal.project.conversion.ProjectConversionManager;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IWorkspaceClassifierResolverManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


public class MavenPluginActivator extends Plugin {
  private final Logger log = LoggerFactory.getLogger(MavenPluginActivator.class);

  public static final String PREFS_ARCHETYPES = "archetypesInfo.xml"; //$NON-NLS-1$

  // The shared instance
  private static MavenPluginActivator plugin;

  private final Collection<PlexusContainer> toDisposeContainers = new HashSet<>();

  private MavenModelManager modelManager;

  private BundleContext bundleContext;

  private MavenProjectManager projectManager;

  private MavenRuntimeManagerImpl runtimeManager;

  private ProjectConfigurationManager configurationManager;

  private ProjectRegistryRefreshJob mavenBackgroundJob;

  private ArchetypeManager archetypeManager;

  private ProjectRegistryManager managerImpl;

  private IMavenMarkerManager mavenMarkerManager;

  private RepositoryRegistry repositoryRegistry;

  private ArtifactFilterManager artifactFilterManager;

  private String version = "0.0.0"; //$NON-NLS-1$

  private IMavenConfiguration mavenConfiguration;

  private final BundleListener bundleListener = event -> LifecycleMappingFactory.setBundleMetadataSources(null);

  private final ISaveParticipant saveParticipant = new ISaveParticipant() {

    @Override
    public void saving(ISaveContext context) {
      if(managerImpl != null) {
        managerImpl.writeWorkspaceState();
      }
    }

    @Override
    public void rollback(ISaveContext context) {
    }

    @Override
    public void prepareToSave(ISaveContext context) {
    }

    @Override
    public void doneSaving(ISaveContext context) {
    }
  };

  private MavenImpl maven;

  private IProjectConversionManager projectConversionManager;

  private IWorkspaceClassifierResolverManager workspaceClassifierResolverManager;

  private ServiceRegistration<URLStreamHandlerService> protocolHandlerService;

  private ServiceTracker<IWorkspace, IWorkspace> workspaceTracker;

  public MavenPluginActivator() {
    plugin = this;

    if(Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/initialization"))) { //$NON-NLS-1$
      System.err.println("### executing constructor " + IMavenConstants.PLUGIN_ID); //$NON-NLS-1$
      new Throwable().printStackTrace();
    }
  }

  public MavenImpl getMaven() {
    return maven;
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    if(Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/initialization"))) { //$NON-NLS-1$
      System.err.println("### executing start() " + IMavenConstants.PLUGIN_ID); //$NON-NLS-1$
      new Throwable().printStackTrace();
    }
    // Workaround MNG-6530
    System.setProperty(DefaultProjectBuilder.DISABLE_GLOBAL_MODEL_CACHE_SYSTEM_PROPERTY, Boolean.toString(true));

    URLConnectionCaches.disable();

    this.bundleContext = context;

    try {
      Version bundleVersion = getBundle().getVersion();
      this.version = bundleVersion.getMajor() + "." + bundleVersion.getMinor() + "." + bundleVersion.getMicro(); //$NON-NLS-1$ //$NON-NLS-2$
    } catch(IllegalArgumentException e) {
      // ignored
    }

    this.mavenConfiguration = new MavenConfigurationImpl();
    File stateLocationDir = getStateLocation().toFile();

    this.mavenMarkerManager = new MavenMarkerManager(mavenConfiguration);

    boolean updateProjectsOnStartup = mavenConfiguration.isUpdateProjectsOnStartup();

    ////////////////////////////////////////////////////////////////////////////////////////////////

    this.maven = new MavenImpl(mavenConfiguration);

    // TODO eagerly reads workspace state cache
    this.managerImpl = new ProjectRegistryManager(maven, stateLocationDir, !updateProjectsOnStartup /* readState */,
        mavenMarkerManager);

    this.mavenBackgroundJob = new ProjectRegistryRefreshJob(managerImpl, mavenConfiguration);

    context.registerService(IResourceChangeListener.class, mavenBackgroundJob, getMaskProperties(
        IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE));

    this.projectManager = new MavenProjectManager(managerImpl, mavenBackgroundJob, stateLocationDir);
    this.projectManager.addMavenProjectChangedListener(new WorkspaceStateWriter(projectManager));
    workspaceTracker = new ServiceTracker<>(bundleContext, IWorkspace.class,
        new ServiceTrackerCustomizer<IWorkspace, IWorkspace>() {

          @Override
          public IWorkspace addingService(ServiceReference<IWorkspace> reference) {
            //TODO there should be a declarative way to do this!
            IWorkspace workspace = bundleContext.getService(reference);
            if(workspace != null) {
              try {
                workspace.addSaveParticipant(IMavenConstants.PLUGIN_ID, saveParticipant);
              } catch(CoreException ex) {
                getLog().log(ex.getStatus());
              }
            }
            return workspace;
          }

          @Override
          public void modifiedService(ServiceReference<IWorkspace> reference, IWorkspace workspace) {
            //we don't care about property changes
          }

          @Override
          public void removedService(ServiceReference<IWorkspace> reference, IWorkspace workspace) {
            workspace.removeSaveParticipant(IMavenConstants.PLUGIN_ID);
          }
        });
    workspaceTracker.open();
    if(updateProjectsOnStartup || managerImpl.getProjects().length == 0) {
      IWorkspace workspace = workspaceTracker.getService();
      if(workspace != null) {
        this.projectManager.refresh(new MavenUpdateRequest(workspace.getRoot().getProjects(), //
          mavenConfiguration.isOffline() /*offline*/, false /* updateSnapshots */));
      }
    }

    this.modelManager = new MavenModelManager(maven, projectManager);

    this.runtimeManager = new MavenRuntimeManagerImpl();

    this.configurationManager = new ProjectConfigurationManager(maven, managerImpl, modelManager, mavenMarkerManager,
        mavenConfiguration);
    this.projectManager.addMavenProjectChangedListener(this.configurationManager);
    context.registerService(IResourceChangeListener.class, configurationManager,
        getMaskProperties(IResourceChangeEvent.PRE_DELETE));

    //create repository registry
    this.repositoryRegistry = new RepositoryRegistry(maven, projectManager);
    this.maven.addSettingsChangeListener(repositoryRegistry);
    this.projectManager.addMavenProjectChangedListener(repositoryRegistry);

    //
    this.artifactFilterManager = new ArtifactFilterManager();

    // fork repository registry update. must after index manager registered as a listener
    this.repositoryRegistry.updateRegistry();

    this.projectConversionManager = new ProjectConversionManager();

    this.workspaceClassifierResolverManager = new WorkspaceClassifierResolverManager();
    //register URL handler, we can't use DS here because this triggers loading of m2e too early
    Map<String, Object> properties = Map.of(URLConstants.URL_HANDLER_PROTOCOL, new String[] {"mvn"});
    this.protocolHandlerService = context.registerService(URLStreamHandlerService.class,
        new MvnProtocolHandlerService(), FrameworkUtil.asDictionary(properties));

    // Automatically delete now obsolete nexus cache (can be removed again if some time has passed and it is unlikely an old workspace that need to be cleaned up is used).
    IPath nexusCache = Platform.getStateLocation(context.getBundle()).append("nexus");
    FileUtils.deleteDirectory(nexusCache.toFile());
    //register as service to make static method access obsolete...
    bundleContext.registerService(IMavenProjectRegistry.class, projectManager, null);
  }

  private static Dictionary<String, ?> getMaskProperties(int mask) {
    Dictionary<String, Object> properties = new Hashtable<>();
    properties.put("event.mask", mask);
    return properties;
  }

  private DefaultPlexusContainer newPlexusContainer(ClassLoader cl) throws PlexusContainerException {
    final Module logginModule = new AbstractModule() {
      @Override
      protected void configure() {
        bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
      }
    };
    final ContainerConfiguration cc = new DefaultContainerConfiguration() //
        .setClassWorld(new ClassWorld("plexus.core", cl)) //$NON-NLS-1$
        .setClassPathScanning(PlexusConstants.SCANNING_INDEX) //
        .setAutoWiring(true) //
        .setName("plexus"); //$NON-NLS-1$
    return new DefaultPlexusContainer(cc, logginModule);
  }

  private static ArchetypeManager newArchetypeManager(PlexusContainer container, File stateLocationDir) {
    ArchetypeManager archetypeManager = new ArchetypeManager(container, new File(stateLocationDir, PREFS_ARCHETYPES));
    archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.InternalCatalogFactory());
    archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.DefaultLocalCatalogFactory());
    for(ArchetypeCatalogFactory archetypeCatalogFactory : ExtensionReader.readArchetypeExtensions()) {
      archetypeManager.addArchetypeCatalogFactory(archetypeCatalogFactory);
    }
    return archetypeManager;
  }

  /**
   * @deprecated provided for backwards compatibility only. all component lookup must go though relevant subsystem --
   *             {@link MavenImpl}, {@link NexusIndexManager} or {@link ArchetypeManager}.
   *             <p>
   *             In most case, use <code>MavenPlugin.getMaven().lookup(...)</code> instead.
   */
  @Deprecated
  public PlexusContainer getPlexusContainer() {
    try {
      return getMaven().getPlexusContainer();
    } catch(CoreException ex) {
      getLog().log(ex.getStatus());
      return null;
    }
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    if(protocolHandlerService != null) {
      protocolHandlerService.unregister();
    }
    context.removeBundleListener(bundleListener);

    this.mavenBackgroundJob.cancel();
    try {
      this.mavenBackgroundJob.join();
    } catch(InterruptedException ex) {
      // ignored
    }
    workspaceTracker.close();
    this.mavenBackgroundJob = null;

    this.projectManager.removeMavenProjectChangedListener(this.configurationManager);
    this.projectManager.removeMavenProjectChangedListener(repositoryRegistry);
    this.projectManager = null;

    toDisposeContainers.forEach(PlexusContainer::dispose);
    this.maven.disposeContainer();

    this.configurationManager = null;
    LifecycleMappingFactory.setBundleMetadataSources(null);

    this.projectConversionManager = null;

    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static MavenPluginActivator getDefault() {
    return plugin;
  }

  public MavenModelManager getMavenModelManager() {
    return this.modelManager;
  }

  public MavenProjectManager getMavenProjectManager() {
    return this.projectManager;
  }

  public ProjectRegistryManager getMavenProjectManagerImpl() {
    return this.managerImpl;
  }

  public MavenRuntimeManagerImpl getMavenRuntimeManager() {
    return this.runtimeManager;
  }

  public ArchetypeManager getArchetypeManager() {
    synchronized(this) {
      if(this.archetypeManager == null) {
        try {
          PlexusContainer archetyperContainer = newPlexusContainer(ArchetypeGenerationRequest.class.getClassLoader());
          // TODO this is broken, need to make it lazy, otherwise we'll deadlock or timeout... or both
          this.archetypeManager = newArchetypeManager(archetyperContainer, getStateLocation().toFile());
          try {
            this.archetypeManager.readCatalogs();
          } catch(Exception ex) {
            String msg = "Can't read archetype catalog configuration";
            log.error(msg, ex);
          }
        } catch(PlexusContainerException ex1) {
          log.error("Failed to initialize the ArchetypeManager", ex1);
        }
      }
    }
    return this.archetypeManager;
  }

  public IMavenMarkerManager getMavenMarkerManager() {
    return this.mavenMarkerManager;
  }

  public IMavenConfiguration getMavenConfiguration() {
    return this.mavenConfiguration;
  }

  public BundleContext getBundleContext() {
    return this.bundleContext;
  }

  public IProjectConfigurationManager getProjectConfigurationManager() {
    return configurationManager;
  }

  /** for use by unit tests */
  public ProjectRegistryRefreshJob getProjectManagerRefreshJob() {
    return mavenBackgroundJob;
  }

  public static String getVersion() {
    return plugin.version;
  }

  public static String getUserAgent() {
    // cast is necessary for eclipse 3.6 compatibility
    Bundle m2eCore = FrameworkUtil.getBundle(MavenPluginActivator.class);
    Version osgiVersion = m2eCore.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getVersion();
    String javaVersion = System.getProperty("java.version", "unknown"); //$NON-NLS-1$ $NON-NLS-1$
    return "m2e/" + osgiVersion + "/" + m2eCore.getVersion() + "/" + javaVersion; //$NON-NLS-1$ $NON-NLS-1$
  }

  public IRepositoryRegistry getRepositoryRegistry() {
    return repositoryRegistry;
  }

  /**
   * @deprecated use {@link ArchetypeManager#getArchetypeDataSource(String)}
   */
  @Deprecated
  public ArchetypeDataSource getArchetypeDataSource(String hint) {
    return getArchetypeManager().getArchetypeDataSource(hint);
  }

  /**
   * @deprecated use {@link ArchetypeManager#getArchetypeArtifactManager()}
   */
  @Deprecated
  public ArchetypeArtifactManager getArchetypeArtifactManager() {
    return getArchetypeManager().getArchetypeArtifactManager();
  }

  public WagonManager getWagonManager() {
    return maven.lookupComponent(WagonManager.class);
  }

  public ArtifactFactory getArtifactFactory() {
    return maven.lookupComponent(ArtifactFactory.class);
  }

  public ArtifactMetadataSource getArtifactMetadataSource() {
    return maven.lookupComponent(ArtifactMetadataSource.class);
  }

  public ArtifactCollector getArtifactCollector() {
    return maven.lookupComponent(ArtifactCollector.class);
  }

  public RepositorySystem getRepositorySystem() {
    return maven.lookupComponent(RepositorySystem.class);
  }

  /**
   * @deprecated use {@link IMavenExecutionContext} instead.
   */
  @Deprecated
  public MavenSession setSession(MavenSession session) {
    LegacySupport legacy = maven.lookupComponent(LegacySupport.class);
    MavenSession old = legacy.getSession();
    legacy.setSession(session);
    return old;
  }

  public ArtifactFilterManager getArifactFilterManager() {
    return artifactFilterManager;
  }

  /**
   * @return
   */
  public IProjectConversionManager getProjectConversionManager() {
    return projectConversionManager;
  }

  public IWorkspaceClassifierResolverManager getWorkspaceClassifierResolverManager() {
    return workspaceClassifierResolverManager;
  }

}
