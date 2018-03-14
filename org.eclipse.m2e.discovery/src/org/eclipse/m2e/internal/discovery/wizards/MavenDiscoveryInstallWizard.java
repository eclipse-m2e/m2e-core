/*******************************************************************************
 * Copyright (c) 2011, 2018 Sonatype, Inc. and others.
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
import org.eclipse.equinox.p2.ui.AcceptLicensesWizardPage;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.m2e.internal.discovery.operation.RestartInstallOperation;


/*
 * This exists to allow us to return a ProfileChangeOperation which changes the restart policy for provisioning jobs. 
 */
@SuppressWarnings("restriction")
public class MavenDiscoveryInstallWizard extends PreselectedIUInstallWizard {

  private boolean waitingForOtherJobs;

  private RestartInstallOperation originalOperation;

  public MavenDiscoveryInstallWizard(ProvisioningUI ui, RestartInstallOperation operation,
      Collection<IInstallableUnit> initialSelections, LoadMetadataRepositoryJob job) {
    super(ui, operation, initialSelections, job);
    this.originalOperation = operation;
  }

  @Override
  protected ProfileChangeOperation getProfileChangeOperation(Object[] elements) {
    RestartInstallOperation op = originalOperation.copy(ElementUtils.elementsToIUs(elements));
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

  @Override
  protected void initializeResolutionModelElements(Object[] selectedElements) {
    super.initializeResolutionModelElements(selectedElements);
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=348660
    // PreselectedIUInstallWizard does not ask to approve licenses if original selection has not changed
    // give license page analyse preselected and do it's thing if necessary
    // TODO remove when Bug348660 is fixed in p2
    workaroundBug348660();
  }

  private void workaroundBug348660() {
    for(IWizardPage page : getPages()) {
      if(page instanceof AcceptLicensesWizardPage) {
        AcceptLicensesWizardPage licensePage = (AcceptLicensesWizardPage) page;
        licensePage.update(ElementUtils.elementsToIUs(planSelections).toArray(new IInstallableUnit[0]), operation);
      }
    }
  }

}
