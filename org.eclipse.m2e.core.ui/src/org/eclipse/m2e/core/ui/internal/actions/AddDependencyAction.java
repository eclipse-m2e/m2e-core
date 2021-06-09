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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.CLASSIFIER;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.SCOPE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.TYPE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childEquals;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;

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


public class AddDependencyAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {
  private static final Logger log = LoggerFactory.getLogger(AddDependencyAction.class);

  public static final String ID = "org.eclipse.m2e.addDependencyAction"; //$NON-NLS-1$

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

    MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchDependencyDialog(getShell(),
        Messages.AddDependencyAction_searchDialog_title, mp, prj, false);
    if(dialog.open() == Window.OK) {
      IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
      if(indexedArtifactFile != null) {
        try {
          final Dependency dependency = indexedArtifactFile.getDependency();
          String selectedScope = dialog.getSelectedScope();
          dependency.setScope(selectedScope);

          if(indexedArtifactFile.version == null) {
            dependency.setVersion(null);
          }
          performOnDOMDocument(new OperationTuple(file, (Operation) document -> {
            Element depsEl = getChild(document.getDocumentElement(), DEPENDENCIES);
            Element dep = findChild(depsEl, DEPENDENCY, childEquals(GROUP_ID, dependency.getGroupId()),
                childEquals(ARTIFACT_ID, dependency.getArtifactId()));
            if(dep == null) {
              dep = PomHelper.createDependency(depsEl, dependency.getGroupId(), dependency.getArtifactId(),
                  dependency.getVersion());
            } else {
              //only set version if already exists
              if(dependency.getVersion() != null) {
                setText(getChild(dep, VERSION), dependency.getVersion());
              }
            }
            if(dependency.getType() != null //
                && !"jar".equals(dependency.getType()) // //$NON-NLS-1$
                && !"null".equals(dependency.getType())) { // guard against MNGECLIPSE-622 //$NON-NLS-1$

              setText(getChild(dep, TYPE), dependency.getType());
            }

            if(dependency.getClassifier() != null) {
              setText(getChild(dep, CLASSIFIER), dependency.getClassifier());
            }

            if(dependency.getScope() != null && !"compile".equals(dependency.getScope())) { //$NON-NLS-1$
              setText(getChild(dep, SCOPE), dependency.getScope());
            }

          }));
        } catch(Exception ex) {
          String msg = NLS.bind(Messages.AddDependencyAction_error_msg, file);
          log.error(msg, ex);
          MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.AddDependencyAction_error_title, msg);
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
