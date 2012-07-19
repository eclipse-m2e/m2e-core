/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.io.File;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.archetype.Archetype;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.index.ArtifactContextProducer;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.plugin.LegacySupport;

import org.sonatype.aether.RepositorySystem;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.core.internal.embedder.MavenEmbeddedRuntime;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.embedder.MavenWorkspaceRuntime;
import org.eclipse.m2e.core.internal.index.IndexManager;
import org.eclipse.m2e.core.internal.index.filter.ArtifactFilterManager;
import org.eclipse.m2e.core.internal.index.nexus.IndexesExtensionReader;
import org.eclipse.m2e.core.internal.index.nexus.IndexingTransferListener;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndexManager;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.MavenMarkerManager;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.internal.project.WorkspaceStateWriter;
import org.eclipse.m2e.core.internal.project.conversion.ProjectConversionManager;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


public class MavenPluginActivator extends Plugin {
  private final Logger log = LoggerFactory.getLogger(MavenPlugin.class);

  public static final String PREFS_ARCHETYPES = "archetypesInfo.xml"; //$NON-NLS-1$

  // The shared instance
  private static MavenPluginActivator plugin;

  /**
   * General purpose plexus container. Contains components from maven embedder and all other bundles visible from this
   * bundle's classloader.
   */
  private MutablePlexusContainer plexus;

  private MavenModelManager modelManager;

  private NexusIndexManager indexManager;

  private BundleContext bundleContext;

  private MavenProjectManager projectManager;

  private MavenRuntimeManager runtimeManager;

  private ProjectConfigurationManager configurationManager;

  private ProjectRegistryRefreshJob mavenBackgroundJob;

  private ArchetypeManager archetypeManager;

  private ProjectRegistryManager managerImpl;

  private IMavenMarkerManager mavenMarkerManager;

  private RepositoryRegistry repositoryRegistry;

  private ArtifactFilterManager artifactFilterManager;

  private String version = "0.0.0"; //$NON-NLS-1$

  private String qualifiedVersion = "0.0.0.qualifier"; //$NON-NLS-1$

  private IMavenConfiguration mavenConfiguration;

  private BundleListener bundleListener = new BundleListener() {

    public void bundleChanged(BundleEvent event) {
      LifecycleMappingFactory.setBundleMetadataSources(null);
    }
  };

  private MavenImpl maven;

  private IProjectConversionManager projectConversionManager;

  public MavenPluginActivator() {
    plugin = this;

    if(Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/initialization"))) { //$NON-NLS-1$
      System.err.println("### executing constructor " + IMavenConstants.PLUGIN_ID); //$NON-NLS-1$
      new Throwable().printStackTrace();
    }
  }

  public IMaven getMaven() {
    return maven;
  }

  /**
   * This method is called upon plug-in activation
   */
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    if(Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/initialization"))) { //$NON-NLS-1$
      System.err.println("### executing start() " + IMavenConstants.PLUGIN_ID); //$NON-NLS-1$
      new Throwable().printStackTrace();
    }

    this.bundleContext = context;

    try {
      this.qualifiedVersion = (String) getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
      Version bundleVersion = Version.parseVersion(this.qualifiedVersion);
      this.version = bundleVersion.getMajor() + "." + bundleVersion.getMinor() + "." + bundleVersion.getMicro(); //$NON-NLS-1$ //$NON-NLS-2$
    } catch(IllegalArgumentException e) {
      // ignored
    }

    this.mavenConfiguration = new MavenConfigurationImpl();

    ClassLoader cl = MavenPlugin.class.getClassLoader();
    ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld(new ClassWorld("plexus.core", cl)) //$NON-NLS-1$
        .setName("plexus"); //$NON-NLS-1$
    this.plexus = new DefaultPlexusContainer(cc);

    File stateLocationDir = getStateLocation().toFile();

    // TODO this is broken, need to make it lazy, otherwise we'll deadlock or timeout... or both 
    this.archetypeManager = newArchetypeManager(stateLocationDir);
    try {
      this.archetypeManager.readCatalogs();
    } catch(Exception ex) {
      String msg = "Can't read archetype catalog configuration";
      log.error(msg, ex);
    }

    this.mavenMarkerManager = new MavenMarkerManager(mavenConfiguration);

    boolean updateProjectsOnStartup = mavenConfiguration.isUpdateProjectsOnStartup();

    ////////////////////////////////////////////////////////////////////////////////////////////////

    this.maven = new MavenImpl(mavenConfiguration);

    // TODO eagerly reads workspace state cache
    this.managerImpl = new ProjectRegistryManager(maven, stateLocationDir, !updateProjectsOnStartup /* readState */,
        mavenMarkerManager);

    this.mavenBackgroundJob = new ProjectRegistryRefreshJob(managerImpl, mavenConfiguration);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.addResourceChangeListener(mavenBackgroundJob, IResourceChangeEvent.POST_CHANGE
        | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);

    this.projectManager = new MavenProjectManager(managerImpl, mavenBackgroundJob, stateLocationDir);
    this.projectManager.addMavenProjectChangedListener(new WorkspaceStateWriter(projectManager));
    if(updateProjectsOnStartup || managerImpl.getProjects().length == 0) {
      this.projectManager.refresh(new MavenUpdateRequest(workspace.getRoot().getProjects(), //
          mavenConfiguration.isOffline() /*offline*/, false /* updateSnapshots */));
    }

    this.modelManager = new MavenModelManager(maven, projectManager);

