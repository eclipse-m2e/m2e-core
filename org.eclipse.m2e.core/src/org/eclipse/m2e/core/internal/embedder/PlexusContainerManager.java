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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LoggerManager;

import org.apache.maven.cli.internal.ExtensionResolutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;

import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.internal.maven.compat.ExtensionResolutionExceptionFacade;
import org.eclipse.m2e.internal.maven.compat.PlexusContainerFacade;


/**
 * The {@link PlexusContainerManager} takes care about creating and caching {@link PlexusContainer}, code should always
 * ask the manager instead of caching container instances as if file-system change containers can also change without
 * notice.
 */
@Component(service = PlexusContainerManager.class)
public class PlexusContainerManager {
  private static final ILog LOG = Platform.getLog(PlexusContainerManager.class);

  private static final String MAVEN_EXTENSION_REALM_PREFIX = "maven.ext.";

  private static final String PLEXUS_CORE_REALM = "plexus.core";

  private final Object containerLock = new Object();

  private final PlexusContainerFactory containerFactory;

  private boolean active = true;

  private IMavenPlexusContainer nonRootedContainer;

  private CompletableFuture<IMavenPlexusContainer> nonRootedContainerCreation;

  private final Map<File, IMavenPlexusContainer> containerMap = new HashMap<>();

  private final Map<File, CompletableFuture<IMavenPlexusContainer>> containerCreations = new HashMap<>();

  @Reference
  private LoggerManager loggerManager;

  @Reference
  private IMavenConfiguration mavenConfiguration;

  @Reference
  private IWorkspace workspace;

  public PlexusContainerManager() {
    this(PlexusContainerManager::newPlexusContainer);
  }

  PlexusContainerManager(PlexusContainerFactory containerFactory) {
    this.containerFactory = Objects.requireNonNull(containerFactory);
  }

  @Deactivate
  void dispose() {
    List<IMavenPlexusContainer> containers;
    IllegalStateException deactivated = new IllegalStateException("Plexus container manager is deactivated");
    synchronized(containerLock) {
      if(!active) {
        return;
      }
      active = false;
      containers = new ArrayList<>(containerMap.values());
      containerMap.clear();
      if(nonRootedContainer != null) {
        containers.add(nonRootedContainer);
        nonRootedContainer = null;
      }
      containerCreations.values().forEach(creation -> creation.completeExceptionally(deactivated));
      containerCreations.clear();
      if(nonRootedContainerCreation != null) {
        nonRootedContainerCreation.completeExceptionally(deactivated);
        nonRootedContainerCreation = null;
      }
    }
    containers.forEach(PlexusContainerManager::disposeContainer);
  }

  /**
   * Performs a cleanup cycle by disposing (and removing) container that are no longer referencing a valid maven root
   */
  void cleanup() {
    List<IMavenPlexusContainer> staleContainers = new ArrayList<>();
    synchronized(containerLock) {
      containerMap.entrySet().removeIf(entry -> {
        if(!new File(entry.getKey(), IMavenPlexusContainer.MVN_FOLDER).isDirectory()) {
          staleContainers.add(entry.getValue());
          return true;
        }
        return false;
      });
    }
    staleContainers.forEach(PlexusContainerManager::disposeContainer);
  }

  private static void disposeContainer(IMavenPlexusContainer mavenPlexusContainer) {
    PlexusContainer plexusContainer = mavenPlexusContainer.getContainer();
    ClassWorld classWorld = plexusContainer.getContainerRealm().getWorld();
    for(ClassRealm realm : classWorld.getRealms()) {
      try {
        classWorld.disposeRealm(realm.getId());
      } catch(NoSuchRealmException e) {
        LOG.error("Failed to dispose ClassRealm", e);
      }
    }
    plexusContainer.dispose();
  }

  public IMavenPlexusContainer aquire() throws Exception {
    cleanup();
    CompletableFuture<IMavenPlexusContainer> creation;
    boolean creator = false;
    synchronized(containerLock) {
      checkActive();
      if(nonRootedContainer == null) {
        creation = nonRootedContainerCreation;
        if(creation == null) {
          creation = new CompletableFuture<>();
          nonRootedContainerCreation = creation;
          creator = true;
        }
      } else {
        return nonRootedContainer;
      }
    }
    return creator ? createContainer(null, creation) : awaitContainer(creation);
  }

  public IMavenPlexusContainer aquire(IResource basedir) throws Exception {
    if(basedir == null || !basedir.isAccessible()) {
      return aquire();
    }
    if(basedir.getLocation() == null) {
      return aquire();
    }
    File file = basedir.getLocation().toFile();
    if(file == null) {
      return aquire();
    }
    return aquire(file);
  }

