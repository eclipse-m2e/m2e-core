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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileModificationJob;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.m2e.internal.discovery.startup.UpdateConfigurationStartup;


/*
 * This operation allows altering the restart policy for the ProvisioningJob returned from getProvisioningJob calls 
 */
public class RestartInstallOperation extends InstallOperation {

  private int restartPolicy = ProvisioningJob.RESTART_ONLY;

  private ProvisioningSession session;

  public RestartInstallOperation(ProvisioningSession session, Collection<IInstallableUnit> toInstall) {
    super(session, toInstall);
    this.session = session;
  }

  @Override
  public ProvisioningJob getProvisioningJob(IProgressMonitor monitor) {
    ProvisioningJob job = super.getProvisioningJob(monitor);
    if(job != null && job instanceof ProfileModificationJob) {
      ((ProfileModificationJob) job).setRestartPolicy(restartPolicy);
      UpdateConfigurationProvisioningJob ucJob = new UpdateConfigurationProvisioningJob(((ProfileModificationJob) job),
          session);
      return ucJob;
    }
    return job;
  }

  public int getRestartPolicy() {
    return restartPolicy;
  }

  public void setRestartPolicy(int restartPolicy) {
    this.restartPolicy = restartPolicy;
  }

  private static class UpdateConfigurationProvisioningJob extends ProfileModificationJob {

    private ProfileModificationJob job;

    public UpdateConfigurationProvisioningJob(ProfileModificationJob job, ProvisioningSession session) {
      super(job.getName(), session, IProfileRegistry.SELF, null, null);
      this.job = job;
    }

    @Override
    public IStatus runModal(IProgressMonitor monitor) {
      IStatus status = job.run(monitor);
      if(status.isOK()) {
        UpdateConfigurationStartup.enableStartup();
      }
      return status;
    }

    @Override
    public String getProfileId() {
      return job.getProfileId();
    }

    @Override
    public int getRestartPolicy() {
      return job.getRestartPolicy();
    }
  }
}
