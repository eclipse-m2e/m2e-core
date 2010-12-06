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

package org.eclipse.m2e.core.archetype;

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
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.archetype.catalog.Archetype;




/**
 * Archetype Manager
 * 
 * @author Eugene Kuleshov
 */
public class ArchetypeManager {

  private final Map<String, ArchetypeCatalogFactory> catalogs = new LinkedHashMap<String, ArchetypeCatalogFactory>();

  private final File configFile;
  
  private final ArchetypeCatalogsWriter writer;

  public ArchetypeManager(File configFile) {
    this.configFile = configFile;
    this.writer = new ArchetypeCatalogsWriter();
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
   * @return the archetypeCatalogFactory containing the archetype parameter, null if none was found.
   */
  public <T extends ArchetypeCatalogFactory> T findParentCatalogFactory(Archetype a, Class<T> type) throws CoreException {
    if (a!=null){
      for (ArchetypeCatalogFactory factory : getArchetypeCatalogs()) {
        if ((type.isAssignableFrom(factory.getClass())) 
           //temporary hack to get around https://issues.sonatype.org/browse/MNGECLIPSE-1792
           //cf. MavenProjectWizardArchetypePage.getAllArchetypes 
           && !(factory.getDescription() != null && factory.getDescription().startsWith("Test")) //$NON-NLS-1$
           && factory.getArchetypeCatalog().getArchetypes().contains(a)) {
          return (T)factory; 
        }
      }
    }
    return null;
  }

}
