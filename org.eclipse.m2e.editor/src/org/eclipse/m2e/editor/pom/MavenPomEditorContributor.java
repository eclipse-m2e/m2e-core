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

package org.eclipse.m2e.editor.pom;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;


/**
 * Manages the installation/deinstallation of global actions for multi-page editors. Responsible for the redirection of
 * global actions to the active editor. Multi-page contributor replaces the contributors for the individual editors in
 * the multi-page editor.
 */
public class MavenPomEditorContributor extends MultiPageEditorActionBarContributor {
  private MavenPomEditor editorPart;

  protected IEditorActionBarContributor sourceViewerActionContributor;

  public MavenPomEditorContributor() {
    sourceViewerActionContributor = new TextEditorActionContributor();
  }

  public void init(IActionBars bars) {
    super.init(bars);
    if(bars != null) {
      sourceViewerActionContributor.init(bars, getPage());
    }
  }

  public void dispose() {
    super.dispose();
    sourceViewerActionContributor.dispose();
  }

  public void setActiveEditor(IEditorPart targetEditor) {
    if(targetEditor instanceof MavenPomEditor) {
      editorPart = (MavenPomEditor) targetEditor;
      setActivePage(editorPart.getActiveEditor());
    }
  }

  public void setActivePage(IEditorPart part) {
    //set the text editor
    IActionBars actionBars = getActionBars();
    if(editorPart != null) {
      if(actionBars != null) {
        actionBars.clearGlobalActionHandlers();

        // undo/redo always enabled
        actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), //
            getAction(ITextEditorActionConstants.UNDO));
        actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), //
            getAction(ITextEditorActionConstants.REDO));

        // all other action, for text editor only (FormPage doesn't provide for these actions...)
        if(part instanceof ITextEditor) {
          actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), //
              getAction(ITextEditorActionConstants.DELETE));
          actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), //
              getAction(ITextEditorActionConstants.CUT));
          actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), //
              getAction(ITextEditorActionConstants.COPY));
          actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), //
              getAction(ITextEditorActionConstants.PASTE));
          actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), //
              getAction(ITextEditorActionConstants.SELECT_ALL));
          actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), //
              getAction(ITextEditorActionConstants.FIND));
          actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), //
              getAction(IDEActionFactory.BOOKMARK.getId()));
        }

        actionBars.updateActionBars();
      }
    }

    if(sourceViewerActionContributor != null && part != null) {
      sourceViewerActionContributor.setActiveEditor(part);
    }

  }

  /**
   * Returns the action registered with the given text editor.
   * 
   * @return IAction or null if editor is null.
   */
  protected IAction getAction(String actionId) {
    if(editorPart != null) {
      try {
        return editorPart.getSourcePage().getAction(actionId);
      } catch(NullPointerException e) {
        //editor has been disposed, ignore
      }
    }
    return null;
  }

}
