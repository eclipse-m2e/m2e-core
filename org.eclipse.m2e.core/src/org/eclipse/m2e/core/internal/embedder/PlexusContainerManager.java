/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LoggerManager;

import org.apache.maven.cli.internal.BootstrapCoreExtensionManager;
import org.apache.maven.cli.internal.ExtensionResolutionException;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.internal.extension.model.io.xpp3.CoreExtensionsXpp3Reader;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.scope.internal.MojoExecutionScopeModule;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.session.scope.internal.SessionScopeModule;

import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.Messages;


/**
 * The {@link PlexusContainerManager} takes care about creating and caching {@link PlexusContainer}, code should always
 * ask the manager instead of caching container instances as if file-system change containers can also change without
 * notice.
 */
@Component(service = PlexusContainerManager.class)
public class PlexusContainerManager {
  private static final ILog LOG = Platform.getLog(PlexusContainerManager.class);

  static final String MVN_FOLDER = ".mvn";

  private static final String EXTENSIONS_FILENAME = MVN_FOLDER + "/extensions.xml";

  private static final String CONTAINER_CONFIGURATION_NAME = "maven";

  private static final String MAVEN_EXTENSION_REALM_PREFIX = "maven.ext.";

  private static final String PLEXUS_CORE_REALM = "plexus.core";

  private PlexusContainer nonRootedContainer;

  private final Map<File, PlexusContainer> containerMap = new HashMap<>();

  @Reference
  private LoggerManager loggerManager;

  @Reference
  private IMavenConfiguration mavenConfiguration;

  @Reference
  private IWorkspace workspace;

  @Deactivate
  void dispose() {
    synchronized(containerMap) {
      containerMap.values().forEach(PlexusContainerManager::disposeContainer);
      containerMap.clear();
      if(nonRootedContainer != null) {
        disposeContainer(nonRootedContainer);
        nonRootedContainer = null;
      }
    }
  }

  /**
   * Performs a cleanup cycle by disposing (and removing) container that are no longer referencing a valid maven root
   */
  void cleanup() {
    synchronized(containerMap) {
      containerMap.entrySet().removeIf(entry -> {
        if(!new File(entry.getKey(), MVN_FOLDER).isDirectory()) {
          disposeContainer(entry.getValue());
          return true;
        }
        return false;
      });
    }
  }

  private static void disposeContainer(PlexusContainer container) {
    ClassWorld classWorld = container.getContainerRealm().getWorld();
    for(ClassRealm realm : classWorld.getRealms()) {
      try {
        classWorld.disposeRealm(realm.getId());
      } catch(NoSuchRealmException e) {
        LOG.error("Failed to dispose ClassRealm", e);
      }
    }
    container.dispose();
  }

  public PlexusContainer aquire() throws Exception {
    synchronized(containerMap) {
      cleanup();
      if(nonRootedContainer == null) {
        nonRootedContainer = newPlexusContainer(null, loggerManager, mavenConfiguration);
      }
      return nonRootedContainer;
    }
  }

  public PlexusContainer aquire(IFile pom) throws Exception {
    if(pom == null) {
      return aquire();
    }
    File file = pom.getLocation().toFile();
    if(file == null) {
      return aquire();
    }
    return aquire(file);
  }

  public PlexusContainer aquire(File basedir) throws Exception {
    File directory = computeMultiModuleProjectDirectory(basedir);
    if(directory == null) {
      return aquire();
    }
    File canonicalDirectory = directory.getCanonicalFile();
    synchronized(containerMap) {
      cleanup();
      PlexusContainer plexusContainer = containerMap.get(canonicalDirectory);
      if(plexusContainer == null) {
        try {
          plexusContainer = newPlexusContainer(canonicalDirectory, loggerManager, mavenConfiguration);
          containerMap.put(canonicalDirectory, plexusContainer);
        } catch(ExtensionResolutionException e) {
          //TODO should we fail or should we return the standard container then and for example create an error marker on the project?
          CoreExtension extension = e.getExtension();
          throw new PlexusContainerException(
              "can't create plexus container for basedir = " + basedir.getAbsolutePath() + " because the extension "
                  + extension.getGroupId() + ":" + extension.getArtifactId() + ":" + extension.getVersion()
                  + " can't be loaded (defined in " + new File(directory, EXTENSIONS_FILENAME).getAbsolutePath() + ").",
              e);
        }
      }
      return plexusContainer;
    }
  }

