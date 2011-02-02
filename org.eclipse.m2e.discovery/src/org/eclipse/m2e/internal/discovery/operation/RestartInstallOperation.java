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

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileModificationJob;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;


/*
 * This operation allows altering the restart policy for the ProvisioningJob returned from getProvisioningJob calls 
 */
public class RestartInstallOperation extends InstallOperation {

  private int restartPolicy = ProvisioningJob.RESTART_ONLY;

  public RestartInstallOperation(ProvisioningSession session, Collection<IInstallableUnit> toInstall) {
    super(session, toInstall);
  }

  @Override
  public ProvisioningJob getProvisioningJob(IProgressMonitor monitor) {
    ProvisioningJob job = super.getProvisioningJob(monitor);
    if(job != null && job instanceof ProfileModificationJob) {
      ((ProfileModificationJob) job).setRestartPolicy(restartPolicy);
    }
    return job;
  }

  public int getRestartPolicy() {
    return restartPolicy;
  }

  public void setRestartPolicy(int restartPolicy) {
    this.restartPolicy = restartPolicy;
  }
}
