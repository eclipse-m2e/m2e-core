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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.project.MarkerUtils;
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

    @Override
    public void processMarker(IMarker marker) throws CoreException {
      MarkerUtils.decorateMarker(marker);
      marker.setAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, getLifecycleId());
      marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_UNKNOWN_LIFECYCLE_ID);
    }
  }

  public static class MissingLifecyclePackaging extends LifecycleMappingProblemInfo {
    private final String packaging;

    MissingLifecyclePackaging(int line, String message, String packaging) {
      super(line, message);
      this.packaging = packaging;
    }

    public String getPackaging() {
      return packaging;
    }

    @Override
    public void processMarker(IMarker marker) throws CoreException {
      marker.setAttribute(IMavenConstants.MARKER_ATTR_PACKAGING, getPackaging());
      marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, IMavenConstants.EDITOR_HINT_UNKNOWN_PACKAGING);
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

  public List<AbstractProjectConfigurator> getProjectConfigurators(IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public List<MojoExecutionKey> getNotCoveredMojoExecutions(IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public boolean isInterestingPhase(String phase) {
    return false;
  }

  public Map<MojoExecutionKey, List<AbstractBuildParticipant>> getBuildParticipants(IProgressMonitor monitor) {
    return Collections.emptyMap();
  }

  public void initializeMapping(List<MojoExecution> mojoExecutions,
      Map<MojoExecutionKey, List<PluginExecutionMetadata>> executionMapping) {
  }

  public void addMissingLifecycleExtensionPoint(int line, String message, String lifecycleId) {
    addProblem(new MissingLifecycleExtensionPoint(line, message, lifecycleId));
  }

  public void addMissingLifecyclePackaging(int line, String message, String packaging) {
    addProblem(new MissingLifecyclePackaging(line, message, packaging));
  }
}
