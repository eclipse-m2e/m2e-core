/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecycle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * Invlid lifecycle mapping provides additional information about lifecycle mapping problems.
 * 
 * @author igor
 */
public class InvalidLifecycleMapping extends AbstractLifecycleMapping {

  public static class MissingLifecycleExtensionPoint extends LifecycleMappingProblemInfo {
    private final String lifecycleId;

    MissingLifecycleExtensionPoint(int line, String message, String lifecycleId) {
      super(line, message);
      this.lifecycleId = lifecycleId;
    }

    public String getLifecycleId() {
      return lifecycleId;
    }
  }

  public String getId() {
    return "invalid";
  }

  public String getName() {
    return "invalid";
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  public List<AbstractBuildParticipant> getBuildParticipants(IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators(IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public Set<AbstractProjectConfigurator> getProjectConfigurators(MojoExecution mojoExecution,
      IProgressMonitor monitor) {
    return Collections.emptySet();
  }

  public List<MojoExecution> getNotCoveredMojoExecutions(IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public boolean isInterestingPhase(String phase) {
    return false;
  }

  public void addMissingLifecycleExtensionPoint(int line, String message, String lifecycleId) {
    addProblem(new MissingLifecycleExtensionPoint(line, message, lifecycleId));
  }

  public Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipantsByMojoExecutionKey(
      IProgressMonitor monitor) throws CoreException {
    return Collections.emptyMap();
  }

  public Map<MojoExecutionKey, Set<AbstractProjectConfigurator>> getProjectConfiguratorsByMojoExecutionKey(
      IProgressMonitor monitor) throws CoreException {
    return Collections.emptyMap();
  }
}
