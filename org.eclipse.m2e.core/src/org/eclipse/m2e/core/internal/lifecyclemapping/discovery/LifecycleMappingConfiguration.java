/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.properties.internal.EnvironmentUtils;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingConfigurationException;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.MappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration.PackagingTypeMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class LifecycleMappingConfiguration {
  private static final Logger log = LoggerFactory.getLogger(LifecycleMappingConfiguration.class);

  private final Map<MavenProjectInfo, ProjectLifecycleMappingConfiguration> allprojects = new HashMap<MavenProjectInfo, ProjectLifecycleMappingConfiguration>();

  /**
   * All proposals to satisfy mapping requirements
   */
  private Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> allproposals;

  /**
   * Mapping proposals selected for implementation, i.e. bundles to be installed and mojo executions to be ignored.
   */
  private final Set<IMavenDiscoveryProposal> selectedProposals = new LinkedHashSet<IMavenDiscoveryProposal>();

  /**
   * Mapping requirements satisfied by installed eclipse bundles and m2e default lifecycle mapping metadata.
   */
  private final Set<ILifecycleMappingRequirement> installedProviders = new HashSet<ILifecycleMappingRequirement>();

  /**
   * Selected projects. null means "nothing is selected".
   */
  private Set<MavenProjectInfo> selectedProjects;

  
  private Map<MavenProjectInfo, Throwable> errors = new HashMap<MavenProjectInfo, Throwable>();

  private LifecycleMappingConfiguration() {
  }

  public List<ProjectLifecycleMappingConfiguration> getProjects() {
    ArrayList<ProjectLifecycleMappingConfiguration> projects = new ArrayList<ProjectLifecycleMappingConfiguration>();
    for(Map.Entry<MavenProjectInfo, ProjectLifecycleMappingConfiguration> project : allprojects.entrySet()) {
      if(selectedProjects.contains(project.getKey())) {
        projects.add(project.getValue());
      }
    }
    return projects;
  }

  private void addProject(MavenProjectInfo info, ProjectLifecycleMappingConfiguration project) {
    this.allprojects.put(info, project);
  }

  public void setProposals(Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals) {
    this.allproposals = proposals;
  }

  /**
   * Returns all proposals available for provided requirement or empty List.
   */
  public List<IMavenDiscoveryProposal> getProposals(ILifecycleMappingRequirement requirement) {
    if(allproposals == null || requirement == null) {
      return Collections.emptyList();
    }
    List<IMavenDiscoveryProposal> result = allproposals.get(requirement);
    if(result == null) {
      return Collections.emptyList();
    }
    return result;
  }

  public Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> getAllProposals() {
    if (allproposals==null) {
      return Collections.emptyMap();
    }
    return allproposals;
  }
  
  public void addSelectedProposal(IMavenDiscoveryProposal proposal) {
    selectedProposals.add(proposal);
  }

  public void removeSelectedProposal(IMavenDiscoveryProposal proposal) {
    selectedProposals.remove(proposal);
  }

  public boolean isRequirementSatisfied(ILifecycleMappingRequirement requirement) {
    return isRequirementSatisfied(requirement, false);
  }

  public boolean isRequirementSatisfied(ILifecycleMappingRequirement requirement, boolean installedOnly) {
    if(requirement == null) {
      return true;
    }

    if(installedProviders.contains(requirement)) {
      return true;
    }

    if(installedOnly || allproposals == null) {
      return false;
    }

    List<IMavenDiscoveryProposal> proposals = allproposals.get(requirement);
    if(proposals != null) {
      for(IMavenDiscoveryProposal proposal : proposals) {
        if(selectedProposals.contains(proposal)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns true if mapping configuration is complete after applying selected proposals.
   */
  public boolean isMappingComplete() {
    return isMappingComplete(false);
  }

  public boolean isMappingComplete(boolean installedOnly) {
    for(ProjectLifecycleMappingConfiguration project : getProjects()) {
      ILifecycleMappingRequirement packagingRequirement = project.getPackagingTypeMappingConfiguration()
          .getLifecycleMappingRequirement();

      if(!(packagingRequirement instanceof PackagingTypeMappingRequirement)
          && !isRequirementSatisfied(packagingRequirement, installedOnly)) {
        return false;
      }

      for(MojoExecutionMappingConfiguration mojoExecutionConfiguration : project.getMojoExecutionConfigurations()) {
        ILifecycleMappingRequirement executionRequirement = mojoExecutionConfiguration.getLifecycleMappingRequirement();
        if(!isRequirementSatisfied(executionRequirement, installedOnly)
            && LifecycleMappingFactory.isInterestingPhase(mojoExecutionConfiguration.getMojoExecutionKey()
                .getLifecyclePhase())) {
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
  public void autoCompleteMapping() {
    LinkedHashSet<ILifecycleMappingRequirement> requirements = new LinkedHashSet<ILifecycleMappingRequirement>();

    for(ProjectLifecycleMappingConfiguration project : getProjects()) {
      ILifecycleMappingRequirement packagingRequirement = project.getPackagingTypeMappingConfiguration()
          .getLifecycleMappingRequirement();

      if(packagingRequirement != null) {
        requirements.add(packagingRequirement);
      }

      for(MojoExecutionMappingConfiguration mojoExecutionConfiguration : project.getMojoExecutionConfigurations()) {
        ILifecycleMappingRequirement executionRequirement = mojoExecutionConfiguration.getLifecycleMappingRequirement();
        if(executionRequirement != null) {
          requirements.add(executionRequirement);
        }
      }
    }

    for(ILifecycleMappingRequirement requirement : requirements) {
      if(!installedProviders.contains(requirement)) {
        List<IMavenDiscoveryProposal> proposals = getProposals(requirement);
        if(proposals.size() == 1) {
          addSelectedProposal(proposals.get(0));
        }
      }
    }
  }

  public IMavenDiscoveryProposal getSelectedProposal(ILifecycleMappingRequirement mojoExecutionKey) {
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

  /**
   * Calculates lifecycle mapping configuration of the specified projects. Only considers mapping metadata specified in
   * projects' pom.xml files and their parent pom.xml files. Does NOT consider mapping metadata available from installed
   * Eclipse plugins and m2e default lifecycle mapping metadata.
   */
  public static LifecycleMappingConfiguration calculate(Collection<MavenProjectInfo> projects,
      ProjectImportConfiguration importConfiguration, IProgressMonitor monitor) {
    monitor.beginTask("Analysing project execution plan", projects.size());
    
    LifecycleMappingConfiguration result = new LifecycleMappingConfiguration();

    List<MavenProjectInfo> nonErrorProjects = new ArrayList<MavenProjectInfo>();
    IMaven maven = MavenPlugin.getMaven();

    for(MavenProjectInfo projectInfo : projects) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      MavenProject mavenProject = null;
      try {
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
  
        mavenProject = executionResult.getProject();
        
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
  
        if(mavenProject != null) {
          if("pom".equals(projectInfo.getModel().getPackaging())) {
            // m2e uses a noop lifecycle mapping for packaging=pom
            List<MojoExecution> mojoExecutions = new ArrayList<MojoExecution>();
            PackagingTypeMappingConfiguration pkgConfiguration = new PackagingTypeMappingConfiguration(
                mavenProject.getPackaging(), null /*lifecycleMappingId*/);
            ProjectLifecycleMappingConfiguration configuration = new ProjectLifecycleMappingConfiguration(
                projectInfo.getLabel(), mavenProject, mojoExecutions, pkgConfiguration);
            result.addProject(projectInfo, configuration);
            nonErrorProjects.add(projectInfo);
            continue;
          }
  
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
  
          List<MappingMetadataSource> metadataSources;
          try {
            metadataSources = LifecycleMappingFactory.getProjectMetadataSources(request, mavenProject,
                LifecycleMappingFactory.getBundleMetadataSources(), mojoExecutions, true, monitor);
          } catch(LifecycleMappingConfigurationException e) {
            // could not read/parse/interpret mapping metadata configured in the pom or inherited from parent pom.
            // record the problem and continue
            log.error(e.getMessage(), e);
            continue;
          }
          
          LifecycleMappingFactory.calculateEffectiveLifecycleMappingMetadata(lifecycleResult, request, metadataSources,
              mavenProject, mojoExecutions, false);
          LifecycleMappingFactory.instantiateLifecycleMapping(lifecycleResult, mavenProject,
              lifecycleResult.getLifecycleMappingId());
          LifecycleMappingFactory.instantiateProjectConfigurators(mavenProject, lifecycleResult,
              lifecycleResult.getMojoExecutionMapping());
  
          PackagingTypeMappingConfiguration pkgConfiguration = new PackagingTypeMappingConfiguration(
              mavenProject.getPackaging(),
              isProjectSource(lifecycleResult.getLifecycleMappingMetadata()) ? lifecycleResult.getLifecycleMappingId()
                  : null);
          ProjectLifecycleMappingConfiguration configuration = new ProjectLifecycleMappingConfiguration(
              projectInfo.getLabel(), mavenProject, mojoExecutions, pkgConfiguration);
  
          if(lifecycleResult.getLifecycleMapping() != null) {
            result.addInstalledProvider(configuration.getPackagingTypeMappingConfiguration()
                .getLifecycleMappingRequirement());
          }
  
          for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : lifecycleResult
              .getMojoExecutionMapping().entrySet()) {
            MojoExecutionKey key = entry.getKey();
            List<IPluginExecutionMetadata> mapppings = entry.getValue();
            IPluginExecutionMetadata primaryMapping = null;
            if(mapppings != null && !mapppings.isEmpty()) {
              primaryMapping = mapppings.get(0);
            }
            MojoExecutionMappingConfiguration executionConfiguration = new MojoExecutionMappingConfiguration(key,
                isProjectSource(primaryMapping)? primaryMapping: null);
            configuration.addMojoExecution(executionConfiguration);
            if(primaryMapping != null) {
              switch(primaryMapping.getAction()) {
                case configurator:
                  AbstractProjectConfigurator projectConfigurator = lifecycleResult.getProjectConfigurators().get(
                      LifecycleMappingFactory.getProjectConfiguratorId(primaryMapping));
                  if(projectConfigurator != null) {
                    result.addInstalledProvider(executionConfiguration.getLifecycleMappingRequirement());
                  }
                  break;
                case error:
                case execute:
                case ignore:
                  result.addInstalledProvider(executionConfiguration.getLifecycleMappingRequirement());
                  break;
                default:
                  throw new IllegalArgumentException("Missing handling for action=" + primaryMapping.getAction());
              }
            }
          }
          result.addProject(projectInfo, configuration);
          nonErrorProjects.add(projectInfo);
        } else {
          //XXX mkleint: what shall happen now? we don't have a valid MavenProject instance to play with,
          // currently we skip such project silently, is that ok?
        }
      
      } catch (OperationCanceledException ex) {
        throw ex;
      } catch (Throwable th) {
        result.addError(projectInfo, th);
      } finally {
        if (mavenProject != null) {
          ((MavenImpl)maven).releaseExtensionsRealm(mavenProject);
        }
      }
    }

    result.setSelectedProjects(nonErrorProjects);
    
    return result;
  }

  private static boolean isProjectSource(IPluginExecutionMetadata primaryMapping) {
    if (primaryMapping == null) {
      return false;
    }
    return isProjectSource(((PluginExecutionMetadata) primaryMapping).getSource());
  }

  private static boolean isProjectSource(LifecycleMappingMetadata mappingMetadata) {
    if (mappingMetadata==null) {
      return false;
    }
    return isProjectSource(mappingMetadata.getSource());
  }

  private static boolean isProjectSource(LifecycleMappingMetadataSource metadataSource) {
    if (metadataSource == null) {
      return false;
    }
    Object source = metadataSource.getSource();
    if (source instanceof MavenProject) {
      return true;
    }
    if (source instanceof Artifact) {
      return true;
    }
    return false;
  }

  private void addInstalledProvider(ILifecycleMappingRequirement requirement) {
    installedProviders.add(requirement);
  }

  public List<ProjectLifecycleMappingConfiguration> getProjects(ILifecycleMappingElement configurationElement) {
    List<ProjectLifecycleMappingConfiguration> result = new ArrayList<ProjectLifecycleMappingConfiguration>();
    for(ProjectLifecycleMappingConfiguration project : getProjects()) {
      if(project.getMojoExecutionConfigurations().equals(configurationElement)
          || project.getMojoExecutionConfigurations().contains(configurationElement)) {
        result.add(project);
      }
    }
    return result;
  }

  public void setSelectedProjects(Collection<MavenProjectInfo> projects) {
    this.selectedProjects = new HashSet<MavenProjectInfo>(projects);
  }
  
  public void addError(MavenProjectInfo info, Throwable th) {
    errors.put(info, th);
  }
  
  public Map<MavenProjectInfo, Throwable> getErrors() {
    return errors;
  }

}
