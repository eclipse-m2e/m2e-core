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

package org.eclipse.m2e.core.wizards;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;
import org.eclipse.ui.progress.IProgressConstants;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.OpenMavenConsoleAction;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.MavenProjectScmInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.scm.MavenCheckoutOperation;


/**
 * Maven project checkout Job
 * 
 * @author Eugene Kuleshov
 */
public abstract class MavenProjectCheckoutJob extends WorkspaceJob {

  final ProjectImportConfiguration configuration;
  
  boolean checkoutAllProjects;

  Collection<MavenProjectInfo> projects;

  File location;
  
  List<String> collectedLocations = new ArrayList<String>();

  public MavenProjectCheckoutJob(ProjectImportConfiguration importConfiguration, boolean checkoutAllProjects) {
    super(Messages.MavenProjectCheckoutJob_title);
    this.configuration = importConfiguration;
    this.checkoutAllProjects = checkoutAllProjects;

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
      MavenPlugin plugin = MavenPlugin.getDefault();
      MavenConsole console = plugin.getConsole();

      MavenCheckoutOperation operation = new MavenCheckoutOperation(location, getProjects(monitor), console);
      operation.run(monitor);
      collectedLocations.addAll(operation.getLocations());

      IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();

      MavenModelManager modelManager = plugin.getMavenModelManager();
      
      LocalProjectScanner scanner = new LocalProjectScanner(workspace.getLocation().toFile(), operation.getLocations(),
          true, modelManager, console);
      scanner.run(monitor);

      this.projects = plugin.getProjectConfigurationManager().collectProjects(scanner.getProjects());

      if(checkoutAllProjects) {
        // check if there any project name conflicts 
        for(MavenProjectInfo projectInfo : projects) {
          Model model = projectInfo.getModel();
          if(model == null) {
            model = modelManager.readMavenModel(projectInfo.getPomFile());
            projectInfo.setModel(model);
          }

          String projectName = configuration.getProjectName(model);
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
        MavenPlugin.getDefault().getConsole().logMessage("No Maven projects to import");
        
        if(collectedLocations.size()==1) {
          final String location = collectedLocations.get(0);
          
          DirectoryScanner projectScanner = new DirectoryScanner();
          projectScanner.setBasedir(location);
          projectScanner.setIncludes(new String[] {"**/.project"}); //$NON-NLS-1$
          projectScanner.scan();
          
          String[] projectFiles = projectScanner.getIncludedFiles();
          if(projectFiles!=null && projectFiles.length>0) {
            Display.getDefault().asyncExec(new Runnable() {
              public void run() {
                boolean res = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), //
                    Messages.MavenProjectCheckoutJob_confirm_title, //
                    Messages.MavenProjectCheckoutJob_confirm_message);
                if(res) {
                  IWizard wizard = new ProjectsImportWizard(collectedLocations.get(0));
                  WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
                  dialog.open();
                } else {
                  cleanup(collectedLocations);
                }
              }
            });
            return;
          }
          
          Display.getDefault().syncExec(new Runnable() {
            public void run() {
              boolean res = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), //
                  Messages.MavenProjectCheckoutJob_confirm2_title, //
                  Messages.MavenProjectCheckoutJob_confirm2_message);
              if(res) {
                Clipboard clipboard = new Clipboard(Display.getDefault());
                clipboard.setContents(new Object[] { location }, new Transfer[] { TextTransfer.getInstance() });
                
                NewProjectAction newProjectAction = new NewProjectAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                newProjectAction.run();
              } else {
                cleanup(collectedLocations);
              }
            }
          });
          return;
        }

        cleanup(collectedLocations);
      }
      
      if(checkoutAllProjects) {
        final MavenPlugin plugin = MavenPlugin.getDefault();
        WorkspaceJob job = new WorkspaceJob(Messages.MavenProjectCheckoutJob_job) {
          public IStatus runInWorkspace(IProgressMonitor monitor) {
            Set<MavenProjectInfo> projectSet = plugin.getProjectConfigurationManager().collectProjects(projects);

            try {
              plugin.getProjectConfigurationManager().importProjects(projectSet, configuration, monitor);
            } catch(CoreException ex) {
              plugin.getConsole().logError("Projects imported with errors");
              return ex.getStatus();
            }

            return Status.OK_STATUS;
          }
        };
        ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(ResourcesPlugin.getWorkspace().getRoot());
        job.setRule(rule);
        job.schedule();

      } else {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            MavenImportWizard wizard = new MavenImportWizard(configuration, collectedLocations);
            WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
            int res = dialog.open();
            if(res == Window.CANCEL) {
              cleanup(collectedLocations);
            }
          }
        });
      }
    }

    protected void cleanup(List<String> locations) {
      MavenConsole console = MavenPlugin.getDefault().getConsole();
      for(String location : locations) {
        try {
          FileUtils.deleteDirectory(location);
        } catch(IOException ex) {
          String msg = "Can't delete " + location;
          console.logError(msg + "; " + (ex.getMessage()==null ? ex.toString() : ex.getMessage())); //$NON-NLS-1$
          MavenLogger.log(msg, ex);
        }
      }
    }
    
  }
  
}
