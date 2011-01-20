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
import org.eclipse.osgi.util.NLS;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.lifecycle.DuplicateMappingException;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.MappingMetadataSource;
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

  @Override
  public void initializeMapping(List<MojoExecution> executionPlan, MappingMetadataSource originalMapping,
      List<MappingMetadataSource> inheritedMapping) {

    this.effectiveMapping = getEffectiveMapping(executionPlan, originalMapping, inheritedMapping);

    this.configurators = new LinkedHashMap<String, AbstractProjectConfigurator>();
    for(List<PluginExecutionMetadata> executionMetadatas : effectiveMapping.values()) {
      for(PluginExecutionMetadata executionMetadata : executionMetadatas) {
        if(PluginExecutionAction.configurator == executionMetadata.getAction()) {
          String configuratorId = LifecycleMappingFactory.getProjectConfiguratorId(executionMetadata);
          if(!configurators.containsKey(configuratorId)) {
            try {
              configurators.put(configuratorId, LifecycleMappingFactory.createProjectConfigurator(executionMetadata));
            } catch(LifecycleMappingConfigurationException e) {
              addProblem(1, NLS.bind(Messages.ProjectConfiguratorNotAvailable, configuratorId));
            }
          }
        }
      }
    }
  }

  protected Map<MojoExecutionKey, List<PluginExecutionMetadata>> getEffectiveMapping(List<MojoExecution> executionPlan,
      MappingMetadataSource originalMapping, List<MappingMetadataSource> inheritedMapping) {
    Map<MojoExecutionKey, List<PluginExecutionMetadata>> executionMapping = new LinkedHashMap<MojoExecutionKey, List<PluginExecutionMetadata>>();

    for(MojoExecution execution : executionPlan) {
      PluginExecutionMetadata primaryMetadata = null;

      // find primary mapping first
      try {
        for(MappingMetadataSource source : inheritedMapping) {
          for(PluginExecutionMetadata executionMetadata : source.getPluginExecutionMetadata(execution)) {
            if(LifecycleMappingFactory.isPrimaryMapping(executionMetadata)) {
              if(primaryMetadata != null) {
                primaryMetadata = null;
                throw new DuplicateMappingException();
              }
              primaryMetadata = executionMetadata;
            }
          }
          if(primaryMetadata != null) {
            break;
          }
        }
      } catch(DuplicateMappingException e) {
        addProblem(1, NLS.bind(Messages.PluginExecutionMappingDuplicate, execution.toString()));
      }

      if(primaryMetadata != null) {
        List<PluginExecutionMetadata> executionMetadatas = new ArrayList<PluginExecutionMetadata>();
        executionMetadatas.add(primaryMetadata);

        // attach any secondary mapping
        for(MappingMetadataSource source : inheritedMapping) {
          List<PluginExecutionMetadata> metadatas = source.getPluginExecutionMetadata(execution);
          if(metadatas != null) {
            for(PluginExecutionMetadata metadata : metadatas) {
              if(LifecycleMappingFactory.isSecondaryMapping(metadata, primaryMetadata)) {
                executionMetadatas.add(metadata);
              }
            }
          }
        }

        executionMapping.put(new MojoExecutionKey(execution), executionMetadatas);
      }
    }

    return executionMapping;
  }

  public Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipants(IProgressMonitor monitor)
      throws CoreException {
    Map<MojoExecutionKey, List<AbstractBuildParticipant>> result = new LinkedHashMap<MojoExecutionKey, List<AbstractBuildParticipant>>();

    MavenExecutionPlan executionPlan = getMavenProjectFacade().getExecutionPlan(monitor);
    for(MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
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
              break;
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

  public List<MojoExecution> getNotCoveredMojoExecutions(IProgressMonitor monitor) throws CoreException {
    List<MojoExecution> result = new ArrayList<MojoExecution>();

    MavenExecutionPlan executionPlan = getMavenProjectFacade().getExecutionPlan(monitor);
    all_mojo_executions: for(MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
      if(!isInterestingPhase(mojoExecution.getLifecyclePhase())) {
        continue;
      }
      List<PluginExecutionMetadata> executionMetadatas = effectiveMapping.get(new MojoExecutionKey(mojoExecution));
      if(executionMetadatas != null) {
        for(PluginExecutionMetadata executionMetadata : executionMetadatas) {
          switch(executionMetadata.getAction()) {
            case ignore:
            case execute:
              continue all_mojo_executions;
            case configurator:
              if(configurators.containsKey(LifecycleMappingFactory.getProjectConfiguratorId(executionMetadata))) {
                continue all_mojo_executions;
              }
          }
        }
      }
      result.add(mojoExecution);
    }
    return result;
  }
}
