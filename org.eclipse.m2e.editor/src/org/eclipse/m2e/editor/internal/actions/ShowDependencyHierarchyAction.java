/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.editor.internal.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionDelegate;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.MavenPomEditor;


/**
 * @author Eugene Kuleshov
 */
public class ShowDependencyHierarchyAction extends ActionDelegate {

  public static final String ID = "org.eclipse.m2e.ShowDependencyHierarchy"; //$NON-NLS-1$

  private IStructuredSelection selection;

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  @Override
  public void run(IAction action) {
    if(selection != null) {
      Object element = this.selection.getFirstElement();
      IMavenProjectFacade projectFacade = SelectionUtil.getType(element, IMavenProjectFacade.class);
      if(projectFacade != null) {
        ArtifactKey artifactKey = SelectionUtil.getType(element, ArtifactKey.class);
        if(artifactKey != null) {
          showDependencyHierarchy(projectFacade.getArtifactKey(), artifactKey);
        }
      }
    }
  }

  private void showDependencyHierarchy(final ArtifactKey projectKey, final ArtifactKey artifactKey) {
    if(artifactKey != null) {
      new Job(Messages.ShowDependencyHierarchyAction_job_openPomEditor) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          final IEditorPart editor = OpenPomAction.openEditor(projectKey.getGroupId(), //
              projectKey.getArtifactId(), projectKey.getVersion(), monitor);
          if(editor instanceof MavenPomEditor) {
            Display.getDefault().asyncExec(() -> ((MavenPomEditor) editor).showDependencyHierarchy(artifactKey));
          }
          return Status.OK_STATUS;
        }
      }.schedule();
    }
  }

}
