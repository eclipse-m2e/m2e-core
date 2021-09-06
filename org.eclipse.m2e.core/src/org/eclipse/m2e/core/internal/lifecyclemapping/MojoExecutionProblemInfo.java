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

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public abstract class MojoExecutionProblemInfo extends MavenProblemInfo {
  private final MojoExecutionKey mojoExecutionKey;

  protected MojoExecutionProblemInfo(String message, MojoExecutionKey mojoExecutionKey, SourceLocation markerLocation) {
    super(message, markerLocation);
    this.mojoExecutionKey = mojoExecutionKey;
  }

  protected MojoExecutionProblemInfo(String message, int severity, MojoExecutionKey mojoExecutionKey,
      SourceLocation markerLocation) {
    super(message, severity, markerLocation);
    this.mojoExecutionKey = mojoExecutionKey;
  }

  @Override
  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);

    //TODO what parameters are important here for the hints?
    marker.setAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, mojoExecutionKey.getGroupId());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, mojoExecutionKey.getArtifactId());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_EXECUTION_ID, mojoExecutionKey.getExecutionId());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_GOAL, mojoExecutionKey.getGoal());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_VERSION, mojoExecutionKey.getVersion());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, mojoExecutionKey.getLifecyclePhase());
  }

}
