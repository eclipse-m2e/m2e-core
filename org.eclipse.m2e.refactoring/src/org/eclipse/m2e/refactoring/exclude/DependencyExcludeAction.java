/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.refactoring.exclude;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer.RequiredProjectWrapper;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

import org.apache.maven.artifact.Artifact;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;


/**
 * This action is intended to be used in popup menus
 *
 * @author Anton Kraev
 */
@SuppressWarnings("restriction")
public class DependencyExcludeAction implements IActionDelegate {

  public static final String ID = "org.eclipse.m2e.refactoring.DependencyExclude"; //$NON-NLS-1$

  private IFile file;

  private ArtifactKey[] keys;

  @Override
  public void run(IAction action) {
    if(keys == null || file == null) {
      return;
    }

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(file.getProject());
    if(facade == null || !file.equals(facade.getPom())) {
      return;
    }

    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    ExcludeArtifactRefactoring r = new ExcludeArtifactRefactoring(keys);
    MavenExcludeWizard wizard = new MavenExcludeWizard(r, facade);
    try {
      String titleForFailedChecks = ""; //$NON-NLS-1$
      RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
      op.run(shell, titleForFailedChecks);
    } catch(InterruptedException e) {
      // do nothing
    }
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    file = null;
    keys = null;

    // TODO move logic into adapters
    if(selection instanceof IStructuredSelection structuredSelection) {

      List<ArtifactKey> keys = new ArrayList<>(structuredSelection.size());
      for(Object selected : structuredSelection.toArray()) {
        if(selected instanceof Artifact artifact) {
          file = getFileFromEditor();
          keys.add(new ArtifactKey(artifact));
        } else if(selected instanceof org.eclipse.aether.graph.DependencyNode node) {
          file = getFileFromEditor();
          org.eclipse.aether.artifact.Artifact artifact = node.getDependency().getArtifact();
          keys.add(new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), null));
        } else if(selected instanceof RequiredProjectWrapper w) {
          file = getFileFromProject(w.getParentClassPathContainer().getJavaProject());
          keys.add(SelectionUtil.getType(selected, ArtifactKey.class));
        } else {
          keys.add(SelectionUtil.getType(selected, ArtifactKey.class));
          if(selected instanceof IJavaElement el) {
            file = getFileFromProject(el.getParent().getJavaProject());
          }
        }
      }
      this.keys = keys.toArray(new ArtifactKey[keys.size()]);
    }
    if(keys != null && keys.length > 0 && file != null) {
      action.setEnabled(true);
    } else {
      action.setEnabled(false);
    }
  }

  private IFile getFileFromProject(IJavaProject javaProject) {
    return javaProject.getProject().getFile("pom.xml"); //$NON-NLS-1$
  }

  //mkleint: scary
  private IFile getFileFromEditor() {
    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    return part != null && part.getEditorInput() instanceof IFileEditorInput input ? input.getFile() : null;
  }
}
