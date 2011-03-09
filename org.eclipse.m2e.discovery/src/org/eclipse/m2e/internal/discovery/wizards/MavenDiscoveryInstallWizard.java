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

import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.PreselectedIUInstallWizard;
import org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionResultsWizardPage;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.m2e.internal.discovery.operation.RestartInstallOperation;

/*
 * This exists to allow us to return a ProfileChangeOperation which changes the restart policy for provisioning jobs. 
 */
@SuppressWarnings("restriction")
public class MavenDiscoveryInstallWizard extends PreselectedIUInstallWizard {

  private boolean waitingForOtherJobs;
  private final IRunnableWithProgress postInstallHook;

  public MavenDiscoveryInstallWizard(ProvisioningUI ui, RestartInstallOperation operation,
      Collection<IInstallableUnit> initialSelections, LoadMetadataRepositoryJob job,
      IRunnableWithProgress postInstallHook) {
    super(ui, operation, initialSelections, job);
    this.postInstallHook = postInstallHook;
  }

  /* (non-Javadoc)
   * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getProfileChangeOperation(java.lang.Object[])
   */
  @Override
  protected ProfileChangeOperation getProfileChangeOperation(Object[] elements) {
    RestartInstallOperation op = new RestartInstallOperation(ui.getSession(), ElementUtils.elementsToIUs(elements),
        postInstallHook);
    op.setRestartPolicy(((RestartInstallOperation) operation).getRestartPolicy());
    op.setProfileId(getProfileId());
    return op;
  }

  public boolean shouldRecomputePlan(ISelectableIUsPage page) {
    boolean previouslyWaiting = waitingForOtherJobs;
    boolean previouslyCanceled = getCurrentStatus().getSeverity() == IStatus.CANCEL;
    waitingForOtherJobs = ui.hasScheduledOperations();
    return waitingForOtherJobs || previouslyWaiting || previouslyCanceled || pageSelectionsHaveChanged(page);
  }

  public void setMainPage(ISelectableIUsPage page) {
    mainPage = page;
  }

  public void setResolutionResultsPage(ResolutionResultsWizardPage page) {
    resolutionPage = page;
  }
}
