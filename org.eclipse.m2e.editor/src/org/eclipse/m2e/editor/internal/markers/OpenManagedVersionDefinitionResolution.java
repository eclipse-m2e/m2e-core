/*******************************************************************************
 * Copyright (c) 2020 Till Brychcy and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Till Brychcy - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.internal.markers;

import java.net.URI;
import java.util.List;

import org.w3c.dom.Element;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import org.eclipse.m2e.core.ui.internal.markers.MarkerLocationService;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.XMLEditorUtility;


@SuppressWarnings("restriction")
public class OpenManagedVersionDefinitionResolution extends AbstractPomProblemResolution {

  public OpenManagedVersionDefinitionResolution(IMarker marker) {
    super(marker);
  }

  @Override
  public int getOrder() {
    return 90;
  }

  @Override
  public boolean canFix(String editorHint) {
    try {
      return getMarker().getAttribute(MarkerLocationService.ATTR_MANAGED_VERSION_LOCATION) != null;
    } catch(CoreException ex) {
      return false;
    }
  }

  @Override
  public String getLabel() {
    return Messages.MavenMarkerResolution_openManaged_label;
  }

  @Override
  public Image getImage() {
    return PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OPEN_MARKER);
  }

  @Override
  public String getDescription() {
    try {
      String locationURIString = (String) getMarker().getAttribute("managedVersionLocation");
      return NLS.bind(Messages.MavenMarkerResolution_openManaged_description, locationURIString);
    } catch(CoreException ex) {
      // ignore
    }
    return null;
  }

  @Override
  protected void processFix(IStructuredDocument doc, Element root, List<IMarker> markers) {
    try {
      String locationURIString = (String) getMarker().getAttribute("managedVersionLocation");
      if(locationURIString != null) {
        IFileStore fileStore = EFS.getLocalFileSystem().getStore(new URI(locationURIString));
        int lineNumber = getMarker().getAttribute("managedVersionLine", -1);
        int columnNumber = Math.max(1, getMarker().getAttribute("managedVersionColumn", -1));
        XMLEditorUtility.openXmlEditor(fileStore, lineNumber, columnNumber, fileStore.getName());
      }
    } catch(Exception ex) {
      // ignore
    }
  }
}
