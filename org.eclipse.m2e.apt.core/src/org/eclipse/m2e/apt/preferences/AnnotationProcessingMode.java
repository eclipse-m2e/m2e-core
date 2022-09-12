/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.apt.preferences;

public enum AnnotationProcessingMode {
  disabled, jdt_apt, maven_execution;

  public static AnnotationProcessingMode getFromString(String val) {
    AnnotationProcessingMode mode = getFromStringOrNull(val);
    return mode == null ? disabled : mode;
  }

  public static AnnotationProcessingMode getFromStringOrNull(String val) {
    if(val != null) {
      for(AnnotationProcessingMode mode : values()) {
        if(mode.name().equals(val)) {
          return mode;
        }
      }
    }
    return null;
  }

}
