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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.operations.DiscoveryInstallOperation;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.Messages;
import org.eclipse.m2e.internal.discovery.wizards.MavenDiscoveryUi;
import org.eclipse.swt.widgets.Display;


/*
 * This class allows us to open MavenDiscoveryInstallWizard instead of the default p2 wizard 
 * to support changing the restart policy for the subsequent ProvisioningJob. 
 */
@SuppressWarnings("restriction")
public class MavenDiscoveryInstallOperation extends DiscoveryInstallOperation {
  private List<CatalogItem> installableConnectors;

  private ProvisioningSession session;

  private final boolean restart;

  public MavenDiscoveryInstallOperation(List<CatalogItem> installableConnectors, boolean restart) {
    super(installableConnectors);
    this.installableConnectors = installableConnectors;
    this.restart = restart;
    this.session = ProvisioningUI.getDefaultUI().getSession();
  }

  @Override
  public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
    try {
      SubMonitor monitor = SubMonitor.convert(progressMonitor, Messages.MavenDiscoveryInstallOperation_Configuring, 100);
      try {
        final IInstallableUnit[] ius = computeInstallableUnits(monitor.newChild(50));

        checkCancelled(monitor);

        final RestartInstallOperation installOperation = createAndResolve(monitor.newChild(50), ius, new URI[0],
            restart && MavenDiscovery.requireRestart(installableConnectors));

        checkCancelled(monitor);

        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            MavenDiscoveryUi.openInstallWizard(Arrays.asList(ius), installOperation, null);
          }
        });
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
   * Create a RestartInstallOperation and resolve
   */
  private RestartInstallOperation createAndResolve(IProgressMonitor monitor, final IInstallableUnit[] ius,
      URI[] repositories,
      boolean requireRestart) throws CoreException {
    SubMonitor mon = SubMonitor.convert(monitor, ius.length);
    try {
      RestartInstallOperation op = new RestartInstallOperation(session, Arrays.asList(ius));
      op.setRestartPolicy(requireRestart ? ProvisioningJob.RESTART_ONLY : ProvisioningJob.RESTART_NONE);
      IStatus operationStatus = op.resolveModal(mon);
      if(operationStatus.getSeverity() > IStatus.WARNING) {
        throw new CoreException(operationStatus);
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

  @Override
  protected IQuery<IInstallableUnit> createInstalledIUsQuery() {
    return QueryUtil.createIUAnyQuery();
  }
}
