/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
