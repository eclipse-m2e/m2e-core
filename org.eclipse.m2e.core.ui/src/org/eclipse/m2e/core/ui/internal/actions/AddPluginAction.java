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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.*;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class AddPluginAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {
  private static final Logger log = LoggerFactory.getLogger(AddPluginAction.class);

  public static final String ID = "org.eclipse.m2e.addPluginAction"; //$NON-NLS-1$

  public void run(IAction action) {
    IFile file = getPomFileFromPomEditorOrViewSelection();

    if(file == null) {
      return;
    }
    MavenProject mp = null;
    IProject prj = file.getProject();
    if (prj != null && IMavenConstants.POM_FILE_NAME.equals(file.getProjectRelativePath().toString())) {
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(prj);
        if (facade != null) {
          mp = facade.getMavenProject();
        }
    }
    

    MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchPluginDialog(getShell(), Messages.AddPluginAction_searchDialog_title, 
        mp, prj, false);
    if(dialog.open() == Window.OK) {
      final IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
      if(indexedArtifactFile != null) {
        try {
          performOnDOMDocument(new OperationTuple(file, new Operation() {
            public void process(Document document) {
              Element pluginsEl = getChild(document.getDocumentElement(), BUILD, PLUGINS);
              PomHelper.createPlugin(pluginsEl, indexedArtifactFile.group, indexedArtifactFile.artifact, indexedArtifactFile.version);
            }
          }));
        } catch(Exception ex) {
          log.error("Can't add plugin to " + file, ex);
        }
      }
    }
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }
}
