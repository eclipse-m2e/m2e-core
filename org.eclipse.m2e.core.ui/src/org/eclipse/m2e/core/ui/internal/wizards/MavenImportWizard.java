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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.LifecycleMappingConfiguration;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Maven Import Wizard
 * 
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class MavenImportWizard extends AbstractMavenProjectWizard implements IImportWizard {

  private static final Logger LOG = LoggerFactory.getLogger(MavenImportWizard.class);
  private MavenImportWizardPage page;

  private LifecycleMappingPage lifecycleMappingPage;

  private List<String> locations;

  private boolean showLocation = true;

  private LifecycleMappingConfiguration mappingConfiguration;

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
    //mkleint: this sounds wrong.
    if(!page.isPageComplete()) {
      return false;
    }

    final MavenPlugin plugin = MavenPlugin.getDefault();
    final List<IMavenDiscoveryProposal> proposals = getMavenDiscoveryProposals();
    final Collection<MavenProjectInfo> projects = getProjects();
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {

        public void run(final IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
          // Use the monitor from run() in order to provide progress to the wizard 
          Job job = new AbstactCreateMavenProjectJob(Messages.MavenImportWizard_job, workingSets) {
            @Override
            protected List<IProject> doCreateMavenProjects(IProgressMonitor pm) throws CoreException {
              SubMonitor monitor = SubMonitor.convert(progressMonitor, 101);
              try {
                IMavenDiscovery discovery = getDiscovery();

                boolean restartRequired = false;
                if(discovery != null && !proposals.isEmpty()) {
                  restartRequired = discovery.isRestartRequired(proposals, monitor);
                  // No restart required, install prior to importing
                  if(!restartRequired) {
                    discovery.implement(proposals, monitor.newChild(50));
                  }
                }
                // Import projects
                monitor.beginTask(Messages.MavenImportWizard_job, proposals.isEmpty() ? 100 : 50);
                List<IMavenProjectImportResult> results = plugin.getProjectConfigurationManager().importProjects(
                    projects, importConfiguration, monitor.newChild(proposals.isEmpty() ? 100 : 50));

                // Restart required, schedule job
                if(restartRequired && !proposals.isEmpty()) {
                  discovery.implement(proposals, monitor.newChild(1));
                }

                return toProjects(results);
              } finally {
                monitor.done();
              }
            }
          };
          job.setRule(plugin.getProjectConfigurationManager().getRule());
          job.schedule();
          job.join();
        }
      });
      return true;
    } catch(InvocationTargetException e) {
      // TODO This doesn't seem like it should occur
    } catch(InterruptedException e) {
      // User cancelled operation, we don't return the 
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.wizard.Wizard#canFinish()
   */
  @Override
  public boolean canFinish() {
    if(isCurrentPageKnown()) {
      // Discovery pages aren't added to the wizard in case they need to go away
      IWizardPage cPage = getContainer().getCurrentPage();
      while(cPage != null && cPage.isPageComplete()) {
        cPage = cPage.getNextPage();
      }
      return cPage == null || cPage.isPageComplete();
    }

    //in here make sure that the lifecycle page is hidden from view when the mappings are fine
    //but disable finish when there are some problems (thus force people to at least look at the other page)
    boolean complete = page.isPageComplete();
    if (complete && getContainer().getCurrentPage() == page) { //only apply this logic on the first page
       LifecycleMappingConfiguration mapping = getMappingConfiguration();
       //mapping is null when the scanning failed to finish. in that case we want to wizard to end on the first page.
       if (mapping != null && !mapping.isMappingComplete()) {
         return false;
       }
    }
    return super.canFinish();
  }

  /*
   * Is the current page known by the wizard (ie, has it been passed to addPage())
   */
  private boolean isCurrentPageKnown() {
    for(IWizardPage p : getPages()) {
      if(p == getContainer().getCurrentPage()) {
        return false;
      }
    }
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

  /**
   * @return null or a clone
   */
  public LifecycleMappingConfiguration getMappingConfiguration() {
    if (mappingConfiguration != null) {
      return LifecycleMappingConfiguration.clone(mappingConfiguration, getProjects());
    }
    return null;
  }
  
  /**
   * @param list 
   * @throws InterruptedException 
   * @throws InvocationTargetException 
   * 
   */
  void scanProjects(final List<MavenProjectInfo> list, IProgressMonitor monitor) throws CoreException {
      LOG.debug("About to calculate lifecycle mapping configuration");
      ProjectImportConfiguration importConfiguration = getProjectImportConfiguration();
      mappingConfiguration = LifecycleMappingConfiguration.calculate(list, importConfiguration, monitor);
  }
}
