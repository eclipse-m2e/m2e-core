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

package org.eclipse.m2e.internal.discovery;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.MappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingElementKey;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDisovery;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.internal.discovery.operation.MavenDiscoveryInstallOperation;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;


@SuppressWarnings({"restriction", "rawtypes"})
public class MavenDiscoveryService implements IMavenDisovery, ServiceFactory {

  public static class CatalogItemCacheEntry {
    private final CatalogItem item;

    private final LifecycleMappingMetadataSource metadataSource;

    public CatalogItemCacheEntry(CatalogItem item, LifecycleMappingMetadataSource metadataSource) {
      this.item = item;
      this.metadataSource = metadataSource;
    }

    public CatalogItem getItem() {
      return item;
    }

    public LifecycleMappingMetadataSource getMetadataSource() {
      return metadataSource;
    }
  }

  private List<CatalogItemCacheEntry> items;

  public MavenDiscoveryService() {
    this(true);
  }

  public MavenDiscoveryService(boolean factory) {
  }

  public Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> discover(MavenProject mavenProject,
      List<MojoExecution> mojoExecutions, List<IMavenDiscoveryProposal> preselected, IProgressMonitor monitor)
      throws CoreException {

    if(items == null) {
      items = new ArrayList<MavenDiscoveryService.CatalogItemCacheEntry>();

      Catalog catalog = MavenDiscovery.getCatalog();
      IStatus status = catalog.performDiscovery(monitor);

      if(!status.isOK()) {
        // XXX log and/or throw something heavy at the caller
        return null;
      }

      for(CatalogItem item : catalog.getItems()) {
        LifecycleMappingMetadataSource metadataSource = MavenDiscovery.getLifecycleMappingMetadataSource(item);
        if(metadataSource != null) {
          addCatalogItem(item, metadataSource);
        }
      }
    }

    Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> proposals = new LinkedHashMap<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>>();

    MavenPlugin mavenPlugin = MavenPlugin.getDefault();
    IMaven maven = mavenPlugin.getMaven();

    MavenExecutionRequest request = maven.createExecutionRequest(monitor); // TODO this ignores workspace dependencies

    List<CatalogItem> selectedItems = toCatalogItems(preselected);
    List<LifecycleMappingMetadataSource> selectedSources = toMetadataSources(preselected);

    for(CatalogItemCacheEntry itemEntry : items) {
      CatalogItem item = itemEntry.getItem();
      LifecycleMappingMetadataSource src = itemEntry.getMetadataSource();

      boolean preselectItem = false;
      for(CatalogItem selectedItem : selectedItems) {
        if(selectedItem.getSiteUrl().equals(item.getSiteUrl())
            && selectedItem.getInstallableUnits().equals(item.getInstallableUnits())) {
          preselectItem = true;
          break;
        }
      }

      if(src != null) {
        src.setSource(item);

        LifecycleMappingResult mappingResult = new LifecycleMappingResult();

        List<LifecycleMappingMetadataSource> sources = new ArrayList<LifecycleMappingMetadataSource>(selectedSources);
        if(!preselectItem) {
          sources.add(src);
        }

        List<MappingMetadataSource> metadataSources = LifecycleMappingFactory.getProjectMetadataSources(request,
            mavenProject, sources, monitor);

        LifecycleMappingFactory.calculateEffectiveLifecycleMappingMetadata(mappingResult, metadataSources,
            mavenProject, mojoExecutions);

        LifecycleMappingMetadata lifecycleMappingMetadata = mappingResult.getLifecycleMappingMetadata();
        if(lifecycleMappingMetadata != null) {
          IMavenDiscoveryProposal proposal = getProposal(lifecycleMappingMetadata.getSource());
          if(proposal != null) {
            put(proposals, new PackagingTypeMappingConfiguration.Key(mavenProject.getPackaging()), proposal);
          }
        }

        for(Map.Entry<MojoExecutionKey, List<PluginExecutionMetadata>> entry : mappingResult.getMojoExecutionMapping()
            .entrySet()) {
          if(entry.getValue() != null) {
            for(PluginExecutionMetadata executionMapping : entry.getValue()) {
              IMavenDiscoveryProposal proposal = getProposal(executionMapping.getSource());
              if(proposal != null) {
                put(proposals, new MojoExecutionMappingConfiguration.Key(entry.getKey()), proposal);
              }
              // TODO match eclipse extensions provided by the catalog item
              // User Story.
              // Project pom.xml explicitly specifies lifecycle mapping strategy implementation, 
              // but the implementation is not currently installed. As a user I expect m2e to search 
              // marketplace for the implementation and offer installation if available
            }
          }
        }
      }
    }

    return proposals;
  }

  public void addCatalogItem(CatalogItem item, LifecycleMappingMetadataSource metadataSource) {
    if(items == null) {
      // for tests
      items = new ArrayList<MavenDiscoveryService.CatalogItemCacheEntry>();
    }
    items.add(new CatalogItemCacheEntry(item, metadataSource));
  }

  private IMavenDiscoveryProposal getProposal(LifecycleMappingMetadataSource src) {
    if(src == null) {
      return null;
    }
    if(src.getSource() instanceof CatalogItem) {
      return new InstallCatalogItemMavenDiscoveryProposal((CatalogItem) src.getSource());
    }
    return null;
  }

  private List<LifecycleMappingMetadataSource> toMetadataSources(List<IMavenDiscoveryProposal> proposals) {
    List<LifecycleMappingMetadataSource> sources = new ArrayList<LifecycleMappingMetadataSource>();
    for(IMavenDiscoveryProposal proposal : proposals) {
      if(proposal instanceof InstallCatalogItemMavenDiscoveryProposal) {
        CatalogItem catalogItem = ((InstallCatalogItemMavenDiscoveryProposal) proposal).getCatalogItem();
        LifecycleMappingMetadataSource source = MavenDiscovery.getLifecycleMappingMetadataSource(catalogItem);
        source.setSource(catalogItem);
        sources.add(source);
      }
    }
    return sources;
  }

  private void put(Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> allproposals,
      ILifecycleMappingElementKey requirement, IMavenDiscoveryProposal proposal) {

    List<IMavenDiscoveryProposal> proposals = allproposals.get(requirement);
    if(proposals == null) {
      proposals = new ArrayList<IMavenDiscoveryProposal>();
      allproposals.put(requirement, proposals);
    }

    if(!proposals.contains(proposal)) {
      proposals.add(proposal);
    }
  }

  public void implement(List<IMavenDiscoveryProposal> proposals, IProgressMonitor monitor) {
    List<CatalogItem> items = toCatalogItems(proposals);

    MavenDiscoveryInstallOperation op = new MavenDiscoveryInstallOperation(items, false);
    try {
      op.run(monitor);
    } catch(InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private List<CatalogItem> toCatalogItems(List<IMavenDiscoveryProposal> proposals) {
    List<CatalogItem> items = new ArrayList<CatalogItem>();
    for(IMavenDiscoveryProposal proposal : proposals) {
      if(proposal instanceof InstallCatalogItemMavenDiscoveryProposal) {
        items.add(((InstallCatalogItemMavenDiscoveryProposal) proposal).getCatalogItem());
      }
    }
    return items;
  }

  public boolean isRestartRequired(List<IMavenDiscoveryProposal> proposals, IProgressMonitor monitor) {
    return MavenDiscovery.requireRestart(toCatalogItems(proposals));
  }

  public Object getService(Bundle bundle, ServiceRegistration registration) {
    return new MavenDiscoveryService(false); // not a factory instance
  }

  public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
  }

}
