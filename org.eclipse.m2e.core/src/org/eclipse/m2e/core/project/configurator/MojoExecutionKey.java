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

  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String goal;

  private final String executionId;

  private final String lifecyclePhase;

  public MojoExecutionKey(MojoExecution mojoExecution) {
    this.groupId = mojoExecution.getGroupId();
    this.artifactId = mojoExecution.getArtifactId();
    this.version = mojoExecution.getVersion();
    this.goal = mojoExecution.getGoal();
    this.executionId = mojoExecution.getExecutionId();
    this.lifecyclePhase = mojoExecution.getLifecyclePhase();
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
    return this.artifactId;
  }

  public String getVersion() {
    return this.version;
  }

  public String getGoal() {
    return this.goal;
  }

  public String getExecutionId() {
    return this.executionId;
  }

  public String getLifecyclePhase() {
    return lifecyclePhase;
  }

  public String getKeyString() {
    return groupId + ":" + artifactId + ":" + version + ":" + goal + ":" + executionId + ":" + lifecyclePhase;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(groupId).append(":").append(artifactId).append(":").append(version);
    sb.append(":").append(goal);
    sb.append(" {execution: ").append(executionId).append("}");
    return sb.toString();
  }
}
