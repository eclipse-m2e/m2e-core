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

package org.eclipse.m2e.core.actions;

import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;


public class AddPluginAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {

  public static final String ID = "org.eclipse.m2e.addPluginAction"; //$NON-NLS-1$

  public void run(IAction action) {
    IFile file = getPomFileFromPomEditorOrViewSelection();

    if(file == null) {
      return;
    }

    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), Messages.AddPluginAction_searchDialog_title, IIndex.SEARCH_PLUGIN, Collections.<ArtifactKey> emptySet());
    if(dialog.open() == Window.OK) {
      final IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
      if(indexedArtifactFile != null) {
        try {
          MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();
          modelManager.updateProject(file, new MavenModelManager.PluginAdder( //
              indexedArtifactFile.group, //
              indexedArtifactFile.artifact, //
              indexedArtifactFile.version));
        } catch(Exception ex) {
          MavenLogger.log("Can't add dependency to " + file, ex);
        }
      }
    }
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }
}
