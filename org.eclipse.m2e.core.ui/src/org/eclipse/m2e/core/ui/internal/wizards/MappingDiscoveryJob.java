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
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.internal.jobs.MavenWorkspaceJob;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;


/**
 * Lifecycle Mapping Discovery Job
 *
 * @author Fred Bricon
 * @since 1.6.0
 */
public class MappingDiscoveryJob extends MavenWorkspaceJob {

  private final Collection<IProject> projects;

  private boolean skipOnEmpty;

  @Deprecated
  public MappingDiscoveryJob(Collection<IProject> projects) {
    this(projects, false);
  }

  /**
   * Creates a new discovery job for the given set of projects
   * 
   * @param projects the projects to discover
   * @param skipOnEmpty if <code>true</code> nothing will be done if no new proposals can be discovered, otherwise the
   *          dialog is even shown if nothing new was discovered just showing possible unmatched items
   */
  public MappingDiscoveryJob(Collection<IProject> projects, boolean skipOnEmpty) {
    super("Discover lifecycle mappings");
    this.projects = projects;
    this.skipOnEmpty = skipOnEmpty;

  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    //Detect and resolve Lifecycle Mapping issues
    var discoveryRequest = LifecycleMappingDiscoveryHelper
        .createLifecycleMappingDiscoveryRequest(projects, monitor);
    if(discoveryRequest.isMappingComplete()) {
      return Status.OK_STATUS;
    }
    //Some errors were detected
    discoverProposals(discoveryRequest, monitor);
    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = discoveryRequest.getAllProposals();
    if(proposals.isEmpty() && skipOnEmpty) {
      //if we can not propose anything, why open the dialog?
      return Status.CANCEL_STATUS;
    }
    openProposalWizard(projects, discoveryRequest);

    return Status.OK_STATUS;
  }

  protected void discoverProposals(
      org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest discoveryRequest,
      IProgressMonitor monitor)
      throws CoreException {
    //LifecycleMappingHelper will discover proposals only if discovery service is available
    LifecycleMappingDiscoveryHelper.discoverProposals(discoveryRequest, monitor);
  }

  protected void openProposalWizard(Collection<IProject> projects,
      org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest discoveryRequest) {

    final MavenDiscoveryProposalWizard proposalWizard = new MavenDiscoveryProposalWizard(projects, discoveryRequest);
    proposalWizard.init(null, null);

    Display.getDefault().asyncExec(() -> {
      final IWorkbench workbench = PlatformUI.getWorkbench();
      WizardDialog dialog = new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), proposalWizard);
      dialog.open();
    });
  }

}
