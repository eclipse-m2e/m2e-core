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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.wizards.MavenMaterializePomWizard;


public class MaterializeAction implements IObjectActionDelegate {

  public static final String ID = "org.eclipse.m2e.materializeAction"; //$NON-NLS-1$

  private IStructuredSelection selection;

  public void run(IAction action) {
    MavenMaterializePomWizard wizard = new MavenMaterializePomWizard();
    wizard.init(PlatformUI.getWorkbench(), selection);
    
    Dependency[] dependencies = wizard.getDependencies();
    if(dependencies!=null && dependencies.length>0) {
      WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
      dialog.open();
    } else {
      // TODO show info dialog
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

}
