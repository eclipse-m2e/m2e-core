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

package org.eclipse.m2e.editor.xml.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.wst.sse.ui.internal.StructuredResourceMarkerAnnotationModel;

import org.eclipse.m2e.core.core.IMavenConstants;

/**
 * created this file to get the proper lightbulb icon for the warnings with hint
 * @author mkleint
 */
public class POMMarkerAnnotationModel extends StructuredResourceMarkerAnnotationModel {

  public POMMarkerAnnotationModel(IResource resource) {
    super(resource);
  }

  public POMMarkerAnnotationModel(IResource resource, String secondaryID) {
    super(resource, secondaryID);
  }

  @Override
  protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
    String hint = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
    if(hint != null) {
      MarkerAnnotation ann = new MarkerAnnotation(marker);
      ann.setQuickFixable(true);
      return ann;
    }
    return super.createMarkerAnnotation(marker);
  }
}
