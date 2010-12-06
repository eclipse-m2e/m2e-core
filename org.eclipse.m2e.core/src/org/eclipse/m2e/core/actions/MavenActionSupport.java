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

package org.eclipse.m2e.core.actions;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;

/**
 * 
 * MavenActionSupport
 *
 * @author Jason van Zyl
 */
public abstract class MavenActionSupport implements IObjectActionDelegate {
  protected IStructuredSelection selection;

  protected IWorkbenchPart targetPart;

  protected Set<ArtifactKey> getArtifacts(IFile file, MavenPlugin plugin) {
    try {
      MavenProjectManager projectManager = plugin.getMavenProjectManager();
      IMavenProjectFacade projectFacade = projectManager.create(file, true, new NullProgressMonitor());
      if(projectFacade != null) {
        return ArtifactRef.toArtifactKey(projectFacade.getMavenProjectArtifacts());
      }
    } catch(Exception ex) {
      String msg = "Can't read Maven project";
      MavenLogger.log(msg, ex);
      plugin.getConsole().logError(msg + "; " + ex.toString());
    }
    return Collections.emptySet();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    this.targetPart = targetPart;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }

  protected Shell getShell() {
    Shell shell = null;
    if(targetPart != null) {
      shell = targetPart.getSite().getShell();
    }
    if(shell != null) {
      return shell;
    }

    IWorkbench workbench = MavenPlugin.getDefault().getWorkbench();
    if(workbench == null) {
      return null;
    }

    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    return window == null ? null : window.getShell();
  }

  protected IFile getPomFileFromPomEditorOrViewSelection() {
    IFile file = null;    
    //
    // If I am in the POM editor I want to get hold of the IFile that is currently in the buffer
    //
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

    if(window != null) {
      IWorkbenchPage page = window.getActivePage();
      if(page != null) {
        IEditorPart editor = page.getActiveEditor();
        if(editor != null) {
          IEditorInput input = editor.getEditorInput();
          if(input instanceof IFileEditorInput) {
            IFileEditorInput fileInput = (IFileEditorInput) input;
            file = fileInput.getFile();
            if(file.getName().equals(IMavenConstants.POM_FILE_NAME)) {
              return file;
            }
          }
        }
      }
    }    

    //
    // Otherwise we will assume a pom.xml file or IProject is being selected in the
    // package explorer and we'll get the IFile from that. Otherwise we'll bail.
    //
    Object o = selection.iterator().next();

    if(o instanceof IProject) {
      file = ((IProject) o).getFile(IMavenConstants.POM_FILE_NAME);
    } else if(o instanceof IFile) {
      file = (IFile) o;
    } else {
      file = null;
    }

    return file;
  }
}
