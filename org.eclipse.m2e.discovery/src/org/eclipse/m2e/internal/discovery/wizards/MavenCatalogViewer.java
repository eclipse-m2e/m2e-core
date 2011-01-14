/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.wizards;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.Messages;
import org.eclipse.osgi.util.NLS;


@SuppressWarnings("restriction")
public class MavenCatalogViewer extends CatalogViewer {

  private static final String PATH = "lifecycle/"; //$NON-NLS-1$

  private static final String EXT = ".xml"; //$NON-NLS-1$

  private static final String ID = "org.eclipse.m2e.discovery"; //$NON-NLS-1$

  public MavenCatalogViewer(Catalog catalog, IShellProvider shellProvider, IRunnableContext context,
      CatalogConfiguration configuration) {
    super(catalog, shellProvider, context, configuration);
  }

  @Override
  protected void postDiscovery() {
    super.postDiscovery();

    final MavenCatalogConfiguration config = (MavenCatalogConfiguration) getConfiguration();
    final Collection<String> selectedPackagingTypes = config.getSelectedPackagingTypes();

    shellProvider.getShell().getDisplay().syncExec(new Runnable() {
      @SuppressWarnings("synthetic-access")
      public void run() {
        Map<String, Set<CatalogItem>> map = new HashMap<String, Set<CatalogItem>>();

        for(String packagingType : selectedPackagingTypes) {
          map.put(packagingType, new HashSet<CatalogItem>());
          for(CatalogItem ci : getCatalog().getItems()) {
            LifecycleMappingMetadataSource src = getLifecycleMappingMetadataSource(ci);
            if(src != null && hasPackaging(src, packagingType)) {
              Set<CatalogItem> items = map.get(packagingType);
              items.add(ci);
            }
          }
        }

        // Select relevant CatalogItems
        // TODO Make selection smarter
        for(Entry<String, Set<CatalogItem>> type : map.entrySet()) {
          if(type.getValue().isEmpty()) {
            MavenLogger.log(NLS.bind(Messages.MavenCatalogViewer_Missing_packaging_type, type.getKey()));
          }
          for(CatalogItem ci : type.getValue()) {
            modifySelection(ci, true);
            ci.addTag(MavenDiscovery.APPLICABLE_TAG);
          }
        }
      }
    });
  }
  
  private static LifecycleMappingMetadataSource getLifecycleMappingMetadataSource(CatalogItem ci) {
    try {
      return LifecycleMappingFactory.createLifecycleMappingMetadataSource(getLifecycleMappingMetadataSourceURL(ci));
    } catch(Exception e) {
      MavenLogger.log(new Status(IStatus.WARNING, ID, NLS.bind(Messages.MavenCatalogViewer_Error_loading_lifecycle,
          ci.getId()), e));
      return null;
    }
  }

  private static URL getLifecycleMappingMetadataSourceURL(CatalogItem ci) {
    return ci.getSource().getResource(PATH + ci.getId() + EXT);
  }

  private static boolean hasPackaging(LifecycleMappingMetadataSource lifecycleMappingMetadataSource,
      String packagingType) {
    for(LifecycleMappingMetadata lifecycleMappingMetadata : lifecycleMappingMetadataSource.getLifecycleMappings()) {
      if(packagingType.equals(lifecycleMappingMetadata.getPackagingType())) {
        return true;
      }
    }
    return false;
  }
}
