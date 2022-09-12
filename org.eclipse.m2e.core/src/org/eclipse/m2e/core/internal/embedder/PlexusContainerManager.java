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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
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

  static final String MVN_FOLDER = ".mvn";

  private static final String EXTENSIONS_FILENAME = MVN_FOLDER + "/extensions.xml";

  private static final String CONTAINER_CONFIGURATION_NAME = "maven";

  private static final String MAVEN_EXTENSION_REALM_PREFIX = "maven.ext.";

  private static final String PLEXUS_CORE_REALM = "plexus.core";

  private static final ClassWorld CLASS_WORLD = new ClassWorld(PLEXUS_CORE_REALM, ClassWorld.class.getClassLoader());

  private static final ClassRealm CORE_REALM;

  private static final CoreExtensionEntry CORE_ENTRY;

  private static final AtomicLong REALM_ID_SEQUENCE = new AtomicLong();

  static {
    try {
      CORE_REALM = CLASS_WORLD.getRealm(PLEXUS_CORE_REALM);
      CORE_ENTRY = CoreExtensionEntry.discoverFrom(CORE_REALM);
    } catch(NoSuchRealmException ex) {
      throw new AssertionError("Should never happen", ex);
    }
  }

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
      containerMap.values().forEach(PlexusContainer::dispose);
      containerMap.clear();
      if(nonRootedContainer != null) {
        nonRootedContainer.dispose();
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
          entry.getValue().dispose();
          return true;
        }
        return false;
      });
    }
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
          containerMap.put(canonicalDirectory,
              plexusContainer = newPlexusContainer(canonicalDirectory, loggerManager, mavenConfiguration));
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
      return new PlexusComponentLookup(aquire());
    } catch(Exception ex) {
      return new ExceptionalLookup(ex);
    }
  }

  public IComponentLookup getComponentLookup(File basedir) {
    try {
      return new PlexusComponentLookup(aquire(basedir));
    } catch(Exception ex) {
      return new ExceptionalLookup(ex);
    }
  }

  private static DefaultPlexusContainer newPlexusContainer(File multiModuleProjectDirectory,
      LoggerManager loggerManager, IMavenConfiguration mavenConfiguration) throws Exception {
    List<CoreExtensionEntry> extensions = loadCoreExtensions(multiModuleProjectDirectory, loggerManager,
        mavenConfiguration);
    List<File> extClassPath = List.of(); //TODO should we allow to set an ext-class path for m2e?
    ClassRealm containerRealm = setupContainerRealm(extClassPath, extensions);

    ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld(CLASS_WORLD).setRealm(containerRealm)
        .setClassPathScanning(PlexusConstants.SCANNING_INDEX).setAutoWiring(true).setJSR250Lifecycle(true)
        .setName(CONTAINER_CONFIGURATION_NAME);

    Set<String> exportedArtifacts = new HashSet<>(CORE_ENTRY.getExportedArtifacts());
    Set<String> exportedPackages = new HashSet<>(CORE_ENTRY.getExportedPackages());
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

  private static ClassRealm setupContainerRealm(List<File> extClassPath, List<CoreExtensionEntry> extensions)
      throws Exception {
    if(extClassPath.isEmpty() && extensions.isEmpty()) {
      return CORE_REALM;
    }
    ClassRealm extRealm = CLASS_WORLD.newRealm(MAVEN_EXTENSION_REALM_PREFIX + REALM_ID_SEQUENCE.getAndIncrement(),
        null);
    extRealm.setParentRealm(CORE_REALM);
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

  private static List<CoreExtensionEntry> loadCoreExtensions(File multiModuleProjectDirectory,
      LoggerManager loggerManager, IMavenConfiguration mavenConfiguration) throws Exception {
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

    ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld(CLASS_WORLD).setRealm(CORE_REALM)
        .setClassPathScanning(PlexusConstants.SCANNING_INDEX).setAutoWiring(true).setJSR250Lifecycle(true)
        .setName(CONTAINER_CONFIGURATION_NAME);

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
          new PlexusComponentLookup(container), MavenPluginActivator.getDefault().getMaven());
      container.lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
      request.setBaseDirectory(multiModuleProjectDirectory);
      request.setMultiModuleProjectDirectory(multiModuleProjectDirectory);
      BootstrapCoreExtensionManager resolver = container.lookup(BootstrapCoreExtensionManager.class);
      return resolver.loadCoreExtensions(request, CORE_ENTRY.getExportedArtifacts(), extensions);
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
      if(exception instanceof CoreException) {
        return (CoreException) exception;
      }
      return new CoreException(Status.error("container creation failed", exception));
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
    File current = basedir;
    while(current != null && !current.equals(workspaceRoot)) {

      if(new File(current, MVN_FOLDER).isDirectory()) {
        return current;
      }
      current = current.getParentFile();
    }
    return null;
  }

}
