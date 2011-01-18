/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;


/**
 * NoopLifecycleMapping
 * 
 * @author igor
 */
public class NoopLifecycleMapping implements ILifecycleMapping {

  public String getId() {
    return "noop";
  }

  public String getName() {
    return "noop";
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators(IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public Set<AbstractProjectConfigurator> getProjectConfigurators(MojoExecution mojoExecution, IProgressMonitor monitor) {
    return Collections.emptySet();
  }

  public List<MojoExecution> getNotCoveredMojoExecutions(IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public boolean isInterestingPhase(String phase) {
    return false;
  }

  public Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipants(IProgressMonitor monitor) {
    return Collections.emptyMap();
  }

  public Map<MojoExecutionKey, Set<AbstractProjectConfigurator>> getProjectConfiguratorsByMojoExecutionKey(
      IProgressMonitor monitor) {
    return Collections.emptyMap();
  }
}
