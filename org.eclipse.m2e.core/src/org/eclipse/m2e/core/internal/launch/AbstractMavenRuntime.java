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

import java.util.List;

import org.eclipse.m2e.core.embedder.MavenRuntime;


/**
 * @since 1.5
 */
public abstract class AbstractMavenRuntime implements MavenRuntime {

  private final String name;

  @Deprecated
  protected AbstractMavenRuntime() {
    this.name = null;
  }

  protected AbstractMavenRuntime(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name != null ? name : getLocation();
  }

  public List<ClasspathEntry> getExtensions() {
    return null;
  }

  public boolean isLegacy() {
    return name == null;
  }
}
