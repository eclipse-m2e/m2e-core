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

package org.eclipse.m2e.editor.xml.internal.lifecycle;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.ui.internal.markers.EditorAwareMavenProblemResolution;


abstract class AbstractLifecycleMappingResolution extends EditorAwareMavenProblemResolution {

  protected final PluginExecutionAction action;

  protected AbstractLifecycleMappingResolution(IMarker marker, PluginExecutionAction action) {
    super(marker);
    this.action = action;
  }

  public Image getImage() {
    return PluginExecutionAction.ignore.equals(action)
        ? PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_DELETE)
        : PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_TOOL_FORWARD);
  }

  public boolean canFix(IMarker marker) throws CoreException {
    return marker.getType().equals(getMarker().getType()) && marker.getResource().equals(getMarker().getResource());
  }

}
