/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype, Inc.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;
import org.apache.maven.archetype.source.RemoteCatalogArchetypeDataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.project.ProjectBuildingRequest;

import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.Messages;


/**
 * Abstract ArchetypeCatalog factory
 */
public abstract class ArchetypeCatalogFactory {
  private static final Logger log = LoggerFactory.getLogger(ArchetypeCatalogFactory.class);

  private final String id;

  private final String description;

  private final boolean editable;

  private boolean enabled;

  protected ArchetypeCatalogFactory(String id, String description, boolean editable) {
    this(id, description, editable, true);
  }

  protected ArchetypeCatalogFactory(String id, String description, boolean editable, boolean enabled) {
    this.id = id;
    this.description = description;
    this.editable = editable;
    this.enabled = enabled;
  }

  public String getId() {
    return this.id;
  }

  public String getDescription() {
    return this.description;
  }

  public boolean isEditable() {
    return editable;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public abstract ArchetypeCatalog getArchetypeCatalog() throws CoreException;

  @Override
  public String toString() {
    return getId();
  }

  /**
   * Factory for internal ArchetypeCatalog
   */
  public static class InternalCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "internal"; //$NON-NLS-1$

    private ArchetypeDataSource source;

    InternalCatalogFactory(ArchetypeDataSource source) {
      super(ID, Messages.ArchetypeCatalogFactory_internal, false);
      this.source = source;
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() {
      try {
        return source.getArchetypeCatalog(null);
      } catch(ArchetypeDataSourceException e) {
        return new ArchetypeCatalog();
      }
    }
  }

  /**
   * Factory for default local ArchetypeCatalog
   */
  public static class DefaultLocalCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "defaultLocal"; //$NON-NLS-1$

    private ArchetypeDataSource source;

    private IMaven maven;

    DefaultLocalCatalogFactory(IMaven maven, ArchetypeDataSource source) {
      super(ID, Messages.ArchetypeCatalogFactory_default_local, false);
      this.maven = maven;
      this.source = source;
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() throws CoreException {
      return maven.createExecutionContext().execute((ctx, m) -> {
        ProjectBuildingRequest buildingRequest = ctx.newProjectBuildingRequest();
        try {
          return source.getArchetypeCatalog(buildingRequest);
        } catch(ArchetypeDataSourceException e) {
          return new ArchetypeCatalog();
        }
      }, null);
    }
  }

  /**
   * Factory for local ArchetypeCatalog
   */
  public static class LocalCatalogFactory extends ArchetypeCatalogFactory {

    private IMaven maven;

    private ArchetypeDataSource source;

    public LocalCatalogFactory(String path, String description, boolean editable, boolean enabled, IMaven maven,
        ArchetypeDataSource source) {
      super(path,
          description == null || description.trim().length() == 0
              ? NLS.bind(Messages.ArchetypeCatalogFactory_local, path)
              : description,
          editable, enabled);
      this.maven = maven;
      this.source = source;
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() throws CoreException {
      ArchetypeCatalog catalog = getEmbeddedCatalog();
      if(catalog == null) {
        //local but not embedded catalog
        IMavenExecutionContext context = maven.createExecutionContext();
        ArtifactRepository localRepository = new MavenArtifactRepository();
        Path path = Path.of(getId());
        File file = path.toFile().getAbsoluteFile();
        if(file.isFile()) {
          file = file.getParentFile();
        }
        localRepository.setUrl(file.toURI().toString());
        context.getExecutionRequest().setLocalRepository(localRepository);
        return context.execute((ctx, m) -> {
          ProjectBuildingRequest buildingRequest = ctx.newProjectBuildingRequest();
          buildingRequest.setLocalRepository(localRepository);
          try {
            return source.getArchetypeCatalog(buildingRequest);
          } catch(ArchetypeDataSourceException e) {
            return new ArchetypeCatalog();
          }
        }, null);
      }
      return catalog;
    }

    private ArchetypeCatalog getEmbeddedCatalog() throws CoreException {
      URL url = getEmbeddedUrl();
      if(url == null) {
        //Not an embedded catalog, nothing else to do
        return null;
      }
      try (InputStream is = new BufferedInputStream(url.openStream())) {
        return new ArchetypeCatalogXpp3Reader().read(is);
      } catch(Exception ex) {
        String msg = NLS.bind(Messages.ArchetypeCatalogFactory_error_missing_catalog, ex.getMessage());
        log.error(msg, ex);
        throw new CoreException(Status.error(msg, ex));
      }
    }

    private URL getEmbeddedUrl() {
      String path = getId();
      if(path != null && path.startsWith("bundleentry://")) {
        try {
          return new URL(path);
        } catch(Exception ex) {
          log.error(ex.getMessage(), ex);
        }
      }
      return null;
    }
  }

  /**
   * Factory for remote ArchetypeCatalog
   */
  public static class RemoteCatalogFactory extends ArchetypeCatalogFactory {

    private String repositoryUrl = null;

    private IMaven maven;

    private ArchetypeDataSource source;

    RemoteCatalogFactory(String url, String description, boolean editable, boolean enabled, IMaven maven,
        ArchetypeDataSource source) {
      super(url,
          description == null || description.trim().length() == 0
              ? NLS.bind(Messages.ArchetypeCatalogFactory_remote, url)
              : description,
          editable, enabled);
      this.maven = maven;
      this.source = source;
      repositoryUrl = parseCatalogUrl(url);
    }

    private String parseCatalogUrl(String url) {
      if(url == null) {
        return null;
      }
      int length = url.length();
      if(length > 1 && url.endsWith("/")) {
        return url.substring(0, url.length() - 1);
      }
      int idx = Math.max(url.lastIndexOf("/"), 0);
      //Assume last fragment of the url is a file, let's keep its parent folder
      return url.lastIndexOf(".") >= idx ? url.substring(0, idx) : url;
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() throws CoreException {
      String url = getId();
      int idx = url.lastIndexOf("/archetype-catalog.xml");
      if(idx > -1) {
        url = url.substring(0, idx);
      }
      final String remoteUrl = url;

      ArchetypeCatalog catalog = maven.createExecutionContext().execute((ctx, m) -> {
        ProjectBuildingRequest buildingRequest = ctx.newProjectBuildingRequest();
        try {
          ArtifactRepository archeTypeRepo = new MavenArtifactRepository();
          archeTypeRepo.setUrl(remoteUrl);
          archeTypeRepo.setId(RemoteCatalogArchetypeDataSource.ARCHETYPE_REPOSITORY_ID);
          buildingRequest.getRemoteRepositories().add(archeTypeRepo);
          return source.getArchetypeCatalog(buildingRequest);
        } catch(ArchetypeDataSourceException e) {
          return new ArchetypeCatalog();
        }
      }, null);

      @SuppressWarnings("serial")
      ArchetypeCatalog catalogWrapper = new ArchetypeCatalog() {
        @Override
        public void addArchetype(Archetype archetype) {
          catalog.addArchetype(archetype);
        }

        @Override
        public List<Archetype> getArchetypes() {
          List<Archetype> archetypes = new ArrayList<>(catalog.getArchetypes());
          for(Archetype arch : archetypes) {
            if(arch.getRepository() == null || arch.getRepository().trim().isEmpty()) {
              arch.setRepository(remoteUrl);
            }
          }
          return archetypes;
        }

        @Override
        public String getModelEncoding() {
          return catalog.getModelEncoding();
        }

        @Override
        public void removeArchetype(Archetype archetype) {
          catalog.removeArchetype(archetype);
        }

        @Override
        public void setModelEncoding(String modelEncoding) {
          catalog.setModelEncoding(modelEncoding);
        }

        @Override
        public void setArchetypes(List<Archetype> archetypes) {
          catalog.setArchetypes(archetypes);
        }

        @Override
        public String toString() {
          return catalog.toString();
        }
      };

      return catalogWrapper;
    }

    /**
     * @return the url of the remote repository hosting the catalog
     */
    public String getRepositoryUrl() {
      return repositoryUrl;
    }
  }

}
