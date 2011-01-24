/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.internal.lifecycle;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.project.MarkerUtils;
import org.eclipse.m2e.core.project.configurator.LifecycleMappingProblemInfo;

public class MissingLifecycleExtensionPoint extends LifecycleMappingProblemInfo {
  private final String lifecycleId;

  MissingLifecycleExtensionPoint(int line, String message, String lifecycleId) {
    super(line, message);
    this.lifecycleId = lifecycleId;
  }

  public String getLifecycleId() {
    return lifecycleId;
  }

  @Override
  public void processMarker(IMarker marker) throws CoreException {
    MarkerUtils.decorateMarker(marker);
    marker.setAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, getLifecycleId());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_UNKNOWN_LIFECYCLE_ID);
  }
}