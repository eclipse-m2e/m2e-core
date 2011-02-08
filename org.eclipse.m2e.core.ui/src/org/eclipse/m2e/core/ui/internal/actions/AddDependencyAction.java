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

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;


public class AddDependencyAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {

    public static final String ID = "org.eclipse.m2e.addDependencyAction"; //$NON-NLS-1$

    public void run(IAction action) {
      IFile file = getPomFileFromPomEditorOrViewSelection();

      if(file == null) {
        return;
      }

      MavenPlugin plugin = MavenPlugin.getDefault();
      MavenProject mp = null;
      IProject prj = file.getProject();
      if (prj != null && IMavenConstants.POM_FILE_NAME.equals(file.getProjectRelativePath().toString())) {
          IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getProject(prj);
          if (facade != null) {
            mp = facade.getMavenProject();
          }
      }
      
      MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchDependencyDialog(getShell(), Messages.AddDependencyAction_searchDialog_title, mp, prj, false);
      if(dialog.open() == Window.OK) {
        IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
        if(indexedArtifactFile != null) {
          try {
            MavenModelManager modelManager = plugin.getMavenModelManager();
            Dependency dependency = indexedArtifactFile.getDependency();
            String selectedScope = dialog.getSelectedScope();
            dependency.setScope(selectedScope);
            
            if (indexedArtifactFile.version == null) {
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
    
    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
    }
  }
