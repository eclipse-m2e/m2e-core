/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDisovery;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;


/**
 * Maven Import Wizard
 * 
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class MavenImportWizard extends AbstractMavenProjectWizard implements IImportWizard {

  private MavenImportWizardPage page;

  private LifecycleMappingPage lifecycleMappingPage;

  private List<String> locations;

  private boolean showLocation = true;

  public MavenImportWizard() {
    setNeedsProgressMonitor(true);
    setWindowTitle(Messages.MavenImportWizard_title);
  }

  public MavenImportWizard(ProjectImportConfiguration importConfiguration, List<String> locations) {
    this.locations = locations;
    this.showLocation = false;
    setNeedsProgressMonitor(true);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    super.init(workbench, selection);

    if(locations == null || locations.isEmpty()) {
      IPath location = SelectionUtil.getSelectedLocation(selection);
      if(location != null) {
        locations = Collections.singletonList(location.toOSString());
      }
    }
  }

  public void addPages() {
    page = new MavenImportWizardPage(importConfiguration, workingSets);
    page.setLocations(locations);
    page.setShowLocation(showLocation);
    addPage(page);

    lifecycleMappingPage = new LifecycleMappingPage();
    addPage(lifecycleMappingPage);
  }

  public boolean performFinish() {
    if(!page.isPageComplete()) {
      return false;
    }

    final Collection<MavenProjectInfo> projects = getProjects();
    final List<IMavenDiscoveryProposal> proposals = getMavenDiscoveryProposals();

    final MavenPlugin plugin = MavenPlugin.getDefault();

    Job job = new AbstactCreateMavenProjectJob(Messages.MavenImportWizard_job, workingSets) {
      @Override
      protected List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException {

        IMavenDisovery discovery = getDiscovery();

        boolean restartRequired = false;

        if(discovery != null && !proposals.isEmpty()) {
          restartRequired = discovery.isRestartRequired(proposals, monitor);

          discovery.implement(proposals, monitor);
        }

        List<IMavenProjectImportResult> results = plugin.getProjectConfigurationManager().importProjects(projects,
            importConfiguration, monitor);

        // XXX move up and implement restart

        return toProjects(results);
      }
    };
    job.setRule(plugin.getProjectConfigurationManager().getRule());
    job.schedule();

    return true;
  }

  /**
   * @return
   */
  private List<IMavenDiscoveryProposal> getMavenDiscoveryProposals() {
    return lifecycleMappingPage.getSelectedDiscoveryProposals();
  }

  public Collection<MavenProjectInfo> getProjects() {
    return page.getProjects();
  }

}
