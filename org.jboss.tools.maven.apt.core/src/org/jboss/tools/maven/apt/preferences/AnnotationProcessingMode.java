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
package org.jboss.tools.maven.apt.preferences;

public enum AnnotationProcessingMode {
  disabled, jdt_apt;
  
  public static AnnotationProcessingMode getFromString(String val) {
    for (AnnotationProcessingMode mode : values() ) {
      if (mode.name().equals(val)) {
        return mode;
      }
    }
    return disabled;
  }
}