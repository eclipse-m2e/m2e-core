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

import org.eclipse.m2e.core.internal.markers.MarkerLocation;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;

public class ActionMessageProblemInfo extends MavenProblemInfo {
  private final MojoExecutionKey mojoExecutionKey;

  public MojoExecutionKey getMojoExecutionKey() {
    return this.mojoExecutionKey;
  }

  public ActionMessageProblemInfo(String message, int severity, MojoExecutionKey mojoExecutionKey,
      MarkerLocation markerLocation) {
    //TODO Use actual location
    super(message, severity, markerLocation);
    this.mojoExecutionKey = mojoExecutionKey;
  }
}
