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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionFilter;


/**
 * @author igor
 */
public class CustomLifecycleMapping extends AbstractCustomizableLifecycleMapping {

  protected void loadProjectConfigurators(IProgressMonitor monitor) throws CoreException {
    allProjectConfigurators = new LinkedHashSet<AbstractProjectConfigurator>();
    projectConfiguratorsByMojoExecution = new LinkedHashMap<MojoExecutionKey, Set<AbstractProjectConfigurator>>();

    MavenExecutionPlan executionPlan = getMavenProjectFacade().getExecutionPlan(monitor);
    for(MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
      MojoExecutionKey key = new MojoExecutionKey(mojoExecution);
      Set<AbstractProjectConfigurator> projectConfiguratorsForMojoExecution = new LinkedHashSet<AbstractProjectConfigurator>();
      projectConfiguratorsByMojoExecution.put(key, projectConfiguratorsForMojoExecution);

      // Look only in the plugin executions explicitly declared in the lifecycle mapping customization 
      AbstractProjectConfigurator projectConfigurator = getProjectConfigurator(customPluginExecutionMetadataList,
          mojoExecution);
      if(projectConfigurator == null) {
        continue;
      }

      // Re-use project configurators
      boolean isNewProjectConfigurator = true;
      for(AbstractProjectConfigurator otherProjectConfigurator : allProjectConfigurators) {
        if(projectConfigurator.equals(otherProjectConfigurator)) {
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
}