  public IComponentLookup getComponentLookup() {
    try {
      return wrap(aquire());
    } catch(Exception ex) {
      return new ExceptionalLookup(ex);
    }
  }

  public IComponentLookup getComponentLookup(File basedir) {
    try {
      return wrap(aquire(basedir));
    } catch(Exception ex) {
      return new ExceptionalLookup(ex);
    }
  }

  private static PlexusContainer newPlexusContainer(File multiModuleProjectDirectory, LoggerManager loggerManager,
      IMavenConfiguration mavenConfiguration) throws Exception {

    // In M2E it can happen that the same extension (with same GAV) is referenced/loaded from multiple locations ('.mvn'-folders).
    // In contrast to a standalone Maven-build, which only has one multi-module-root ('.mvn'-folder), M2E can import multiple 
    // projects with different '.mvn'-folder. Because the id of an extension's realm is only based on the GAV, attempts to load 
    // the same extension from different locations result in a DuplicateRealmException. Therefore each container needs its own ClassWorld.
    ClassWorld classWorld = new ClassWorld(PLEXUS_CORE_REALM, ClassWorld.class.getClassLoader());
    ClassRealm coreRealm = classWorld.getRealm(PLEXUS_CORE_REALM);
    CoreExtensionEntry coreEntry = CoreExtensionEntry.discoverFrom(coreRealm);

    List<CoreExtensionEntry> extensions = loadCoreExtensions(coreRealm, coreEntry, multiModuleProjectDirectory,
        loggerManager, mavenConfiguration);
    List<File> extClassPath = List.of(); //TODO should we allow to set an ext-class path for m2e?
    ClassRealm containerRealm = setupContainerRealm(coreRealm, extClassPath, extensions);

    ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld(classWorld).setRealm(containerRealm)
        .setClassPathScanning(PlexusConstants.SCANNING_INDEX).setAutoWiring(true).setJSR250Lifecycle(true)
        .setName(CONTAINER_CONFIGURATION_NAME);

    Set<String> exportedArtifacts = new HashSet<>(coreEntry.getExportedArtifacts());
    Set<String> exportedPackages = new HashSet<>(coreEntry.getExportedPackages());
    for(CoreExtensionEntry extension : extensions) {
      exportedArtifacts.addAll(extension.getExportedArtifacts());
      exportedPackages.addAll(extension.getExportedPackages());
    }

    final CoreExports exports = new CoreExports(containerRealm, exportedArtifacts, exportedPackages);

    DefaultPlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule() {
      @Override
      protected void configure() {
        bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
        bind(CoreExports.class).toInstance(exports);
      }
    }, new ExtensionModule());
    container.setLookupRealm(null);
    Thread thread = Thread.currentThread();
    ClassLoader ccl = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(container.getContainerRealm());
      container.setLoggerManager(loggerManager);
      for(CoreExtensionEntry extension : extensions) {
        container.discoverComponents(extension.getClassRealm(), new SessionScopeModule(container),
            new MojoExecutionScopeModule(container));
      }
    } finally {
      thread.setContextClassLoader(ccl);
    }
    return container;
  }

  private static ClassRealm setupContainerRealm(ClassRealm coreRealm, List<File> extClassPath,
      List<CoreExtensionEntry> extensions) throws DuplicateRealmException, MalformedURLException {
    if(extClassPath.isEmpty() && extensions.isEmpty()) {
      return coreRealm;
    }
    ClassRealm extRealm = coreRealm.getWorld().newRealm(MAVEN_EXTENSION_REALM_PREFIX, null);
    extRealm.setParentRealm(coreRealm);
    for(File file : extClassPath) {
      extRealm.addURL(file.toURI().toURL());
    }
    for(int i = extensions.size() - 1; i >= 0; i-- ) {
      CoreExtensionEntry entry = extensions.get(i);
      Set<String> exportedPackages = entry.getExportedPackages();
      ClassRealm realm = entry.getClassRealm();
      for(String exportedPackage : exportedPackages) {
        extRealm.importFrom(realm, exportedPackage);
      }
      if(exportedPackages.isEmpty()) {
        extRealm.importFrom(realm, realm.getId());
      }
    }

    return extRealm;
  }

  private static List<CoreExtensionEntry> loadCoreExtensions(ClassRealm coreRealm, CoreExtensionEntry coreEntry,
      File multiModuleProjectDirectory, LoggerManager loggerManager, IMavenConfiguration mavenConfiguration)
      throws Exception {
    if(multiModuleProjectDirectory == null) {
      return Collections.emptyList();
    }
    File extensionsXml = new File(multiModuleProjectDirectory, EXTENSIONS_FILENAME);
    if(!extensionsXml.isFile()) {
      return Collections.emptyList();
    }
    List<CoreExtension> extensions;
    try (InputStream is = new FileInputStream(extensionsXml)) {
      extensions = new CoreExtensionsXpp3Reader().read(is).getExtensions();
    }
    if(extensions.isEmpty()) {
      return Collections.emptyList();
    }

    ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld(coreRealm.getWorld())
        .setRealm(coreRealm).setClassPathScanning(PlexusConstants.SCANNING_INDEX).setAutoWiring(true)
        .setJSR250Lifecycle(true).setName(CONTAINER_CONFIGURATION_NAME);

    DefaultPlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule() {
      @Override
      protected void configure() {
        bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
      }
    });

    Thread thread = Thread.currentThread();
    ClassLoader ccl = thread.getContextClassLoader();
    try {
      container.setLookupRealm(null);
      container.setLoggerManager(loggerManager);
      thread.setContextClassLoader(container.getContainerRealm());
      MavenExecutionRequest request = MavenExecutionContext.createExecutionRequest(mavenConfiguration,
          wrap(container), MavenPluginActivator.getDefault().getMaven().getSettings());
      container.lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
      request.setBaseDirectory(multiModuleProjectDirectory);
      request.setMultiModuleProjectDirectory(multiModuleProjectDirectory);
      BootstrapCoreExtensionManager resolver = container.lookup(BootstrapCoreExtensionManager.class);
      return resolver.loadCoreExtensions(request, coreEntry.getExportedArtifacts(), extensions);
    } finally {
      thread.setContextClassLoader(ccl);
      container.dispose();
    }
  }

  private static final class PlexusComponentLookup implements IComponentLookup {

    private PlexusContainer container;

    public PlexusComponentLookup(PlexusContainer container) {
      this.container = container;
    }

    @Override
    public <C> C lookup(Class<C> type) throws CoreException {
      try {
        return container.lookup(type);
      } catch(ComponentLookupException ex) {
        throw new CoreException(Status.error(Messages.MavenImpl_error_lookup, ex));
      }
    }

    @Override
    public <C> Collection<C> lookupCollection(Class<C> type) {
      try {
        return container.lookupList(type);
      } catch(ComponentLookupException ex) {
        return List.of();
      }
    }

  }

  private static final class ExceptionalLookup implements IComponentLookup {

    private Exception exception;

    public ExceptionalLookup(Exception exception) {
      this.exception = exception;
    }

    @Override
    public <C> C lookup(Class<C> type) throws CoreException {
      throw throwException();
    }

    @Override
    public <C> Collection<C> lookupCollection(Class<C> type) throws CoreException {
      throw throwException();
    }

    private CoreException throwException() {
      return exception instanceof CoreException coreException ? coreException
          : new CoreException(Status.error("container creation failed", exception));
    }

  }

  /**
   * @param file a base file or directory, may be <code>null</code>
   * @return the value for `maven.multiModuleProjectDirectory` as defined in Maven launcher
   */
  public static File computeMultiModuleProjectDirectory(File file) {
    if(file == null) {
      return null;
    }
    final File basedir = file.isDirectory() ? file : file.getParentFile();
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    File workspaceRoot = workspace.getRoot().getLocation().toFile();

    for(File root = basedir; root != null && !root.equals(workspaceRoot); root = root.getParentFile()) {
      if(new File(root, MVN_FOLDER).isDirectory()) {
        return root;
      }
    }
    return null;
  }

  /**
   * @param container
   * @return
   */
  public static IComponentLookup wrap(PlexusContainer container) {
    return new PlexusComponentLookup(container);
  }


}