    this.runtimeManager = new MavenRuntimeManager();
    this.runtimeManager.setEmbeddedRuntime(new MavenEmbeddedRuntime(getBundleContext()));
    this.runtimeManager.setWorkspaceRuntime(new MavenWorkspaceRuntime(projectManager));

    this.configurationManager = new ProjectConfigurationManager(maven, managerImpl, modelManager, mavenMarkerManager,
        mavenConfiguration);
    this.projectManager.addMavenProjectChangedListener(this.configurationManager);
    workspace.addResourceChangeListener(configurationManager, IResourceChangeEvent.PRE_DELETE);

    //create repository registry
    this.repositoryRegistry = new RepositoryRegistry(maven, projectManager);
    this.maven.addSettingsChangeListener(repositoryRegistry);
    this.projectManager.addMavenProjectChangedListener(repositoryRegistry);

    //create the index manager
    this.indexManager = new NexusIndexManager(projectManager, repositoryRegistry, stateLocationDir);
    this.projectManager.addMavenProjectChangedListener(indexManager);
    this.maven.addLocalRepositoryListener(new IndexingTransferListener(indexManager));
    this.repositoryRegistry.addRepositoryIndexer(indexManager);
    this.repositoryRegistry.addRepositoryDiscoverer(new IndexesExtensionReader(indexManager));
    context.addBundleListener(bundleListener);

    //
    this.artifactFilterManager = new ArtifactFilterManager();

    // fork repository registry update. must after index manager registered as a listener
    this.repositoryRegistry.updateRegistry();
    
    this.projectConversionManager = new ProjectConversionManager();
  }

  private static ArchetypeManager newArchetypeManager(File stateLocationDir) {
    ArchetypeManager archetypeManager = new ArchetypeManager(new File(stateLocationDir, PREFS_ARCHETYPES));
    archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.NexusIndexerCatalogFactory());
    archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.InternalCatalogFactory());
    archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.DefaultLocalCatalogFactory());
    for(ArchetypeCatalogFactory archetypeCatalogFactory : ExtensionReader.readArchetypeExtensions()) {
      archetypeManager.addArchetypeCatalogFactory(archetypeCatalogFactory);
    }
    return archetypeManager;
  }

  public PlexusContainer getPlexusContainer() {
    return plexus;
  }

  /**
   * This method is called when the plug-in is stopped
   */
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    this.managerImpl.writeWorkspaceState();
    context.removeBundleListener(bundleListener);

    this.mavenBackgroundJob.cancel();
    try {
      this.mavenBackgroundJob.join();
    } catch(InterruptedException ex) {
      // ignored
    }
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.removeResourceChangeListener(this.mavenBackgroundJob);
    this.mavenBackgroundJob = null;

    this.projectManager.removeMavenProjectChangedListener(this.configurationManager);
    this.projectManager.removeMavenProjectChangedListener(indexManager);
    this.projectManager.removeMavenProjectChangedListener(repositoryRegistry);
    this.projectManager = null;

    this.plexus.dispose();
    this.maven.disposeContainer();

    workspace.removeResourceChangeListener(configurationManager);
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

  public IndexManager getIndexManager() {
    return this.indexManager;
  }

  public MavenRuntimeManager getMavenRuntimeManager() {
    return this.runtimeManager;
  }

  public ArchetypeManager getArchetypeManager() {
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

  private <C> C lookup(Class<C> role) {
    try {
      return plexus.lookup(role);
    } catch(ComponentLookupException ex) {
      throw new NoSuchComponentException(ex);
    }
  }

  private <T> T lookup(Class<T> role, String roleHint) {
    try {
      return plexus.lookup(role, roleHint);
    } catch(ComponentLookupException ex) {
      throw new NoSuchComponentException(ex);
    }
  }

  public static String getVersion() {
    return plugin.version;
  }

  public static String getQualifiedVersion() {
    return plugin.qualifiedVersion;
  }

  public static String getUserAgent() {
    // cast is necessary for eclipse 3.6 compatibility
    String osgiVersion = (String) Platform.getBundle("org.eclipse.osgi").getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION); //$NON-NLS-1$
    String m2eVersion = plugin.qualifiedVersion;
    return "m2e/" + osgiVersion + "/" + m2eVersion; //$NON-NLS-1$
  }

  public IRepositoryRegistry getRepositoryRegistry() {
    return repositoryRegistry;
  }

  public Archetype getArchetype() {
    return lookup(Archetype.class);
  }

  public ArchetypeDataSource getArchetypeDataSource(String hint) {
    return lookup(ArchetypeDataSource.class, hint);
  }

  public ArchetypeArtifactManager getArchetypeArtifactManager() {
    return lookup(ArchetypeArtifactManager.class);
  }

  public IndexUpdater getIndexUpdater() {
    return lookup(IndexUpdater.class);
  }

  public WagonManager getWagonManager() {
    return lookup(WagonManager.class);
  }

  public NexusIndexer getNexusIndexer() {
    return lookup(NexusIndexer.class);
  }

  public ArtifactContextProducer getArtifactContextProducer() {
    return lookup(ArtifactContextProducer.class);
  }

  public ArtifactFactory getArtifactFactory() {
    return lookup(ArtifactFactory.class);
  }

  public ArtifactMetadataSource getArtifactMetadataSource() {
    return lookup(ArtifactMetadataSource.class);
  }

  public ArtifactCollector getArtifactCollector() {
    return lookup(ArtifactCollector.class);
  }

  public RepositorySystem getRepositorySystem() {
    return lookup(RepositorySystem.class);
  }

  public MavenSession setSession(MavenSession session) {
    LegacySupport legacy = lookup(LegacySupport.class);
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
}
