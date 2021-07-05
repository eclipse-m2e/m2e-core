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
public class ManagedVersionRemovalResolution extends AbstractPomProblemResolution {

  final boolean isDependency;

  public ManagedVersionRemovalResolution(IMarker marker, boolean dependency) {
    super(marker);
    isDependency = dependency;
  }

  @Override
  public int getOrder() {
    return 100;
  }

  @Override
  public boolean canFix(String editorHint) {
    return isDependency ? MavenMarkerResolutionGenerator.isDependencyVersionOverride(editorHint)
        : MavenMarkerResolutionGenerator.isPluginVersionOverride(editorHint);
  }

  @Override
  public String getLabel() {
    return Messages.PomQuickAssistProcessor_title_version;
  }

  @Override
  public Image getImage() {
    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
  }

  @Override
  public String getDescription() {
    if(getQuickAssistContext() != null) {
      final IDocument doc = getQuickAssistContext().getSourceViewer().getDocument();
      final String[] toRet = new String[1];
      XmlUtils.performOnRootElement(doc, (node, structured) -> {
        Element artifact = findArtifactElement(node, getMarker());
        if(artifact != null) {
          Element value = XmlUtils.findChild(artifact, VERSION_NODE);
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
    if(PROJECT_NODE.equals(root.getNodeName())) {
      for(IMarker marker : markers) {
        Element artifact = findArtifactElement(root, marker);
        if(artifact == null) {
          //TODO report somehow?
          LOG.error("Unable to find the marked element"); //$NON-NLS-1$
          continue;
        }
        Element value = XmlUtils.findChild(artifact, VERSION_NODE);
        if(value instanceof IndexedRegion) {
          IndexedRegion off = (IndexedRegion) value;

          int offset = off.getStartOffset();
          if(offset <= 0) {
            continue;
          }
          Node prev = value.getNextSibling();
          if(prev instanceof Text) {
            //check the content as well??
            off = ((IndexedRegion) prev);
          }
          DeleteEdit edit = new DeleteEdit(offset, off.getEndOffset() - offset);
          try {
            edit.apply(doc);
            marker.delete();
          } catch(Exception e) {
            LOG.error("Unable to remove the element", e); //$NON-NLS-1$
          }
        }
      }
    }
  }

  Element findArtifactElement(Element root, IMarker marker) {
    if(root == null) {
      return null;
    }
    String groupId = marker.getAttribute("groupId", null);
    String artifactId = marker.getAttribute("artifactId", null);
    assert groupId != null;
    assert artifactId != null;

    String profile = marker.getAttribute("profile", null);
    Element artifactParent = root;
    if(profile != null) {
      Element profileRoot = XmlUtils.findChild(root, "profiles");
      if(profileRoot != null) {
        for(Element prf : XmlUtils.findChilds(profileRoot, "profile")) {
          if(profile.equals(XmlUtils.getTextValue(XmlUtils.findChild(prf, "id")))) {
            artifactParent = prf;
            break;
          }
        }
      }
    }
    if(!isDependency) {
      //we have plugins now, need to go one level down to build
      artifactParent = XmlUtils.findChild(artifactParent, "build");
    }
    if(artifactParent == null) {
      return null;
    }
    Element list = XmlUtils.findChild(artifactParent, isDependency ? "dependencies" : "plugins");
    if(list == null) {
      return null;
    }
    Element artifact = null;
    for(Element art : XmlUtils.findChilds(list, isDependency ? "dependency" : "plugin")) {
      String grpString = XmlUtils.getTextValue(XmlUtils.findChild(art, GROUP_ID_NODE));
      String artString = XmlUtils.getTextValue(XmlUtils.findChild(art, ARTIFACT_ID_NODE));
      if(groupId.equals(grpString) && artifactId.equals(artString)) {
        artifact = art;
        break;
      }
    }
    return artifact;
  }
}
