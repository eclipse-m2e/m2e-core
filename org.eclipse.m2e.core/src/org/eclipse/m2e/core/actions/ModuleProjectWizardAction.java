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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.wizards.MavenModuleWizard;

/**
 * A module project wizard action.
 */
public class ModuleProjectWizardAction implements IObjectActionDelegate {

  /** action id */
  public static final String ID =
    "org.eclipse.m2e.actions.moduleProjectWizardAction"; //$NON-NLS-1$
  
  /** the current selection */
  private IStructuredSelection selection;
  
  /** parent shell */
  private Shell parent;

  /** Runs the action. */
  public void run( IAction action ) {
    MavenModuleWizard wizard = new MavenModuleWizard();
    wizard.init( PlatformUI.getWorkbench(), selection );
    WizardDialog dialog = new WizardDialog( parent, wizard );
    dialog.open();
  }

  
  /** Sets the active workbench part. */
  public void setActivePart( IAction action, IWorkbenchPart part ) {
    parent = part.getSite().getShell();
  }


  /** Handles the selection change */
  public void selectionChanged( IAction action, ISelection selection ) {
    if( selection instanceof IStructuredSelection ) {
      this.selection = ( IStructuredSelection ) selection;
    }
  }
}
