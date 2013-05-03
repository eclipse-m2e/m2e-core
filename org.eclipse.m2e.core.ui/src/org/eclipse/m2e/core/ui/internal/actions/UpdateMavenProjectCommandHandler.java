/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - transformed into a CommandHandler
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.eclipse.m2e.core.ui.internal.dialogs.UpdateMavenProjectsDialog;


/**
 * Handler for the Update Projectcommand. This can then be bound to whatever keybinding the user prefers (defaults to
 * Ctrl+Alt+U).
 * 
 * @author Fred Bricon
 */
public class UpdateMavenProjectCommandHandler extends AbstractHandler {

  public Object execute(final ExecutionEvent event) {

    ISelection selection = HandlerUtil.getCurrentSelection(event);

    Shell shell = HandlerUtil.getActiveWorkbenchWindow(event).getShell();

    IProject[] projects = SelectionUtil.getProjects(selection);

    openUpdateProjectsDialog(shell, projects);

    return null;
  }

  /* package */static void openUpdateProjectsDialog(Shell shell, IProject[] projects) {
    UpdateMavenProjectsDialog dialog = new UpdateMavenProjectsDialog(shell, projects);
    if(dialog.open() == Window.OK) {
      new UpdateMavenProjectJob(dialog.getSelectedProjects(), dialog.isOffline(), dialog.isForceUpdateDependencies(),
          dialog.isUpdateConfiguration(), dialog.isCleanProjects(), dialog.isRefreshFromLocal()).schedule();
    }
  }
}
