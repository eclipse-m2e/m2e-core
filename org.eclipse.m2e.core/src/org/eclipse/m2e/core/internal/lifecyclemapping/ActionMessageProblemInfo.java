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
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class ActionMessageProblemInfo extends MojoExecutionProblemInfo {

  private final boolean pomMapping;

  public ActionMessageProblemInfo(String message, int severity, MojoExecutionKey mojoExecutionKey,
      SourceLocation markerLocation, boolean pomMapping) {
    super(message, severity, mojoExecutionKey, markerLocation);
    this.pomMapping = pomMapping;
  }

  @Override
  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);

    if(!pomMapping) {
      marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT,
          IMavenConstants.EDITOR_HINT_IMPLICIT_LIFECYCLEMAPPING);
    }
  }

}
