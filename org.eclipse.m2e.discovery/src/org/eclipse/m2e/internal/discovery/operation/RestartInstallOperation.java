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
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileModificationJob;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.m2e.core.ui.internal.wizards.AbstractCreateMavenProjectsOperation;
import org.eclipse.m2e.internal.discovery.startup.UpdateConfigurationStartup;


/*
 * This operation allows altering the restart policy for the ProvisioningJob returned from getProvisioningJob calls 
 */
public class RestartInstallOperation extends InstallOperation {

  private int restartPolicy = ProvisioningJob.RESTART_ONLY;

  private final ProvisioningSession session;

  private final Collection<IInstallableUnit> toInstall;

  private final IRunnableWithProgress postInstallHook;

  public RestartInstallOperation(ProvisioningSession session, Collection<IInstallableUnit> toInstall,
      IRunnableWithProgress postInstallHook) {
    super(session, toInstall);
    this.session = session;
    this.toInstall = toInstall;
    this.postInstallHook = postInstallHook;
  }

  @Override
  public ProvisioningJob getProvisioningJob(IProgressMonitor monitor) {
    ProvisioningJob job = super.getProvisioningJob(monitor);
    if(job != null && job instanceof ProfileModificationJob) {
      ((ProfileModificationJob) job).setRestartPolicy(restartPolicy);
      UpdateMavenConfigurationProvisioningJob ucJob = new UpdateMavenConfigurationProvisioningJob(((ProfileModificationJob) job),
          session, postInstallHook);
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

  public Collection<IInstallableUnit> getIUs() {
    return toInstall;
  }

  /*
   * The ProfileModificationJob is wrapped to allow us to know when the job finishes successfully so we can 
   * ensure that early startup for update configuration is enabled.
   */
  private static class UpdateMavenConfigurationProvisioningJob extends ProfileModificationJob {

    private ProfileModificationJob job;

    private final IRunnableWithProgress postInstallHook;

    public UpdateMavenConfigurationProvisioningJob(ProfileModificationJob job, ProvisioningSession session, IRunnableWithProgress postInstallHook) {
      super(job.getName(), session, job.getProfileId(), null, null);
      this.job = job;
      this.postInstallHook = postInstallHook;
    }

    @Override
    public IStatus runModal(IProgressMonitor monitor) {
      // install
      IStatus status = job.run(monitor);

      if (status.isOK() && postInstallHook != null) {
        try {
          postInstallHook.run(monitor);
        } catch(InvocationTargetException e) {
          // TODO need a better place for this helper
          return AbstractCreateMavenProjectsOperation.toStatus(e);
        } catch(InterruptedException e) {
          return Status.CANCEL_STATUS;
        }
      }

      if(status.isOK()) {
        // If the installation doesn't require a restart, launch the reconfiguration now.
        if(getRestartPolicy() == ProvisioningJob.RESTART_NONE) {
          UpdateConfigurationStartup.updateConfiguration();
        } else {
          UpdateConfigurationStartup.enableStartup();
        }
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
