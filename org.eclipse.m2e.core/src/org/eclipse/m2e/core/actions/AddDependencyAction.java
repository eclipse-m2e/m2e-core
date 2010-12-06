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

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;


public class AddDependencyAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {

  public static final String ID = "org.eclipse.m2e.addDependencyAction"; //$NON-NLS-1$

  public void run(IAction action) {
    IFile file = getPomFileFromPomEditorOrViewSelection();

    if(file == null) {
      return;
    }

    MavenPlugin plugin = MavenPlugin.getDefault();

    Set<ArtifactKey> artifacts = getArtifacts(file, plugin);
    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), Messages.AddDependencyAction_searchDialog_title, IIndex.SEARCH_ARTIFACT, artifacts, true);
    if(dialog.open() == Window.OK) {
      IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
      if(indexedArtifactFile != null) {
        try {
          MavenModelManager modelManager = plugin.getMavenModelManager();
          Dependency dependency = indexedArtifactFile.getDependency();
          String selectedScope = dialog.getSelectedScope();
          dependency.setScope(selectedScope);
          modelManager.addDependency(file, dependency);
        } catch(Exception ex) {
          String msg = NLS.bind(Messages.AddDependencyAction_error_msg, file);
          MavenLogger.log(msg, ex);
          MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.AddDependencyAction_error_title, msg);
        }
      }
    }
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }
}
