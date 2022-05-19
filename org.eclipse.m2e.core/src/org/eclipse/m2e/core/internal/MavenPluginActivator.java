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
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import org.eclipse.aether.RepositorySystem;
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
import org.apache.maven.project.DefaultProjectBuilder;

import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IWorkspaceClassifierResolverManager;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


public class MavenPluginActivator extends Plugin {
  private final Logger log = LoggerFactory.getLogger(MavenPluginActivator.class);

  public static final String PREFS_ARCHETYPES = "archetypesInfo.xml"; //$NON-NLS-1$

  // The shared instance
  private static MavenPluginActivator plugin;

  private final Collection<PlexusContainer> toDisposeContainers = new HashSet<>();

  private BundleContext bundleContext;

  private ArchetypeManager archetypeManager;

  private String version = "0.0.0"; //$NON-NLS-1$

  private final BundleListener bundleListener = event -> LifecycleMappingFactory.setBundleMetadataSources(null);

  private ServiceRegistration<URLStreamHandlerService> protocolHandlerService;

  private Map<Class<?>, ServiceTracker<?, ?>> trackers = new ConcurrentHashMap<>();

  public MavenPluginActivator() {
    plugin = this;

    if(Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/initialization"))) { //$NON-NLS-1$
      System.err.println("### executing constructor " + IMavenConstants.PLUGIN_ID); //$NON-NLS-1$
      new Throwable().printStackTrace();
    }
  }

  public IMaven getMaven() {
    return getService(IMaven.class);
  }

  /**
   * @param class1
   * @return
   */
  private <T> T getService(Class<T> service) {
    BundleContext context = getBundleContext();
    if(context == null) {
      return null;
    }
    return service.cast(trackers.computeIfAbsent(service, key -> {
      ServiceTracker<?, ?> tracker = new ServiceTracker<>(context, key, null);
      tracker.open();
      return tracker;
    }).getService());
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);
    if(Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/initialization"))) { //$NON-NLS-1$
      System.err.println("### executing start() " + IMavenConstants.PLUGIN_ID); //$NON-NLS-1$
      Thread.dumpStack();
    }
    try {
      Version bundleVersion = getBundle().getVersion();
      this.version = bundleVersion.getMajor() + "." + bundleVersion.getMinor() + "." + bundleVersion.getMicro(); //$NON-NLS-1$ //$NON-NLS-2$
    } catch(IllegalArgumentException e) {
      // ignored
    }

    // Workaround MNG-6530
    System.setProperty(DefaultProjectBuilder.DISABLE_GLOBAL_MODEL_CACHE_SYSTEM_PROPERTY, Boolean.toString(true));
    URLConnectionCaches.disable();
    // For static access, this also enables any of the services and keep them running forever...
    this.bundleContext = context;
    //register URL handler, we can't use DS here because this triggers loading of m2e too early
    Map<String, Object> properties = Map.of(URLConstants.URL_HANDLER_PROTOCOL, new String[] {"mvn"});
    this.protocolHandlerService = context.registerService(URLStreamHandlerService.class,
        new MvnProtocolHandlerService(), FrameworkUtil.asDictionary(properties));

    // Automatically delete now obsolete nexus cache (can be removed again if some time has passed and it is unlikely an old workspace that need to be cleaned up is used).
    IPath nexusCache = Platform.getStateLocation(context.getBundle()).append("nexus");
    FileUtils.deleteDirectory(nexusCache.toFile());
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
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    if(protocolHandlerService != null) {
      protocolHandlerService.unregister();
    }
    context.removeBundleListener(bundleListener);

    toDisposeContainers.forEach(PlexusContainer::dispose);

    LifecycleMappingFactory.setBundleMetadataSources(null);

    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static MavenPluginActivator getDefault() {
    return plugin;
  }

  public MavenModelManager getMavenModelManager() {
    return getService(MavenModelManager.class);
  }

  public IMavenProjectRegistry getMavenProjectManager() {
    return getService(IMavenProjectRegistry.class);
  }

  public ProjectRegistryManager getMavenProjectManagerImpl() {
    return getService(ProjectRegistryManager.class);
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
    return getService(IMavenMarkerManager.class);
  }

  public IMavenConfiguration getMavenConfiguration() {
    return getService(IMavenConfiguration.class);
  }

  public BundleContext getBundleContext() {
    return this.bundleContext;
  }

  public IProjectConfigurationManager getProjectConfigurationManager() {
    return getService(IProjectConfigurationManager.class);
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
    return getService(IRepositoryRegistry.class);
  }

  public RepositorySystem getRepositorySystem() throws CoreException {
    return getMaven().lookup(RepositorySystem.class);
  }

  /**
   * @return
   */
  public IProjectConversionManager getProjectConversionManager() {
    return getService(IProjectConversionManager.class);
  }

  public IWorkspaceClassifierResolverManager getWorkspaceClassifierResolverManager() {
    return getService(IWorkspaceClassifierResolverManager.class);
  }

}
