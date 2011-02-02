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

import org.eclipse.equinox.internal.p2.ui.dialogs.PreselectedIUInstallWizard;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.m2e.internal.discovery.operation.RestartInstallOperation;


/*
 * Needed to override getProvisioningContext()
 */
public class InstallWizard extends PreselectedIUInstallWizard {

  public InstallWizard(ProvisioningUI ui, RestartInstallOperation operation,
      Collection<IInstallableUnit> initialSelections, LoadMetadataRepositoryJob job) {
    super(ui, operation, initialSelections, job);
  }

  /* (non-Javadoc)
   * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getProfileChangeOperation(java.lang.Object[])
   */
  @Override
  protected ProfileChangeOperation getProfileChangeOperation(Object[] elements) {
    RestartInstallOperation op = new RestartInstallOperation(ui.getSession(), ElementUtils.elementsToIUs(elements));
    op.setRestartPolicy(((RestartInstallOperation) operation).getRestartPolicy());
    op.setProfileId(getProfileId());
    //    op.setRootMarkerKey(getRootMarkerKey());
    op.setProvisioningContext(getProvisioningContext());
    return op;
  }
}
