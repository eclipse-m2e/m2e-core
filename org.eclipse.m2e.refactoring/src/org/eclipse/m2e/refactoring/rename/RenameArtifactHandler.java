/*******************************************************************************
 * Copyright (c) 2011, 2018 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.refactoring.rename;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.m2e.refactoring.internal.SaveDirtyFilesDialog;


public class RenameArtifactHandler extends AbstractHandler {
  private static final Logger log = LoggerFactory.getLogger(RenameArtifactHandler.class);

  /* (non-Javadoc)
   * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
   */
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ISelection selection = computeSelection(event);
    if(!(selection instanceof IStructuredSelection)) {
      return null;
    }
    Object element = ((IStructuredSelection) selection).getFirstElement();
    if(element instanceof IFile) {
      rename((IFile) element);
    } else if(element instanceof IAdaptable) {
      IProject project = ((IAdaptable) element).getAdapter(IProject.class);
      if(project == null) {
        return null;
      }
      IFile file = project.getFile("pom.xml"); //$NON-NLS-1$
      if(file != null) {
        rename(file);
      }
    }
    return null;
  }

  private void rename(IFile file) {
    try {
      // get the model from existing file
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      boolean rc = SaveDirtyFilesDialog.saveDirtyFiles("pom.xml"); //$NON-NLS-1$
      if(!rc)
        return;
      MavenRenameWizard wizard = new MavenRenameWizard(file);
      RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
      String titleForFailedChecks = ""; //$NON-NLS-1$
      op.run(shell, titleForFailedChecks);
    } catch(Exception e) {
      log.error("Unable to rename " + file, e);
    }
  }

  protected ISelection computeSelection(ExecutionEvent event) {
    ISelection selection = HandlerUtil.getActiveMenuSelection(event);
    if(!(selection instanceof IStructuredSelection)) {
      selection = HandlerUtil.getActiveMenuEditorInput(event);
    }
    if(!(selection instanceof IStructuredSelection)) {
      selection = HandlerUtil.getCurrentSelection(event);
    }
    if(!(selection instanceof IStructuredSelection)) {
      selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
    }
    return selection;
  }

}
