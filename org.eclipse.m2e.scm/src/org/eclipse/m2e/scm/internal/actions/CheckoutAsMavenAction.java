/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc.
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

package org.eclipse.m2e.scm.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.scm.ScmUrl;
import org.eclipse.m2e.scm.internal.wizards.MavenCheckoutWizard;


/**
 * Checkout as Maven project action
 * 
 * @author @author Eugene Kuleshov
 */
public class CheckoutAsMavenAction implements IObjectActionDelegate {

  private IStructuredSelection selection;

  private IWorkbenchPart targetPart;

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    this.targetPart = targetPart;
  }

  public void run(IAction action) {
    ScmUrl[] urls = null;
    if(selection != null) {
      urls = new ScmUrl[selection.size()];
      int i = 0;
      for(Object name : selection) {
        urls[i++ ] = (ScmUrl) name;
      }
    }

    MavenCheckoutWizard wizard = new MavenCheckoutWizard(urls);
    WizardDialog dialog = new WizardDialog(getShell(), wizard);
    dialog.open();
  }

  protected Shell getShell() {
    Shell shell = null;
    if(targetPart != null) {
      shell = targetPart.getSite().getShell();
    }
    if(shell != null) {
      return shell;
    }

    IWorkbench workbench = PlatformUI.getWorkbench();
    if(workbench == null) {
      return null;
    }

    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    return window == null ? null : window.getShell();
  }

}
