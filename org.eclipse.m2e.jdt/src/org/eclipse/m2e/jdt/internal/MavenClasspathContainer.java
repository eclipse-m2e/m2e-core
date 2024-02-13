/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.jdt.internal;

import java.io.Serializable;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;


/**
 * Maven classpath container
 */
public class MavenClasspathContainer implements IClasspathContainer, Serializable {
  private static final long serialVersionUID = -5976726121300869771L;

  private final IClasspathEntry[] entries;

  private final IPath path;

  public MavenClasspathContainer(IPath path, IClasspathEntry[] entries) {
    this.path = path;
    this.entries = entries;
  }

  @Override
  public String getDescription() {
    return Messages.MavenClasspathContainer_description;
  }

  @Override
  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  @Override
  public synchronized IClasspathEntry[] getClasspathEntries() {
    return entries;
  }

  @Override
  public IPath getPath() {
    return path;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "@" + System.identityHashCode(this) + "{path=" + path + "}";
  }
}
