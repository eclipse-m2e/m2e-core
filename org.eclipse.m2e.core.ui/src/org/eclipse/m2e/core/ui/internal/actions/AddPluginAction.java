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

package org.eclipse.m2e.core.ui.internal.actions;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.BUILD;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PLUGINS;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;


public class AddPluginAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {
  private static final Logger log = LoggerFactory.getLogger(AddPluginAction.class);

  public static final String ID = "org.eclipse.m2e.addPluginAction"; //$NON-NLS-1$

  @Override
  public void run(IAction action) {
    IFile file = getPomFileFromPomEditorOrViewSelection();

    if(file == null) {
      return;
    }
    MavenProject mp = null;
    IProject prj = file.getProject();
    if(prj != null && IMavenConstants.POM_FILE_NAME.equals(file.getProjectRelativePath().toString())) {
      IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(prj);
      if(facade != null) {
        mp = facade.getMavenProject();
      }
    }

    MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchPluginDialog(getShell(),
        Messages.AddPluginAction_searchDialog_title, mp, prj, false);
    if(dialog.open() == Window.OK) {
      final IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
      if(indexedArtifactFile != null) {
        try {
          performOnDOMDocument(new OperationTuple(file, (Operation) document -> {
            Element pluginsEl = getChild(document.getDocumentElement(), BUILD, PLUGINS);
            PomHelper.createPlugin(pluginsEl, indexedArtifactFile.group, indexedArtifactFile.artifact,
                indexedArtifactFile.version);
          }));
        } catch(Exception ex) {
          log.error("Can't add plugin to " + file, ex); //$NON-NLS-1$
        }
      }
    }
  }

  @Override
  public void dispose() {
  }

  @Override
  public void init(IWorkbenchWindow window) {
  }
}
