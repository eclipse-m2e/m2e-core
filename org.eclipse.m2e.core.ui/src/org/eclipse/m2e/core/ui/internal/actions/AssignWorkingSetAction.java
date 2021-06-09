/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

    @Override
    public void run(IAction action) {
    IProject[] initialSelection = getProjects();

    AssignWorkingSetDialog dialog = new AssignWorkingSetDialog(getShell(), initialSelection);
    if(dialog.open() == Window.OK) {
      dialog.assignWorkingSets();
    }
  }

}
