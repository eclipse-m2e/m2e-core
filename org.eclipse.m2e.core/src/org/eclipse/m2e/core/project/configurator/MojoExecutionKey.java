/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Hannes Wellmann - Convert to record
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import java.io.Serializable;

import org.apache.maven.plugin.MojoExecution;


public record MojoExecutionKey(String groupId, String artifactId, String version, String goal, String lifecyclePhase,
    String executionId) implements Serializable {

  public MojoExecutionKey(MojoExecution mojoExecution) {
    this(mojoExecution.getGroupId(), mojoExecution.getArtifactId(), mojoExecution.getVersion(), mojoExecution.getGoal(),
        mojoExecution.getLifecyclePhase(), mojoExecution.getExecutionId());
  }

  public String getKeyString() {
    return groupId + ":" + artifactId + ":" + version + ":" + goal + ":" + executionId + ":" + lifecyclePhase;
  }

  @Override
  public String toString() {
    return groupId + ":" + artifactId + ":" + version + ":" + goal + " (execution: " + executionId + ", phase: "
        + lifecyclePhase + ")";
  }
}
