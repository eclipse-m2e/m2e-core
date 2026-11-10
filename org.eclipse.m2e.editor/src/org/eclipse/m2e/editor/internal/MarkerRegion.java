/*******************************************************************************
 * Copyright (c) 2008-2026 Sonatype, Inc.
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

package org.eclipse.m2e.editor.internal;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.editor.pom.XMLEditorUtility;


/**
 * A region holding a marker annotation shown in the pom editor's marker hover.
 */
public class MarkerRegion implements IRegion {

  private final MarkerAnnotation ann;

  final int offset;

  final int length;

  public MarkerRegion(int offset, int length, MarkerAnnotation applicable) {
    this.offset = offset;
    this.length = length;
    this.ann = applicable;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  public MarkerAnnotation getAnnotation() {
    return ann;
  }

  public boolean isDefinedInParent() {
    IMarker mark = ann.getMarker();
    String isElsewhere = mark.getAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_PATH, null);
    return isElsewhere != null;
  }

  /**
   * Opens the location in the parent pom where the cause of the marker is defined.
   */
  public void openDefinitionInParent() {
    IMarker marker = ann.getMarker();
    String loc = marker.getAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_PATH, null);
    if(loc != null) {
      IFileStore fileStore = EFS.getLocalFileSystem().getStore(IPath.fromOSString(loc));
      int row = marker.getAttribute(IMavenConstants.MARKER_CAUSE_LINE_NUMBER, 0);
      int column = marker.getAttribute(IMavenConstants.MARKER_CAUSE_COLUMN_START, 0);
      String name = marker.getAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_ID, null);
      XMLEditorUtility.openXmlEditor(fileStore, row, column, name);
    }
  }

}
