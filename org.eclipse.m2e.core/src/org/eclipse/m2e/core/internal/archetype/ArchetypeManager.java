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

package org.eclipse.m2e.core.internal.archetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.archetype.metadata.ArchetypeDescriptor;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.NoSuchComponentException;


/**
 * Archetype Manager
 * 
 * @author Eugene Kuleshov
 */
public class ArchetypeManager {

  private final Map<String, ArchetypeCatalogFactory> catalogs = new LinkedHashMap<String, ArchetypeCatalogFactory>();

  private final File configFile;

  private final ArchetypeCatalogsWriter writer;

  final ArchetypeArtifactManager aaMgr;

  private final org.apache.maven.archetype.ArchetypeManager archetyper;

  private final PlexusContainer container;

  public ArchetypeManager(PlexusContainer container, File configFile) {
    this.container = container;
    this.configFile = configFile;
    this.writer = new ArchetypeCatalogsWriter();
    try {
      this.aaMgr = container.lookup(ArchetypeArtifactManager.class);
      this.archetyper = container.lookup(org.apache.maven.archetype.ArchetypeManager.class);
    } catch(ComponentLookupException ex) {
      throw new NoSuchComponentException(ex);
    }
  }

  /**
   * @return Collection of ArchetypeCatalogFactory
   */
  public Collection<ArchetypeCatalogFactory> getArchetypeCatalogs() {
    return new ArrayList<ArchetypeCatalogFactory>(catalogs.values());
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
      InputStream is = null;
      try {
        is = new FileInputStream(configFile);
        Collection<ArchetypeCatalogFactory> catalogs = writer.readArchetypeCatalogs(is);
        for(Iterator<ArchetypeCatalogFactory> it = catalogs.iterator(); it.hasNext();) {
          addArchetypeCatalogFactory(it.next());
        }
      } finally {
        IOUtil.close(is);
      }
    }
  }

  public void saveCatalogs() throws IOException {
    OutputStream os = null;
    try {
      os = new FileOutputStream(configFile);
      writer.writeArchetypeCatalogs(getArchetypeCatalogs(), os);
    } finally {
      IOUtil.close(os);
    }
  }

  /**
   * Gets the remote {@link ArtifactRepository} of the given {@link Archetype}, or null if none is found. The repository
   * url is extracted from {@link Archetype#getRepository(). The {@link ArtifactRepository} id is set to
   * <strong>archetypeId+"-repo"</strong>, to enable authentication on that repository.
   * 
   * @see <a
   *      href="http://maven.apache.org/archetype/maven-archetype-plugin/faq.html">http://maven.apache.org/archetype/maven-archetype-plugin/faq.html</a>
   * @param archetype
   * @return the remote {@link ArtifactRepository} of the given {@link Archetype}, or null if none is found.
   * @throws CoreException
   */
  public ArtifactRepository getArchetypeRepository(Archetype archetype) throws CoreException {
    String repoUrl = archetype.getRepository();
    if(repoUrl == null || repoUrl.trim().isEmpty()) {
      return null;
    }
    return MavenPlugin.getMaven().createArtifactRepository(archetype.getArtifactId() + "-repo", repoUrl); //$NON-NLS-1$
  }

  /**
   * Gets the required properties of an {@link Archetype}.
   * 
   * @param archetype the archetype possibly declaring required properties
   * @param remoteArchetypeRepository the remote archetype repository, can be null.
   * @param monitor the progress monitor, can be null.
   * @return the required properties of the archetypes, null if none is found.
   * @throws UnknownArchetype thrown if no archetype is can be resolved
   * @throws CoreException
   */
  public List<?> getRequiredProperties(Archetype archetype, ArtifactRepository remoteArchetypeRepository,
      IProgressMonitor monitor) throws UnknownArchetype, CoreException {
    Assert.isNotNull(archetype, "Archetype can not be null");

    if(monitor == null) {
      monitor = new NullProgressMonitor();
    }

    final String groupId = archetype.getGroupId();
    final String artifactId = archetype.getArtifactId();
    final String version = archetype.getVersion();

    IMaven maven = MavenPlugin.getMaven();

    final List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>(maven.getArtifactRepositories());
    if(remoteArchetypeRepository != null) {
      repositories.add(0, remoteArchetypeRepository);
    }

    @SuppressWarnings("serial")
    final class WrappedUnknownArchetype extends RuntimeException {
      public WrappedUnknownArchetype(UnknownArchetype ex) {
        super(ex);
      }
    }

    try {
      return maven.execute(new ICallable<List<?>>() {
        public List<?> call(IMavenExecutionContext context, IProgressMonitor monitor) {
          ArtifactRepository localRepository = context.getLocalRepository();
          if(aaMgr.isFileSetArchetype(groupId, artifactId, version, null, localRepository, repositories)) {
            ArchetypeDescriptor descriptor;
            try {
              descriptor = aaMgr.getFileSetArchetypeDescriptor(groupId, artifactId, version, null, localRepository,
                  repositories);
            } catch(UnknownArchetype ex) {
              throw new WrappedUnknownArchetype(ex);
            }
            return descriptor.getRequiredProperties();
          }
          return null;
        }
      }, monitor);
    } catch(WrappedUnknownArchetype e) {
      throw (UnknownArchetype) e.getCause();
    }
  }

  /**
   * @since 1.5
   */
  public ArchetypeArtifactManager getArchetypeArtifactManager() {
    return aaMgr;
  }

  /**
   * @since 2.0
   */
  public org.apache.maven.archetype.ArchetypeManager getArchetyper() {
    return archetyper;
  }

  /**
   * @since 1.5
   */
  public ArchetypeDataSource getArchetypeDataSource(String hint) {
    try {
      return container.lookup(ArchetypeDataSource.class, hint);
    } catch(ComponentLookupException ex) {
      throw new NoSuchComponentException(ex);
    }
  }
}
