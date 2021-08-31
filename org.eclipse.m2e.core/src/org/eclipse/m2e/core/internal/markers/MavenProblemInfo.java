/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingConfigurationException;


public class MavenProblemInfo {
  private final SourceLocation location;

  private String message;

  private final int severity;

  public MavenProblemInfo(int line, Throwable error) {
    this(new SourceLocation(line, 0, 0), error);
  }

  public MavenProblemInfo(SourceLocation location, Throwable error) {
    this.location = location;
    this.message = getErrorMessage(error);
    this.severity = IMarker.SEVERITY_ERROR;
  }

  public MavenProblemInfo(MavenProject mavenProject, LifecycleMappingConfigurationException error) {
    SourceLocation errorLocation = error.getLocation();
    if(errorLocation != null) {
      if(mavenProject.getFile().getAbsolutePath().equals(errorLocation.getResourcePath())) {
        this.location = new SourceLocation(errorLocation.getLineNumber(), errorLocation.getColumnStart(),
            errorLocation.getColumnEnd());
      } else {
        this.location = new SourceLocation(1, 1, 1, errorLocation);
      }
    } else {
      this.location = new SourceLocation(1, 0, 0);
    }
    this.message = error.getMessage();
    this.severity = IMarker.SEVERITY_ERROR;
  }

  public MavenProblemInfo(int line, String message) {
    //TODO
    this.location = new SourceLocation(line, 0, 0);
    this.message = message;
    this.severity = IMarker.SEVERITY_ERROR;
  }

  public MavenProblemInfo(String message, SourceLocation location) {
    this(message, IMarker.SEVERITY_ERROR, location);
  }

  public MavenProblemInfo(String message, int severity, SourceLocation location) {
    if(location == null) {
      throw new IllegalArgumentException("MarkerLocation.location cannot be null"); //$NON-NLS-1$
    }
    this.message = message;
    this.severity = severity;
    this.location = location;
  }

  protected MavenProblemInfo(SourceLocation location) {
    this(null /*message*/, IMarker.SEVERITY_ERROR, location);
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public int getSeverity() {
    return severity;
  }

  public void processMarker(IMarker marker) throws CoreException {
    marker.setAttribute(IMarker.LINE_NUMBER, location.getLineNumber());
    marker.setAttribute(IMavenConstants.MARKER_COLUMN_START, location.getColumnStart());
    marker.setAttribute(IMavenConstants.MARKER_COLUMN_END, location.getColumnEnd());
    if(location.getLinkedLocation() != null) {
      marker.setAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_PATH, location.getLinkedLocation().getResourcePath());
      marker.setAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_ID, location.getLinkedLocation().getResourceId());
      marker.setAttribute(IMavenConstants.MARKER_CAUSE_LINE_NUMBER, location.getLinkedLocation().getLineNumber());
      marker.setAttribute(IMavenConstants.MARKER_CAUSE_COLUMN_START, location.getLinkedLocation().getColumnStart());
      marker.setAttribute(IMavenConstants.MARKER_CAUSE_COLUMN_END, location.getLinkedLocation().getColumnEnd());
    }
  }

  @Override
  public String toString() {
    return message;
  }

  public SourceLocation getLocation() {
    return location;
  }

  private String getErrorMessage(Throwable e) {
    StringBuilder message = new StringBuilder();
    while(e != null) {
      if(e.getMessage() != null && message.indexOf(e.getMessage()) < 0) {
        if(message.length() > 0) {
          message.append(": ");
        }
        message.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
      }
      e = e.getCause();
    }
    return message.toString();
  }
}
