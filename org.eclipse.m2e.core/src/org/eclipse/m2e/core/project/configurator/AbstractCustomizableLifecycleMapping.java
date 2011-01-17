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

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Abstract base class for customizable lifecycle mappings
 * 
 * @author igor
 */
public abstract class AbstractCustomizableLifecycleMapping extends AbstractLifecycleMapping {

  protected final Map<MojoExecutionKey, Set<AbstractProjectConfigurator>> projectConfiguratorsByMojoExecution = new LinkedHashMap<MojoExecutionKey, Set<AbstractProjectConfigurator>>();

  @Override
  public List<AbstractProjectConfigurator> getProjectConfigurators(IProgressMonitor monitor) {
    Set<AbstractProjectConfigurator> allProjectConfigurators = new LinkedHashSet<AbstractProjectConfigurator>();
    for(Set<AbstractProjectConfigurator> projectConfigurators : projectConfiguratorsByMojoExecution.values()) {
      allProjectConfigurators.addAll(projectConfigurators);
    }
    return new ArrayList<AbstractProjectConfigurator>(allProjectConfigurators);
  }

  public Set<AbstractProjectConfigurator> getProjectConfiguratorsForMojoExecution(MojoExecution mojoExecution,
      IProgressMonitor monitor) {
    return projectConfiguratorsByMojoExecution.get(new MojoExecutionKey(mojoExecution));
  }

  public void initialize(IMavenProjectFacade mavenProjectFacade, List<PluginExecutionMetadata> configuration,
      Map<MojoExecutionKey, List<PluginExecutionMetadata>> mapping, IProgressMonitor monitor) throws CoreException {
    super.initialize(mavenProjectFacade, configuration, mapping, monitor);

    for(Map.Entry<MojoExecutionKey, List<PluginExecutionMetadata>> entry : mapping.entrySet()) {
      Set<AbstractProjectConfigurator> configurators = new LinkedHashSet<AbstractProjectConfigurator>();
      for(PluginExecutionMetadata pluginExecutionMetadata : entry.getValue()) {
        try {
          configurators.add(LifecycleMappingFactory.createProjectConfigurator(pluginExecutionMetadata));
        } catch(LifecycleMappingConfigurationException e) {
          addProblem(1, e.getMessage());
        }
      }
      projectConfiguratorsByMojoExecution.put(entry.getKey(), configurators);
    }
  }

}
