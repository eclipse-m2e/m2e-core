/*******************************************************************************
 * Copyright (c) 2010-2018 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - added getMojoExecution(IMarker)
 *******************************************************************************/

package org.eclipse.m2e.core.internal.markers;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * MarkerUtils
 * 
 * @author mkleint
 */
public class MarkerUtils {
  private static Logger log = LoggerFactory.getLogger(MarkerUtils.class);

  public static void decorateMarker(IMarker marker) {
    BundleContext context = MavenPluginActivator.getDefault().getBundleContext();
    ServiceReference<IMarkerLocationService> ref = context.getServiceReference(IMarkerLocationService.class);
    if(ref == null) {
      log.warn("Could not find OSGI service for " + IMarkerLocationService.class.getName());
      return;
    }
    IMarkerLocationService service = context.getService(ref);
    if(service != null) {
      try {
        service.findLocationForMarker(marker);
      } finally {
        context.ungetService(ref);
      }
    }
  }

  public static void addEditorHintMarkers(IMavenMarkerManager markerManager, IFile pom, MavenProject mavenProject,
      String type) {
    BundleContext context = MavenPluginActivator.getDefault().getBundleContext();
    ServiceReference<IEditorMarkerService> ref = context.getServiceReference(IEditorMarkerService.class);
    if(ref == null) {
      log.warn("Could not find OSGI service for " + IEditorMarkerService.class.getName());
      return;
    }
    IEditorMarkerService service = context.getService(ref);
    if(service != null) {
      try {
        service.addEditorHintMarkers(markerManager, pom, mavenProject, type);
      } finally {
        context.ungetService(ref);
      }
    }
  }

  /**
   * Returns the {@link MojoExecutionKey} bound to an {@link IMarker}, or null if one of the groupId, artifactId,
   * executionId or goal attribute is missing.
   * 
   * @since 1.5.0
   */
  public static MojoExecutionKey getMojoExecution(IMarker marker) {
    if(marker == null) {
      return null;
    }
    // TODO Which of these are actually required?
    String groupId = marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, null);
    String artifactId = marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, null);
    String executionId = marker.getAttribute(IMavenConstants.MARKER_ATTR_EXECUTION_ID, null);
    String version = marker.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, null);
    String goal = marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, null);
    String lifecyclePhase = marker.getAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, null);
    if(goal != null && executionId != null && artifactId != null && groupId != null) {
      return new MojoExecutionKey(groupId, artifactId, version, goal, lifecyclePhase, executionId);
    }
    return null;
  }

}
