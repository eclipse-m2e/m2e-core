/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.ui.internal.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.wizards.MavenPomWizard;


public class EnableNatureAction implements IObjectActionDelegate, IExecutableExtension {
  private static final Logger log = LoggerFactory.getLogger(EnableNatureAction.class);

  public static final String ID = "org.eclipse.m2e.enableNatureAction"; //$NON-NLS-1$

  static final String ID_WORKSPACE = "org.eclipse.m2e.enableWorkspaceResolutionAction"; //$NON-NLS-1$

  static final String ID_MODULES = "org.eclipse.m2e.enableModulesAction"; //$NON-NLS-1$

  private boolean workspaceProjects = true;

  private ISelection selection;

  public EnableNatureAction() {
  }

  public EnableNatureAction(String option) {
    setInitializationData(null, null, option);
  }

  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if(IMavenConstants.NO_WORKSPACE_PROJECTS.equals(data)) {
      this.workspaceProjects = false;
    }
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  @Override
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  @Override
  public void run(IAction action) {
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      for(Object element : structuredSelection) {
        IProject project = null;
        if(element instanceof IProject) {
          project = (IProject) element;
        } else if(element instanceof IAdaptable) {
          project = ((IAdaptable) element).getAdapter(IProject.class);
        }
        if(project != null) {
          enableNature(project, structuredSelection.size() == 1);
        }
      }
    }
  }

  private void enableNature(final IProject project, boolean isSingle) {
    final M2EUIPluginActivator plugin = M2EUIPluginActivator.getDefault();
    IFile pom = project.getFile(IMavenConstants.POM_FILE_NAME);
    if(!pom.exists()) {
      if(isSingle) {
        // XXX move into AbstractProjectConfigurator and use Eclipse project settings
        IWorkbench workbench = PlatformUI.getWorkbench();

        MavenPomWizard wizard = new MavenPomWizard();
        wizard.init(workbench, (IStructuredSelection) selection);

        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
        WizardDialog wizardDialog = new WizardDialog(shell, wizard);
        wizardDialog.create();
        wizardDialog.getShell().setText(Messages.EnableNatureAction_wizard_shell);
        if(wizardDialog.open() == Window.CANCEL) {
          return;
        }
      } else {
        //if we have multiple selection and this project has no pom, just skip it.
        // do not enable maven nature for projects without pom.
        log.warn(NLS.bind(
            "Skipping project {0}, no pom.xml file present, no reason to have maven nature enabled", project.getName())); //$NON-NLS-1$
        return;
      }
    }
    Job job = new Job(Messages.EnableNatureAction_job_enable) {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
        try {
          ResolverConfiguration configuration = new ResolverConfiguration();
          configuration.setResolveWorkspaceProjects(workspaceProjects);
          configuration.setSelectedProfiles(""); //$NON-NLS-1$

          boolean hasMavenNature = project.hasNature(IMavenConstants.NATURE_ID);

          IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

          configurationManager.enableMavenNature(project, configuration, monitor);

          if(!hasMavenNature) {
            configurationManager.updateProjectConfiguration(project, monitor);
          }
        } catch(CoreException ex) {
          log.error(ex.getMessage(), ex);
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }
}
