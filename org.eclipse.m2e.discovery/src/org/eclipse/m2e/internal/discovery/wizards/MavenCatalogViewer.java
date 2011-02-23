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

import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogConfiguration;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("restriction")
public class MavenCatalogViewer extends CatalogViewer {
  public static final Logger log = LoggerFactory.getLogger(MavenCatalogViewer.class);

  private static final String CONFIGURATOR_PREFIX = "configurator:"; //$NON-NLS-1$

  private static final String LIFECYCLE_PREFIX = "lifecycle:"; //$NON-NLS-1$

  private static final String PATH = "lifecycle/"; //$NON-NLS-1$

  private static final String EXT = ".xml"; //$NON-NLS-1$

  public MavenCatalogViewer(Catalog catalog, IShellProvider shellProvider, IRunnableContext context,
      CatalogConfiguration configuration) {
    super(catalog, shellProvider, context, configuration);
  }

  @Override
  protected void postDiscovery() {
    super.postDiscovery();

    final MavenCatalogConfiguration config = (MavenCatalogConfiguration) getConfiguration();
    final Collection<String> selectedPackagingTypes = config.getSelectedPackagingTypes();
    final Collection<MojoExecutionKey> selectedMojos = config.getSelectedMojos();
    final Collection<String> selectedLifecycleIds = config.getSelectedLifecycleIds();
    final Collection<String> selectedConfiguratorIds = config.getSelectedConfiguratorIds();

    shellProvider.getShell().getDisplay().syncExec(new Runnable() {
      @SuppressWarnings("synthetic-access")
      public void run() {
        for(CatalogItem ci : getCatalog().getItems()) {
          boolean selected = false;

          LifecycleMappingMetadataSource src = MavenDiscovery.getLifecycleMappingMetadataSource(ci);
          if(src != null) {
            for(String packagingType : selectedPackagingTypes) {
              if(hasPackaging(src, packagingType)) {
                selected = true;
                select(ci);
                break;
              }
            }
            if(selected) {
              continue;
            }
            for(MojoExecutionKey mojoExecution : selectedMojos) {
              if(matchesFilter(src, mojoExecution)) {
                selected = true;
                select(ci);
                break;
              }
            }
            if(selected) {
              continue;
            }
          }

          for(String configuratorId : selectedConfiguratorIds) {
            Tag configuratorIdTag = new Tag(CONFIGURATOR_PREFIX + configuratorId, CONFIGURATOR_PREFIX + configuratorId);
            if(ci.hasTag(configuratorIdTag)) {
              selected = true;
              select(ci);
              break;
            }
          }
          if(selected) {
            continue;
          }

          for(String lifecycleId : selectedLifecycleIds) {
            Tag lifecycleIdTag = new Tag(LIFECYCLE_PREFIX + lifecycleId, LIFECYCLE_PREFIX + lifecycleId);
            if(ci.hasTag(lifecycleIdTag)) {
              select(ci);
              break;
            }
          }
        }
      }
    });
  }

  private void select(CatalogItem ci) {
    modifySelection(ci, true);
    ci.addTag(MavenDiscovery.APPLICABLE_TAG);
  }

  private static boolean matchesFilter(LifecycleMappingMetadataSource src, MojoExecutionKey mojoExecution) {
    for(PluginExecutionMetadata p : src.getPluginExecutions()) {
      if(p.getFilter().match(mojoExecution)) {
        return true;
      }
    }
    return false;
  }

  public static URL getLifecycleMappingMetadataSourceURL(CatalogItem ci) {
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
