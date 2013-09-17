/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;

import org.eclipse.m2e.core.ui.internal.dialogs.AssignWorkingSetDialog;


/**
 * @since 1.5
 */
public class AssignWorkingSetAction extends MavenProjectActionSupport {

  public void run(IAction action) {
    IProject[] initialSelection = getProjects();

    AssignWorkingSetDialog dialog = new AssignWorkingSetDialog(getShell(), initialSelection);
    if(dialog.open() == Window.OK) {
      dialog.assignWorkingSets();
    }
  }

}
