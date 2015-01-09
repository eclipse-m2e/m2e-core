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

package org.eclipse.m2e.core.internal.lifecyclemapping;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class MissingConfiguratorProblemInfo extends MojoExecutionProblemInfo {
  private final String configuratorId;

  public MissingConfiguratorProblemInfo(String configuratorId, MojoExecutionKey mojoExecutionKey, int severity,
      SourceLocation markerLocation) {
    super(NLS.bind(Messages.ProjectConfiguratorNotAvailable, configuratorId, mojoExecutionKey.toString()), severity,
        mojoExecutionKey, markerLocation);
    this.configuratorId = configuratorId;
  }

  public String getConfiguratorId() {
    return configuratorId;
  }

  @Override
  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);
    marker.setAttribute(IMavenConstants.MARKER_ATTR_CONFIGURATOR_ID, getConfiguratorId());
    marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_MISSING_CONFIGURATOR);
  }
}
