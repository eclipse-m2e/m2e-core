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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionAction;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;


/**
 * Abstract base class for customizable lifecycle mappings
 * 
 * @author igor
 */
public abstract class AbstractCustomizableLifecycleMapping extends AbstractLifecycleMapping {

  private Map<MojoExecutionKey, List<PluginExecutionMetadata>> effectiveMapping;

  private Map<String, AbstractProjectConfigurator> configurators;

  private List<MojoExecution> executionPlan;

  private List<MojoExecutionKey> notCoveredExecutions;

  @Override
  public void initializeMapping(List<MojoExecution> mojoExecutions,
      Map<MojoExecutionKey, List<PluginExecutionMetadata>> executionMapping) {

    this.effectiveMapping = executionMapping;

    this.executionPlan = mojoExecutions;

    // instantiate configurator
    this.configurators = new LinkedHashMap<String, AbstractProjectConfigurator>();
    for(List<PluginExecutionMetadata> executionMetadatas : effectiveMapping.values()) {
      for(PluginExecutionMetadata executionMetadata : executionMetadatas) {
        if(PluginExecutionAction.configurator == executionMetadata.getAction()) {
          String configuratorId = LifecycleMappingFactory.getProjectConfiguratorId(executionMetadata);
          if(!configurators.containsKey(configuratorId)) {
            try {
              configurators.put(configuratorId, LifecycleMappingFactory.createProjectConfigurator(executionMetadata));
            } catch(LifecycleMappingConfigurationException e) {
              addMissingConfiguratorProblem(1, NLS.bind(Messages.ProjectConfiguratorNotAvailable, configuratorId),
                  configuratorId);
            }
          }
        }
      }
    }

    // find and cache not-covered mojo execution keys
    notCoveredExecutions = new ArrayList<MojoExecutionKey>();
    all_mojo_executions: for(MojoExecution mojoExecution : executionPlan) {
      if(!isInterestingPhase(mojoExecution.getLifecyclePhase())) {
        continue;
      }
      MojoExecutionKey executionKey = new MojoExecutionKey(mojoExecution);
      List<PluginExecutionMetadata> executionMetadatas = effectiveMapping.get(executionKey);
      if(executionMetadatas != null) {
        for(PluginExecutionMetadata executionMetadata : executionMetadatas) {
          switch(executionMetadata.getAction()) {
            case ignore:
            case execute:
              String message = LifecycleMappingFactory.getActionMessage(executionMetadata);
              if(message != null) {
                addProblem(new ActionMessageProblemInfo(executionKey, 1 /*line*/, message, IMarker.SEVERITY_WARNING));
              }
              continue all_mojo_executions;
            case error:
              message = LifecycleMappingFactory.getActionMessage(executionMetadata);
              if(message == null) {
                message = NLS.bind(Messages.LifecycleConfigurationMojoExecutionErrorMessage, executionKey.toString(),
                    executionKey.getLifecyclePhase());
              }
              addProblem(new ActionMessageProblemInfo(executionKey, 1 /*line*/, message, IMarker.SEVERITY_ERROR));
              continue all_mojo_executions;
            case configurator:
              if(configurators.containsKey(LifecycleMappingFactory.getProjectConfiguratorId(executionMetadata))) {
                continue all_mojo_executions;
              }
              break;
            default:
              throw new IllegalArgumentException("Missing handling for action=" + executionMetadata.getAction());
          }
        }
      }
      notCoveredExecutions.add(executionKey);
    }
  }

  public Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipants(IProgressMonitor monitor) {
    Map<MojoExecutionKey, List<AbstractBuildParticipant>> result = new LinkedHashMap<MojoExecutionKey, List<AbstractBuildParticipant>>();

    for(MojoExecution mojoExecution : executionPlan) {
      MojoExecutionKey executionKey = new MojoExecutionKey(mojoExecution);
      List<AbstractBuildParticipant> executionMappings = new ArrayList<AbstractBuildParticipant>();
      List<PluginExecutionMetadata> executionMetadatas = effectiveMapping.get(executionKey);
      if(executionMetadatas != null) {
        for(PluginExecutionMetadata executionMetadata : executionMetadatas) {
          switch(executionMetadata.getAction()) {
            case execute:
              executionMappings.add(LifecycleMappingFactory.createMojoExecutionBuildParicipant(mojoExecution,
                  executionMetadata));
              break;
            case configurator:
              AbstractProjectConfigurator configurator = configurators.get(LifecycleMappingFactory
                  .getProjectConfiguratorId(executionMetadata));
              AbstractBuildParticipant buildParticipant = configurator.getBuildParticipant(mojoExecution);
              if(buildParticipant != null) {
                executionMappings.add(buildParticipant);
              }
              break;
            case ignore:
            case error:
              break;
            default:
              throw new IllegalArgumentException("Missing handling for action=" + executionMetadata.getAction());
          }
        }
      }

      result.put(executionKey, executionMappings);
    }

    return result;
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators(IProgressMonitor monitor) {
    return new ArrayList<AbstractProjectConfigurator>(configurators.values());
  }

  public List<MojoExecutionKey> getNotCoveredMojoExecutions(IProgressMonitor monitor) {
    return notCoveredExecutions;
  }

  public void addMissingConfiguratorProblem(int line, String message, String configuratorId) {
    addProblem(new MissingConfiguratorProblemInfo(line, message, configuratorId));
  }
}
