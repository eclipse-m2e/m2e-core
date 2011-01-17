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
  private final MojoExecution mojoExecution;

  public MojoExecutionKey(MojoExecution mojoExecution) {
    this.mojoExecution = mojoExecution;
  }

  public MojoExecution getMojoExecution() {
    return mojoExecution;
  }

  public int hashCode() {
    int hash = mojoExecution.getGroupId().hashCode();
    hash = 17 * hash + mojoExecution.getArtifactId().hashCode();
    hash = 17 * hash + mojoExecution.getVersion().hashCode();
    hash = 17 * mojoExecution.getGoal().hashCode();
    if(mojoExecution.getExecutionId() != null) {
      hash = 17 * mojoExecution.getExecutionId().hashCode();
    }
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

    boolean equals = mojoExecution.getGroupId().equals(other.mojoExecution.getGroupId())
        && mojoExecution.getArtifactId().equals(other.mojoExecution.getArtifactId())
        && mojoExecution.getVersion().equals(other.mojoExecution.getVersion())
        && mojoExecution.getGoal().equals(other.mojoExecution.getGoal());
    if(!equals) {
      return false;
    }

    if(mojoExecution.getExecutionId() == null) {
      return other.mojoExecution.getExecutionId() == null;
    }
    return mojoExecution.getExecutionId().equals(other.mojoExecution.getExecutionId());
  }

  public String getKeyString() {
    return mojoExecution.getGroupId() + ":" + mojoExecution.getArtifactId() + ":" + mojoExecution.getVersion() + ":"
        + mojoExecution.getGoal() + ":" + mojoExecution.getExecutionId();
  }
}
