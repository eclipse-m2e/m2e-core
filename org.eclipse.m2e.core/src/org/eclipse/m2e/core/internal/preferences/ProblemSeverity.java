/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.preferences;

import org.eclipse.core.resources.IMarker;


/**
 * Problem Severity enum mapping {@link IMarker} severities
 *
 * @author Fred Bricon
 * @since 1.5.0
 */
public enum ProblemSeverity {

  ignore(-1), info(IMarker.SEVERITY_INFO), warning(IMarker.SEVERITY_WARNING), error(IMarker.SEVERITY_ERROR);

  private int severity;

  ProblemSeverity(int severity) {
    this.severity = severity;
  }

  /**
   * Returns matching {@link IMarker} severity value or -1 if ignore
   * 
   * @see {@link IMarker#SEVERITY}
   */
  public int getSeverity() {
    return severity;
  }

  public static ProblemSeverity get(String value) {
    try {
      if(value != null) {
        return ProblemSeverity.valueOf(value);
      }
    } catch(IllegalArgumentException iae) {
    }
    return ProblemSeverity.error;
  }
}
