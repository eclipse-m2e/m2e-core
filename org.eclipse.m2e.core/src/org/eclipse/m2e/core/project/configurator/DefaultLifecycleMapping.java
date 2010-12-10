/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * DefaultLifecycleMapping
 * 
 * @author igor
 */
public class DefaultLifecycleMapping extends CustomizableLifecycleMapping {

  private static class MojoExecutionKey {
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

  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    // Get project configurators configured explicitly in the lifecycle mapping configuration
    List<AbstractProjectConfigurator> configurators = super.getProjectConfigurators(facade, monitor);

    Map<MojoExecutionKey, AbstractProjectConfigurator> executions = new LinkedHashMap<MojoExecutionKey, AbstractProjectConfigurator>();

    // Filter project configurators by the mojo executions in the maven execution plan
    MavenExecutionPlan executionPlan = facade.getExecutionPlan(monitor);
    execution: for(MojoExecution execution : executionPlan.getMojoExecutions()) {
      MojoExecutionKey key = new MojoExecutionKey(execution);
      for(AbstractProjectConfigurator configurator : configurators) {
        if(configurator.isSupportedExecution(execution)) {
          executions.put(key, configurator);
          continue execution;
        }
      }
      executions.put(key, null);
    }

    // Re-use project configurators.
    // Find project configurators for not covered mojo executions.
    for(Map.Entry<MojoExecutionKey, AbstractProjectConfigurator> entry : executions.entrySet()) {
      MojoExecutionKey key = entry.getKey();
      if(entry.getValue() == null) {
        // make sure to reuse the same instance of project configurator
        for(AbstractProjectConfigurator configurator : executions.values()) {
          if(configurator != null && configurator.isSupportedExecution(key.getMojoExecution())) {
            entry.setValue(configurator);
            break;
          }
        }
      }
      if(entry.getValue() == null) {
        AbstractProjectConfigurator configurator = LifecycleMappingFactory.createProjectConfiguratorFor(key
            .getMojoExecution());
        if(configurator != null) {
          entry.setValue(configurator);
        }
      }
    }

    ArrayList<AbstractProjectConfigurator> result = new ArrayList<AbstractProjectConfigurator>();

    for(AbstractProjectConfigurator configurator : executions.values()) {
      if(configurator != null && !result.contains(configurator)) {
        result.add(configurator);
      }
    }

    return result;
  }
}
