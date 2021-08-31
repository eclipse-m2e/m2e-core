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

import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * Represents Maven plugin execution bound to project lifecycle and corresponding lifecycle mapping metadata. Only
 * considers primary mapping, secondary project configurators are ignored.
 */
public class MojoExecutionMappingConfiguration implements ILifecycleMappingElement {

  public static class MojoExecutionMappingRequirement implements ILifecycleMappingRequirement {
    private final MojoExecutionKey execution;

    private final String executionId;

    private String packaging;

    public MojoExecutionMappingRequirement(MojoExecutionKey execution) {
      Assert.isNotNull(execution);
      this.execution = new MojoExecutionKey(execution.getGroupId(), execution.getArtifactId(), execution.getVersion(),
          execution.getGoal(), null, null);

      executionId = execution.getExecutionId();
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

      if(!(obj instanceof MojoExecutionMappingRequirement)) {
        return false;
      }

      MojoExecutionMappingRequirement other = (MojoExecutionMappingRequirement) obj;

      return execution.equals(other.execution);

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

  public static class ProjectConfiguratorMappingRequirement implements ILifecycleMappingRequirement {
    private final MojoExecutionKey execution;

    private final String configuratorId;

    public ProjectConfiguratorMappingRequirement(MojoExecutionKey execution, String configuratorId) {
      this.execution = execution;
      this.configuratorId = configuratorId;
    }

    @Override
    public int hashCode() {
      return configuratorId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }

      if(!(obj instanceof ProjectConfiguratorMappingRequirement)) {
        return false;
      }

      ProjectConfiguratorMappingRequirement other = (ProjectConfiguratorMappingRequirement) obj;

      return configuratorId.equals(other.configuratorId) && execution.equals(other.execution);
    }

    public MojoExecutionKey getExecution() {
      return execution;
    }

    public String getProjectConfiguratorId() {
      return configuratorId;
    }
  }

  private final MojoExecutionKey execution;

  private final PluginExecutionMetadata mapping;

  private final ILifecycleMappingRequirement requirement;

  public MojoExecutionMappingConfiguration(MojoExecutionKey execution, IPluginExecutionMetadata mapping) {
    this.execution = execution;
    this.mapping = (PluginExecutionMetadata) mapping;

    if(mapping == null) {
      requirement = new MojoExecutionMappingRequirement(execution);
    } else if(mapping.getAction() == PluginExecutionAction.configurator) {
      requirement = new ProjectConfiguratorMappingRequirement(execution,
          LifecycleMappingFactory.getProjectConfiguratorId(mapping));
    } else {
      requirement = null; // this execution is fully mapped with <execute/>, <ignore/> or <error/> action
    }
  }

  public String getArtifactId() {
    return execution.getArtifactId();
  }

  public String getGoal() {
    return execution.getGoal();
  }

  public boolean isMapped() {
    return false;
  }

  public boolean isExtensionAvailable() {
    return false;
  }

  public MojoExecutionKey getMojoExecutionKey() {
    return execution;
  }

  public MojoExecutionKey getExecution() {
    return this.execution;
  }

  public PluginExecutionMetadata getMapping() {
    return this.mapping;
  }

  /**
   * Mapping requirement key. Null if this mojo execution configuration is complete, i.e. mapped to ignore, execute or
   * error actions.
   */
  @Override
  public ILifecycleMappingRequirement getLifecycleMappingRequirement() {
    return requirement;
  }

  @Override
  public int hashCode() {
    int hash = execution.hashCode();

    if(mapping != null) {
      hash = 17 * hash + mapping.getAction().hashCode();
      if(mapping.getAction() == PluginExecutionAction.configurator) {
        hash += LifecycleMappingFactory.getProjectConfiguratorId(mapping).hashCode();
      }
    }

    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof MojoExecutionMappingConfiguration)) {
      return false;
    }
    MojoExecutionMappingConfiguration other = (MojoExecutionMappingConfiguration) obj;

    if(!execution.equals(other.execution)) {
      return false;
    }

    if(mapping == null) {
      return other.mapping == null;
    }

    if(other.mapping == null) {
      return false;
    }

    if(mapping.getAction() != other.mapping.getAction()) {
      return false;
    }

    if(mapping.getAction() == PluginExecutionAction.configurator) {
      String configuratorId = LifecycleMappingFactory.getProjectConfiguratorId(mapping);
      String otherConfiguratorId = LifecycleMappingFactory.getProjectConfiguratorId(other.mapping);
      if(!eq(configuratorId, otherConfiguratorId)) {
        return false;
      }
    }

    return true;
  }

  private static <T> boolean eq(T a, T b) {
    return a != null ? a.equals(b) : b == null;
  }
}
