/*******************************************************************************
 * Copyright (c) 2010-2015 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Fred Bricon (Red Hat Inc.)-extracted mapping discovery to workspace job
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;


/**
 * Lifecycle Mapping Discovery Job
 *
 * @author Fred Bricon
 * @since 1.6.0
 */
public class MappingDiscoveryJob extends WorkspaceJob {

  private final Collection<IProject> projects;

  public MappingDiscoveryJob(Collection<IProject> projects) {
    super("Discover lifecycle mappings");
    this.projects = projects;

  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    //Detect and resolve Lifecycle Mapping issues
    LifecycleMappingDiscoveryRequest discoveryRequest = LifecycleMappingDiscoveryHelper
        .createLifecycleMappingDiscoveryRequest(projects, monitor);
    if(discoveryRequest.isMappingComplete()) {
      return Status.OK_STATUS;
    }
    //Some errors were detected
    discoverProposals(discoveryRequest, monitor);

    openProposalWizard(projects, discoveryRequest);

    return Status.OK_STATUS;
  }

  protected void discoverProposals(LifecycleMappingDiscoveryRequest discoveryRequest, IProgressMonitor monitor)
      throws CoreException {
    //LifecycleMappingHelper will discover proposals only if discovery service is available
    LifecycleMappingDiscoveryHelper.discoverProposals(discoveryRequest, monitor);
  }

  protected void openProposalWizard(Collection<IProject> projects, LifecycleMappingDiscoveryRequest discoveryRequest) {

    final MavenDiscoveryProposalWizard proposalWizard = new MavenDiscoveryProposalWizard(projects, discoveryRequest);
    proposalWizard.init(null, null);

    Display.getDefault().asyncExec(() -> {
      final IWorkbench workbench = PlatformUI.getWorkbench();
      WizardDialog dialog = new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), proposalWizard);
      dialog.open();
    });
  }

}
