/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingConfigurationException;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Abstract base class for customizable lifecycle mappings
 *
 * @author igor
 */
public abstract class AbstractCustomizableLifecycleMapping extends AbstractLifecycleMapping {
  private static Logger log = LoggerFactory.getLogger(AbstractCustomizableLifecycleMapping.class);

  @Override
  public Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipants(IMavenProjectFacade projectFacade,
      IProgressMonitor monitor) throws CoreException {
    log.debug("Build participants for {}", projectFacade.getMavenProject());
    Map<MojoExecutionKey, List<AbstractBuildParticipant>> result = new LinkedHashMap<>();

    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mapping = projectFacade.getMojoExecutionMapping();
    Map<String, AbstractProjectConfigurator> configurators = getProjectConfigurators(projectFacade);

    List<MojoExecution> mojoExecutions = ((MavenProjectFacade) projectFacade)
        .getExecutionPlan(ProjectRegistryManager.LIFECYCLE_DEFAULT, monitor);

    if(mojoExecutions != null) { // null if execution plan could not be calculated
      for(MojoExecution mojoExecution : mojoExecutions) {
        MojoExecutionKey mojoExecutionKey = new MojoExecutionKey(mojoExecution);
        log.debug("Mojo execution key: {}", mojoExecutionKey);
        List<IPluginExecutionMetadata> executionMetadatas = mapping.get(mojoExecutionKey);
        List<AbstractBuildParticipant> executionMappings = new ArrayList<>();
        if(executionMetadatas != null && !executionMetadatas.isEmpty()) {
          for(IPluginExecutionMetadata executionMetadata : executionMetadatas) {
            switch(executionMetadata.getAction()) {
              case execute:
                log.debug("\tAction: {}", executionMetadata.getAction());
                executionMappings.add(LifecycleMappingFactory.createMojoExecutionBuildParicipant(
                    projectFacade.getMojoExecution(mojoExecutionKey, monitor), executionMetadata));
                break;
              case configurator:
                String configuratorId = LifecycleMappingFactory.getProjectConfiguratorId(executionMetadata);
                AbstractProjectConfigurator configurator = configurators.get(configuratorId);
                if(configurator == null) {
                  log.debug("\t\tProject configurator with id {} was not found", configuratorId);
                  break;
                }
                AbstractBuildParticipant buildParticipant = configurator.getBuildParticipant(projectFacade,
                    projectFacade.getMojoExecution(mojoExecutionKey, monitor), executionMetadata);
                if(buildParticipant != null) {
                  log.debug("\tAction: {}", executionMetadata.getAction());
                  log.debug("\t\tProject configurator : id={} class={}", configuratorId,
                      configurator.getClass().getName());
                  log.debug("\t\tBuild participant: {}", buildParticipant.getClass().getName());
                  executionMappings.add(buildParticipant);
                }
                break;
              case ignore, warn, error:
                log.debug("\tAction: {}", executionMetadata.getAction());
                break;
              default:
                throw new IllegalArgumentException("Missing handling for action=" + executionMetadata.getAction());
            }
          }
        } else {
          log.debug("\t- execution is skipped as nothing is configured -");
        }

        result.put(mojoExecutionKey, executionMappings);
      }
    }

    return result;
  }

  @Override
  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade projectFacade,
      IProgressMonitor monitor) {
    return new ArrayList<>(getProjectConfigurators(projectFacade).values());
  }

  private Map<String, AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade projectFacade) {
    return LifecycleMappingFactory.getProjectConfigurators(projectFacade);
  }

  @Override
  public boolean hasLifecycleMappingChanged(IMavenProjectFacade newFacade,
      ILifecycleMappingConfiguration oldConfiguration, IProgressMonitor monitor) {
    if(!getId().equals(newFacade.getLifecycleMappingId())) {
      throw new IllegalArgumentException();
    }

    if(oldConfiguration == null || !getId().equals(oldConfiguration.getLifecycleMappingId())) {
      return true;
    }

    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> oldMappings = oldConfiguration.getMojoExecutionMapping();

    for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : newFacade.getMojoExecutionMapping()
        .entrySet()) {
      List<IPluginExecutionMetadata> metadatas = entry.getValue();
      List<IPluginExecutionMetadata> oldMetadatas = oldMappings.get(entry.getKey());
      if(metadatas == null || metadatas.isEmpty()) {
        if(oldMetadatas != null && !oldMetadatas.isEmpty()) {
          return true; // different
        }
        continue; // mapping is null/empty and did not change
      }
      if(oldMetadatas == null || oldMetadatas.isEmpty() || metadatas.size() != oldMetadatas.size()) {
        return true;
      }
      for(int i = 0; i < metadatas.size(); i++ ) {
        IPluginExecutionMetadata metadata = metadatas.get(i);
        IPluginExecutionMetadata oldMetadata = oldMetadatas.get(i);
        if(metadata == null) {
          if(oldMetadata != null) {
            return true;
          }
          continue;
        }
        if(oldMetadata == null || metadata.getAction() != oldMetadata.getAction()) {
          return true;
        }
        if(metadata.getAction() == PluginExecutionAction.configurator) {
          String configuratorId = LifecycleMappingFactory.getProjectConfiguratorId(metadata);
          String oldConfiguratorId = LifecycleMappingFactory.getProjectConfiguratorId(oldMetadata);
          if(!Objects.equals(configuratorId, oldConfiguratorId)) {
            return true;
          }
          try {
            AbstractProjectConfigurator configurator = LifecycleMappingFactory.createProjectConfigurator(metadata);
            if(configurator.hasConfigurationChanged(newFacade, oldConfiguration, entry.getKey(), monitor)) {
              return true;
            }
          } catch(LifecycleMappingConfigurationException e) {
            // installation problem/misconfiguration
          }
        }
        // TODO verify error message did not change in case metadata.getAction() == error ...
      }
    }
    return false;
  }

}
