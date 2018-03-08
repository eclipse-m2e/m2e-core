/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Fred Bricon (Red Hat, Inc.) - auto update project configuration
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.index.filter.FilteredIndex;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.ui.internal.console.MavenConsoleImpl;
import org.eclipse.m2e.core.ui.internal.project.MavenUpdateConfigurationChangeListener;
import org.eclipse.m2e.core.ui.internal.search.util.IndexSearchEngine;
import org.eclipse.m2e.core.ui.internal.search.util.SearchEngine;
import org.eclipse.m2e.core.ui.internal.wizards.IMavenDiscoveryUI;


@SuppressWarnings("restriction")
public class M2EUIPluginActivator extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.eclipse.m2e.core.ui"; //$NON-NLS-1$

  private static M2EUIPluginActivator instance;

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
      preferenceStore = new ScopedPreferenceStore(new InstanceScope(), IMavenConstants.PLUGIN_ID);

    }
    return preferenceStore;
  }

  private MavenConsoleImpl console;

  private MavenUpdateConfigurationChangeListener mavenUpdateConfigurationChangeListener;

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
      registry.put(path, imageDescriptorFromPlugin(IMavenConstants.PLUGIN_ID, path));
      image = registry.get(path);
    }
    return image;
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(IMavenConstants.PLUGIN_ID, path);
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

  public SearchEngine getSearchEngine(IProject project) throws CoreException {
    return new IndexSearchEngine(new FilteredIndex(project, MavenPlugin.getIndexManager().getIndex(project)));
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
}
