/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - packaging and executionId attributes to MojoExecutionMappingRequirement
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping.discovery;

import org.eclipse.core.runtime.Assert;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * Represents Maven plugin execution bound to project lifecycle and corresponding lifecycle mapping metadata. Only
 * considers primary mapping, secondary project configurators are ignored.
 */
public class MojoExecutionMappingConfiguration {
  private MojoExecutionMappingConfiguration() {
  } // static use only

  public static class MojoExecutionMappingRequirement implements ILifecycleMappingRequirement {
    private final MojoExecutionKey execution;

    private final String executionId;

    private String packaging;

    public MojoExecutionMappingRequirement(MojoExecutionKey execution) {
      Assert.isNotNull(execution);
      this.execution = new MojoExecutionKey(execution.groupId(), execution.artifactId(), execution.version(),
          execution.goal(), null, null);

      executionId = execution.executionId();
    }

    /**
     * @since 1.5.0
     */
    public MojoExecutionMappingRequirement(MojoExecutionKey execution, String packaging) {
      this(execution);
      this.packaging = packaging;
    }

    @Override
    public int hashCode() {
      int hash = execution.hashCode();
      if(executionId != null) {
        //hash = 17 * hash + executionId.hashCode();
      }
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }

      return obj instanceof MojoExecutionMappingRequirement other && execution.equals(other.execution);
    }

    /**
     * @since 1.5.0
     */
    public String getExecutionId() {
      return executionId;
    }

    public MojoExecutionKey getExecution() {
      return execution;
    }

    /**
     * @since 1.5.0
     */
    public String getPackaging() {
      return packaging;
    }

  }

  public static record ProjectConfiguratorMappingRequirement(MojoExecutionKey execution, String configuratorId)
      implements ILifecycleMappingRequirement {
  }
}
