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
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import org.eclipse.m2e.core.ui.internal.util.XmlUtils;
import org.eclipse.m2e.editor.internal.Messages;


@SuppressWarnings("restriction")
public class IdPartRemovalResolution extends AbstractPomProblemResolution {

  final boolean isVersion;

  public IdPartRemovalResolution(IMarker marker, boolean version) {
    super(marker);
    isVersion = version;
  }

  @Override
  public int getOrder() {
    return 100;
  }

  @Override
  protected boolean canFix(String editorHint) {
    return isVersion ? MavenMarkerResolutionGenerator.isUnneededParentVersion(editorHint)
        : MavenMarkerResolutionGenerator.isUnneededParentGroupId(editorHint);
  }

  @Override
  public String getLabel() {
    return isVersion ? Messages.PomQuickAssistProcessor_title_version : Messages.PomQuickAssistProcessor_title_groupId;
  }

  @Override
  public Image getImage() {
    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
  }

  @Override
  public String getDescription() {
    if(getQuickAssistContext() != null) {
      final IDocument doc = getQuickAssistContext().getSourceViewer().getDocument();
      //oh, how do I miss scala here..
      final String[] toRet = new String[1];
      XmlUtils.performOnRootElement(doc, (root, structured) -> {
        //now check parent version and groupid against the current project's ones..
        if(PROJECT_NODE.equals(root.getNodeName())) {
          Element value = XmlUtils.findChild(root, isVersion ? VERSION_NODE : GROUP_ID_NODE);
          toRet[0] = previewForRemovedElement(doc, value);
        }
      });
      if(toRet[0] != null) {
        return toRet[0];
      }
    }

    return Messages.PomQuickAssistProcessor_remove_hint;
  }

  @Override
  protected void processFix(IStructuredDocument doc, Element root, List<IMarker> markers) {
    //now check parent version and groupid against the current project's ones..
    if(PROJECT_NODE.equals(root.getNodeName())) {
      Element value = XmlUtils.findChild(root, isVersion ? VERSION_NODE : GROUP_ID_NODE);
      if(value instanceof IndexedRegion) {
        IndexedRegion off = (IndexedRegion) value;

        int offset = off.getStartOffset();
        if(offset <= 0) {
          return;
        }
        Node prev = value.getNextSibling();
        if(prev instanceof Text) {
          //check the content as well??
          off = ((IndexedRegion) prev);
        }
        DeleteEdit edit = new DeleteEdit(offset, off.getEndOffset() - offset);
        try {
          edit.apply(doc);
          for(IMarker m : markers) {
            m.delete();
          }
        } catch(Exception e) {
          LOG.error("Unable to remove the element", e); //$NON-NLS-1$
        }
      }
    }
  }
}
