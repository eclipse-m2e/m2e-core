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

import org.eclipse.aether.RepositorySystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.project.DefaultProjectBuilder;

import org.eclipse.m2e.core.ExtendedServiceTracker;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.index.filter.ArtifactFilterManager;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IWorkspaceClassifierResolverManager;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


public class MavenPluginActivator extends Plugin {

  // The shared instance
  private static MavenPluginActivator plugin;

  private final Collection<PlexusContainer> toDisposeContainers = new HashSet<>();

  private BundleContext bundleContext;

  private static String version = "0.0.0"; //$NON-NLS-1$

  private final BundleListener bundleListener = event -> LifecycleMappingFactory.setBundleMetadataSources(null);

  private ServiceRegistration<URLStreamHandlerService> protocolHandlerService;

  private Map<Class<?>, ExtendedServiceTracker<?>> trackers = new ConcurrentHashMap<>();

  public IMaven getMaven() {
    return getService(IMaven.class);
  }

  /**
   * @param class1
   * @return the service
   * @throws IllegalStateException in case the service could no be retrieved together with a meaningful message
   */
  private <T> T getService(Class<T> service) {
    BundleContext context = getBundleContext();
    if(context == null) {
      return null;
    }
    return service.cast(trackers.computeIfAbsent(service, key -> {
      ExtendedServiceTracker<?> tracker = new ExtendedServiceTracker(context, key);
      return tracker;
    }).getNotNull());
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);
    plugin = this;

    try {
      Version bundleVersion = getBundle().getVersion();
      version = bundleVersion.getMajor() + "." + bundleVersion.getMinor() + "." + bundleVersion.getMicro(); //$NON-NLS-1$ //$NON-NLS-2$
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

  public MavenRuntimeManagerImpl getMavenRuntimeManager() {
    return getService(MavenRuntimeManagerImpl.class);
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

  /** for use by unit tests */
  public ProjectRegistryRefreshJob getProjectManagerRefreshJob() {
    return getService(ProjectRegistryRefreshJob.class);
  }

  public static String getVersion() {
    return version;
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

  public ArtifactFilterManager getArifactFilterManager() {
    return getService(ArtifactFilterManager.class);
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
