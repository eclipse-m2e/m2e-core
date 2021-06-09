/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.editor.internal.markers;

import java.util.List;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.eclipse.m2e.editor.MavenEditorImages;


@SuppressWarnings("restriction")
public class IgnoreWarningResolution extends AbstractPomProblemResolution {

  private final String markupText;

  public IgnoreWarningResolution(IMarker marker, String markupText) {
    super(marker);
    this.markupText = markupText;
  }

  @Override
  public int getOrder() {
    return 200;
  }

  @Override
  public boolean canFix(String editorHint) {
    return MavenMarkerResolutionGenerator.isDependencyVersionOverride(editorHint)
        || MavenMarkerResolutionGenerator.isPluginVersionOverride(editorHint);
  }

  @Override
  public String getLabel() {
    return "Ignore this warning";
  }

  @Override
  public Image getImage() {
    return MavenEditorImages.IMG_CLOSE;
  }

  @Override
  public String getDescription() {
    if(getQuickAssistContext() != null) {
      IDOMModel domModel = null;
      try {
        IDocument doc = getQuickAssistContext().getSourceViewer().getDocument();
        domModel = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(doc);
        //the offset of context is important here, not the offset of the marker!!!
        //line/offset of marker only gets updated hen file gets saved.
        //we need the proper handling also for unsaved documents..
        int line = doc.getLineOfOffset(getQuickAssistContext().getOffset());
        int linestart = doc.getLineOffset(line);
        int lineend = linestart + doc.getLineLength(line);
        int start = linestart;
        IndexedRegion reg = domModel.getIndexedRegion(start);
        while(reg != null && !(reg instanceof Element) && start < lineend) {
          reg = domModel.getIndexedRegion(reg.getEndOffset() + 1);
          if(reg != null) {
            start = reg.getStartOffset();
          }
        }
        if(reg instanceof Element) { //just a simple guard against moved marker
          String currentLine = StringUtils
              .convertToHTMLContent(doc.get(reg.getStartOffset(), reg.getEndOffset() - reg.getStartOffset()));
          String insert = StringUtils.convertToHTMLContent("<!--" + markupText + "-->");
          return "<html>...<br>" + currentLine + "<b>" + insert + "</b><br>...<html>"; //$NON-NLS-1$ //$NON-NLS-2$
        }
      } catch(BadLocationException e1) {
        LOG.error("Error while computing completion proposal", e1);
      } finally {
        if(domModel != null) {
          domModel.releaseFromRead();
        }
      }
    }
    return "Adds comment markup next to the affected element. No longer shows the warning afterwards";
  }

  @Override
  protected void processFix(IStructuredDocument doc, Element root, List<IMarker> markers) {
    for(IMarker marker : markers) {
      IDOMModel domModel = null;
      try {
        domModel = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(doc);
        int line;
        if(getQuickAssistContext() != null) {
          line = doc.getLineOfOffset(getQuickAssistContext().getOffset());
        } else {
          line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
          assert line != -1;
          line = line - 1;
        }
        try {
          int linestart = doc.getLineOffset(line);
          int lineend = linestart + doc.getLineLength(line);
          int start = linestart;
          IndexedRegion reg = domModel.getIndexedRegion(start);
          while(reg != null && !(reg instanceof Element) && start < lineend) {
            reg = domModel.getIndexedRegion(reg.getEndOffset() + 1);
            if(reg != null) {
              start = reg.getStartOffset();
            }
          }
          if(reg instanceof Element) {
            InsertEdit edit = new InsertEdit(reg.getEndOffset(), "<!--" + markupText + "-->");
            try {
              edit.apply(doc);
              marker.delete();
            } catch(Exception e) {
              LOG.error("Unable to insert", e); //$NON-NLS-1$
            }
          }
        } catch(BadLocationException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      } finally {
        if(domModel != null) {
          domModel.releaseFromRead();
        }
      }
    }
  }
}
