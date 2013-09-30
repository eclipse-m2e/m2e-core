/*******************************************************************************
 * Copyright (c) 2011-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - discover proposals for ILifecycleMappingRequirements
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.Messages;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.MappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.SimpleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.ProjectConfiguratorMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.LifecycleStrategyMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.PackagingTypeMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.wizards.IMavenDiscoveryUI;
import org.eclipse.m2e.internal.discovery.operation.MavenDiscoveryInstallOperation;
import org.eclipse.m2e.internal.discovery.wizards.MavenDiscoveryUi;


@SuppressWarnings({"restriction", "rawtypes"})
public class MavenDiscoveryService implements IMavenDiscoveryUI, IMavenDiscovery, ServiceFactory {
  private static final Logger log = LoggerFactory.getLogger(MavenDiscoveryService.class);

  public static class CatalogItemCacheEntry {
    private final CatalogItem item;

    private final LifecycleMappingMetadataSource metadataSource;

    private final List<String> projectConfigurators;

    private final List<String> mappingStrategies;

    public CatalogItemCacheEntry(CatalogItem item, LifecycleMappingMetadataSource metadataSource,
        List<String> projectConfigurators, List<String> mappingStrategies) {
      this.item = item;
      this.metadataSource = metadataSource;
      this.projectConfigurators = projectConfigurators;
      this.mappingStrategies = mappingStrategies;
    }

    public CatalogItem getItem() {
      return item;
    }

    public LifecycleMappingMetadataSource getMetadataSource() {
      return metadataSource;
    }

    public List<String> getProjectConfigurators() {
      return projectConfigurators;
    }

    public List<String> getMappingStrategies() {
      return mappingStrategies;
    }
  }

  private List<CatalogItemCacheEntry> items;

  /**
   * Lock guarding lazy instantiation of item instance
   */
  private final Object itemsLock = new Object();

  public MavenDiscoveryService() {
    this(true);
  }

  public MavenDiscoveryService(boolean factory) {
  }

  public Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> discover(final MavenProject mavenProject,
      final List<MojoExecution> mojoExecutions, final List<IMavenDiscoveryProposal> preselected,
      final IProgressMonitor monitor) throws CoreException {

    initializeCatalog(monitor);
    if(items == null) {
      return Collections.emptyMap();
    }

    return MavenPlugin.getMaven().execute(
        new ICallable<Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>>>() {
          public Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> call(IMavenExecutionContext context,
              IProgressMonitor monitor) throws CoreException {
            return discover0(mavenProject, mojoExecutions, preselected, monitor);
          }
        }, monitor);
  }

  private void initializeCatalog(final IProgressMonitor monitor) {
    synchronized(itemsLock) {
      if(items == null) {
        items = new ArrayList<MavenDiscoveryService.CatalogItemCacheEntry>();

        Catalog catalog = MavenDiscovery.getCatalog();
        IStatus status = catalog.performDiscovery(monitor);

        if(!status.isOK()) {
          log.error(status.toString());
          return;
        }

        IProvisioningAgent p2agent = ProvisioningUI.getDefaultUI().getSession().getProvisioningAgent();
        IProfileRegistry profRegistry = (IProfileRegistry) p2agent.getService(IProfileRegistry.SERVICE_NAME);
        IProfile profile = profRegistry.getProfile(IProfileRegistry.SELF);

        for(CatalogItem item : catalog.getItems()) {
          LifecycleMappingMetadataSource metadataSource = MavenDiscovery.getLifecycleMappingMetadataSource(item);
          List<String> projectConfigurators = new ArrayList<String>();
          List<String> mappingStrategies = new ArrayList<String>();
          MavenDiscovery.getProvidedProjectConfigurators(item, projectConfigurators, mappingStrategies);
          if(metadataSource != null && !itemInstalled(profile, item, monitor)) {
            addCatalogItem(item, metadataSource, projectConfigurators, mappingStrategies);
          }
        }
        catalog.dispose();
      }
    }
  }

  /*package*/Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> discover0(MavenProject mavenProject,
      List<MojoExecution> mojoExecutions, List<IMavenDiscoveryProposal> preselected, IProgressMonitor monitor)
      throws CoreException {
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = new LinkedHashMap<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>>();

    Collection<CatalogItem> selectedItems = toCatalogItems(preselected);
    List<LifecycleMappingMetadataSource> selectedSources = toMetadataSources(preselected);

    Map<String, List<MappingMetadataSource>> metadataSourcesMap = LifecycleMappingFactory.getProjectMetadataSourcesMap(
        mavenProject, null, mojoExecutions, false, monitor);

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
        log.debug("Considering catalog item '{}' for project {}", item.getName(), mavenProject.getName()); //$NON-NLS-1$

        src.setSource(item);

        LifecycleMappingResult mappingResult = new LifecycleMappingResult();

        List<LifecycleMappingMetadataSource> sources = new ArrayList<LifecycleMappingMetadataSource>(selectedSources);
        if(!preselectItem) {
          sources.add(src);
        }

        metadataSourcesMap.put("bundleMetadataSources",
            Collections.singletonList((MappingMetadataSource) new SimpleMappingMetadataSource(sources)));

        List<MappingMetadataSource> metadataSources = LifecycleMappingFactory.asList(metadataSourcesMap);
        LifecycleMappingFactory.calculateEffectiveLifecycleMappingMetadata(mappingResult, metadataSources,
            mavenProject, mojoExecutions, false, monitor);

        LifecycleMappingMetadata lifecycleMappingMetadata = mappingResult.getLifecycleMappingMetadata();
        if(lifecycleMappingMetadata != null) {
          IMavenDiscoveryProposal proposal = getProposal(lifecycleMappingMetadata.getSource());
          if(proposal != null) {
            put(proposals,
                new PackagingTypeMappingConfiguration.PackagingTypeMappingRequirement(mavenProject.getPackaging()),
                proposal);
          } else if(!LifecycleMappingFactory.getLifecycleMappingExtensions().containsKey(
              lifecycleMappingMetadata.getLifecycleMappingId())) {
            if(itemEntry.getMappingStrategies().contains(lifecycleMappingMetadata.getLifecycleMappingId())) {
              put(proposals, new PackagingTypeMappingConfiguration.LifecycleStrategyMappingRequirement(
                  lifecycleMappingMetadata.getPackagingType(), lifecycleMappingMetadata.getLifecycleMappingId()),
                  new InstallCatalogItemMavenDiscoveryProposal(item));
            }
          }
        }

        for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : mappingResult.getMojoExecutionMapping()
            .entrySet()) {
          if(entry.getValue() != null) {
            for(IPluginExecutionMetadata executionMapping : entry.getValue()) {
              log.debug("mapping proposal {} => {}", entry.getKey().toString(), executionMapping.getAction().toString()); //$NON-NLS-1$
              IMavenDiscoveryProposal proposal = getProposal(((PluginExecutionMetadata) executionMapping).getSource());
              if(proposal != null) {
                // assumes installation of mapping proposal installs all required project configurators 
                put(proposals, new MojoExecutionMappingConfiguration.MojoExecutionMappingRequirement(entry.getKey()),
                    proposal);
              } else if(executionMapping.getAction() == PluginExecutionAction.configurator) {
                // we have <configurator/> mapping from pom.xml
                String configuratorId = LifecycleMappingFactory.getProjectConfiguratorId(executionMapping);
                if(!LifecycleMappingFactory.getProjectConfiguratorExtensions().containsKey(configuratorId)) {
                  // User Story.
                  // Project pom.xml explicitly specifies lifecycle mapping strategy implementation, 
                  // but the implementation is not currently installed. As a user I expect m2e to search 
                  // marketplace for the implementation and offer installation if available

                  if(itemEntry.getProjectConfigurators().contains(configuratorId)) {
                    put(proposals,
                        new MojoExecutionMappingConfiguration.ProjectConfiguratorMappingRequirement(entry.getKey(),
                            configuratorId), new InstallCatalogItemMavenDiscoveryProposal(item));
                  }
                }
              }
            }
          }
        }
      }
    }

    return proposals;
  }

  /**
   * Returns true if all IUs specified in the catalog item are installed in the profile
   */
  public boolean itemInstalled(IProfile profile, CatalogItem item, IProgressMonitor monitor) {
    if(profile == null) {
      return false;
    }

    List<IQuery<IInstallableUnit>> queries = new ArrayList<IQuery<IInstallableUnit>>();

    for(String iuId : item.getInstallableUnits()) {
      queries.add(QueryUtil.createIUQuery(iuId));
    }

    IQueryResult<IInstallableUnit> result = profile.query(QueryUtil.createCompoundQuery(queries, true), monitor);

    return !result.isEmpty();
  }

  public void addCatalogItem(CatalogItem item, LifecycleMappingMetadataSource metadataSource,
      List<String> projectConfigurators, List<String> mappingStrategies) {
    if(items == null) {
      // for tests
      items = new ArrayList<MavenDiscoveryService.CatalogItemCacheEntry>();
    }
    items.add(new CatalogItemCacheEntry(item, metadataSource, projectConfigurators, mappingStrategies));
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

  private void put(Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> allproposals,
      ILifecycleMappingRequirement requirement, IMavenDiscoveryProposal proposal) {

    List<IMavenDiscoveryProposal> proposals = allproposals.get(requirement);
    if(proposals == null) {
      proposals = new ArrayList<IMavenDiscoveryProposal>();
      allproposals.put(requirement, proposals);
    }

    if(!proposals.contains(proposal)) {
      proposals.add(proposal);
    }
  }

  public boolean implement(List<IMavenDiscoveryProposal> proposals, IRunnableWithProgress postInstallHook,
      IRunnableContext context, Collection<String> projectsToConfigure) {
    try {
      MavenDiscoveryInstallOperation runner = new MavenDiscoveryInstallOperation(toCatalogItems(proposals),
          postInstallHook, true, false, projectsToConfigure);
      context.run(true, true, runner);
      int openInstallWizard = MavenDiscoveryUi.openInstallWizard(runner.getOperation(), true);
      return openInstallWizard == Window.OK;
    } catch(InvocationTargetException e) {
      IStatus status = new Status(IStatus.ERROR, DiscoveryActivator.PLUGIN_ID, NLS.bind(
          Messages.ConnectorDiscoveryWizard_installProblems, new Object[] {e.getCause().getMessage()}), e.getCause());
      StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
      return false;
    } catch(InterruptedException e) {
      // canceled
      return false;
    }
  }

  private Collection<CatalogItem> toCatalogItems(List<IMavenDiscoveryProposal> proposals) {
    if(proposals == null) {
      return Collections.emptyList();
    }
    Set<CatalogItem> items = new HashSet<CatalogItem>(proposals.size());
    for(IMavenDiscoveryProposal proposal : proposals) {
      if(proposal instanceof InstallCatalogItemMavenDiscoveryProposal) {
        items.add(((InstallCatalogItemMavenDiscoveryProposal) proposal).getCatalogItem());
      }
    }
    return items;
  }

  public Object getService(Bundle bundle, ServiceRegistration registration) {
    return new MavenDiscoveryService(false); // not a factory instance
  }

  public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery#discover(java.util.Collection, java.util.List, org.eclipse.core.runtime.IProgressMonitor)
   */
  public Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> discover(
      Collection<ILifecycleMappingRequirement> requirements, List<IMavenDiscoveryProposal> preselected,
      IProgressMonitor monitor) throws CoreException {

    if(requirements == null || requirements.isEmpty()) {
      return Collections.emptyMap();
    }

    initializeCatalog(monitor);
    if(items == null) {
      return Collections.emptyMap();
    }

    Collection<CatalogItem> selectedItems = toCatalogItems(preselected);

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> allproposals = new LinkedHashMap<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>>(
        requirements.size());

    for(CatalogItemCacheEntry itemEntry : items) {
      if(monitor.isCanceled()) {
        break;
      }
      CatalogItem item = itemEntry.getItem();
      if(selectedItems.contains(item)) {
        continue;
      }
      LifecycleMappingMetadataSource src = itemEntry.getMetadataSource();
      log.debug("Considering catalog item '{}'", item.getName()); //$NON-NLS-1$        
      for(ILifecycleMappingRequirement requirement : requirements) {
        boolean matchFound = false;
        if(requirement instanceof MojoExecutionMappingRequirement) {
          MojoExecutionMappingRequirement meReq = ((MojoExecutionMappingRequirement) requirement);
          MojoExecutionKey mek = meReq.getExecution();
          if(matchesFilter(src, mek, meReq.getPackaging())) {
            matchFound = true;
          }
        } else if(requirement instanceof PackagingTypeMappingRequirement) {
          String packaging = ((PackagingTypeMappingRequirement) requirement).getPackaging();
          if(hasPackaging(src, packaging)) {
            matchFound = true;
          }
        } else if(requirement instanceof LifecycleStrategyMappingRequirement) {
          String mappingId = ((LifecycleStrategyMappingRequirement) requirement).getLifecycleMappingId();
          if(itemEntry.getMappingStrategies().contains(mappingId)) {
            matchFound = true;
          }

        } else if(requirement instanceof ProjectConfiguratorMappingRequirement) {
          String configuratorId = ((ProjectConfiguratorMappingRequirement) requirement).getProjectConfiguratorId();
          if(itemEntry.getProjectConfigurators().contains(configuratorId)) {
            matchFound = true;
          }

        }
        if(matchFound) {
          IMavenDiscoveryProposal proposal = new InstallCatalogItemMavenDiscoveryProposal(item);
          put(allproposals, requirement, proposal);
        }
      }
    }

    return allproposals;
  }

  private static boolean matchesFilter(LifecycleMappingMetadataSource src, MojoExecutionKey mojoExecution, String type) {
    for(PluginExecutionMetadata p : src.getPluginExecutions()) {
      if(p.getFilter().match(mojoExecution)) {
        return true;
      }
    }
    for(LifecycleMappingMetadata lm : src.getLifecycleMappings()) {
      if((type == null && lm.getPackagingType() == null) || (type != null && type.equals(lm.getPackagingType()))) {
        for(PluginExecutionMetadata p : lm.getPluginExecutions()) {
          if(p.getFilter().match(mojoExecution)) {
            return true;
          }
        }
      }
    }
    return false;
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
