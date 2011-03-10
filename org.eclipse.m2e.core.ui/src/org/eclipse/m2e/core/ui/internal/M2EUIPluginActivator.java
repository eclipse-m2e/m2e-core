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

package org.eclipse.m2e.core.ui.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.ui.internal.console.MavenConsoleImpl;
import org.eclipse.m2e.core.ui.internal.search.util.IndexSearchEngine;
import org.eclipse.m2e.core.ui.internal.search.util.SearchEngine;
import org.eclipse.m2e.core.ui.internal.wizards.IImportWizardPageFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


@SuppressWarnings("restriction")
public class M2EUIPluginActivator extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.eclipse.m2e.core.ui"; //$NON-NLS-1$

  private static M2EUIPluginActivator instance;

  public M2EUIPluginActivator() {
    M2EUIPluginActivator.instance = this;
  }

  private MavenConsoleImpl console;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
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
    return new IndexSearchEngine(MavenPlugin.getDefault().getIndexManager().getIndex(project));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public synchronized IMavenDiscovery getMavenDiscovery() {
    // TODO this leaks service references
    BundleContext context = getBundle().getBundleContext();
    ServiceReference serviceReference = context.getServiceReference(IMavenDiscovery.class.getName());
    if(serviceReference != null) {
      return (IMavenDiscovery) context.getService(serviceReference);
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
  public IImportWizardPageFactory getImportWizardPageFactory() {
    // TODO this leaks service references
    BundleContext context = getBundle().getBundleContext();
    ServiceReference serviceReference = context.getServiceReference(IImportWizardPageFactory.class.getName());
    if(serviceReference != null) {
      return (IImportWizardPageFactory) context.getService(serviceReference);
    }
    return null;
  }
}
