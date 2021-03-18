/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

  private int restartPolicy;

  private final ProvisioningSession session;

  private final Collection<IInstallableUnit> toInstall;

  private final IRunnableWithProgress postInstallHook;

  private Collection<String> projectsToConfigure;

  public RestartInstallOperation(ProvisioningSession session, Collection<IInstallableUnit> toInstall,
      IRunnableWithProgress postInstallHook) {
    this(session, toInstall, postInstallHook, null, ProvisioningJob.RESTART_ONLY);
  }

  public RestartInstallOperation(ProvisioningSession session, Collection<IInstallableUnit> toInstall,
      IRunnableWithProgress postInstallHook, Collection<String> projectsToConfigure, int restartPolicy) {
    super(session, toInstall);
    this.session = session;
    this.toInstall = toInstall;
    this.postInstallHook = postInstallHook;
    this.projectsToConfigure = projectsToConfigure;
    this.restartPolicy = restartPolicy;
  }

  @Override
  public ProvisioningJob getProvisioningJob(IProgressMonitor monitor) {
    ProvisioningJob job = super.getProvisioningJob(monitor);
    if(job instanceof ProfileModificationJob) {
      ((ProfileModificationJob) job).setRestartPolicy(restartPolicy);
      UpdateMavenConfigurationProvisioningJob ucJob = new UpdateMavenConfigurationProvisioningJob(
          ((ProfileModificationJob) job), session, postInstallHook, projectsToConfigure);
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
   * Creates a shallow copy of this operation changing IUs to install. 
   */
  public RestartInstallOperation copy(Collection<IInstallableUnit> toInstall) {
    return new RestartInstallOperation(session, toInstall, postInstallHook, projectsToConfigure, restartPolicy);
  }

  /*
   * The ProfileModificationJob is wrapped to allow us to know when the job finishes successfully so we can 
   * ensure that early startup for update configuration is enabled.
   */
  private static class UpdateMavenConfigurationProvisioningJob extends ProfileModificationJob {

    private ProfileModificationJob job;

    private final IRunnableWithProgress postInstallHook;

    private Collection<String> projectsToConfigure;

    public UpdateMavenConfigurationProvisioningJob(ProfileModificationJob job, ProvisioningSession session,
        IRunnableWithProgress postInstallHook, Collection<String> projectsToConfigure) {
      super(job.getName(), session, job.getProfileId(), null, null);
      this.job = job;
      this.postInstallHook = postInstallHook;
      this.projectsToConfigure = projectsToConfigure;
    }

    @Override
    public IStatus runModal(IProgressMonitor monitor) {
      // install
      IStatus status = job.run(monitor);

      if(status.isOK() && postInstallHook != null) {
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
          UpdateConfigurationStartup.enableStartup(projectsToConfigure);
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
