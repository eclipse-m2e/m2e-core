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
package org.eclipse.m2e.core.internal.project;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IEditorMarkerService;
import org.eclipse.m2e.core.project.IMarkerLocationService;
import org.eclipse.m2e.core.project.IMavenMarkerManager;

/**
 * MarkerUtils
 *
 * @author mkleint
 */
public class MarkerUtils {
  
  
  public static void decorateMarker(IMarker marker) {
    BundleContext context = MavenPlugin.getDefault().getBundleContext();
    ServiceReference ref = context.getServiceReference(IMarkerLocationService.class);
    IMarkerLocationService service = (IMarkerLocationService)context.getService(ref);
    if (service != null) {
      try {
        service.findLocationForMarker(marker);
      } finally {
        context.ungetService(ref);
      }
    }
  }
  
  /**
   * @param markerManager
   * @param pom
   * @param mavenProject
   * @param markerPomLoadingId
   */
  public static void addEditorHintMarkers(IMavenMarkerManager markerManager, IFile pom, MavenProject mavenProject,
      String type) {
    BundleContext context = MavenPlugin.getDefault().getBundleContext();
    ServiceReference ref = context.getServiceReference(IEditorMarkerService.class);
    IEditorMarkerService service = (IEditorMarkerService)context.getService(ref);
    if (service != null) {
      try {
        service.addEditorHintMarkers(markerManager, pom, mavenProject, type);
      } finally {
        context.ungetService(ref);
      }
    }
  }  


}
