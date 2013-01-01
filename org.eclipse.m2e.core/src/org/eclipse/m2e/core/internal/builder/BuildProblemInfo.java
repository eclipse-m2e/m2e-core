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

package org.eclipse.m2e.core.internal.builder;

import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


class BuildProblemInfo extends MavenProblemInfo {
  public BuildProblemInfo(Throwable error, MojoExecutionKey mojoExecutionKey, SourceLocation markerLocation) {
    super(
        error.getMessage() + (mojoExecutionKey == null ? "" : " (" + mojoExecutionKey.getKeyString() + ')'), markerLocation); //$NON-NLS-1$
  }
}
