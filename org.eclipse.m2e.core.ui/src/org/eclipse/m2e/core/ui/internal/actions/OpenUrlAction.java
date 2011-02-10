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

package org.eclipse.m2e.core.ui.internal.actions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Open Url Action
 * 
 * @author Eugene Kuleshov
 */
public class OpenUrlAction extends ActionDelegate implements IWorkbenchWindowActionDelegate, IExecutableExtension {
  private static final Logger log = LoggerFactory.getLogger(OpenUrlAction.class);

  public static final String ID_PROJECT = "org.eclipse.m2e.openProjectPage"; //$NON-NLS-1$

  public static final String ID_ISSUES = "org.eclipse.m2e.openIssuesPage"; //$NON-NLS-1$

  public static final String ID_SCM = "org.eclipse.m2e.openScmPage"; //$NON-NLS-1$

  public static final String ID_CI = "org.eclipse.m2e.openCiPage"; //$NON-NLS-1$

  String actionId;
  
  private IStructuredSelection selection;

  public OpenUrlAction() {
  }
  
  public OpenUrlAction(String id) {
    this.actionId = id;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    if(selection != null) {
      try {
        Object element = this.selection.getFirstElement();
        final ArtifactKey a = SelectionUtil.getArtifactKey(element);
        if(a != null) {
          new Job(Messages.OpenUrlAction_job_browser) {
            protected IStatus run(IProgressMonitor monitor) {
              openBrowser(actionId, a.getGroupId(), a.getArtifactId(), a.getVersion(), monitor);
              return Status.OK_STATUS;
            }
            
          }.schedule();
          return;
        }
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
          public void run() {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
                Messages.OpenUrlAction_open_url_title, Messages.OpenUrlAction_open_url_message);
          }
        });
      }
    }
  }

  public static void openBrowser(String actionId, String groupId, String artifactId, String version, IProgressMonitor monitor) {
    try {
      MavenProject mavenProject = getMavenProject(groupId, artifactId, version, monitor);
      final String url = getUrl(actionId, mavenProject);
      if(url!=null) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
          public void run() {
            try {
              IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
              IWebBrowser browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR
                  | IWorkbenchBrowserSupport.LOCATION_BAR, url, url, url);
              browser.openURL(new URL(url));
            } catch(PartInitException ex) {
              log.error(ex.getMessage(), ex);
            } catch(MalformedURLException ex) {
              log.error("Malformed url " + url, ex);
            }
          }
        });
      }
    } catch(Exception ex) {
      log.error("Can't open URL", ex);
    }
  }

  private static String getUrl(String actionId, MavenProject mavenProject) {
    String url = null;
    if(ID_PROJECT.equals(actionId)) {
      url = mavenProject.getUrl();
      if(url == null) {
        openDialog(Messages.OpenUrlAction_error_no_url);
      }
    } else if(ID_ISSUES.equals(actionId)) {
      IssueManagement issueManagement = mavenProject.getIssueManagement();
      if(issueManagement != null) {
        url = issueManagement.getUrl();
      }
      if(url == null) {
        openDialog(Messages.OpenUrlAction_error_no_issues);
      }
    } else if(ID_SCM.equals(actionId)) {
      Scm scm = mavenProject.getScm();
      if(scm != null) {
        url = scm.getUrl();
      }
      if(url == null) {
        openDialog(Messages.OpenUrlAction_error_no_scm);
      }
    } else if(ID_CI.equals(actionId)) {
      CiManagement ciManagement = mavenProject.getCiManagement();
      if(ciManagement != null) {
        url = ciManagement.getUrl();
      }
      if(url == null) {
        openDialog(Messages.OpenUrlAction_error_no_ci);
      }
    }
    return url;
  }

  private static MavenProject getMavenProject(String groupId, String artifactId, String version, IProgressMonitor monitor) throws Exception {
    String name = groupId + ":" + artifactId + ":" + version;

    MavenPlugin plugin = MavenPlugin.getDefault();
    IMaven maven = MavenPlugin.getDefault().getMaven();

    IMavenProjectFacade projectFacade = plugin.getMavenProjectManager().getMavenProject(groupId, artifactId, version);
    if(projectFacade != null) {
      return projectFacade.getMavenProject(monitor);
    }

    List<ArtifactRepository> artifactRepositories = maven.getArtifactRepositories();

    Artifact a = maven.resolve(groupId, artifactId, version, "pom", null, artifactRepositories, monitor); //$NON-NLS-1$

    File pomFile = a.getFile();
    if(pomFile == null) {
      openDialog(NLS.bind(Messages.OpenUrlAction_error_open, name));
      return null;
    }

    return maven.readProject(pomFile, monitor);
  }

  private static void openDialog(final String msg) {
    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
      public void run() {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
            Messages.OpenUrlAction_browser_title, msg);
      }
    });
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
   */
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if(data != null) {
      actionId = (String) data;
    }
  }

}
