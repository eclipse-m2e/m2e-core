/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.lifecyclemapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingConfigurationException;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingElementKey;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.osgi.util.NLS;


/**
 * @TODO I don't like this code in UI, but I don't see a nice way to move it to the backend
 * @author igor
 */
@SuppressWarnings("restriction")
public class LifecycleMappingConfiguration {

  private final Map<MavenProjectInfo, ProjectLifecycleMappingConfiguration> projects = new HashMap<MavenProjectInfo, ProjectLifecycleMappingConfiguration>();

  private Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> allproposals;

  private final Set<IMavenDiscoveryProposal> selectedProposals = new LinkedHashSet<IMavenDiscoveryProposal>();

  private LifecycleMappingConfiguration() {
  }

  public List<ProjectLifecycleMappingConfiguration> getProjects() {
    return new ArrayList<ProjectLifecycleMappingConfiguration>(projects.values());
  }

  private void addProject(MavenProjectInfo info, ProjectLifecycleMappingConfiguration project) {
    this.projects.put(info, project);
  }

  public void setProposals(Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> proposals) {
    this.allproposals = proposals;
  }

  /**
   * Returns all proposals available for provided requirement or empty List.
   */
  public List<IMavenDiscoveryProposal> getProposals(ILifecycleMappingElementKey requirement) {
    if(allproposals == null) {
      return Collections.emptyList();
    }
    List<IMavenDiscoveryProposal> result = allproposals.get(requirement);
    if(result == null) {
      return Collections.emptyList();
    }
    return result;
  }

  public void addSelectedProposal(IMavenDiscoveryProposal proposal) {
    selectedProposals.add(proposal);
  }

  public void removeSelectedProposal(IMavenDiscoveryProposal proposal) {
    selectedProposals.remove(proposal);
  }