  public IMavenPlexusContainer aquire(File basedir) throws Exception {
    File directory = MavenProperties.computeMultiModuleProjectDirectory(basedir);
    if(directory == null) {
      return aquire();
    }
    File canonicalDirectory = directory.getCanonicalFile();
    cleanup();
    CompletableFuture<IMavenPlexusContainer> creation;
    boolean creator = false;
    synchronized(containerLock) {
      checkActive();
      IMavenPlexusContainer plexusContainer = containerMap.get(canonicalDirectory);
      if(plexusContainer != null) {
        return plexusContainer;
      }
      creation = containerCreations.get(canonicalDirectory);
      if(creation == null) {
        creation = new CompletableFuture<>();
        containerCreations.put(canonicalDirectory, creation);
        creator = true;
      }
    }
    try {
      return creator ? createContainer(canonicalDirectory, creation) : awaitContainer(creation);
    } catch(ExtensionResolutionException e) {
      //TODO how can we create an error marker on the extension file?
      ExtensionResolutionExceptionFacade.throwForFile(e,
          new File(directory, IMavenPlexusContainer.EXTENSIONS_FILENAME));
      return null;
    }
  }

  private IMavenPlexusContainer createContainer(File directory,
      CompletableFuture<IMavenPlexusContainer> creation) throws Exception {
    IMavenPlexusContainer plexusContainer;
    try {
      plexusContainer = Objects.requireNonNull(containerFactory.create(directory, loggerManager, mavenConfiguration));
    } catch(Throwable failure) {
      synchronized(containerLock) {
        clearCreation(directory, creation);
        creation.completeExceptionally(failure);
      }
      return awaitContainer(creation);
    }

    boolean published;
    synchronized(containerLock) {
      published = active && isCurrentCreation(directory, creation);
      clearCreation(directory, creation);
      if(published) {
        if(directory == null) {
          nonRootedContainer = plexusContainer;
        } else {
          containerMap.put(directory, plexusContainer);
        }
        creation.complete(plexusContainer);
      } else {
        creation.completeExceptionally(new IllegalStateException("Plexus container manager is deactivated"));
      }
    }
    if(!published) {
      disposeContainer(plexusContainer);
    }
    return awaitContainer(creation);
  }

  private boolean isCurrentCreation(File directory, CompletableFuture<IMavenPlexusContainer> creation) {
    return directory == null ? nonRootedContainerCreation == creation : containerCreations.get(directory) == creation;
  }

  private void clearCreation(File directory, CompletableFuture<IMavenPlexusContainer> creation) {
    if(directory == null) {
      if(nonRootedContainerCreation == creation) {
        nonRootedContainerCreation = null;
      }
    } else {
      containerCreations.remove(directory, creation);
    }
  }

  private void checkActive() {
    if(!active) {
      throw new IllegalStateException("Plexus container manager is deactivated");
    }
  }

