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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.archetype.source.RemoteCatalogArchetypeDataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.project.ProjectBuildingRequest;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;


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

  ArchetypeManager getArchetyper() {
    return M2EUIPluginActivator.getDefault().getArchetypeManager().getArchetyper();
  }

  /**
   * Factory for internal ArchetypeCatalog
   */
  public static class InternalCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "internal"; //$NON-NLS-1$

    public InternalCatalogFactory() {
      super(ID, Messages.ArchetypeCatalogFactory_internal, false);
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() {
      return getArchetyper().getInternalCatalog();
    }
  }

  /**
   * Factory for default local ArchetypeCatalog
   */
  public static class DefaultLocalCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "defaultLocal"; //$NON-NLS-1$

    public DefaultLocalCatalogFactory() {
      super(ID, Messages.ArchetypeCatalogFactory_default_local, false);
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() throws CoreException {
      return executeWithRequest(getArchetyper()::getLocalCatalog);
    }
  }

  /**
   * Factory for local ArchetypeCatalog
   */
  public static class LocalCatalogFactory extends ArchetypeCatalogFactory {

    public LocalCatalogFactory(String path, String description, boolean editable) {
      this(path, description, editable, true);
    }

    public LocalCatalogFactory(String path, String description, boolean editable, boolean enabled) {
      super(path,
          description == null || description.trim().length() == 0
              ? NLS.bind(Messages.ArchetypeCatalogFactory_local, path)
              : description,
          editable, enabled);
    }

    @Override
    public ArchetypeCatalog getArchetypeCatalog() throws CoreException {
      ArchetypeCatalog catalog = getEmbeddedCatalog();
      if(catalog == null) {
        //local but not embedded catalog
        catalog = executeWithRequest(request -> {
          ArtifactRepository localRepository = new MavenArtifactRepository();
          //TODO: test this
          String path = getId();
          localRepository.setUrl(Path.of(path).toUri().toString());
          request.setLocalRepository(localRepository);
          return getArchetyper().getLocalCatalog(request);
        });
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

    public RemoteCatalogFactory(String url, String description, boolean editable) {
      this(url, description, editable, true);
    }

    public RemoteCatalogFactory(String url, String description, boolean editable, boolean enabled) {
      super(url,
          description == null || description.trim().length() == 0
              ? NLS.bind(Messages.ArchetypeCatalogFactory_remote, url)
              : description,
          editable, enabled);
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
      ArchetypeCatalog catalog = executeWithRequest(request -> {
        ArtifactRepository archeTypeRepo = new MavenArtifactRepository();
        archeTypeRepo.setUrl(remoteUrl);
        archeTypeRepo.setId(RemoteCatalogArchetypeDataSource.ARCHETYPE_REPOSITORY_ID);
        request.getRemoteRepositories().add(archeTypeRepo);
        return getArchetyper().getRemoteCatalog(request);
      });

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

  private static <T> T executeWithRequest(Function<ProjectBuildingRequest, T> action) throws CoreException {
    return MavenPlugin.getMaven().createExecutionContext().execute((ctx, m) -> {
      ProjectBuildingRequest buildingRequest = ctx.newProjectBuildingRequest();
      return action.apply(buildingRequest);
    }, null);
  }

}
