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

import org.apache.maven.plugin.MojoExecution;


public class MojoExecutionKey {
  private final MojoExecution execution;

  public MojoExecutionKey(MojoExecution execution) {
    this.execution = execution;
  }

  public MojoExecution getMojoExecution() {
    return execution;
  }

  public int hashCode() {
    int hash = execution.getGroupId().hashCode();
    hash = 17 * hash + execution.getArtifactId().hashCode();
    hash = 17 * hash + execution.getVersion().hashCode();
    hash = 17 * execution.getGoal().hashCode();
    return hash;
  }

  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof MojoExecutionKey)) {
      return false;
    }

    MojoExecutionKey other = (MojoExecutionKey) obj;

    return execution.getGroupId().equals(other.execution.getGroupId())
        && execution.getArtifactId().equals(other.execution.getArtifactId())
        && execution.getVersion().equals(other.execution.getVersion())
        && execution.getGoal().equals(other.execution.getGoal());
  }
}
