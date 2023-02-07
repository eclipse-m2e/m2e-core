/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - Refactored project import
 *******************************************************************************/

package org.eclipse.m2e.scm.internal.wizards;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.wizards.datatransfer.ExternalProjectImportWizard;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.jobs.MavenWorkspaceJob;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.actions.OpenMavenConsoleAction;
import org.eclipse.m2e.core.ui.internal.wizards.ImportMavenProjectsJob;
import org.eclipse.m2e.core.ui.internal.wizards.MavenImportWizard;
import org.eclipse.m2e.scm.MavenCheckoutOperation;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.internal.Messages;


/**
 * Maven project checkout Job
 *
 * @author Eugene Kuleshov
 */
public abstract class MavenProjectCheckoutJob extends MavenWorkspaceJob {
  private static final Logger log = LoggerFactory.getLogger(MavenProjectCheckoutJob.class);

  final ProjectImportConfiguration configuration;

  boolean checkoutAllProjects;

  Collection<MavenProjectInfo> projects;

  File location;

  List<String> collectedLocations = new ArrayList<>();

  final List<IWorkingSet> workingSets;

  MavenProjectCheckoutJob(ProjectImportConfiguration importConfiguration, boolean checkoutAllProjects,
      List<IWorkingSet> workingSets) {
    super(Messages.MavenProjectCheckoutJob_title);
    this.configuration = importConfiguration;
    this.checkoutAllProjects = checkoutAllProjects;
    this.workingSets = workingSets;

    setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
    addJobChangeListener(new CheckoutJobChangeListener());
  }

  public void setLocation(File location) {
    this.location = location;
  }

  protected abstract Collection<MavenProjectScmInfo> getProjects(IProgressMonitor monitor) throws InterruptedException;

  // WorkspaceJob

  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    try {
      MavenCheckoutOperation operation = new MavenCheckoutOperation(location, getProjects(monitor));
      operation.run(monitor);
      collectedLocations.addAll(operation.getLocations());

      IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

      MavenModelManager modelManager = MavenPlugin.getMavenModelManager();

      LocalProjectScanner scanner = new LocalProjectScanner(operation.getLocations(), true, modelManager);
      scanner.run(monitor);

      this.projects = MavenPlugin.getProjectConfigurationManager().collectProjects(scanner.getProjects());

      if(checkoutAllProjects) {
        // check if there any project name conflicts
        for(MavenProjectInfo projectInfo : projects) {
          Model model = projectInfo.getModel();
          if(model == null) {
            model = modelManager.readMavenModel(projectInfo.getPomFile());
            projectInfo.setModel(model);
          }

          String projectName = ProjectConfigurationManager.getProjectName(configuration, model);
          IProject project = workspace.getProject(projectName);
          if(project.exists()) {
            checkoutAllProjects = false;
            break;
          }
        }
      }

      return Status.OK_STATUS;

    } catch(InterruptedException ex) {
      return Status.CANCEL_STATUS;
    }
  }

  /**
   * Checkout job listener
   */
  final class CheckoutJobChangeListener extends JobChangeAdapter {

    public void done(IJobChangeEvent event) {
      IStatus result = event.getResult();
      if(result.getSeverity() == IStatus.CANCEL) {
        return;
      } else if(!result.isOK()) {
        // XXX report errors
        return;
      }

      if(projects.isEmpty()) {
        log.info("No Maven projects to import");

        if(collectedLocations.size() == 1) {
          final String location = collectedLocations.get(0);

          DirectoryScanner projectScanner = new DirectoryScanner();
          projectScanner.setBasedir(location);
          projectScanner.setIncludes(new String[] {"**/.project"}); //$NON-NLS-1$
          projectScanner.scan();

          String[] projectFiles = projectScanner.getIncludedFiles();
          if(projectFiles != null && projectFiles.length > 0) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
              boolean res = MessageDialog.openConfirm(PlatformUI.getWorkbench().getDisplay().getActiveShell(), //
                  Messages.MavenProjectCheckoutJob_confirm_title, //
                  Messages.MavenProjectCheckoutJob_confirm_message);
              if(res) {
                IWizard wizard = new ExternalProjectImportWizard(collectedLocations.get(0));
                WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
                dialog.open();
              } else {
                cleanup(collectedLocations);
              }
            });
            return;
          }

          PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
            boolean res = MessageDialog.openConfirm(PlatformUI.getWorkbench().getDisplay().getActiveShell(), //
                Messages.MavenProjectCheckoutJob_confirm2_title, //
                Messages.MavenProjectCheckoutJob_confirm2_message);
            if(res) {
              Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
              clipboard.setContents(new Object[] {location}, new Transfer[] {TextTransfer.getInstance()});

              NewProjectAction newProjectAction = new NewProjectAction(
                  PlatformUI.getWorkbench().getActiveWorkbenchWindow());
              newProjectAction.run();
            } else {
              cleanup(collectedLocations);
            }
          });
          return;
        }

        cleanup(collectedLocations);
      }

      if(checkoutAllProjects) {
        WorkspaceJob job = new ImportMavenProjectsJob(projects, workingSets, configuration);
        job.schedule();
      } else {
        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
          MavenImportWizard wizard = new MavenImportWizard(configuration, collectedLocations);
          wizard.setBasedirRemameRequired(true);
          WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
          int res = dialog.open();
          if(res == Window.CANCEL) {
            cleanup(collectedLocations);
          }
        });
      }
    }

    protected void cleanup(List<String> locations) {
      for(String location : locations) {
        try {
          FileUtils.deleteDirectory(location);
        } catch(IOException ex) {
          String msg = "Can't delete " + location + "; " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()); //$NON-NLS-1$
          log.error(msg, ex);
        }
      }
    }
  }
}
