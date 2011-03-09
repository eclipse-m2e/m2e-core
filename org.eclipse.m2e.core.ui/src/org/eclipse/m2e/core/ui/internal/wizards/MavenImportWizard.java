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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.osgi.util.NLS;
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
      IRunnableWithProgress importOperation = new IRunnableWithProgress() {

        public void run(final IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
          // Use the monitor from run() in order to provide progress to the wizard 
          Job job = new AbstactCreateMavenProjectJob(Messages.MavenImportWizard_job, workingSets) {
            @Override
            protected List<IProject> doCreateMavenProjects(IProgressMonitor pm) throws CoreException {
              SubMonitor monitor = SubMonitor.convert(progressMonitor, 101);
              try {
                List<IMavenProjectImportResult> results = plugin.getProjectConfigurationManager().importProjects(
                    projects, importConfiguration, monitor.newChild(proposals.isEmpty() ? 100 : 50));
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
      };

      boolean doImport = true;

      IImportWizardPageFactory discovery = getPageFactory();
      if(discovery != null) {
        doImport = !discovery.implement(proposals, importOperation, getContainer());
      }

      if(doImport) {
        getContainer().run(true, true, importOperation);
      }

      return true;
    } catch(InvocationTargetException e) {
      // TODO This doesn't seem like it should occur
    } catch(InterruptedException e) {
      // User cancelled operation, we don't return the 
    }
    return false;
  }

  @Override
  public boolean canFinish() {
    IWizardPage currentPage = getContainer().getCurrentPage();

    if(!currentPage.isPageComplete()) {
      return false;
    }

    if(currentPage == page) {
      // allow finish if there are no mapping problems and no selected proposals. 
      // the latter is important to force the user to go through p2 license page
      return getMappingConfiguration().isMappingComplete(true)
          && getMappingConfiguration().getSelectedProposals().isEmpty();
    }
//
//    if(currentPage == lifecycleMappingPage) {
//      return true;
//    }

    return super.canFinish();
  }

  private List<IMavenDiscoveryProposal> getMavenDiscoveryProposals() {
    return lifecycleMappingPage.getSelectedDiscoveryProposals();
  }

  public Collection<MavenProjectInfo> getProjects() {
    return page.getProjects();
  }

  /**
   * @return mapping configuration or null
   */
  public LifecycleMappingConfiguration getMappingConfiguration() {
    return mappingConfiguration;
  }

  void scanProjects(final List<MavenProjectInfo> list, IProgressMonitor monitor) throws CoreException {
    LOG.debug("About to calculate lifecycle mapping configuration");
    ProjectImportConfiguration importConfiguration = getProjectImportConfiguration();
    mappingConfiguration = LifecycleMappingConfiguration.calculate(list, importConfiguration, monitor);
    discoverProposals(mappingConfiguration, monitor);
  }

  void discoverProposals(LifecycleMappingConfiguration mappingConfiguration, IProgressMonitor monitor)  {
    final IMavenDiscovery discovery = getDiscovery();

    Collection<ProjectLifecycleMappingConfiguration> projects = mappingConfiguration.getProjects();
    monitor.beginTask("Searching m2e marketplace", projects.size());

    Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = new LinkedHashMap<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>>();

    for(ProjectLifecycleMappingConfiguration project : projects) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      MavenProject mavenProject = project.getMavenProject();
      List<MojoExecution> mojoExecutions = project.getMojoExecutions();
      try {
        proposals.putAll(discovery.discover(mavenProject, mojoExecutions,
            mappingConfiguration.getSelectedProposals(),
            SubMonitor.convert(monitor, NLS.bind("Analysing {0}", project.getRelpath()), 1)));
      } catch(CoreException e) {
        // TODO Auto-generated catch block
        //XXX we shall not swallow this exception but associate with the project/execution
        e.printStackTrace();
      }
      monitor.worked(1);
    }

    mappingConfiguration.setProposals(proposals);
  }
}