  private static IMavenPlexusContainer awaitContainer(CompletableFuture<IMavenPlexusContainer> creation)
      throws Exception {
    try {
      return creation.get();
    } catch(ExecutionException e) {
      Throwable cause = e.getCause();
      if(cause instanceof Exception exception) {
        throw exception;
      }
      if(cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException(cause);
    }
  }

  public IComponentLookup getComponentLookup() {
    try {
      return aquire().getComponentLookup();
    } catch(Exception ex) {
      return new ExceptionalLookup(ex);
    }
  }

  public IComponentLookup getComponentLookup(File basedir) {
    try {
      return aquire(basedir).getComponentLookup();
    } catch(Exception ex) {
      return new ExceptionalLookup(ex);
    }
  }

  private static IMavenPlexusContainer newPlexusContainer(File multiModuleProjectDirectory, LoggerManager loggerManager,
      IMavenConfiguration mavenConfiguration) throws Exception {

    // In M2E it can happen that the same extension (with same GAV) is referenced/loaded from multiple locations ('.mvn'-folders).
    // In contrast to a standalone Maven-build, which only has one multi-module-root ('.mvn'-folder), M2E can import multiple 
    // projects with different '.mvn'-folder. Because the id of an extension's realm is only based on the GAV, attempts to load 
    // the same extension from different locations result in a DuplicateRealmException. Therefore each container needs its own ClassWorld.
    ClassWorld classWorld = new M2EClassWorld(PLEXUS_CORE_REALM, ClassWorld.class.getClassLoader(),
        multiModuleProjectDirectory);
    ClassRealm coreRealm = classWorld.getRealm(PLEXUS_CORE_REALM);
    CoreExtensionEntry coreEntry = CoreExtensionEntry.discoverFrom(coreRealm);
    List<CoreExtensionEntry> extensions = PlexusContainerFacade.loadCoreExtensions(coreRealm, coreEntry,
        multiModuleProjectDirectory, loggerManager, container -> {
          Optional<MavenProperties> mavenProperties = MavenProperties.getMavenArgs(multiModuleProjectDirectory);
          IMavenConfiguration workspaceConfiguration = IMavenConfiguration.getWorkspaceConfiguration();
          MavenExecutionRequest request = MavenExecutionContext.createExecutionRequest(mavenConfiguration,
              wrap(container), mavenProperties.map(mavenCfg -> mavenCfg.getSettingsLocations(workspaceConfiguration))
                  .orElseGet(workspaceConfiguration::getSettingsLocations),
              multiModuleProjectDirectory);
          container.lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
          request.setBaseDirectory(multiModuleProjectDirectory);
          request.setMultiModuleProjectDirectory(multiModuleProjectDirectory);
          Properties userProperties = request.getUserProperties();
          mavenProperties.ifPresent(prop -> prop.getCliProperties(userProperties::setProperty));
          return request;
        });
    List<File> extClassPath = List.of(); //TODO should we allow to set an ext-class path for m2e?
    ClassRealm containerRealm = setupContainerRealm(coreRealm, extClassPath, extensions);

    ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld(classWorld).setRealm(containerRealm)
        .setClassPathScanning(PlexusConstants.SCANNING_INDEX).setAutoWiring(true).setJSR250Lifecycle(true)
        .setName(PlexusContainerFacade.CONTAINER_CONFIGURATION_NAME);

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
    PlexusContainerFacade facade = new PlexusContainerFacade(container);
    classWorld.addListener(new LifecycleManagerDisposer(container));
    container.setLookupRealm(null);
    Thread thread = Thread.currentThread();
    ClassLoader ccl = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(container.getContainerRealm());
      container.setLoggerManager(loggerManager);
      for(CoreExtensionEntry extension : extensions) {
        facade.loadExtension(extension);
      }
    } finally {
      thread.setContextClassLoader(ccl);
    }
    return new IMavenPlexusContainer() {

      private IComponentLookup lookup;

      @Override
      public Optional<File> getMavenDirectory() {
        return Optional.ofNullable(multiModuleProjectDirectory);
      }

      @Override
      public PlexusContainer getContainer() {
        return container;
      }

      @Override
      public IComponentLookup getComponentLookup() {
        if(lookup == null) {
          lookup = wrap(getContainer());
        }
        return lookup;
      }
    };
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

  private static final class M2EClassWorld extends ClassWorld {

    private File multiModuleProjectDirectory;

    M2EClassWorld(String plexusCoreRealm, ClassLoader classLoader, File multiModuleProjectDirectory) {
      super(plexusCoreRealm, classLoader);
      this.multiModuleProjectDirectory = multiModuleProjectDirectory;
    }

    @Override
    public String toString() {
      String name = multiModuleProjectDirectory == null ? "GLOBAL" : multiModuleProjectDirectory.getAbsolutePath();
      return "ClassWorld [" + name + "] "
          + getRealms().stream().map(ClassRealm::getId).collect(Collectors.joining(", "));
    }

  }

  private static final class PlexusComponentLookup implements IComponentLookup {

    private PlexusContainer container;

    private ClassRealm lookupRealm;

    public PlexusComponentLookup(PlexusContainer container, ClassRealm lookupRealm) {
      this.container = container;
      this.lookupRealm = lookupRealm;
    }

    @Override
    public <C> C lookup(Class<C> type) throws CoreException {
      if(type == PlexusContainer.class) {
        return type.cast(container);
      }
      Thread thread = Thread.currentThread();
      ClassLoader ccl = thread.getContextClassLoader();
      try {
        thread.setContextClassLoader(lookupRealm);
        return container.lookup(type);
      } catch(ComponentLookupException ex) {
        throw new CoreException(Status.error(Messages.MavenImpl_error_lookup, ex));
      } finally {
        thread.setContextClassLoader(ccl);
      }
    }

    @Override
    public <C> Collection<C> lookupCollection(Class<C> type) {
      Thread thread = Thread.currentThread();
      ClassLoader ccl = thread.getContextClassLoader();
      try {
        thread.setContextClassLoader(lookupRealm);
        //copy is important here! Plexus is filtering items based on the current caller thread context!
        return List.copyOf(container.lookupList(type));
      } catch(ComponentLookupException ex) {
        return List.of();
      } finally {
        thread.setContextClassLoader(ccl);
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

  public static IComponentLookup wrap(PlexusContainer container) {
    return wrap(container, container.getContainerRealm());
  }

  public static IComponentLookup wrap(PlexusContainer container, ClassRealm realm) {
    return new PlexusComponentLookup(container, realm);
  }


}
