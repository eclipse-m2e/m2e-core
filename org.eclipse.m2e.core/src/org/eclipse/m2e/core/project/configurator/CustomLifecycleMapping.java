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
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * @author igor
 */
public class CustomLifecycleMapping extends AbstractLifecycleMapping {

  private List<PluginExecutionMetadata> configuration;

  public Set<AbstractProjectConfigurator> getProjectConfigurators(MojoExecution mojoExecution,
      IProgressMonitor monitor) {
    LinkedHashSet<AbstractProjectConfigurator> configurators = new LinkedHashSet<AbstractProjectConfigurator>();
    for(PluginExecutionMetadata metadata : configuration) {
      if(metadata.getFilter().match(mojoExecution)) {
        configurators.add(LifecycleMappingFactory.createProjectConfigurator(metadata));
      }
    }
    return configurators;
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators(IProgressMonitor monitor) {
    LinkedHashSet<AbstractProjectConfigurator> configurators = new LinkedHashSet<AbstractProjectConfigurator>();
    for(PluginExecutionMetadata metadata : configuration) {
      configurators.add(LifecycleMappingFactory.createProjectConfigurator(metadata));
    }
    return new ArrayList<AbstractProjectConfigurator>(configurators);
  }

  public void initialize(IMavenProjectFacade mavenProjectFacade, List<PluginExecutionMetadata> configuration,
      Map<MojoExecutionKey, List<PluginExecutionMetadata>> mapping, IProgressMonitor monitor) throws CoreException {
    this.configuration = configuration;
    super.initialize(mavenProjectFacade, configuration, mapping, monitor);
  }

  public Map<MojoExecutionKey, Set<AbstractProjectConfigurator>> getProjectConfiguratorsByMojoExecutionKey(
      IProgressMonitor monitor) throws CoreException {
    Map<MojoExecutionKey, Set<AbstractProjectConfigurator>> result = new LinkedHashMap<MojoExecutionKey, Set<AbstractProjectConfigurator>>();

    MavenExecutionPlan executionPlan = getMavenProjectFacade().getExecutionPlan(monitor);
    for(MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
      Set<AbstractProjectConfigurator> projectConfigurators = getProjectConfigurators(mojoExecution,
          monitor);
      if(projectConfigurators == null || projectConfigurators.isEmpty()) {
        continue;
      }
      result.put(new MojoExecutionKey(mojoExecution), projectConfigurators);
    }
    return result;
  }
}
