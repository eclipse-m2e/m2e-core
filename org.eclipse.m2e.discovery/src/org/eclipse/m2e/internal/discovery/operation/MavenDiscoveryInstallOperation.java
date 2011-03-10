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
package org.eclipse.m2e.internal.discovery.operation;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.m2e.internal.discovery.DiscoveryActivator;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.Messages;
import org.eclipse.osgi.util.NLS;


/*
 * This class allows us to open MavenDiscoveryInstallWizard instead of the default p2 wizard 
 * to support changing the restart policy for the subsequent ProvisioningJob. 
 */
@SuppressWarnings("restriction")
public class MavenDiscoveryInstallOperation implements IRunnableWithProgress {
  private List<CatalogItem> installableConnectors;

  private ProvisioningSession session;

  private Set<URI> repositoryLocations;

  private final boolean restart;

  private List<IStatus> statuses = new ArrayList<IStatus>();

  private RestartInstallOperation operation;

  private final IRunnableWithProgress postInstallHook;

  private Collection<String> projectsToConfigure;

  private boolean shouldResolve;

  public MavenDiscoveryInstallOperation(List<CatalogItem> installableConnectors, IRunnableWithProgress postInstallHook,
      boolean restart) {
    this(installableConnectors, postInstallHook, restart, true, null);
  }

  public MavenDiscoveryInstallOperation(List<CatalogItem> installableConnectors, IRunnableWithProgress postInstallHook,
      boolean restart, boolean shouldResolve, Collection<String> projectsToConfigure) {
    this.installableConnectors = installableConnectors;
    this.postInstallHook = postInstallHook;
    this.restart = restart;
    this.session = ProvisioningUI.getDefaultUI().getSession();
    this.shouldResolve = shouldResolve;
    this.projectsToConfigure = projectsToConfigure;
  }

  public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
    try {
      SubMonitor monitor = SubMonitor
          .convert(progressMonitor, Messages.MavenDiscoveryInstallOperation_Configuring, 100);
      try {
        final IInstallableUnit[] ius = computeInstallableUnits(monitor.newChild(50));

        checkCancelled(monitor);

        operation = createAndResolve(monitor.newChild(50), ius, new URI[0],
            restart && MavenDiscovery.requireRestart(installableConnectors));

        checkCancelled(monitor);
      } finally {
        monitor.done();
      }
    } catch(OperationCanceledException e) {
      throw new InterruptedException();
    } catch(Exception e) {
      throw new InvocationTargetException(e);
    }
  }

  /*
   * Should only be called after a successful call to run
   */
  public RestartInstallOperation getOperation() {
    return operation;
  }

  /*
   * Compute the InstallableUnits & IMetadataRepository
   */
  public IInstallableUnit[] computeInstallableUnits(IProgressMonitor progressMonitor) throws CoreException {
    SubMonitor monitor = SubMonitor.convert(progressMonitor);
    try {

      List<IMetadataRepository> repositories = addRepositories(monitor.newChild(50));
      final List<IInstallableUnit> installableUnits = queryInstallableUnits(monitor.newChild(50), repositories);

      if(!statuses.isEmpty()) {
        throw new CoreException(new MultiStatus(DiscoveryActivator.PLUGIN_ID, 0, statuses.toArray(new IStatus[statuses
            .size()]), Messages.MavenDiscoveryInstallOperation_ErrorMessage, null));
      }
      return installableUnits.toArray(new IInstallableUnit[installableUnits.size()]);
    } finally {
      monitor.done();
    }
  }

  /*
   * Get IUs to install from the specified repository 
   */
  private List<IInstallableUnit> queryInstallableUnits(IProgressMonitor progressMonitor,
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
          statuses.add(new Status(IStatus.ERROR, DiscoveryActivator.PLUGIN_ID, NLS.bind(
              Messages.MavenDiscoveryInstallOperation_missingRepository, item.getName(), item.getSiteUrl())));
          // Continue so we gather all the problems before telling the user
          continue;
        }
        // get IUs
        checkCancelled(monitor);

        Set<IVersionedId> ids = getDescriptorIds(repository);
        for(IVersionedId versionedId : ids) {
          IQueryResult<IInstallableUnit> result = repository.query(QueryUtil.createIUQuery(versionedId),
              subMon.newChild(1));
          Set<IInstallableUnit> matches = result.toSet();
          if(matches.size() == 1) {
            installableUnits.addAll(matches);
          } else if(matches.size() == 0) {
            statuses.add(new Status(IStatus.ERROR, DiscoveryActivator.PLUGIN_ID, NLS.bind(
                Messages.MavenDiscoveryInstallOperation_missingIU, item.getName(), versionedId.toString())));
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
   * Get the IVersionedId expected to be in the repository  
   */
  protected Set<IVersionedId> getDescriptorIds(IMetadataRepository repository) {
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

  /*
   * Add the necessary repositories
   */
  protected List<IMetadataRepository> addRepositories(SubMonitor monitor) {
    // TODO this isn't right 
    // tell p2 that it's okay to use these repositories
    RepositoryTracker repositoryTracker = ProvisioningUI.getDefaultUI().getRepositoryTracker();
    repositoryLocations = new HashSet<URI>();
    monitor.setWorkRemaining(installableConnectors.size() * 5);
    for(CatalogItem descriptor : installableConnectors) {
      URI uri = URI.create(descriptor.getSiteUrl());
      if(repositoryLocations.add(uri)) {
        checkCancelled(monitor);
        repositoryTracker.addRepository(uri, null, session);
      }
      monitor.worked(1);
    }

    // fetch meta-data for these repositories
    ArrayList<IMetadataRepository> repositories = new ArrayList<IMetadataRepository>();
    monitor.setWorkRemaining(repositories.size());
    IMetadataRepositoryManager manager = (IMetadataRepositoryManager) session.getProvisioningAgent().getService(
        IMetadataRepositoryManager.SERVICE_NAME);
    for(URI uri : repositoryLocations) {
      checkCancelled(monitor);
      try {
        IMetadataRepository repository = manager.loadRepository(uri, monitor.newChild(1));
        repositories.add(repository);
      } catch(ProvisionException e) {
        statuses.add(e.getStatus());
      }
    }
    return repositories;
  }

  /*
   * Create a RestartInstallOperation and resolve
   */
  private RestartInstallOperation createAndResolve(IProgressMonitor monitor, final IInstallableUnit[] ius,
      URI[] repositories, boolean requireRestart) throws CoreException {
    SubMonitor mon = SubMonitor.convert(monitor, ius.length);
    try {
      RestartInstallOperation op = new RestartInstallOperation(session, Arrays.asList(ius), postInstallHook,
          projectsToConfigure, requireRestart ? ProvisioningJob.RESTART_ONLY : ProvisioningJob.RESTART_NONE);
      if(shouldResolve) {
        IStatus operationStatus = op.resolveModal(mon);
        if(operationStatus.getSeverity() > IStatus.WARNING) {
          throw new CoreException(operationStatus);
        }
      }
      return op;
    } finally {
      mon.done();
    }
  }

  private void checkCancelled(IProgressMonitor monitor) {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }
}
