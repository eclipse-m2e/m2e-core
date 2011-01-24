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
package org.eclipse.m2e.core.project.configurator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

public class LifecycleMappingProblemInfo {
  private final int line;

  private final String message;

  private final int severity;

  protected LifecycleMappingProblemInfo(int line, String message) {
    this.line = line;
    this.message = message;
    this.severity = IMarker.SEVERITY_ERROR;
  }

  protected LifecycleMappingProblemInfo(int line, String message, int severity) {
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

  public void processMarker(IMarker marker) throws CoreException {
  }
}