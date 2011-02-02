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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
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

  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade project, IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public List<MojoExecutionKey> getNotCoveredMojoExecutions(IMavenProjectFacade project, IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public boolean isInterestingPhase(String phase) {
    return false;
  }

  public Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipants(IMavenProjectFacade project,
      IProgressMonitor monitor) {
    return Collections.emptyMap();
  }

  public void initializeMapping(List<MojoExecution> mojoExecutions,
      Map<MojoExecutionKey, List<PluginExecutionMetadata>> executionMapping) {
  }

  public boolean hasLifecycleMappingChanged(IMavenProjectFacade oldFacade, IMavenProjectFacade newFacade,
      IProgressMonitor monitor) {
    return false;
  }

  public List<MavenProblemInfo> validateLifecycleMapping(final IMavenProjectFacade mavenProjectFacade) {
    MavenProblemInfo problem = new MavenProblemInfo(1, NLS.bind(Messages.LifecycleMissing,
        mavenProjectFacade.getPackaging()), IMarker.SEVERITY_ERROR) {
      public void processMarker(IMarker marker) throws CoreException {
        marker.setAttribute(IMavenConstants.MARKER_ATTR_PACKAGING, mavenProjectFacade.getPackaging());
        marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_UNKNOWN_PACKAGING);
      }
    };
    return Arrays.asList(problem);
  }
}
