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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.MappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.wizards.IImportWizardPageFactory;
import org.eclipse.m2e.internal.discovery.operation.MavenDiscoveryInstallOperation;
import org.eclipse.m2e.internal.discovery.operation.RestartInstallOperation;
import org.eclipse.m2e.internal.discovery.wizards.DiscoverySelectableIUsPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.progress.IProgressConstants2;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"restriction", "rawtypes"})
public class MavenDiscoveryService implements IImportWizardPageFactory, IMavenDiscovery, ServiceFactory {

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

  public MavenDiscoveryService() {
    this(true);
  }

  public MavenDiscoveryService(boolean factory) {
  }

  public Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> discover(MavenProject mavenProject,
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
        List<String> projectConfigurators = new ArrayList<String>();
        List<String> mappingStrategies = new ArrayList<String>();
        MavenDiscovery.getProvidedProjectConfigurators(item, projectConfigurators, mappingStrategies);
        if(metadataSource != null) {
          addCatalogItem(item, metadataSource, projectConfigurators, mappingStrategies);
        }
      }
    }

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = new LinkedHashMap<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>>();

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
            mavenProject, sources, false, monitor);

        LifecycleMappingFactory.calculateEffectiveLifecycleMappingMetadata(mappingResult, request, metadataSources,
            mavenProject, mojoExecutions, false);

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

        for(Map.Entry<MojoExecutionKey, List<PluginExecutionMetadata>> entry : mappingResult.getMojoExecutionMapping()
            .entrySet()) {
          if(entry.getValue() != null) {
            for(PluginExecutionMetadata executionMapping : entry.getValue()) {
              IMavenDiscoveryProposal proposal = getProposal(executionMapping.getSource());
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

  public void implement(List<IMavenDiscoveryProposal> proposals, IProgressMonitor monitor) {
    List<CatalogItem> items = toCatalogItems(proposals);

    boolean restart = isRestartRequired(proposals, monitor);
    final MavenDiscoveryInstallOperation op = new MavenDiscoveryInstallOperation(items, restart);
    try {
      op.run(monitor);

      if(restart) {
        ProvisioningUI.getDefaultUI().schedule(op.getOperation().getProvisioningJob(monitor), 0);
      } else {
        ProvisioningJob job = op.getOperation().getProvisioningJob(monitor);
        job.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
        job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
        job.setProperty(IProgressConstants.ICON_PROPERTY, ProvUIImages.getImageDescriptor(ProvUIImages.IMG_PROFILE));
        job.setProperty(IProgressConstants2.SHOW_IN_TASKBAR_ICON_PROPERTY, Boolean.TRUE);
        IStatus status = job.runModal(monitor);
        if(status.isOK()) {
          applyProfileChanges();
        } else {
          StatusManager.getManager().handle(status);
        }
      }

    } catch(InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void applyProfileChanges() {
    Configurator configurator = (Configurator) ServiceHelper.getService(ProvUIActivator.getContext(),
        Configurator.class.getName());
    try {
      configurator.applyConfiguration();
    } catch(IOException e) {
      ProvUI.handleException(e, ProvUIMessages.ProvUI_ErrorDuringApplyConfig, StatusManager.LOG | StatusManager.BLOCK);
    } catch(IllegalStateException e) {
      IStatus illegalApplyStatus = new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, 0,
          ProvUIMessages.ProvisioningOperationRunner_CannotApplyChanges, e);
      ProvUI.reportStatus(illegalApplyStatus, StatusManager.LOG | StatusManager.BLOCK);
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

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.ui.internal.wizards.IImportWizardPageFactory#getPage(java.util.List, org.eclipse.jface.operation.IRunnableContext)
   */
  public IWizardPage getPage(final List<IMavenDiscoveryProposal> proposals, IRunnableContext context)
      throws InvocationTargetException, InterruptedException {

    final IWizardPage[] page = new IWizardPage[1];
    if(proposals != null && !proposals.isEmpty()) {
      context.run(false, false, new IRunnableWithProgress() {

        public void run(IProgressMonitor monitor) throws InvocationTargetException {
          SubMonitor subMon = SubMonitor.convert(monitor, 11);
          try {
            List<CatalogItem> installableConnectors = new ArrayList<CatalogItem>(proposals.size());
            for(IMavenDiscoveryProposal proposal : proposals) {
              if(proposal instanceof InstallCatalogItemMavenDiscoveryProposal) {
                installableConnectors.add(((InstallCatalogItemMavenDiscoveryProposal) proposal).getCatalogItem());
              }
            }
            addRepositories(installableConnectors, subMon.newChild(10));
            IInstallableUnit[] ius = computeInstallableUnits(installableConnectors, subMon.newChild(1));
            RestartInstallOperation operation = new RestartInstallOperation(ProvisioningUI.getDefaultUI().getSession(),
                Arrays.asList(ius));

            IUElementListRoot root = new IUElementListRoot();
            ArrayList<AvailableIUElement> list = new ArrayList<AvailableIUElement>(operation.getIUs().size());
            for(IInstallableUnit iu : operation.getIUs()) {
              AvailableIUElement element = new AvailableIUElement(root, iu, ProvisioningUI.getDefaultUI()
                  .getProfileId(), false);
              list.add(element);
            }
            root.setChildren(list.toArray());
            page[0] = new DiscoverySelectableIUsPage(ProvisioningUI.getDefaultUI(), operation, root, ius);
          } catch(CoreException e) {
            throw new InvocationTargetException(e);
          } finally {
            subMon.done();
          }
        }
      });
    }
    return page[0];
  }

  /*
   * Compute the InstallableUnits & IMetadataRepository
   */
  private static IInstallableUnit[] computeInstallableUnits(List<CatalogItem> installableConnectors,
      IProgressMonitor progressMonitor) throws CoreException {
    SubMonitor monitor = SubMonitor.convert(progressMonitor);
    try {
      List<IMetadataRepository> repositories = addRepositories(installableConnectors, monitor.newChild(50));
      final List<IInstallableUnit> installableUnits = queryInstallableUnits(installableConnectors,
          monitor.newChild(50), repositories);

      return installableUnits.toArray(new IInstallableUnit[installableUnits.size()]);
    } finally {
      monitor.done();
    }
  }

  /*
   * Get IUs to install from the specified repository 
   */
  private static List<IInstallableUnit> queryInstallableUnits(List<CatalogItem> installableConnectors, IProgressMonitor progressMonitor,
      List<IMetadataRepository> repositories) {
    final List<IInstallableUnit> installableUnits = new ArrayList<IInstallableUnit>(installableConnectors.size());

    SubMonitor monitor = SubMonitor.convert(progressMonitor, installableConnectors.size());
    try {
      for(CatalogItem item : installableConnectors) {
        SubMonitor subMon = monitor.newChild(1);
        checkCancelled(monitor);
        URI address = URI.create(item.getSiteUrl());
        // get repository
        IMetadataRepository repository = null;
        for(IMetadataRepository candidate : repositories) {
          if(address.equals(candidate.getLocation())) {
            repository = candidate;
            break;
          }
        }
        if(repository == null) {
          log.warn(NLS.bind(Messages.MavenDiscoveryInstallOperation_missingRepository, item.getName(),
              item.getSiteUrl()));
          // Continue so we gather all the problems before telling the user
          continue;
        }
        // get IUs
        checkCancelled(monitor);

        Set<IVersionedId> ids = getDescriptorIds(installableConnectors, repository);
        for(IVersionedId versionedId : ids) {
          IQueryResult<IInstallableUnit> result = repository.query(QueryUtil.createIUQuery(versionedId),
              subMon.newChild(1));
          Set<IInstallableUnit> matches = result.toSet();
          if(matches.size() == 1) {
            installableUnits.addAll(matches);
          } else if(matches.size() == 0) {
            log.warn(NLS.bind(Messages.MavenDiscoveryInstallOperation_missingIU, item.getName(), versionedId.toString()));
          } else {
            // Choose the highest available version
            IInstallableUnit match = null;
            for(IInstallableUnit iu : matches) {
              if(match == null || iu.getVersion().compareTo(match.getVersion()) > 0) {
                match = iu;
              }
            }
            if(match != null) {
              installableUnits.add(match);
            }
          }
        }
      }
      return installableUnits;
    } finally {
      monitor.done();
    }
  }

  /*
   * Add the necessary repositories
   */
  private static List<IMetadataRepository> addRepositories(List<CatalogItem> installableConnectors, SubMonitor monitor)
      throws CoreException {
    // tell p2 that it's okay to use these repositories
    Set<URI> repositoryLocations = new HashSet<URI>();
    for(CatalogItem items : installableConnectors) {
      repositoryLocations.add(URI.create(items.getSiteUrl()));
    }

    RepositoryTracker repositoryTracker = ProvisioningUI.getDefaultUI().getRepositoryTracker();
    monitor.setWorkRemaining(installableConnectors.size() * 5);
    for(CatalogItem descriptor : installableConnectors) {
      URI uri = URI.create(descriptor.getSiteUrl());
      if(repositoryLocations.add(uri)) {
        checkCancelled(monitor);
        repositoryTracker.addRepository(uri, null, ProvisioningUI.getDefaultUI().getSession());
      }
      monitor.worked(1);
    }

    // fetch meta-data for these repositories
    ArrayList<IMetadataRepository> repositories = new ArrayList<IMetadataRepository>();
    monitor.setWorkRemaining(repositories.size());
    IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ProvisioningUI.getDefaultUI().getSession()
        .getProvisioningAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
    monitor.setTaskName("Contacting repositories");
    for(URI uri : repositoryLocations) {
      checkCancelled(monitor);
      IMetadataRepository repository = manager.loadRepository(uri, new NullProgressMonitor());
      monitor.worked(1);
      repositories.add(repository);
    }
    return repositories;
  }

  /*
   * Get the IVersionedId expected to be in the repository  
   */
  protected static Set<IVersionedId> getDescriptorIds(List<CatalogItem> installableConnectors,
      IMetadataRepository repository) {
    Set<IVersionedId> ids = new HashSet<IVersionedId>();
    for(CatalogItem item : installableConnectors) {
      if(repository.getLocation().equals(URI.create(item.getSiteUrl()))) {
        for(String id : item.getInstallableUnits()) {
          ids.add(VersionedId.parse(id));
        }
      }
    }
    return ids;
  }

  private static void checkCancelled(IProgressMonitor monitor) {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }
}
