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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.archetype.metadata.ArchetypeDescriptor;
import org.apache.maven.archetype.metadata.RequiredProperty;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.ProjectBuildingRequest;

import org.eclipse.m2e.core.embedder.IMaven;
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

  public static final String ARCHETYPE_PREFIX = "archetype";

  private final Map<String, ArchetypeCatalogFactory> catalogs = new LinkedHashMap<>();

  private final File configFile;

  private final ArchetypeCatalogsWriter writer;

  @Reference
  ArchetypeGenerator archetypeGenerator;

  @Reference
  IMaven maven;

  private ArchetypeArtifactManager archetypeArtifactManager;

  private Map<String, ArchetypeDataSource> archetypeDataSourceMap;

  private DefaultPlexusContainer container;

  public ArchetypePlugin() {
    this.configFile = new File(MavenPluginActivator.getDefault().getStateLocation().toFile(),
        M2EUIPluginActivator.PREFS_ARCHETYPES);
    this.writer = new ArchetypeCatalogsWriter();
  }

  @Activate
  void activate() throws PlexusContainerException, ComponentLookupException {
    final Module logginModule = new AbstractModule() {
      @Override
      protected void configure() {
        bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
      }
    };
    final ContainerConfiguration cc = new DefaultContainerConfiguration() //
        .setClassWorld(new ClassWorld("plexus.core", ArchetypeArtifactManager.class.getClassLoader())) //$NON-NLS-1$
        .setClassPathScanning(PlexusConstants.SCANNING_INDEX) //
        .setAutoWiring(true) //
        .setName("plexus"); //$NON-NLS-1$
    container = new DefaultPlexusContainer(cc, logginModule);
    archetypeArtifactManager = container.lookup(ArchetypeArtifactManager.class);
    archetypeDataSourceMap = container.lookupMap(ArchetypeDataSource.class);
    addArchetypeCatalogFactory(
        new ArchetypeCatalogFactory.InternalCatalogFactory(archetypeDataSourceMap.get("internal-catalog")));
    addArchetypeCatalogFactory(
        new ArchetypeCatalogFactory.DefaultLocalCatalogFactory(maven, archetypeDataSourceMap.get("catalog")));
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
    container.dispose();
  }

  public LocalCatalogFactory newLocalCatalogFactory(String path, String description, boolean editable,
      boolean enabled) {
    return new LocalCatalogFactory(path, description, editable, enabled, maven, archetypeDataSourceMap.get("catalog"));
  }

  public RemoteCatalogFactory newRemoteCatalogFactory(String url, String description, boolean editable,
      boolean enabled) {
    return new RemoteCatalogFactory(url, description, editable, enabled, maven,
        archetypeDataSourceMap.get("remote-catalog"));
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

    final List<ArtifactRepository> repositories = new ArrayList<>(maven.getArtifactRepositories());

    return maven.createExecutionContext().execute((context, monitor1) -> {
      ArtifactRepository localRepository = context.getLocalRepository();
      if(archetypeArtifactManager.isFileSetArchetype(groupId, artifactId, version, null, localRepository, repositories,
          context.newProjectBuildingRequest())) {
        ArchetypeDescriptor descriptor;
        try {
          descriptor = archetypeArtifactManager.getFileSetArchetypeDescriptor(groupId, artifactId, version, null,
              localRepository, repositories, context.newProjectBuildingRequest());
        } catch(UnknownArchetype ex) {
          throw new CoreException(Status.error("UnknownArchetype", ex));
        }
        return descriptor.getRequiredProperties();
      }
      return null;
    }, monitor);
  }

  public void updateLocalCatalog(Archetype archetype) throws CoreException {
    maven.createExecutionContext().execute((ctx, m) -> {
      ProjectBuildingRequest request = ctx.newProjectBuildingRequest();
      try {
        ArchetypeDataSource source = archetypeDataSourceMap.get("catalog");

        source.updateCatalog(request, archetype);
      } catch(ArchetypeDataSourceException e) {
      }
      return null;
    }, null);
  }

}
