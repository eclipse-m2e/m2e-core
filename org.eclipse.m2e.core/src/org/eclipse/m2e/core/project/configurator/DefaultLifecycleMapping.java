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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionFilter;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * DefaultLifecycleMapping
 * 
 * @author igor
 */
public class DefaultLifecycleMapping extends CustomLifecycleMapping {

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

    Set<AbstractProjectConfigurator> configuratorsToUse = new LinkedHashSet<AbstractProjectConfigurator>();

    Map<MojoExecutionKey, Set<AbstractProjectConfigurator>> projectConfiguratorsByMojoExecution = new LinkedHashMap<MojoExecutionKey, Set<AbstractProjectConfigurator>>();

    // Filter project configurators by the mojo executions in the maven execution plan
    MavenExecutionPlan executionPlan = facade.getExecutionPlan(monitor);
    for(MojoExecution execution : executionPlan.getMojoExecutions()) {
      MojoExecutionKey key = new MojoExecutionKey(execution);
      Set<AbstractProjectConfigurator> projectConfiguratorsForMojoExecution = new LinkedHashSet<AbstractProjectConfigurator>();
      projectConfiguratorsByMojoExecution.put(key, projectConfiguratorsForMojoExecution);
      configurators: for(AbstractProjectConfigurator configurator : configurators) {
        if(!configurator.isSupportedExecution(execution)) {
          continue;
        }
        // Re-use project configurators.
        for(AbstractProjectConfigurator otherConfigurator : configuratorsToUse) {
          if(otherConfigurator.equals(configurator)) {
            for(PluginExecutionFilter pluginExecutionFilter : configurator.getPluginExecutionFilters()) {
              otherConfigurator.addPluginExecutionFilter(pluginExecutionFilter);
            }
            projectConfiguratorsForMojoExecution.add(otherConfigurator);

            continue configurators;
          }
        }
        projectConfiguratorsForMojoExecution.add(configurator);
        configuratorsToUse.add(configurator);
      }
    }

    // Find other project configurators
    for(Map.Entry<MojoExecutionKey, Set<AbstractProjectConfigurator>> entry : projectConfiguratorsByMojoExecution
        .entrySet()) {
      MojoExecutionKey key = entry.getKey();
      boolean useDefaultMetadata = entry.getValue().size() == 0;
      List<AbstractProjectConfigurator> newConfigurators = LifecycleMappingFactory.createProjectConfiguratorFor(facade,
          key.getMojoExecution(), useDefaultMetadata);
      if(newConfigurators.size() == 0) {
        continue;
      }
      // Re-use project configurators.
      Set<AbstractProjectConfigurator> projectConfiguratorsForMojoExecution = entry.getValue();
      for(AbstractProjectConfigurator newConfigurator : newConfigurators) {
        boolean isNew = true;
        for(AbstractProjectConfigurator otherConfigurator : configuratorsToUse) {
          if(otherConfigurator.equals(newConfigurator)) {
            for(PluginExecutionFilter pluginExecutionFilter : newConfigurator.getPluginExecutionFilters()) {
              otherConfigurator.addPluginExecutionFilter(pluginExecutionFilter);
            }
            projectConfiguratorsForMojoExecution.add(otherConfigurator);
            isNew = false;
          }
        }
        if(isNew) {
          projectConfiguratorsForMojoExecution.add(newConfigurator);
          configuratorsToUse.add(newConfigurator);
        }
      }
    }

    ArrayList<AbstractProjectConfigurator> result = new ArrayList<AbstractProjectConfigurator>();

    for(AbstractProjectConfigurator configurator : configuratorsToUse) {
      if(configurator != null && !result.contains(configurator)) {
        result.add(configurator);
      }
    }

    return result;
  }
}
