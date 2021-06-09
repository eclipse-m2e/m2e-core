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
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import org.eclipse.m2e.editor.internal.Messages;


@SuppressWarnings("restriction")
public class SchemaCompletionResolution extends AbstractPomProblemResolution {

  public static final String XSI_VALUE = " xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" //$NON-NLS-1$
      + "    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\""; //$NON-NLS-1$

  public SchemaCompletionResolution(IMarker marker) {
    super(marker);
  }

  @Override
  protected boolean canFix(String editorHint) {
    return MavenMarkerResolutionGenerator.isMissingSchema(editorHint);
  }

  @Override
  public String getLabel() {
    return Messages.PomQuickAssistProcessor_name;
  }

  @Override
  public Image getImage() {
    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD);
  }

  @Override
  public String getDescription() {
    return "<html>...<br>&lt;project <b>" + XSI_VALUE + "</b>&gt;<br>...</html>"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  protected void processFix(IStructuredDocument doc, Element root, List<IMarker> markers) {
    for(IMarker marker : markers) {
      if(PROJECT_NODE.equals(root.getNodeName()) && root instanceof IndexedRegion) {
        IndexedRegion off = (IndexedRegion) root;

        int offset = off.getStartOffset() + PROJECT_NODE.length() + 1;
        if(offset <= 0) {
          return;
        }
        InsertEdit edit = new InsertEdit(offset, XSI_VALUE);
        try {
          edit.apply(doc);
          marker.delete();
        } catch(Exception e) {
          LOG.error("Unable to insert schema info", e); //$NON-NLS-1$
        }
      }
    }
  }
}
