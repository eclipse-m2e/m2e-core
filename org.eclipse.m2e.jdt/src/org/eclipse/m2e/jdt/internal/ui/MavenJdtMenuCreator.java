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

package org.eclipse.m2e.jdt.internal.ui;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.AbstractMavenMenuCreator;
import org.eclipse.m2e.core.actions.MaterializeAction;
import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.actions.OpenUrlAction;
import org.eclipse.m2e.core.actions.SelectionUtil;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.jdt.internal.MavenJdtImages;
import org.eclipse.m2e.jdt.internal.Messages;
import org.eclipse.m2e.jdt.internal.actions.DownloadSourcesAction;
import org.eclipse.m2e.jdt.internal.actions.OpenJavaDocAction;


/**
 * Maven menu creator for JDT
 * 
 * @author Eugene Kuleshov
 */
public class MavenJdtMenuCreator extends AbstractMavenMenuCreator {

  private static final String ID_SOURCES = "org.eclipse.m2e.downloadSourcesAction"; //$NON-NLS-1$

  private static final String ID_JAVADOC = "org.eclipse.m2e.downloadJavaDocAction"; //$NON-NLS-1$

  /* (non-Javadoc)
   * @see org.eclipse.m2e.internal.actions.AbstractMavenMenuCreator#createMenu(org.eclipse.jface.action.MenuManager)
   */
  public void createMenu(IMenuManager mgr) {
    int selectionType = SelectionUtil.getSelectionType(selection);
    if(selectionType == SelectionUtil.UNSUPPORTED) {
      return;
    }

    if(selectionType == SelectionUtil.PROJECT_WITH_NATURE) {
      mgr.appendToGroup(UPDATE, getAction(new DownloadSourcesAction(ID_JAVADOC), //
          DownloadSourcesAction.ID_JAVADOC, Messages.MavenJdtMenuCreator_action_javadoc));
      mgr.appendToGroup(UPDATE, getAction(new DownloadSourcesAction(ID_SOURCES), // 
          DownloadSourcesAction.ID_SOURCES, Messages.MavenJdtMenuCreator_action_sources));
    }

    if(selectionType == SelectionUtil.JAR_FILE) {
      boolean isProject = false;
      if(selection.size() == 1) {
        ArtifactKey key = SelectionUtil.getType(selection.getFirstElement(), ArtifactKey.class);
        if(key != null) {
          MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
          IMavenProjectFacade mavenProject = null;
          mavenProject = projectManager.getMavenProject( //
              key.getGroupId(), key.getArtifactId(), key.getVersion());
          if(mavenProject!=null) {
            isProject = true;
          }
        }
      }
      
      if(!isProject) {
        mgr.appendToGroup(UPDATE, getAction(new DownloadSourcesAction(ID_SOURCES), //
            DownloadSourcesAction.ID_SOURCES, Messages.MavenJdtMenuCreator_action_sources));
        mgr.appendToGroup(UPDATE, getAction(new DownloadSourcesAction(ID_JAVADOC), //
            DownloadSourcesAction.ID_JAVADOC, Messages.MavenJdtMenuCreator_action_javadoc));
        mgr.prependToGroup(OPEN, new Separator());
      }

      mgr.appendToGroup(OPEN, getAction(new OpenPomAction(), OpenPomAction.ID, Messages.MavenJdtMenuCreator_action_openPom));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_PROJECT), //
          OpenUrlAction.ID_PROJECT, Messages.MavenJdtMenuCreator_action_openProject));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_ISSUES), //
          OpenUrlAction.ID_ISSUES, Messages.MavenJdtMenuCreator_action_open_issue));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_SCM), // 
          OpenUrlAction.ID_SCM, Messages.MavenJdtMenuCreator_axtion_openScm));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_CI), //
          OpenUrlAction.ID_CI, Messages.MavenJdtMenuCreator_action_openCI));
      mgr.appendToGroup(OPEN, getAction(new OpenJavaDocAction(), //
          OpenJavaDocAction.ID, Messages.MavenJdtMenuCreator_action_openJavadoc, MavenJdtImages.JAVA_DOC));

      if(!isProject) {
        mgr.prependToGroup(IMPORT, new Separator());
        mgr.appendToGroup(IMPORT, getAction(new MaterializeAction(), //
            MaterializeAction.ID, //
            selection.size() == 1 ? Messages.MavenJdtMenuCreator_action_materialize1 : Messages.MavenJdtMenuCreator_action_materializeMany, "icons/import_m2_project.gif")); //$NON-NLS-3$
      }
    }
    
    if(selectionType == SelectionUtil.WORKING_SET) {
      mgr.appendToGroup(UPDATE, getAction(new DownloadSourcesAction(ID_SOURCES), //
          DownloadSourcesAction.ID_SOURCES, Messages.MavenJdtMenuCreator_action_downloadSources));
      mgr.appendToGroup(UPDATE, getAction(new DownloadSourcesAction(ID_JAVADOC), //
          DownloadSourcesAction.ID_JAVADOC, Messages.MavenJdtMenuCreator_action_downloadJavadoc));
    }
  }

}
