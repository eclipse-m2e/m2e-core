/*******************************************************************************
 * Copyright (c) 2010-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation (in o.e.m.c.u.i.w.MavenImportWizard)
 *      Red Hat, Inc. - Extracted import workflow as standalone job
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Workspace Job for importing {@link MavenProjectInfo}s into the workspace. After the projects are imported, if
 * lifecycle mappings errors have been detected on the imported projects, the Lifecycle Mapping wizard is shown to help
 * users fix these errors.
 * 
 * @author Eugene Kuleshov
 * @author Fred Bricon
 */
public class ImportMavenProjectsJob extends WorkspaceJob {

  private List<IWorkingSet> workingSets;

  private Collection<MavenProjectInfo> projects;

  private ProjectImportConfiguration importConfiguration;

  public ImportMavenProjectsJob(Collection<MavenProjectInfo> projects, List<IWorkingSet> workingSets,
      ProjectImportConfiguration importConfiguration) {
    super(Messages.MavenImportWizard_job);
    this.projects = projects;
    this.workingSets = workingSets;
    this.importConfiguration = importConfiguration;
  }

  @Override
  public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {

    final AbstractCreateMavenProjectsOperation importOperation = new AbstractCreateMavenProjectsOperation(workingSets) {

      @Override
      protected List<IProject> doCreateMavenProjects(IProgressMonitor progressMonitor) throws CoreException {
        SubMonitor monitor = SubMonitor.convert(progressMonitor, 101);
        try {
          List<IMavenProjectImportResult> results = MavenPlugin.getProjectConfigurationManager().importProjects(
              projects, importConfiguration, monitor.newChild(100));
          return toProjects(results);
        } finally {
          monitor.done();
        }
      }
    };
    try {
      importOperation.run(monitor);
      List<IProject> createdProjects = importOperation.getCreatedProjects();
      //Detect and resolve Lifecycle Mapping issues
      LifecycleMappingDiscoveryRequest discoveryRequest = LifecycleMappingDiscoveryHelper
          .createLifecycleMappingDiscoveryRequest(createdProjects, monitor);
      if(discoveryRequest.isMappingComplete()) {
        return Status.OK_STATUS;
      }
      //LifecycleMappingHelper will discover proposals only if discovery service is available
      LifecycleMappingDiscoveryHelper.discoverProposals(discoveryRequest, monitor);
      final MavenDiscoveryProposalWizard proposalWizard = new MavenDiscoveryProposalWizard(createdProjects,
          discoveryRequest);
      proposalWizard.init(null, null);
      //Some errors were detected

      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          final IWorkbench workbench = PlatformUI.getWorkbench();
          WizardDialog dialog = new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), proposalWizard);
          dialog.open();
        }
      });
    } catch(InvocationTargetException e) {
      return AbstractCreateMavenProjectsOperation.toStatus(e);
    } catch(InterruptedException e) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

}
