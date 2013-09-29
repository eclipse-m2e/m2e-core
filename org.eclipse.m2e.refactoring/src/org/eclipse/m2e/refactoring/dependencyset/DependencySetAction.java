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

package org.eclipse.m2e.refactoring.dependencyset;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * This action is intended to be used in popup menus
 * 
 * @author Milos Kleint
 */
public class DependencySetAction implements IActionDelegate {

  public static final String ID = "org.eclipse.m2e.refactoring.DependencySet"; //$NON-NLS-1$

  private IFile file;

  private List<ArtifactKey> keys;

  public void run(IAction action) {
    if(keys != null && keys.size() > 0 && file != null) {
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      MavenDependencySetWizard wizard = new MavenDependencySetWizard(file, keys);
      try {
        String titleForFailedChecks = ""; //$NON-NLS-1$
        RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
        op.run(shell, titleForFailedChecks);
      } catch(InterruptedException e) {
        // do nothing
      }
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    file = null;
    keys = new ArrayList<ArtifactKey>();

    if(selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      for(Object selected : structuredSelection.toArray()) {
        /*if (selected instanceof Artifact) {
          file = getFileFromEditor();
          keys.add(new ArtifactKey((Artifact) selected));
          
        } else*/if(selected instanceof org.eclipse.aether.graph.DependencyNode) {
          file = getFileFromEditor();
          org.eclipse.aether.graph.DependencyNode selected2 = (org.eclipse.aether.graph.DependencyNode) selected;
          if(selected2.getData().get("LEVEL") == null) {
            keys.add(new ArtifactKey(selected2.getDependency().getArtifact()));
          }

        } /*else if (selected instanceof RequiredProjectWrapper) {
          RequiredProjectWrapper w = (RequiredProjectWrapper) selected;
          file = getFileFromProject(w.getParentClassPathContainer().getJavaProject());
          keys.add(SelectionUtil.getType(selected, ArtifactKey.class));
          
          } else {
          keys.add(SelectionUtil.getType(selected, ArtifactKey.class));
          if (selected instanceof IJavaElement) {
            IJavaElement el = (IJavaElement) selected;
            file = getFileFromProject(el.getParent().getJavaProject());
          }
          
          }
          */
      }
    }

    if(keys.size() > 0 && file != null) {
      action.setEnabled(true);
    } else {
      action.setEnabled(false);
    }
  }

  //mkleint: scary
  private IFile getFileFromEditor() {
    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    if(part != null && part.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) part.getEditorInput();
      return input.getFile();
    }
    return null;
  }
}
