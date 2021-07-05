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

import java.io.IOException;
import java.util.List;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.utils.StringUtils;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.markers.EditorAwareMavenProblemResolution;
import org.eclipse.m2e.core.ui.internal.util.XmlUtils;


@SuppressWarnings("restriction")
public abstract class AbstractPomProblemResolution extends EditorAwareMavenProblemResolution {

  protected static final String PROJECT_NODE = "project"; //$NON-NLS-1$

  protected static final String GROUP_ID_NODE = "groupId"; //$NON-NLS-1$

  protected static final String ARTIFACT_ID_NODE = "artifactId"; //$NON-NLS-1$

  protected static final String VERSION_NODE = "version"; //$NON-NLS-1$

  protected AbstractPomProblemResolution(IMarker marker) {
    super(marker);
  }

  protected abstract boolean canFix(String editorHint);

  protected abstract void processFix(IStructuredDocument doc, Element root, List<IMarker> markers);

  @Override
  public final boolean canFix(IMarker marker) {
    String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
    return hint != null && canFix(hint);
  }

  @Override
  protected final void fix(IDocument document, List<IMarker> markers, IProgressMonitor monitor) {
    XmlUtils.performOnRootElement(document, (node, structured) -> processFix(structured, node, markers));
  }

  @Override
  protected final void fix(IResource resource, List<IMarker> markers, IProgressMonitor monitor) {
    try {
      XmlUtils.performOnRootElement((IFile) resource, (node, structured) -> processFix(structured, node, markers),
          true);
    } catch(IOException | CoreException e) {
      LOG.error("Error processing marker", e);
    }
  }

  static String previewForRemovedElement(IDocument doc, Element removed) {
    if(removed instanceof IndexedRegion) {
      IndexedRegion reg = (IndexedRegion) removed;
      try {
        int maxl = doc.getNumberOfLines() - 1;

        int line = doc.getLineOfOffset(reg.getStartOffset());
        int startLine = doc.getLineOffset(line);
        int prev2Start = doc.getLineOffset(Math.max(line - 2, 0));

//        String currentLine = doc.get(startLine, doc.getLineLength(line));
        int next2Start = doc.getLineOffset(Math.min(line + 1, maxl));
        int next2End = doc.getLineOffset(Math.min(line + 3, maxl));

        String prevString = doc.get(prev2Start, startLine - prev2Start);
        String nextString = next2Start < next2End ? doc.get(next2Start, next2End - next2Start) : "";
        // tabs in html make the DefaultInformationControl go mad
        prevString = prevString.replace("\t", "  "); //$NON-NLS-1$ //$NON-NLS-2$
        nextString = nextString.replace("\t", "  "); //$NON-NLS-1$ //$NON-NLS-2$
        prevString = StringUtils.convertToHTMLContent(prevString);
        nextString = StringUtils.convertToHTMLContent(nextString);
        return "<html>...<br>" + prevString + /** "<del>" + currentLine + "</del>" + */
            nextString + "...<html>"; //$NON-NLS-1$
      } catch(BadLocationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return null;
  }
}
