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

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.ui.internal.dialogs.UpdateDepenciesDialog;


public class UpdateDependenciesAction extends MavenProjectActionSupport  {

  public static final String ID = "org.eclipse.m2e.refreshMavenModelsAction"; //$NON-NLS-1$

  public UpdateDependenciesAction() {
  }

  public void run(IAction action) {
    UpdateDepenciesDialog dialog = new UpdateDepenciesDialog(getShell(), getProjects());
    if(dialog.open() == Window.OK) {
      IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
      projectManager.refresh(new MavenUpdateRequest(dialog.getSelectedProjects(), //
          dialog.isOffline(), dialog.isForceUpdate()));
    }
  }

}
