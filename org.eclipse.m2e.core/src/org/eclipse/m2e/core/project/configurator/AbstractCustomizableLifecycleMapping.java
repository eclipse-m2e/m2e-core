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
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Abstract base class for customizable lifecycle mappings
 * 
 * @author igor
 */
public abstract class AbstractCustomizableLifecycleMapping extends AbstractLifecycleMapping {

  protected static class MojoExecutionKey {
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

  protected static AbstractProjectConfigurator getProjectConfigurator(
      List<PluginExecutionMetadata> pluginExecutionMetadataList,
      MojoExecution mojoExecution) {
    for(PluginExecutionMetadata pluginExecutionMetadata : pluginExecutionMetadataList) {
      if(pluginExecutionMetadata.getFilter().match(mojoExecution)) {
        return LifecycleMappingFactory.createProjectConfigurator(pluginExecutionMetadata);
      }
    }

    return null;
  }

  protected Set<AbstractProjectConfigurator> allProjectConfigurators;

  protected Map<MojoExecutionKey, Set<AbstractProjectConfigurator>> projectConfiguratorsByMojoExecution;

  protected void loadProjectConfigurators(IProgressMonitor monitor)
      throws CoreException {
    allProjectConfigurators = new LinkedHashSet<AbstractProjectConfigurator>();
    projectConfiguratorsByMojoExecution = new LinkedHashMap<MojoExecutionKey, Set<AbstractProjectConfigurator>>();

    MavenExecutionPlan executionPlan = getMavenProjectFacade().getExecutionPlan(monitor);
    for(MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
      MojoExecutionKey key = new MojoExecutionKey(mojoExecution);
      Set<AbstractProjectConfigurator> projectConfiguratorsForMojoExecution = new LinkedHashSet<AbstractProjectConfigurator>();
      projectConfiguratorsByMojoExecution.put(key, projectConfiguratorsForMojoExecution);

      // Look in the plugin executions explicitly declared in the lifecycle mapping customization 
      AbstractProjectConfigurator projectConfigurator = getProjectConfigurator(customPluginExecutionMetadataList,
          mojoExecution);

      // Look in the plugin executions declared in any lifecycle metadata sources (embedded or referenced)
      if(projectConfigurator == null) {
        projectConfigurator = LifecycleMappingFactory.createProjectConfiguratorFromMetadataSources(
            getMavenProjectFacade(), mojoExecution);
      }

      // Look in the plugin executions declared in the lifecycle metadata eclipse extension
      if(projectConfigurator == null) {
        projectConfigurator = getProjectConfigurator(eclipseExtensionPluginExecutionMetadataList, mojoExecution);
      }

      // Look in the plugin executions declared in project configurator eclipse extensions and the default lifecycle metadata
      if(projectConfigurator == null) {
        projectConfigurator = LifecycleMappingFactory.createProjectConfiguratorFor(getMavenProjectFacade(),
            mojoExecution);
      }

      if(projectConfigurator == null) {
        continue;
      }

      // Re-use project configurators
      boolean isNewProjectConfigurator  = true;
      for (AbstractProjectConfigurator otherProjectConfigurator : allProjectConfigurators) {
        if (projectConfigurator.equals(otherProjectConfigurator)) {
          isNewProjectConfigurator = false;
          
          for(PluginExecutionFilter filter : projectConfigurator.getPluginExecutionFilters()) {
            otherProjectConfigurator.addPluginExecutionFilter(filter);
          }
          
          projectConfiguratorsForMojoExecution.add(otherProjectConfigurator);
          
          break;
        }
      }
      if(isNewProjectConfigurator) {
        projectConfiguratorsForMojoExecution.add(projectConfigurator);
        allProjectConfigurators.add(projectConfigurator);
      }
    }
  }

  @Override
  public List<AbstractProjectConfigurator> getProjectConfigurators(IProgressMonitor monitor) throws CoreException {
    List<AbstractProjectConfigurator> result = new ArrayList<AbstractProjectConfigurator>();
    result.addAll(allProjectConfigurators);
    return result;
  }

  protected List<PluginExecutionMetadata> eclipseExtensionPluginExecutionMetadataList = new ArrayList<PluginExecutionMetadata>();

  protected List<PluginExecutionMetadata> customPluginExecutionMetadataList = new ArrayList<PluginExecutionMetadata>();

  public void addEclipseExtensionPluginExecutionMetadata(PluginExecutionMetadata pluginExecutionMetadata) {
    //TODO Detect conflicts
    eclipseExtensionPluginExecutionMetadataList.add(pluginExecutionMetadata);
  }

  public void addCustomPluginExecutionMetadata(PluginExecutionMetadata pluginExecutionMetadata) {
    //TODO Detect conflicts
    customPluginExecutionMetadataList.add(pluginExecutionMetadata);
  }

  @Override
  public void initialize(IMavenProjectFacade mavenProjectFacade, IProgressMonitor monitor) throws CoreException {
    super.initialize(mavenProjectFacade, monitor);
    loadProjectConfigurators(monitor);
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.configurator.ILifecycleMapping#getProjectConfiguratorsForMojoExecution(org.apache.maven.plugin.MojoExecution, org.eclipse.core.runtime.IProgressMonitor)
   */
  public Set<AbstractProjectConfigurator> getProjectConfiguratorsForMojoExecution(MojoExecution mojoExecution,
      IProgressMonitor monitor) throws CoreException {
    return projectConfiguratorsByMojoExecution.get(new MojoExecutionKey(mojoExecution));
  }
}
