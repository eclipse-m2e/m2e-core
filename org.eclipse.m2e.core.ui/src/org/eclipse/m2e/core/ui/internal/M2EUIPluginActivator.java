/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Fred Bricon (Red Hat, Inc.) - auto update project configuration
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal;

import java.io.File;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;

import org.apache.maven.archetype.ArchetypeGenerationRequest;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeGenerator;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.core.ui.internal.archetype.ExtensionReader;
import org.eclipse.m2e.core.ui.internal.console.MavenConsoleImpl;
import org.eclipse.m2e.core.ui.internal.project.MavenUpdateConfigurationChangeListener;
import org.eclipse.m2e.core.ui.internal.search.util.SearchEngine;
import org.eclipse.m2e.core.ui.internal.wizards.IMavenDiscoveryUI;


public class M2EUIPluginActivator extends AbstractUIPlugin {

  private final Logger log = LoggerFactory.getLogger(M2EUIPluginActivator.class);

  public static final String PREFS_ARCHETYPES = "archetypesInfo.xml"; //$NON-NLS-1$

  public static final String PLUGIN_ID = "org.eclipse.m2e.core.ui"; //$NON-NLS-1$

  private static M2EUIPluginActivator instance;

  private ArchetypeManager archetypeManager;

  /**
   * Storage for preferences.
   */
  private ScopedPreferenceStore preferenceStore;

  public M2EUIPluginActivator() {
    M2EUIPluginActivator.instance = this;
  }

  @Override
  public IPreferenceStore getPreferenceStore() {
    // Create the preference store lazily.
    if(preferenceStore == null) {
      // InstanceScope.INSTANCE added in 3.7
      preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, IMavenConstants.PLUGIN_ID);

    }
    return preferenceStore;
  }

  private MavenConsoleImpl console;

  private MavenUpdateConfigurationChangeListener mavenUpdateConfigurationChangeListener;

  public static final String PROP_SHOW_EXPERIMENTAL_FEATURES = "m2e.showExperimentalFeatures";

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    mavenUpdateConfigurationChangeListener = new MavenUpdateConfigurationChangeListener();
    workspace.addResourceChangeListener(mavenUpdateConfigurationChangeListener, IResourceChangeEvent.POST_CHANGE);

  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.removeResourceChangeListener(this.mavenUpdateConfigurationChangeListener);
    this.mavenUpdateConfigurationChangeListener = null;

  }

  public static M2EUIPluginActivator getDefault() {
    return instance;
  }

  /**
   * Returns an Image for the file at the given relative path.
   */
  public static Image getImage(String path) {
    ImageRegistry registry = getDefault().getImageRegistry();
    Image image = registry.get(path);
    if(image == null) {
      registry.put(path, ResourceLocator.imageDescriptorFromBundle(IMavenConstants.PLUGIN_ID, path).get());
      image = registry.get(path);
    }
    return image;
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return ResourceLocator.imageDescriptorFromBundle(IMavenConstants.PLUGIN_ID, path).get();
  }

  public synchronized MavenConsoleImpl getMavenConsole() {
    if(console == null) {
      console = new MavenConsoleImpl(MavenImages.M2);
    }
    return console;
  }

  public boolean hasMavenConsoleImpl() {
    return console != null;
  }

  public SearchEngine getSearchEngine(IProject project) {
    return null; // used to be only Index based search, need to hook other engines
  }

  public synchronized IMavenDiscovery getMavenDiscovery() {
    // TODO this leaks service references
    BundleContext context = getBundle().getBundleContext();
    ServiceReference<IMavenDiscovery> serviceReference = context.getServiceReference(IMavenDiscovery.class);
    if(serviceReference != null) {
      return context.getService(serviceReference);
    }
    return null;
  }

  /**
   * @param discovery
   */
  public void ungetMavenDiscovery(IMavenDiscovery discovery) {
    // TODO Auto-generated method ungetMavenDiscovery

  }

  /**
   * @return
   */
  public IMavenDiscoveryUI getImportWizardPageFactory() {
    // TODO this leaks service references
    BundleContext context = getBundle().getBundleContext();
    ServiceReference<IMavenDiscoveryUI> serviceReference = context.getServiceReference(IMavenDiscoveryUI.class);
    if(serviceReference != null) {
      return context.getService(serviceReference);
    }
    return null;
  }

  public static boolean showExperimentalFeatures() {
    return Boolean.parseBoolean(System.getProperty(PROP_SHOW_EXPERIMENTAL_FEATURES));
  }

  public ArchetypeManager getArchetypeManager() {
    synchronized(this) {
      if(this.archetypeManager == null) {
        try {
          PlexusContainer archetyperContainer = newPlexusContainer(ArchetypeGenerationRequest.class.getClassLoader());
          this.archetypeManager = newArchetypeManager(archetyperContainer,
              MavenPluginActivator.getDefault().getStateLocation().toFile());
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

  private ArchetypeManager newArchetypeManager(PlexusContainer container, File stateLocationDir) {
    ServiceTracker<ArchetypeGenerator, ArchetypeGenerator> tracker = new ServiceTracker<ArchetypeGenerator, ArchetypeGenerator>(getBundle().getBundleContext(),
        ArchetypeGenerator.class, null);
    tracker.open();
    ArchetypeManager archetypeManager = new ArchetypeManager(container, new File(stateLocationDir, PREFS_ARCHETYPES),
        tracker);
    archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.InternalCatalogFactory());
    archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.DefaultLocalCatalogFactory());
    for(ArchetypeCatalogFactory archetypeCatalogFactory : ExtensionReader.readArchetypeExtensions()) {
      archetypeManager.addArchetypeCatalogFactory(archetypeCatalogFactory);
    }
    return archetypeManager;
  }
}
