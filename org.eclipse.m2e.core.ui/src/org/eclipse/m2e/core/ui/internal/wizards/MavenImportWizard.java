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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.core.ui.internal.editing.LifecycleMappingOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider;

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
  
  private boolean basedirRemameRequired = false;

  private boolean initialized = false;

  private LifecycleMappingConfiguration mappingConfiguration;

  public MavenImportWizard() {
    setNeedsProgressMonitor(true);
    setWindowTitle(Messages.MavenImportWizard_title);
  }

  public MavenImportWizard(ProjectImportConfiguration importConfiguration, List<String> locations) {
    this();
    this.locations = locations;
    this.showLocation = false;
  }

  public MavenImportWizard(ProjectImportConfiguration importConfiguration, List<String> locations,
      LifecycleMappingConfiguration mappingConfiguration) {
    this(importConfiguration, locations);
    this.mappingConfiguration = mappingConfiguration;
  }
  
  public void setBasedirRemameRequired(boolean basedirRemameRequired) {
    this.basedirRemameRequired = basedirRemameRequired;
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    super.init(workbench, selection);

    initialized = true;

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=341047
    // prepopulate from workspace selection, 
    // allows convenient import of nested projects by right-click->import on a workspace project or folder
    if(locations == null || locations.isEmpty()) {
      IPath location = SelectionUtil.getSelectedLocation(selection);
      if(location != null) {
        locations = Collections.singletonList(location.toOSString());
      }
    }
  }

  public void addPages() {
    if(!initialized) {
      init(null, null);
    }
    page = new MavenImportWizardPage(importConfiguration, workingSets);
    page.setLocations(locations);
    page.setShowLocation(showLocation);
    page.setBasedirRemameRequired(basedirRemameRequired);
    addPage(page);

    if(getDiscovery() != null) {
      lifecycleMappingPage = new LifecycleMappingPage();
      addPage(lifecycleMappingPage);
    }
  }

  public boolean performFinish() {
    //mkleint: this sounds wrong.
    if(!page.isPageComplete()) {
      return false;
    }
    if(lifecycleMappingPage != null && !lifecycleMappingPage.isMappingComplete() && !warnIncompleteMapping()) {
      return false;
    }

    final List<IMavenDiscoveryProposal> proposals = getMavenDiscoveryProposals();
    final Collection<MavenProjectInfo> projects = getProjects();

    final IRunnableWithProgress importOperation = new AbstractCreateMavenProjectsOperation(workingSets) {
      @Override
      protected List<IProject> doCreateMavenProjects(IProgressMonitor progressMonitor) throws CoreException {
        SubMonitor monitor = SubMonitor.convert(progressMonitor, 101);
        try {
          List<IMavenProjectImportResult> results = MavenPlugin.getProjectConfigurationManager().importProjects(
              projects, importConfiguration, monitor.newChild(proposals.isEmpty() ? 100 : 50));
          return toProjects(results);
        } finally {
          monitor.done();
        }
      }
    };

    boolean doImport = true;

    IMavenDiscoveryUI discovery = getPageFactory();
    if(discovery != null && !proposals.isEmpty()) {
      Set<String> projectsToConfigure = new HashSet<String>();
      for(MavenProjectInfo projectInfo : projects) {
        if(projectInfo.getModel() != null) {
          projectsToConfigure.add(importConfiguration.getProjectName(projectInfo.getModel()));
        }
      }
      doImport = discovery.implement(proposals, importOperation, getContainer(), projectsToConfigure);
    }

    if(doImport) {
      final IRunnableWithProgress ignoreJob = new IRunnableWithProgress() {

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          List<IProject> changed = new LinkedList<IProject>();
          for(ILifecycleMappingLabelProvider prov : lifecycleMappingPage.getIgnore()) {
            ILifecycleMappingRequirement req = prov.getKey();
            if(req instanceof MojoExecutionMappingRequirement) {
              changed.addAll(getProject(prov.getProjects()));
              ignore(((MojoExecutionMappingRequirement) req).getExecution(), prov.getProjects());
            }
          }

          for(ILifecycleMappingLabelProvider prov : lifecycleMappingPage.getIgnoreParent()) {
            ILifecycleMappingRequirement req = prov.getKey();
            if(req instanceof MojoExecutionMappingRequirement) {
              changed.addAll(getProject(prov.getProjects()));
              ignoreAtDefinition(((MojoExecutionMappingRequirement) req).getExecution(), prov.getProjects());
            }
          }

          new UpdateMavenProjectJob(changed.toArray(new IProject[changed.size()])).schedule();
        }

        private Collection<IProject> getProject(Collection<MavenProject> projects) {
          List<IProject> workspaceProjects = new LinkedList<IProject>();
          for(MavenProject project : projects) {
            IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(project.getGroupId(),
                project.getArtifactId(), project.getVersion());
            if(facade != null) {
              workspaceProjects.add(facade.getProject());
            }
          }
          return workspaceProjects;
        }

        private void ignore(MojoExecutionKey key, Collection<MavenProject> projects) {
          String pluginGroupId = key.getGroupId();
          String pluginArtifactId = key.getArtifactId();
          String pluginVersion = key.getVersion();
          String[] goals = new String[] {key.getGoal()};
          for(MavenProject project : projects) {
            IFile pomFile = M2EUtils.getPomFile(project);
            try {
              PomEdits.performOnDOMDocument(new OperationTuple(pomFile, new LifecycleMappingOperation(pluginGroupId,
                  pluginArtifactId, pluginVersion, PluginExecutionAction.ignore, goals)));
            } catch(IOException ex) {
              LOG.error(ex.getMessage(), ex);
            } catch(CoreException ex) {
              LOG.error(ex.getMessage(), ex);
            }
          }
        }

        private void ignoreAtDefinition(MojoExecutionKey key, Collection<MavenProject> projects) {
          ignore(key, M2EUtils.getDefiningProjects(key, projects));
        }
      };

      Job job = new WorkspaceJob(Messages.MavenImportWizard_job) {
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
          try {
            importOperation.run(monitor);
            if(lifecycleMappingPage != null) {
              ignoreJob.run(monitor);
            }
          } catch(InvocationTargetException e) {
            return AbstractCreateMavenProjectsOperation.toStatus(e);
          } catch(InterruptedException e) {
            return Status.CANCEL_STATUS;
          }
          return Status.OK_STATUS;
        }
      };
      job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
      job.schedule();
    }

    return doImport;
  }

  @Override
  public boolean canFinish() {
    IWizardPage currentPage = getContainer().getCurrentPage();

    if(!currentPage.isPageComplete()) {
      return false;
    }

    if(getDiscovery() == null) {
      return true;
    }

    if(currentPage == page) {
      // allow finish if there are no mapping problems and no selected proposals. 
      // the latter is important to force the user to go through p2 license page
      return getMappingConfiguration().isMappingComplete(true)
          && getMappingConfiguration().getSelectedProposals().isEmpty();
    }

    return super.canFinish();
  }

  private List<IMavenDiscoveryProposal> getMavenDiscoveryProposals() {
    if(lifecycleMappingPage == null) {
      return Collections.emptyList();
    }
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

  void scanProjects(final List<MavenProjectInfo> list, IProgressMonitor monitor) {
    LOG.debug("About to calculate lifecycle mapping configuration"); //$NON-NLS-1$
    ProjectImportConfiguration importConfiguration = getProjectImportConfiguration();
    mappingConfiguration = LifecycleMappingConfiguration.calculate(list, importConfiguration, monitor);
    discoverProposals(mappingConfiguration, monitor);
  }

  void discoverProposals(LifecycleMappingConfiguration mappingConfiguration, IProgressMonitor monitor)  {
    final IMavenDiscovery discovery = getDiscovery();

    if(discovery == null) {
      return;
    }

    Collection<ProjectLifecycleMappingConfiguration> projects = mappingConfiguration.getProjects();
    monitor.beginTask(Messages.MavenImportWizard_searchingTaskTitle, projects.size());

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
            SubMonitor.convert(monitor, NLS.bind(Messages.MavenImportWizard_analyzingProject, project.getRelpath()), 1)));
      } catch(CoreException e) {
        //XXX we shall not swallow this exception but associate with the project/execution
        LOG.error(e.getMessage(), e);
      }
      monitor.worked(1);
    }

    mappingConfiguration.setProposals(proposals);
  }

  private boolean skipIncompleteWarning() {
    return M2EUIPluginActivator.getDefault().getPreferenceStore()
        .getBoolean(MavenPreferenceConstants.P_WARN_INCOMPLETE_MAPPING);
  }

  private boolean warnIncompleteMapping() {
    if (!skipIncompleteWarning()) {
      MessageDialogWithToggle dialog = MessageDialogWithToggle.open(MessageDialog.CONFIRM, getShell(),
          Messages.MavenImportWizard_titleIncompleteMapping, Messages.MavenImportWizard_messageIncompleteMapping,
          Messages.MavenImportWizard_hideWarningMessage, false, null, null, SWT.SHEET);
      if(dialog.getReturnCode() == Window.OK) {
        M2EUIPluginActivator.getDefault().getPreferenceStore()
            .setValue(MavenPreferenceConstants.P_WARN_INCOMPLETE_MAPPING, dialog.getToggleState());
        return true;
      }
      return false;
    }
    return true;
  }
}
