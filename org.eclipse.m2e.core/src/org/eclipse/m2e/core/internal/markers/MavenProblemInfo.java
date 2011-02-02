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

package org.eclipse.m2e.core.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;


public class MavenProblemInfo {
  private final int line;

  private final String message;

  private final int severity;

  public MavenProblemInfo(int line, String message) {
    this.line = line;
    this.message = message;
    this.severity = IMarker.SEVERITY_ERROR;
  }

  public MavenProblemInfo(int line, String message, int severity) {
    this.line = line;
    this.message = message;
    this.severity = severity;
  }

  public int getLine() {
    return line;
  }

  public String getMessage() {
    return message;
  }

  public int getSeverity() {
    return this.severity;
  }

  @SuppressWarnings("unused")
  public void processMarker(IMarker marker) throws CoreException {
  }

  public String toString() {
    return message;
  }
}
