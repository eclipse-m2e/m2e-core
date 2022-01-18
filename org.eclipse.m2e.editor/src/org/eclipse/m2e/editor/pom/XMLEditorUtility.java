/*******************************************************************************
 * Copyright (c) 2008-2020 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation (code moved here from PomHyperlinkDetector).
 *
 *******************************************************************************/

package org.eclipse.m2e.editor.pom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;


public class XMLEditorUtility {
  private static final Logger log = LoggerFactory.getLogger(XMLEditorUtility.class);

  public static void openXmlEditor(final IFileStore fileStore) {
    openXmlEditor(fileStore, -1, -1, fileStore.getName());
  }

  public static void openXmlEditor(final IFileStore fileStore, int line, int column, String name) {
    assert fileStore != null;
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if(window != null) {
      IWorkbenchPage page = window.getActivePage();
      if(page != null) {
        try {
          IEditorPart part = IDE.openEditorOnFileStore(page, fileStore);
          reveal(selectEditorPage(part), line, column);
        } catch(PartInitException e) {
          MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
              org.eclipse.m2e.editor.internal.Messages.PomHyperlinkDetector_error_title,
              NLS.bind(org.eclipse.m2e.editor.internal.Messages.PomHyperlinkDetector_error_message, fileStore,
                  e.toString()));

        }
      }
    }
  }

  private static ITextEditor selectEditorPage(IEditorPart part) {
    if(part == null) {
      return null;
    }
    if(part instanceof FormEditor) {
      FormEditor ed = (FormEditor) part;
      ed.setActivePage(null); //null means source, always or just in the case of MavenPomEditor?
      if(ed instanceof MavenPomEditor) {
        return ((MavenPomEditor) ed).getSourcePage();
      }
    }
    return null;
  }

  private static void reveal(ITextEditor editor, int line, int column) {
    if(editor == null || line < 0 || column < 0) {
      return;
    }
    IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
    try {
      int offset = doc.getLineOffset(line - 1);
      editor.selectAndReveal(offset + column - 1, 0);
    } catch(BadLocationException e) {
      log.error("failed selecting part of editor", e);
    }
  }

}
