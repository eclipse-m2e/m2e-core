/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - transformed into a CommandHandler
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.eclipse.m2e.core.ui.internal.dialogs.UpdateMavenProjectsDialog;


/**
 * Handler for the Update Project command. This can then be bound to whatever key binding the user prefers (defaults to
 * Alt+F5).
 *
 * @author Fred Bricon
 * @since 1.4.0
 */
public class UpdateMavenProjectCommandHandler extends AbstractHandler {

  private static final Logger log = LoggerFactory.getLogger(UpdateMavenProjectCommandHandler.class);

  @Override
  public Object execute(final ExecutionEvent event) {

    ISelection selection = HandlerUtil.getCurrentSelection(event);

    Shell shell = HandlerUtil.getActiveWorkbenchWindow(event).getShell();

    IProject[] projects = SelectionUtil.getProjects(selection, false);

    //If no projects in the current selection, look at the active editor
    if(projects == null || projects.length == 0) {
      projects = getProjectInActiveEditor(event);
    }

    //If no projects found, select all projects in the workspace
    if(projects == null || projects.length == 0) {
      projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    }

    openUpdateProjectsDialog(shell, projects);

    return null;
  }

  /**
   * get the (maven) project in current active editor
   */
  private IProject[] getProjectInActiveEditor(ExecutionEvent event) {
    try {
      IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
      if(activePart instanceof IEditorPart) {
        IEditorPart editorPart = (IEditorPart) activePart;
        if(editorPart.getEditorInput() instanceof IFileEditorInput) {
          IFileEditorInput fileInput = (IFileEditorInput) editorPart.getEditorInput();
          IProject project = fileInput.getFile().getProject();
          if(project != null && project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)) {
            return new IProject[] {project};
          }
        }
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
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
