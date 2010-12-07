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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;


public class AddPluginAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {

  public static final String ID = "org.eclipse.m2e.addPluginAction"; //$NON-NLS-1$

  public void run(IAction action) {
    IFile file = getPomFileFromPomEditorOrViewSelection();

    if(file == null) {
      return;
    }
    //TODO attempts to populate the managed keys here, but works on reliably as facade.getMavenProject can be null
    //depending on the user's preferences and previous IDE interactions.
    IProject prj = file.getProject();
    Set<ArtifactKey> managedKeys = new HashSet<ArtifactKey>();
    if (prj != null && IMavenConstants.POM_FILE_NAME.equals(file.getProjectRelativePath().toString())) {
        IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getProject(prj);
        if (facade != null) {
          MavenProject mp = facade.getMavenProject();
          if (mp != null) {
            PluginManagement pm = mp.getPluginManagement();
            if (pm != null && pm.getPlugins() != null) {
              for (Plugin plug : pm.getPlugins()) {
                managedKeys.add(new ArtifactKey(plug.getGroupId(), plug.getArtifactId(), plug.getVersion(), null));
              }
            }
          }
        }
    }
    

    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), Messages.AddPluginAction_searchDialog_title, 
        IIndex.SEARCH_PLUGIN, Collections.<ArtifactKey> emptySet(), managedKeys);
    if(dialog.open() == Window.OK) {
      final IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
      if(indexedArtifactFile != null) {
        try {
          ArtifactKey key = new ArtifactKey(indexedArtifactFile.group, indexedArtifactFile.artifact, indexedArtifactFile.version, null);
          boolean isManaged = managedKeys.contains(key);
          MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();
          modelManager.updateProject(file, new MavenModelManager.PluginAdder( //
              indexedArtifactFile.group, //
              indexedArtifactFile.artifact, //
              isManaged ? null : indexedArtifactFile.version));
        } catch(Exception ex) {
          MavenLogger.log("Can't add plugin to " + file, ex);
        }
      }
    }
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }
}
