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

package org.eclipse.m2e.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

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

import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeManager;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.core.internal.ExtensionReader;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.console.MavenConsoleImpl;
import org.eclipse.m2e.core.internal.embedder.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.embedder.MavenEmbeddedRuntime;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.embedder.MavenWorkspaceRuntime;
import org.eclipse.m2e.core.internal.index.IndexesExtensionReader;
import org.eclipse.m2e.core.internal.index.IndexingTransferListener;
import org.eclipse.m2e.core.internal.index.NexusIndexManager;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.internal.project.MavenMarkerManager;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.internal.project.WorkspaceStateWriter;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.project.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;
import org.eclipse.m2e.core.util.search.IndexSearchEngine;
import org.eclipse.m2e.core.util.search.SearchEngine;


/**
 * MavenPlugin main plug-in class.
 */
public class MavenPlugin extends AbstractUIPlugin implements IStartup {

  // preferences
  private static final String PREFS_ARCHETYPES = "archetypesInfo.xml"; //$NON-NLS-1$

  // The shared instance
  private static MavenPlugin plugin;

  /**
   * General purpose plexus container. Contains components from maven embedder and all other bundles visible from this
   * bundle's classloader.
   */
  private MutablePlexusContainer plexus;

  private MavenConsole console;

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

  private String version = "0.0.0"; //$NON-NLS-1$

  private String qualifiedVersion = "0.0.0.qualifier"; //$NON-NLS-1$

  private IMavenConfiguration mavenConfiguration;

  private MavenImpl maven;

  public MavenPlugin() {
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

    MavenLogger.setLog(getLog());

    try {
      this.console = new MavenConsoleImpl(MavenImages.M2); //$NON-NLS-1$
    } catch(RuntimeException ex) {
      MavenLogger.log(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Unable to start console: "
          + ex.toString(), ex));
    }