  /**
   * Returns true if mapping configuration is complete after applying selected proposals.
   */
  public boolean isMappingComplete() {
    for(ProjectLifecycleMappingConfiguration project : projects.values()) {
      PackagingTypeMappingConfiguration packagingTypeMappingConfiguration = project
          .getPackagingTypeMappingConfiguration();
      if(!packagingTypeMappingConfiguration.isOK()
          && getSelectedProposal(packagingTypeMappingConfiguration.getLifecycleMappingElementKey()) == null) {
        return false;
      }

      for(MojoExecutionMappingConfiguration mojoExecutionConfiguration : project.getMojoExecutionConfigurations()) {
        if(!mojoExecutionConfiguration.isOK()
            && getSelectedProposal(mojoExecutionConfiguration.getLifecycleMappingElementKey()) == null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Automatically selects proposals when where is only one possible solution to a problem. Returns true if mapping
   * configuration is complete after applying automatically selected proposals.
   */
  public boolean autoCompleteMapping() {
    LinkedHashSet<ILifecycleMappingElementKey> elements = new LinkedHashSet<ILifecycleMappingElementKey>();

    for(ProjectLifecycleMappingConfiguration project : projects.values()) {
      PackagingTypeMappingConfiguration packagingTypeMappingConfiguration = project
          .getPackagingTypeMappingConfiguration();

      if(!packagingTypeMappingConfiguration.isOK()) {
        elements.add(packagingTypeMappingConfiguration.getLifecycleMappingElementKey());
      }

      for(MojoExecutionMappingConfiguration mojoExecutionConfiguration : project.getMojoExecutionConfigurations()) {
        if(!mojoExecutionConfiguration.isOK()) {
          elements.add(mojoExecutionConfiguration.getLifecycleMappingElementKey());
        }
      }
    }

    for(ILifecycleMappingElementKey element : elements) {
      List<IMavenDiscoveryProposal> proposals = getProposals(element);
      if(proposals.size() == 1) {
        addSelectedProposal(proposals.get(0));
      }
    }

    return isMappingComplete();
  }

  public IMavenDiscoveryProposal getSelectedProposal(ILifecycleMappingElementKey mojoExecutionKey) {
    if(allproposals == null) {
      return null;
    }
    List<IMavenDiscoveryProposal> proposals = allproposals.get(mojoExecutionKey);
    if(proposals == null) {
      return null;
    }
    for(IMavenDiscoveryProposal proposal : proposals) {
      if(getSelectedProposals().contains(proposal)) {
        return proposal;
      }
    }
    return null;
  }

  public List<IMavenDiscoveryProposal> getSelectedProposals() {
    return new ArrayList<IMavenDiscoveryProposal>(selectedProposals);
  }

  public void clearSelectedProposals() {
    selectedProposals.clear();
  }
  
  public static LifecycleMappingConfiguration clone(LifecycleMappingConfiguration original, Collection<MavenProjectInfo> filter) {
    if (original == null) {
      throw new IllegalArgumentException("The original LifecycleMappingConfiguration shall not be null."); 
    }
    LifecycleMappingConfiguration result = new LifecycleMappingConfiguration();
    for (MavenProjectInfo info : filter) {
      ProjectLifecycleMappingConfiguration res = original.projects.get(info);
      if (res != null) {
        result.addProject(info, res);
      }
    }
    return result;
  }

  public static LifecycleMappingConfiguration calculate(Collection<MavenProjectInfo> projects,
      ProjectImportConfiguration importConfiguration, IProgressMonitor monitor) throws CoreException {

    monitor.beginTask("Analysing project execution plan", projects.size());

    LifecycleMappingConfiguration result = new LifecycleMappingConfiguration();

    MavenPlugin mavenPlugin = MavenPlugin.getDefault();
    IMaven maven = mavenPlugin.getMaven();

    for(MavenProjectInfo projectInfo : projects) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      SubMonitor subMmonitor = SubMonitor.convert(monitor, NLS.bind("Analysing {0}",projectInfo.getLabel()), 1);

      MavenExecutionRequest request = maven.createExecutionRequest(subMmonitor);

      request.setPom(projectInfo.getPomFile());
      request.addActiveProfiles(importConfiguration.getResolverConfiguration().getActiveProfileList());

      // jdk-based profile activation
      Properties systemProperties = new Properties();
      EnvironmentUtils.addEnvVars(systemProperties);
      systemProperties.putAll(System.getProperties());
      request.setSystemProperties(systemProperties);

      request.setLocalRepository(maven.getLocalRepository());

      MavenExecutionResult executionResult = maven.readProject(request, subMmonitor);

      MavenProject mavenProject = executionResult.getProject();

      if(mavenProject != null) {
        MavenSession session = maven.createSession(request, mavenProject);

        List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();
        MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(session, mavenProject,
            Arrays.asList(ProjectRegistryManager.LIFECYCLE_CLEAN), false, subMmonitor);
        mojoExecutions.addAll(executionPlan.getMojoExecutions());
        executionPlan = maven.calculateExecutionPlan(session, mavenProject,
            Arrays.asList(ProjectRegistryManager.LIFECYCLE_DEFAULT), false, subMmonitor);
        mojoExecutions.addAll(executionPlan.getMojoExecutions());
        executionPlan = maven.calculateExecutionPlan(session, mavenProject,
            Arrays.asList(ProjectRegistryManager.LIFECYCLE_SITE), false, subMmonitor);
        mojoExecutions.addAll(executionPlan.getMojoExecutions());

        LifecycleMappingResult lifecycleResult = new LifecycleMappingResult();
        LifecycleMappingFactory.calculateEffectiveLifecycleMappingMetadata(lifecycleResult, request, mavenProject,
            mojoExecutions, subMmonitor);
        LifecycleMappingFactory.instantiateLifecycleMapping(lifecycleResult, mavenProject,
            lifecycleResult.getLifecycleMappingId());
        LifecycleMappingFactory.instantiateProjectConfigurators(mavenProject, lifecycleResult,
            lifecycleResult.getMojoExecutionMapping());

        ProjectLifecycleMappingConfiguration configuration = new ProjectLifecycleMappingConfiguration(
            projectInfo.getLabel(), mavenProject, mojoExecutions, new PackagingTypeMappingConfiguration(
                mavenProject.getPackaging(), lifecycleResult.getLifecycleMappingId(),
                lifecycleResult.getLifecycleMapping()));

        for(Map.Entry<MojoExecutionKey, List<PluginExecutionMetadata>> entry : lifecycleResult
            .getMojoExecutionMapping().entrySet()) {
          MojoExecutionKey key = entry.getKey();
          MojoExecutionMappingConfiguration mojoExecutionConfiguration = new MojoExecutionMappingConfiguration(key);
          for(PluginExecutionMetadata pluginExecutionMetadata : entry.getValue()) {
            AbstractProjectConfigurator configurator = null;
            if(pluginExecutionMetadata.getAction() == PluginExecutionAction.configurator) {
              try {
                configurator = lifecycleResult.getProjectConfigurators().get(
                    LifecycleMappingFactory.getProjectConfiguratorId(pluginExecutionMetadata));
              } catch(LifecycleMappingConfigurationException e) {
                // TODO what do I do with this?
              }
            }
            mojoExecutionConfiguration.addMapping(pluginExecutionMetadata, configurator);
          }
          configuration.addMojoExecution(mojoExecutionConfiguration);
        }
        result.addProject(projectInfo, configuration);
      }
    }

    return result;
  }

}
