/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import org.apache.maven.plugin.MojoExecution;

/**
 * MavenExecutionUtils
 *
 * @author mpoindexter
 */
public class MojoExecutionUtils {
  public static String getExecutionKey(MojoExecution execution) {
    return nvl(execution.getGroupId()) + ":" + nvl(execution.getArtifactId()) + ":" + nvl(execution.getVersion()) + ":" + nvl(execution.getGoal()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }
  
  private static String nvl(String s) {
    return s == null ? "" : s; //$NON-NLS-1$
  }
}
