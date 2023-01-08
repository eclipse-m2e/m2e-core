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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.progress.UIJob;

import org.codehaus.plexus.util.FileUtils;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.jobs.MavenJob;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypePlugin;
import org.eclipse.m2e.core.ui.internal.console.MavenConsoleImpl;
import org.eclipse.m2e.core.ui.internal.project.MavenUpdateConfigurationChangeListener;
import org.eclipse.m2e.core.ui.internal.search.util.SearchEngine;
import org.eclipse.m2e.core.ui.internal.wizards.IMavenDiscoveryUI;


public class M2EUIPluginActivator extends AbstractUIPlugin {

  public static final String PREFS_ARCHETYPES = "archetypesInfo.xml"; //$NON-NLS-1$

  public static final String PLUGIN_ID = "org.eclipse.m2e.core.ui"; //$NON-NLS-1$

  private static M2EUIPluginActivator instance;

  private ServiceTracker<ArchetypePlugin, ArchetypePlugin> archetypeManager;

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

  private BundleContext context;

  @Override
  public void start(BundleContext context) throws Exception {
    this.context = context;
    super.start(context);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    mavenUpdateConfigurationChangeListener = new MavenUpdateConfigurationChangeListener();
    workspace.addResourceChangeListener(mavenUpdateConfigurationChangeListener, IResourceChangeEvent.POST_CHANGE);

    // Automatically delete obsolete caches
    // TODO: can be removed when some time has passed and it is unlikely old workspaces that need clean-up are used.
    MavenPluginActivator mavenPlugin = MavenPluginActivator.getDefault();
    IPath nexusCache = Platform.getStateLocation(mavenPlugin.getBundle()).append("nexus");
    FileUtils.deleteDirectory(nexusCache.toFile());

    File localRepo = mavenPlugin.getRepositoryRegistry().getLocalRepository().getBasedir();
    Path m2eCache = localRepo.toPath().resolve(".cache/m2e/");
    if(Files.isDirectory(m2eCache)) {
      deleteLegacyCacheDirectory(m2eCache);
    }
    // use a custom icon in the progress user interface
    PlatformUI.getWorkbench().getProgressService().registerIconForFamily(MavenImages.M2, MavenJob.FAMILY_M2);
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

  public ArchetypePlugin getArchetypePlugin() {
    synchronized(this) {
      if(this.archetypeManager == null) {
        archetypeManager = new ServiceTracker<>(context, ArchetypePlugin.class, null);
        archetypeManager.open();
      }
    }
    return this.archetypeManager.getService();
  }

  private static void deleteLegacyCacheDirectory(Path m2eCache) {
    Path resolve = m2eCache.resolve("DELETE_ME.txt");
    if(Files.isRegularFile(resolve) || Boolean.parseBoolean(System.getProperty("m2e.keep.legacy.cache"))) {
      return;
    }
    new UIJob("Delete legacy M2E cache") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        boolean[] askAgain = new boolean[] {true};
        MessageDialog dialog = new MessageDialog(getDisplay().getActiveShell(), "Delete obsolete M2E cache?", null,
            "A cache directory used by previous M2E versions was detected:\n\n" + m2eCache + "\n\n"
                + "It's no longer used by newer M2E versions and, unless older Eclipse installations need it, can be safely deleted.",
            MessageDialog.QUESTION, 0, "Keep Cache", "Delete Cache") {
          @Override
          protected Control createCustomArea(Composite parent) {
            Button checkbox = new Button(parent, SWT.CHECK);
            checkbox.setText("Don't ask me again?");
            checkbox.addSelectionListener(
                SelectionListener.widgetSelectedAdapter(e -> askAgain[0] = !checkbox.getSelection()));
            return super.createCustomArea(parent);
          }
        };
        int selection = dialog.open();
        if(selection == 1) {
          try {
            FileUtils.deleteDirectory(m2eCache.toFile());
          } catch(IOException e) {
            return Status.error("Failed to delete legacy M2E cache", e);
          }
        } else if(!askAgain[0]) {
          try {
            Files.writeString(resolve,
                """
                    This cache directory was created by a previous Maven2Eclipse 1.x version and is no longer used since M2E 2.0.
                    Unless older Eclipse installations need it, it can be deleted safely."
                    """);
          } catch(IOException ex) { // Ignore
          }
        }
        return Status.OK_STATUS;
      }
    }.schedule(10_000);
  }

}
