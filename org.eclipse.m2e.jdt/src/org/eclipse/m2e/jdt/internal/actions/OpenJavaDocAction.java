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

package org.eclipse.m2e.jdt.internal.actions;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.SelectionUtil;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.jdt.internal.Messages;

/**
 * Open JavaDoc action
 *
 * @author Eugene Kuleshov
 */
public class OpenJavaDocAction extends ActionDelegate {

  public static final String ID = "org.eclipse.m2e.openJavaDocAction"; //$NON-NLS-1$
  
  private IStructuredSelection selection;

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }
  
  public void run(IAction action) {
    if(selection != null) {
      try {
        final ArtifactKey ak = SelectionUtil.getArtifactKey(this.selection.getFirstElement());
        if(ak == null) {
          openDialog(Messages.OpenJavaDocAction_message1);
          return;
        }

        new Job(NLS.bind(Messages.OpenJavaDocAction_job_open_javadoc,ak)) {
          protected IStatus run(IProgressMonitor monitor) {
            openJavaDoc(ak.getGroupId(), ak.getArtifactId(), ak.getVersion(), monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
        
      } catch(CoreException ex) {
        MavenLogger.log(ex);
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
          public void run() {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
                Messages.OpenJavaDocAction_error_title, Messages.OpenJavaDocAction_error_message);
          }
        });
      }
    }
  }

  protected void openJavaDoc(String groupId, String artifactId, String version, IProgressMonitor monitor) {
    final String name = groupId + ":" + artifactId + ":" + version + ":javadoc"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    try {
      IMaven maven = MavenPlugin.getDefault().getMaven();

      List<ArtifactRepository> artifactRepositories = maven.getArtifactRepositories();
      
      Artifact artifact = maven.resolve(groupId, artifactId, version, "javadoc", "javadoc", artifactRepositories, monitor); //$NON-NLS-1$ //$NON-NLS-2$
      
      final File file = artifact.getFile();
      if(file == null) {
        openDialog(NLS.bind(Messages.OpenJavaDocAction_error_download, name));
        return;
      }

      PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
        public void run() {
          try {
            String url = "jar:" + file.toURI().toString() + "!/index.html"; //$NON-NLS-1$ //$NON-NLS-2$
            URL helpUrl = PlatformUI.getWorkbench().getHelpSystem().resolve(url, true);
            
            IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR, //
                name, name, name);
            browser.openURL(helpUrl);
          } catch(PartInitException ex) {
            MavenLogger.log(ex);
          }
        }
      });
      
    } catch(CoreException ex) {
      MavenLogger.log("Can't download JavaDoc for " + name, ex);
      openDialog(NLS.bind(Messages.OpenJavaDocAction_error_download,name));
      // TODO search index and offer to select other version
    }    

  }

  private static void openDialog(final String msg) {
    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
      public void run() {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
            Messages.OpenJavaDocAction_info_title, msg);
      }
    });
  }
  
}
