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
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class NotCoveredMojoExecution extends MojoExecutionProblemInfo {

  /**
   * @deprecated use {@link #NotCoveredMojoExecution(MojoExecutionKey, int, SourceLocation)}
   */
  @Deprecated
  public NotCoveredMojoExecution(MojoExecutionKey mojoExecutionKey, SourceLocation markerLocation) {
    this(mojoExecutionKey, IMarker.SEVERITY_ERROR, markerLocation);
  }

  /**
   * @since 1.5
   */
  public NotCoveredMojoExecution(MojoExecutionKey mojoExecutionKey, int severity, SourceLocation markerLocation) {
    super(NLS.bind(Messages.LifecycleConfigurationPluginExecutionNotCovered, mojoExecutionKey.toString()), severity,
        mojoExecutionKey, markerLocation);
  }

  @Override
  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);

    marker
        .setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION);
  }
}
