/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;


abstract class AbstractLifecycleMappingProposal extends WorkbenchMarkerResolution {
  private static final Logger log = LoggerFactory.getLogger(AbstractLifecycleMappingProposal.class);

  protected final IMarker marker;

  protected final PluginExecutionAction action;

  protected AbstractLifecycleMappingProposal(IMarker marker, PluginExecutionAction action) {
    this.marker = marker;
    this.action = action;
  }

  public Image getImage() {
    return PluginExecutionAction.ignore.equals(action) ? PlatformUI.getWorkbench().getSharedImages()
        .getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_DELETE) : PlatformUI.getWorkbench().getSharedImages()
        .getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_FORWARD);
  }

  public String getLabel() {
    return getDisplayString();
  }

  public String getDescription() {
    return getDisplayString();
  }

  @Override
  public IMarker[] findOtherMarkers(IMarker[] markers) {
    List<IMarker> toRet = new ArrayList<IMarker>();

    for(IMarker mark : markers) {
      if(mark == this.marker) {
        continue;
      }
      try {
        if(mark.getType().equals(this.marker.getType()) && mark.getResource().equals(this.marker.getResource())) {
          toRet.add(mark);
        }
      } catch(CoreException e) {
        log.error(e.getMessage(), e);
      }
    }
    return toRet.toArray(new IMarker[0]);
  }

  public void run(final IMarker marker) {
    run(new IMarker[] {marker}, new NullProgressMonitor());
  }

  @Override
  public abstract void run(IMarker[] markers, IProgressMonitor monitor);

  public abstract String getDisplayString();
}
