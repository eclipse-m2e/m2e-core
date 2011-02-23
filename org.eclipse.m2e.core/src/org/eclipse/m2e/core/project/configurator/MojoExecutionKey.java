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

import java.io.Serializable;

import org.apache.maven.plugin.MojoExecution;


public class MojoExecutionKey implements Serializable {

  private static final long serialVersionUID = -2074582830199438890L;

  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String goal;

  private final String executionId;

  private final String lifecyclePhase;

  public MojoExecutionKey(MojoExecution mojoExecution) {
    groupId = mojoExecution.getGroupId();
    artifactId = mojoExecution.getArtifactId();
    version = mojoExecution.getVersion();
    goal = mojoExecution.getGoal();
    executionId = mojoExecution.getExecutionId();
    lifecyclePhase = mojoExecution.getLifecyclePhase();
  }

  public MojoExecutionKey(String groupId, String artifactId, String version, String goal, String lifecyclePhase,
      String executionId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.goal = goal;
    this.lifecyclePhase = lifecyclePhase;
    this.executionId = executionId;
  }

  public int hashCode() {
    int hash = groupId.hashCode();
    hash = 17 * hash + artifactId.hashCode();
    hash = 17 * hash + version.hashCode();
    hash = 17 * goal.hashCode();
    hash = 17 * executionId.hashCode();
    hash = 17 * lifecyclePhase.hashCode();
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

    return groupId.equals(other.groupId) && artifactId.equals(other.artifactId) && version.equals(other.version)
        && goal.equals(other.goal) && executionId.equals(other.executionId)
        && lifecyclePhase.equals(other.lifecyclePhase);
  }

  public String getGroupId() {
    return this.groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getGoal() {
    return goal;
  }

  public String getExecutionId() {
    return executionId;
  }

  public String getLifecyclePhase() {
    return lifecyclePhase;
  }

  public String getKeyString() {
    return groupId + ":" + artifactId + ":" + version + ":" + goal + ":" + executionId + ":" + lifecyclePhase;
  }

  public String toString() {
    return groupId + ":" + artifactId + ":" + version + ":" + goal + " (execution: " + executionId + ", phase: "
        + lifecyclePhase + ")";
  }

  public boolean match(MojoExecution mojoExecution) {
    if(mojoExecution == null) {
      return false;
    }
    return groupId.equals(mojoExecution.getGroupId()) && artifactId.equals(mojoExecution.getArtifactId())
        && version.equals(mojoExecution.getVersion()) && goal.equals(mojoExecution.getGoal())
        && executionId.equals(mojoExecution.getExecutionId());
  }
}
