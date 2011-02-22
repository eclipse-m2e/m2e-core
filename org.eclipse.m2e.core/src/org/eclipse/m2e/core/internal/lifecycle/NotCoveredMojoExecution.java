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
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class NotCoveredMojoExecution extends MavenProblemInfo {
  
  private final MojoExecutionKey mojoExecutionKey;

  public NotCoveredMojoExecution(MojoExecutionKey mojoExecutionKey, SourceLocation markerLocation) {
    super(NLS.bind(Messages.LifecycleConfigurationPluginExecutionNotCovered, mojoExecutionKey.toString()),
        markerLocation);
    this.mojoExecutionKey = mojoExecutionKey;
  }

  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);
    marker
        .setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION);
    //TODO what parameters are important here for the hints?
    marker.setAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, mojoExecutionKey.getGroupId());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, mojoExecutionKey.getArtifactId());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_EXECUTION_ID, mojoExecutionKey.getExecutionId());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_GOAL, mojoExecutionKey.getGoal());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_VERSION, mojoExecutionKey.getVersion());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, mojoExecutionKey.getLifecyclePhase());
  }
}

