/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.lifecyclemapping;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;


public class MissingLifecycleExtensionPoint extends MavenProblemInfo {
  private final String lifecycleMappingId;

  MissingLifecycleExtensionPoint(String lifecycleMappingId, SourceLocation markerLocation) {
    super(NLS.bind(Messages.LifecycleMappingNotAvailable, lifecycleMappingId), markerLocation);
    this.lifecycleMappingId = lifecycleMappingId;
  }

  public String getLifecycleId() {
    return lifecycleMappingId;
  }

  @Override
  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);
    marker.setAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, getLifecycleId());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_UNKNOWN_LIFECYCLE_ID);
  }
}
