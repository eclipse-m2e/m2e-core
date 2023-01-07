/********************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.io.Serializable;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;


/**
 * Metadata for an default executed plugin execution.
 */
public class DefaultPluginExecutionMetadata implements IPluginExecutionMetadata, Serializable {

  private static final long serialVersionUID = 1L;

  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String executionId;

  private PluginExecutionAction action;

  DefaultPluginExecutionMetadata(MojoExecution execution, PluginExecutionAction action) {
    this.action = action;
    this.groupId = execution.getGroupId();
    this.artifactId = execution.getArtifactId();
    this.version = execution.getVersion();
    this.executionId = execution.getExecutionId();
  }

  @Override
  public PluginExecutionAction getAction() {
    return action;
  }

  @Override
  public String toString() {
    return String.format("Default MojoExecution %s:%s:%s (id=%s) with action = %s", groupId, artifactId, version,
        executionId, action);
  }

}
