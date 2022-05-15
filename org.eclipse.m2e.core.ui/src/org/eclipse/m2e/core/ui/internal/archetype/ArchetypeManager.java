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

import org.osgi.util.tracker.ServiceTracker;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.archetype.metadata.ArchetypeDescriptor;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.NoSuchComponentException;
import org.eclipse.m2e.core.project.IArchetype;


/**
 * Archetype Manager
 *
 * @author Eugene Kuleshov
 */
public class ArchetypeManager {

  private final Map<String, ArchetypeCatalogFactory> catalogs = new LinkedHashMap<>();

  private final File configFile;

  private final ArchetypeCatalogsWriter writer;

  final ArchetypeArtifactManager aaMgr;

  private final org.apache.maven.archetype.ArchetypeManager archetyper;

  private final PlexusContainer container;

  private ServiceTracker<ArchetypeGenerator, ArchetypeGenerator> serviceTracker;

  public ArchetypeManager(PlexusContainer container, File configFile,
      ServiceTracker<ArchetypeGenerator, ArchetypeGenerator> serviceTracker) {
    this.container = container;
    this.configFile = configFile;
    this.serviceTracker = serviceTracker;
    this.writer = new ArchetypeCatalogsWriter();
    try {
      this.aaMgr = container.lookup(ArchetypeArtifactManager.class);
      this.archetyper = container.lookup(org.apache.maven.archetype.ArchetypeManager.class);
    } catch(ComponentLookupException ex) {
      throw new NoSuchComponentException(ex);
    }
  }

  public ArchetypeGenerator getGenerator() {
    return serviceTracker.getService();
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
        Collection<ArchetypeCatalogFactory> userDefinedCatalogs = writer.readArchetypeCatalogs(is, catalogs);
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
   * Gets the remote {@link ArtifactRepository} of the given {@link IArchetype}, or null if none is found. The
   * repository url is extracted from {@link Archetype#getRepository(). The {@link ArtifactRepository} id is set to
   * <strong>archetypeId+"-repo"</strong>, to enable authentication on that repository.
   *
   * @see <a href=
   *      "http://maven.apache.org/archetype/maven-archetype-plugin/faq.html">http://maven.apache.org/archetype/maven-archetype-plugin/faq.html</a>
   * @param archetype
   * @return the remote {@link ArtifactRepository} of the given {@link IArchetype}, or null if none is found.
   * @throws CoreException
   */
  public ArtifactRepository getArchetypeRepository(IArchetype archetype) throws CoreException {
    String repoUrl = archetype.getRepository();
    if(repoUrl == null || repoUrl.trim().isEmpty()) {
      return null;
    }
    return MavenPlugin.getMaven().createArtifactRepository(archetype.getArtifactId() + "-repo", repoUrl); //$NON-NLS-1$
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
  public List<?> getRequiredProperties(IArchetype archetype, ArtifactRepository remoteArchetypeRepository,
      IProgressMonitor monitor) throws CoreException {
    Assert.isNotNull(archetype, "Archetype can not be null");

    if(monitor == null) {
      monitor = new NullProgressMonitor();
    }

    final String groupId = archetype.getGroupId();
    final String artifactId = archetype.getArtifactId();
    final String version = archetype.getVersion();

    IMaven maven = MavenPlugin.getMaven();

    final List<ArtifactRepository> repositories = new ArrayList<>(maven.getArtifactRepositories());
    if(remoteArchetypeRepository != null) {
      repositories.add(0, remoteArchetypeRepository);
    }

    return maven.createExecutionContext().execute((context, monitor1) -> {
      ArtifactRepository localRepository = context.getLocalRepository();
      if(aaMgr.isFileSetArchetype(groupId, artifactId, version, null, localRepository, repositories,
          context.newProjectBuildingRequest())) {
        ArchetypeDescriptor descriptor;
        try {
          descriptor = aaMgr.getFileSetArchetypeDescriptor(groupId, artifactId, version, null, localRepository,
              repositories, context.newProjectBuildingRequest());
        } catch(UnknownArchetype ex) {
          throw new CoreException(Status.error("UnknownArchetype", ex));
        }
        return descriptor.getRequiredProperties();
      }
      return null;
    }, monitor);
  }

  /**
   * @since 2.0
   */
  public org.apache.maven.archetype.ArchetypeManager getArchetyper() {
    return archetyper;
  }

}
