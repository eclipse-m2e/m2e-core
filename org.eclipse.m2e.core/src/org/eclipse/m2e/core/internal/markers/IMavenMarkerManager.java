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

package org.eclipse.m2e.core.internal.markers;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.apache.maven.execution.MavenExecutionResult;


/**
 * IMavenMarkerManager
 * 
 * @author Fred Bricon
 * @provisional This interface couples both marker management and error processing and will be refactored in a future
 *              version
 */
public interface IMavenMarkerManager {

  /**
   * Add markers to a pom file from a MavenExecutionResult.
   * 
   * @param pomFile the pom file to attach markers to.
   * @param result containing messages to be addedd as markers
   */
  public void addMarkers(IResource pomFile, String type, MavenExecutionResult result);

  /**
   * Add a Maven marker to a resource
   * 
   * @param resource : the IResource to attach the marker to.
   * @param message : the marker's message.
   * @param lineNumber : the resource line to attach the marker to.
   * @param severity : the severity of the marker.
   */
  public IMarker addMarker(IResource resource, String type, String message, int lineNumber, int severity);

  /**
   * Delete all Maven markers of the specified type (including subtypes) from an IResource
   */
  public void deleteMarkers(IResource resource, String type) throws CoreException;

  /**
   * Delete all Maven markers of the specified type from an IResource
   */
  public void deleteMarkers(IResource resource, boolean includeSubtypes, String type) throws CoreException;

  /**
   * Delete all Maven markers that have the specified type and attribute from an IResource
   */
  public void deleteMarkers(IResource resource, String type, String attrName, String attrValue) throws CoreException;

  /**
   * Transform an exception into an error marker on an IResource
   * 
   * @since 1.5
   */
  public void addErrorMarkers(IResource resource, String type, Throwable ex);

  /**
   * Transform an exception into an error marker on an IResource. This method is used by mavenarchiver and likely other
   * configurations. Removing it is binary incompatible change (but is source compatible).
   */
  public void addErrorMarkers(IResource resource, String type, Exception ex);

  void addErrorMarkers(IResource resource, String type, List<MavenProblemInfo> problems) throws CoreException;

  void addErrorMarker(IResource resource, String type, MavenProblemInfo problem);
}
