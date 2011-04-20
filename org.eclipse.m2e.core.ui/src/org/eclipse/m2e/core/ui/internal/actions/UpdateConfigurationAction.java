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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;

import org.eclipse.m2e.core.ui.internal.UpdateConfigurationJob;
import org.eclipse.m2e.core.ui.internal.dialogs.UpdateDepenciesDialog;


public class UpdateConfigurationAction extends MavenProjectActionSupport  {

  public static final String ID = "org.eclipse.m2e.updateConfigurationAction"; //$NON-NLS-1$

  public UpdateConfigurationAction() {
  }

  public void run(IAction action) {
    UpdateDepenciesDialog dialog = new UpdateDepenciesDialog(getShell(), getProjects());
    if(dialog.open() == Window.OK) {
      new UpdateConfigurationJob(dialog.getSelectedProjects(), dialog.isOffline(), dialog.isForceUpdate()).schedule();
    }
  }

}
