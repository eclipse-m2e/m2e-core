/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;


public class MavenDiscoveryMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

  public boolean hasResolutions(IMarker marker) {
    return canResolve(marker);
  }

  public IMarkerResolution[] getResolutions(IMarker marker) {
    if(canResolve(marker)) {
      return new IMarkerResolution[] {new DiscoveryWizardProposal(marker)};
    }
    return new IMarkerResolution[0];
  }

  public static boolean canResolve(IMarker marker) {
    String type = marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null);
    return IMavenConstants.EDITOR_HINT_UNKNOWN_PACKAGING.equals(type)
        || IMavenConstants.EDITOR_HINT_MISSING_CONFIGURATOR.equals(type)
        || IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION.equals(type)
        || IMavenConstants.EDITOR_HINT_UNKNOWN_LIFECYCLE_ID.equals(type);
  }
}
