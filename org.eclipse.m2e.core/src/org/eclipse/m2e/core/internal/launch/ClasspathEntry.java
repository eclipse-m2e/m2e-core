/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.launch;



/**
 * @since 1.5
 */
public abstract class ClasspathEntry {
  public String toExternalForm() {
    if(this instanceof ProjectClasspathEntry) {
      return "P/" + ((ProjectClasspathEntry) this).getProject();
    }
    throw new IllegalArgumentException();
  }

  public static ClasspathEntry fromExternalForm(String str) {
    if(str.startsWith("P/")) {
      return new ProjectClasspathEntry(str.substring(2));
    }
    return null;
  }
}
