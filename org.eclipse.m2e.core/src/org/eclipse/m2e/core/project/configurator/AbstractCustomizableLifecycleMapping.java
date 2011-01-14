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

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;


/**
 * Abstract base class for customizable lifecycle mappings
 * 
 * @author igor
 */
public abstract class AbstractCustomizableLifecycleMapping extends AbstractLifecycleMapping {

  protected Map<MojoExecutionKey, Set<AbstractProjectConfigurator>> projectConfiguratorsByMojoExecution;

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

  /**
   * @param projectConfiguratorsByMojoExecution2
   */
  public void setProjectConfiguratorsByMojoExecution(
      Map<MojoExecutionKey, Set<AbstractProjectConfigurator>> projectConfiguratorsByMojoExecution) {
    this.projectConfiguratorsByMojoExecution = new LinkedHashMap<MojoExecutionKey, Set<AbstractProjectConfigurator>>(
        projectConfiguratorsByMojoExecution);
  }

}
