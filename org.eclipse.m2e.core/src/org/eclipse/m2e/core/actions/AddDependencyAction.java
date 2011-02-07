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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
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


public class AddDependencyAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {

    public static final String ID = "org.eclipse.m2e.addDependencyAction"; //$NON-NLS-1$

    public void run(IAction action) {
      IFile file = getPomFileFromPomEditorOrViewSelection();

      if(file == null) {
        return;
      }

      MavenPlugin plugin = MavenPlugin.getDefault();

      Set<ArtifactKey> artifacts = getArtifacts(file, plugin);
      Set<ArtifactKey> managedKeys = populateManagedArtifactKeys(file);
      
      MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), Messages.AddDependencyAction_searchDialog_title, IIndex.SEARCH_ARTIFACT, artifacts, managedKeys, true);
      if(dialog.open() == Window.OK) {
        IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
        if(indexedArtifactFile != null) {
          try {
            MavenModelManager modelManager = plugin.getMavenModelManager();
            Dependency dependency = indexedArtifactFile.getDependency();
            String selectedScope = dialog.getSelectedScope();
            dependency.setScope(selectedScope);
            
            ArtifactKey key = new ArtifactKey(indexedArtifactFile.group, indexedArtifactFile.artifact, indexedArtifactFile.version, null);
            boolean isManaged = managedKeys.contains(key);
            if (isManaged) {
              dependency.setVersion(null);
            }
            
            modelManager.addDependency(file, dependency);
          } catch(Exception ex) {
            String msg = NLS.bind(Messages.AddDependencyAction_error_msg, file);
            MavenLogger.log(msg, ex);
            MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.AddDependencyAction_error_title, msg);
          }
        }
      }
    }
    
    /**
     * @param file
     * @return
     */
    public static Set<ArtifactKey> populateManagedArtifactKeys(IFile file) {
      //TODO attempts to populate the managed keys here, but works not reliably as facade.getMavenProject can be null
      //depending on the user's preferences and previous IDE interactions.
      IProject prj = file.getProject();
      Set<ArtifactKey> managedKeys = new HashSet<ArtifactKey>();
      if (prj != null && IMavenConstants.POM_FILE_NAME.equals(file.getProjectRelativePath().toString())) {
          IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getProject(prj);
          if (facade != null) {
            MavenProject mp = facade.getMavenProject();
            if (mp != null) {
              DependencyManagement dm = mp.getDependencyManagement();
              if (dm != null && dm.getDependencies() != null) {
                for (Dependency dep : dm.getDependencies()) {
                  managedKeys.add(new ArtifactKey(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getClassifier()));
                }
              }
            }
          }
      }
      return managedKeys;
    }

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
    }
  }