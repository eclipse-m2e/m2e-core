/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.archetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.BundleClassSpace;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.wire.WireModule;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.archetype.metadata.ArchetypeDescriptor;
import org.apache.maven.archetype.metadata.RequiredProperty;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;
import org.apache.maven.archetype.source.InternalCatalogArchetypeDataSource;
import org.apache.maven.archetype.source.LocalCatalogArchetypeDataSource;
import org.apache.maven.archetype.source.RemoteCatalogArchetypeDataSource;

import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IArchetype;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;


/**
 * Archetype Manager
 *
 * @author Eugene Kuleshov
 */
@Component(service = ArchetypePlugin.class)
public class ArchetypePlugin {

  private static final InternalCatalogArchetypeDataSource INTERNAL_CATALOG_ARCHETYPE_DATA_SOURCE = new InternalCatalogArchetypeDataSource();

  private static final LocalCatalogArchetypeDataSource LOCAL_CATALOG = new LocalCatalogArchetypeDataSource();

  public static final String ARCHETYPE_PREFIX = "archetype";

  private final Map<String, ArchetypeCatalogFactory> catalogs = new LinkedHashMap<>();

  private final File configFile;

  private final ArchetypeCatalogsWriter writer;

  @Reference
  ArchetypeGenerator archetypeGenerator;

  @Reference
  IMaven maven;

  private ArchetypeArtifactManager archetypeArtifactManager;

  public ArchetypePlugin() {
    this.configFile = new File(MavenPluginActivator.getDefault().getStateLocation().toFile(),
        M2EUIPluginActivator.PREFS_ARCHETYPES);
    this.writer = new ArchetypeCatalogsWriter();
  }

  @Activate
  void activate() {
    final Module logginModule = new AbstractModule() {
      @Override
      protected void configure() {
        bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
      }
    };
    ClassSpace space = new BundleClassSpace(FrameworkUtil.getBundle(ArchetypeArtifactManager.class));
    final Module repositorySystemModule = new AbstractModule() {
      @Override
      protected void configure() {
        try {
          bind(RepositorySystem.class).toInstance(MavenPluginActivator.getDefault().getRepositorySystem());
        } catch(CoreException ex) {
          ex.printStackTrace();
        }
      }
    };
    Injector injector = Guice.createInjector(
        new WireModule(logginModule, repositorySystemModule, new SpaceModule(space, BeanScanning.INDEX)));
    archetypeArtifactManager = injector.getInstance(ArchetypeArtifactManager.class);
    addArchetypeCatalogFactory(
        new ArchetypeCatalogFactory.InternalCatalogFactory(INTERNAL_CATALOG_ARCHETYPE_DATA_SOURCE));
    addArchetypeCatalogFactory(new ArchetypeCatalogFactory.DefaultLocalCatalogFactory(maven, LOCAL_CATALOG));
    for(ArchetypeCatalogFactory archetypeCatalogFactory : ExtensionReader.readArchetypeExtensions(this)) {
      addArchetypeCatalogFactory(archetypeCatalogFactory);
    }
    try {
      readCatalogs();
    } catch(IOException e) {
      M2EUIPluginActivator.getDefault().getLog().error("Can't read catalogs!", e);
    }
  }

  @Deactivate
  void shutdown() throws IOException {
    saveCatalogs();
  }

  public LocalCatalogFactory newLocalCatalogFactory(String path, String description, boolean editable,
      boolean enabled) {
    return new LocalCatalogFactory(path, description, editable, enabled, maven, LOCAL_CATALOG);
  }

  public RemoteCatalogFactory newRemoteCatalogFactory(String url, String description, boolean editable,
      boolean enabled) {
    return new RemoteCatalogFactory(url, description, editable, enabled, maven, new RemoteCatalogArchetypeDataSource());
  }

  public ArchetypeGenerator getGenerator() {
    return archetypeGenerator;
  }

  /**
   * @return Collection of ArchetypeCatalogFactory
   */
  public Collection<ArchetypeCatalogFactory> getArchetypeCatalogs() {
    return new ArrayList<>(catalogs.values());
  }

  /**
   * @return all active ArchetypeCatalogFactory
   */
  public Collection<ArchetypeCatalogFactory> getActiveArchetypeCatalogs() {
    return catalogs.values().stream().filter(ArchetypeCatalogFactory::isEnabled).collect(Collectors.toList());
  }

  public void addArchetypeCatalogFactory(ArchetypeCatalogFactory factory) {
    if(factory != null) {
      catalogs.put(factory.getId(), factory);
    }
  }

  public void removeArchetypeCatalogFactory(String catalogId) {
    catalogs.remove(catalogId);
  }

  public ArchetypeCatalogFactory getArchetypeCatalogFactory(String catalogId) {
    return catalogs.get(catalogId);
  }

  public void readCatalogs() throws IOException {
    if(configFile.exists()) {
      try (InputStream is = new FileInputStream(configFile)) {
        Collection<ArchetypeCatalogFactory> userDefinedCatalogs = writer.readArchetypeCatalogs(is, catalogs, this);
        for(ArchetypeCatalogFactory it : userDefinedCatalogs) {
          addArchetypeCatalogFactory(it);
        }
      }
    }
  }

  public void saveCatalogs() throws IOException {
    try (OutputStream os = new FileOutputStream(configFile)) {
      writer.writeArchetypeCatalogs(getArchetypeCatalogs(), os);
    }
  }

  /**
   * Gets the required properties of an {@link IArchetype}.
   *
   * @param archetype the archetype possibly declaring required properties
   * @param remoteArchetypeRepository the remote archetype repository, can be null.
   * @param monitor the progress monitor, can be null.
   * @return the required properties of the archetypes, null if none is found.
   * @throws CoreException if no archetype can be resolved
   */
  public List<RequiredProperty> getRequiredProperties(IArchetype archetype, IProgressMonitor monitor)
      throws CoreException {
    Assert.isNotNull(archetype, "Archetype can not be null");

    if(monitor == null) {
      monitor = new NullProgressMonitor();
    }

    final String groupId = archetype.getGroupId();
    final String artifactId = archetype.getArtifactId();
    final String version = archetype.getVersion();

    return maven.createExecutionContext().execute((context, monitor1) -> {
      try {
        File archetypeFile = archetypeArtifactManager.getArchetypeFile(groupId, artifactId, version,
            getRemoteRepositories(context), context.getRepositorySession());
        if(archetypeArtifactManager.isFileSetArchetype(archetypeFile)) {
          ArchetypeDescriptor descriptor = archetypeArtifactManager.getFileSetArchetypeDescriptor(archetypeFile);
          return descriptor.getRequiredProperties();
        }
        return null;
      } catch(UnknownArchetype ex) {
        throw new CoreException(Status.error("UnknownArchetype", ex));
      }
    }, monitor);
  }

  public void updateLocalCatalog(Archetype archetype) throws CoreException {
    maven.createExecutionContext().execute((ctx, m) -> {
      try {
        LOCAL_CATALOG.updateCatalog(ctx.getRepositorySession(), archetype);
      } catch(ArchetypeDataSourceException e) {
      }
      return null;
    }, null);
  }

  static List<RemoteRepository> getRemoteRepositories(IMavenExecutionContext ctx) throws CoreException {
    return RepositoryUtils.toRepos(ctx.getExecutionRequest().getRemoteRepositories());
  }
}
