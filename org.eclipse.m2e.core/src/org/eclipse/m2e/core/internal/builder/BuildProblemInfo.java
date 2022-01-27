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

package org.eclipse.m2e.core.internal.builder;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


class BuildProblemInfo extends MavenProblemInfo {
  public BuildProblemInfo(Throwable error, MojoExecutionKey mojoExecutionKey, SourceLocation markerLocation) {
    super(formatMessage(error, mojoExecutionKey), markerLocation);
  }

  private static String formatMessage(Throwable error, MojoExecutionKey mojoExecutionKey) {
    StringBuilder msg = new StringBuilder(String.valueOf(error.getMessage()));
    if(mojoExecutionKey != null) {
      msg.append(" (").append(mojoExecutionKey.getKeyString()).append(')'); //$NON-NLS-1$ $NON-NLS-2$
    }
    StringWriter errorStackTrace = new StringWriter();
    error.printStackTrace(new PrintWriter(errorStackTrace));
    return msg.append("\n\n").append(errorStackTrace).toString(); //$NON-NLS-1$
  }
}
