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
package org.eclipse.m2e.core.project.configurator;

public class ActionMessageProblemInfo extends LifecycleMappingProblemInfo {
  private final MojoExecutionKey mojoExecutionKey;

  public MojoExecutionKey getMojoExecutionKey() {
    return this.mojoExecutionKey;
  }

  protected ActionMessageProblemInfo(MojoExecutionKey mojoExecutionKey, int line, String message, int severity) {
    super(line, message, severity);
    this.mojoExecutionKey = mojoExecutionKey;
  }
}
