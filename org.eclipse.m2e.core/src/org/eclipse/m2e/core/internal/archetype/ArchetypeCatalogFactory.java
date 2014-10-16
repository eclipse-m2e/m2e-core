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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.Messages;


/**
 * Abstract ArchetypeCatalog factory
 */
public abstract class ArchetypeCatalogFactory {
  private static final Logger log = LoggerFactory.getLogger(ArchetypeCatalogFactory.class);

  private final String id;

  private final String description;

  private final boolean editable;

  public ArchetypeCatalogFactory(String id, String description, boolean editable) {
    this.id = id;
    this.description = description;
    this.editable = editable;
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

  public abstract ArchetypeCatalog getArchetypeCatalog() throws CoreException;

  public String toString() {
    return getId();
  }

  protected ArchetypeManager getArchetyper() {
    return MavenPluginActivator.getDefault().getArchetypeManager().getArchetyper();
  }

  /**
   * Factory for Nexus Indexer ArchetypeCatalog
   */
  public static class NexusIndexerCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "nexusIndexer"; //$NON-NLS-1$

    public NexusIndexerCatalogFactory() {
      super(ID, Messages.ArchetypeCatalogFactory_indexer_catalog, false);
    }

    public ArchetypeCatalog getArchetypeCatalog() throws CoreException {
      try {
        ArchetypeDataSource source = MavenPluginActivator.getDefault().getIndexManager().getArchetypeCatalog();
        return source.getArchetypeCatalog(new Properties());
      } catch(ArchetypeDataSourceException ex) {
        String msg = NLS.bind(Messages.ArchetypeCatalogFactory_error_missing_catalog, ex.getMessage());
        log.error(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
      }
    }

  }

  /**
   * Factory for internal ArchetypeCatalog
   */
  public static class InternalCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "internal"; //$NON-NLS-1$

    public InternalCatalogFactory() {
      super(ID, Messages.ArchetypeCatalogFactory_internal, false);
    }

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

    public ArchetypeCatalog getArchetypeCatalog() {
      return getArchetyper().getDefaultLocalCatalog();
    }
  }

  /**
   * Factory for local ArchetypeCatalog
   */
  public static class LocalCatalogFactory extends ArchetypeCatalogFactory {

    public LocalCatalogFactory(String path, String description, boolean editable) {
      super(path, description == null || description.trim().length() == 0 ? NLS.bind(
          Messages.ArchetypeCatalogFactory_local, path) : description, editable);
    }

    public ArchetypeCatalog getArchetypeCatalog() {
      return getArchetyper().getLocalCatalog(getId());
    }
  }

  /**
   * Factory for remote ArchetypeCatalog
   */
  public static class RemoteCatalogFactory extends ArchetypeCatalogFactory {

    private String repositoryUrl = null;

    public RemoteCatalogFactory(String url, String description, boolean editable) {
      super(url, description == null || description.trim().length() == 0 ? NLS.bind(
          Messages.ArchetypeCatalogFactory_remote, url) : description, editable);
      repositoryUrl = parseCatalogUrl(url);
    }

    /**
     * @param url
     * @return
     */
    private String parseCatalogUrl(String url) {
      if(url == null) {
        return null;
      }
      int length = url.length();
      if(length > 1 && url.endsWith("/")) //$NON-NLS-1$
      {
        return url.substring(0, url.length() - 1);
      }
      int idx = url.lastIndexOf("/"); //$NON-NLS-1$
      idx = (idx > 0) ? idx : 0;
      if(url.lastIndexOf(".") >= idx) { //$NON-NLS-1$
        //Assume last fragment of the url is a file, let's keep its parent folder
        return url.substring(0, idx);
      }
      return url;
    }

    public ArchetypeCatalog getArchetypeCatalog() {
      String url = getId();
      int idx = url.lastIndexOf("/archetype-catalog.xml");
      if(idx > -1) {
        url = url.substring(0, idx);
      }
      final ArchetypeCatalog catalog = getArchetyper().getRemoteCatalog(url);
      final String remoteUrl = url;
      @SuppressWarnings("serial")
      ArchetypeCatalog catalogWrapper = new ArchetypeCatalog() {
        public void addArchetype(org.apache.maven.archetype.catalog.Archetype archetype) {
          catalog.addArchetype(archetype);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public List getArchetypes() {
          List<org.apache.maven.archetype.catalog.Archetype> archetypes = new ArrayList<org.apache.maven.archetype.catalog.Archetype>(
              catalog.getArchetypes());
          for(org.apache.maven.archetype.catalog.Archetype arch : archetypes) {
            if(arch.getRepository() == null || arch.getRepository().trim().isEmpty()) {
              arch.setRepository(remoteUrl);
            }
          }
          return archetypes;
        }

        public String getModelEncoding() {
          return catalog.getModelEncoding();
        }

        public void removeArchetype(org.apache.maven.archetype.catalog.Archetype archetype) {
          catalog.removeArchetype(archetype);
        }

        public void setModelEncoding(String modelEncoding) {
          catalog.setModelEncoding(modelEncoding);
        }

        public void setArchetypes(List archetypes) {
          catalog.setArchetypes(archetypes);
        }

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
