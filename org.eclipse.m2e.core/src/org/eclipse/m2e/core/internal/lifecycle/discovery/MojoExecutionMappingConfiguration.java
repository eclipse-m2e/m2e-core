/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecycle.discovery;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionAction;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * MojoExecutionMappingConfiguration
 * 
 * @author igor
 */
public class MojoExecutionMappingConfiguration {

  public static class MojoExecutionMappingInfo {
    private final PluginExecutionMetadata mapping;

    private final AbstractProjectConfigurator configurator;

    public MojoExecutionMappingInfo(PluginExecutionMetadata mapping, AbstractProjectConfigurator configurator) {
      this.mapping = mapping;
      this.configurator = configurator;
    }

    public PluginExecutionMetadata getMapping() {
      return mapping;
    }

    public AbstractProjectConfigurator getConfigurator() {
      return configurator;
    }
  }

  public static class Key implements ILifecycleMappingElementKey {
    private final MojoExecutionKey execution;

    public Key(MojoExecutionKey execution) {
      this.execution = execution;
    }

    public int hashCode() {
      return execution.hashCode();
    }

    public boolean equals(Object obj) {
      if(obj == this) {
        return true;
      }
      if(!(obj instanceof Key)) {
        return false;
      }
      return execution.equals(((Key) obj).execution);
    }

    public MojoExecutionKey getExecution() {
      return execution;
    }
  }

  private final MojoExecutionKey execution;

  private final List<MojoExecutionMappingInfo> mappings = new ArrayList<MojoExecutionMappingInfo>();

  public MojoExecutionMappingConfiguration(MojoExecutionKey execution) {
    this.execution = execution;
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

  public boolean isOK() {
    if(mappings == null || mappings.isEmpty()) {
      // TODO not sure I like this here
      return !LifecycleMappingFactory.isInterestingPhase(execution.getLifecyclePhase());
    }
    for(MojoExecutionMappingInfo info : mappings) {
      if(info.getMapping().getAction() == PluginExecutionAction.error) {
        return false;
      }
      if(info.getMapping().getAction() == PluginExecutionAction.configurator && info.getConfigurator() == null) {
        return false;
      }
    }
    return true;
  }
  
  public MojoExecutionKey getExecution() {
    return this.execution;
  }
  
  public List<MojoExecutionMappingInfo> getMappings() {
    return this.mappings;
  }

  public void addMapping(PluginExecutionMetadata pluginExecutionMetadata, AbstractProjectConfigurator configurator) {
    mappings.add(new MojoExecutionMappingInfo(pluginExecutionMetadata, configurator));
  }

  public ILifecycleMappingElementKey getLifecycleMappingElementKey() {
    return new Key(execution);
  }
}