    this.mavenConfiguration = new MavenConfigurationImpl(getPreferenceStore());

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
      this.console.logError(msg + "; " + ex.getMessage()); //$NON-NLS-1$
      MavenLogger.log(msg, ex);
    }

    this.mavenMarkerManager = new MavenMarkerManager(console, mavenConfiguration);

    boolean updateProjectsOnStartup = mavenConfiguration.isUpdateProjectsOnStartup();

    ////////////////////////////////////////////////////////////////////////////////////////////////

    this.maven = new MavenImpl(mavenConfiguration, console);

    // TODO eagerly reads workspace state cache
    this.managerImpl = new ProjectRegistryManager(maven, console, stateLocationDir,
        !updateProjectsOnStartup /* readState */, mavenMarkerManager);

    this.mavenBackgroundJob = new ProjectRegistryRefreshJob(managerImpl, console, mavenConfiguration);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.addResourceChangeListener(mavenBackgroundJob, IResourceChangeEvent.POST_CHANGE
        | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);

    this.projectManager = new MavenProjectManager(managerImpl, mavenBackgroundJob, stateLocationDir);
    this.projectManager.addMavenProjectChangedListener(new WorkspaceStateWriter(projectManager));
    if(updateProjectsOnStartup || managerImpl.getProjects().length == 0) {
      this.projectManager.refresh(new MavenUpdateRequest(workspace.getRoot().getProjects(), //
          mavenConfiguration.isOffline() /*offline*/, false /* updateSnapshots */));
    }

    this.modelManager = new MavenModelManager(maven, projectManager, console);

    this.runtimeManager = new MavenRuntimeManager(getPreferenceStore());
    this.runtimeManager.setEmbeddedRuntime(new MavenEmbeddedRuntime(getBundleContext()));
    this.runtimeManager.setWorkspaceRuntime(new MavenWorkspaceRuntime(projectManager));

    this.configurationManager = new ProjectConfigurationManager(maven, console, projectManager, modelManager,
        mavenMarkerManager, mavenConfiguration);
    this.projectManager.addMavenProjectChangedListener(this.configurationManager);

    //create repository registry
    this.repositoryRegistry = new RepositoryRegistry(maven, projectManager);
    this.maven.addSettingsChangeListener(repositoryRegistry);
    this.projectManager.addMavenProjectChangedListener(repositoryRegistry);

    //create the index manager
    this.indexManager = new NexusIndexManager(console, projectManager, repositoryRegistry, stateLocationDir);
    this.projectManager.addMavenProjectChangedListener(indexManager);
    this.maven.addLocalRepositoryListener(new IndexingTransferListener(indexManager));
    this.repositoryRegistry.addRepositoryIndexer(indexManager);
    this.repositoryRegistry.addRepositoryDiscoverer(new IndexesExtensionReader(indexManager));

    // fork repository registry update. must after index manager registered as a listener
    this.repositoryRegistry.updateRegistry();

    checkJdk();
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

  public void earlyStartup() {
    // nothing to do here, all startup work is done in #start(BundleContext)
  }

  public PlexusContainer getPlexusContainer() {
    return plexus;
  }

  /**
   * This method is called when the plug-in is stopped
   */
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

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

    this.configurationManager = null;

    if(this.console != null) {
      this.console.shutdown();
    }

    plugin = null;
  }

  private void checkJdk() {
    if(getPreferenceStore().getBoolean(MavenPreferenceConstants.P_DISABLE_JDK_CHECK)) {
      return;
    }
    // There is no tools.jar on Mac OS X
    // http://developer.apple.com/documentation/Java/Conceptual/Java14Development/02-JavaDevTools/JavaDevTools.html
    String osName = System.getProperty("os.name", ""); //$NON-NLS-1$ //$NON-NLS-2$
    if(osName.toLowerCase().indexOf("mac os") == -1) { //$NON-NLS-1$
      String javaHome = System.getProperty("java.home"); //$NON-NLS-1$
      File toolsJar = new File(javaHome, "../lib/tools.jar"); //$NON-NLS-1$
      if(!toolsJar.exists()) {
        getConsole().logError("Eclipse is running in a JRE, but a JDK is required\n" // 
            + "  Some Maven plugins may not work when importing projects or updating source folders.");
        if(!getPreferenceStore().getBoolean(MavenPreferenceConstants.P_DISABLE_JDK_WARNING)) {
          showJdkWarning();
        }
      }
    }
  }

  private void showJdkWarning() {
    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
      public void run() {
        Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
        MessageDialogWithToggle dialog = new MessageDialogWithToggle(shell, //
            Messages.MavenPlugin_error_jre_title, //
            null, Messages.MavenPlugin_error_jre_message, MessageDialog.WARNING, //
            new String[] {IDialogConstants.OK_LABEL}, //
            0, Messages.MavenPlugin_error_warn_again, false) {
          protected Control createMessageArea(Composite composite) {
            Image image = getImage();
            if(image != null) {
              imageLabel = new Label(composite, SWT.NULL);
              image.setBackground(imageLabel.getBackground());
              imageLabel.setImage(image);
              GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(imageLabel);
            }

            Link link = new Link(composite, getMessageLabelStyle());
            link.setText(message);
            link.addSelectionListener(new SelectionAdapter() {
              public void widgetSelected(SelectionEvent e) {
                if("eclipse.ini".equals(e.text)) { //$NON-NLS-1$
//                    String href = "topic=/org.eclipse.platform.doc.user/tasks/running_eclipse.htm";
//                    BaseHelpSystem.getHelpDisplay().displayHelpResource(href, false);

                  try {
                    IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                    // browser.openURL(new URL("http://www.eclipse.org/swt/launcher.html"));
                    browser
                        .openURL(new URL(
                            "http://help.eclipse.org/help33/index.jsp?topic=/org.eclipse.platform.doc.user/tasks/running_eclipse.htm")); //$NON-NLS-1$
                  } catch(MalformedURLException ex) {
                    MavenLogger.log("Malformed URL", ex);
                  } catch(PartInitException ex) {
                    MavenLogger.log(ex);
                  }
                } else {
                  PreferencesUtil.createPreferenceDialogOn(getShell(),
                      "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage", null, null).open(); //$NON-NLS-1$
                }
              }
            });

            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false)
                .hint(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT)
                .applyTo(link);

            return composite;
          }
        };

        dialog.setPrefStore(getPreferenceStore());
        dialog.setPrefKey(MavenPreferenceConstants.P_DISABLE_JDK_WARNING);

        dialog.open();

        getPreferenceStore().setValue(MavenPreferenceConstants.P_DISABLE_JDK_WARNING, dialog.getToggleState());
      }
    });
  }

  /**
   * Returns the shared instance.
   */
  public static MavenPlugin getDefault() {
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

  public MavenConsole getConsole() {
    return this.console;
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

  public SearchEngine getSearchEngine(IProject context) throws CoreException {
    return new IndexSearchEngine(MavenPlugin.getDefault().getIndexManager().getIndex(context));
  }
}
