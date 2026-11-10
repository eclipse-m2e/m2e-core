/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.apt.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import org.eclipse.m2e.apt.internal.IMavenAptConstants;


public class WorkspaceProcessorMarkerResolutionGenerator
    implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

  @Override
  public boolean hasResolutions(IMarker marker) {
    return canResolve(marker);
  }

  @Override
  public IMarkerResolution[] getResolutions(IMarker marker) {
    if(canResolve(marker)) {
      return new IMarkerResolution[] {new WorkspaceProcessorInstallResolution(marker)};
    }
    return new IMarkerResolution[0];
  }

  private static boolean canResolve(IMarker marker) {
    return marker.exists()
        && marker.getAttribute(IMavenAptConstants.WORKSPACE_PROCESSOR_ARTIFACT_ATTRIBUTE, null) != null
        && marker.getAttribute(IMavenAptConstants.WORKSPACE_PROCESSOR_PROJECT_ATTRIBUTE, null) != null;
  }
}
