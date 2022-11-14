/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Christoph Läubrich - M2Eclipse gets stuck in endless update loop
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;


/**
 * @author igor
 */
public class DependencyResolutionContext {

  /** Set of all pom files to resolve */
  private final LinkedHashSet<IFile> pomFiles;

  public DependencyResolutionContext(Collection<IFile> pomFiles) {
    this.pomFiles = new LinkedHashSet<>(pomFiles);
  }

  public synchronized boolean isEmpty() {
    return pomFiles.isEmpty();
  }

  public synchronized void forcePomFiles(Collection<IFile> pomFiles) {
    this.pomFiles.addAll(pomFiles);
  }

  public synchronized IFile pop() {
    Iterator<IFile> i = pomFiles.iterator();
    IFile pom = i.next();
    i.remove();
    return pom;
  }

  synchronized Set<IFile> getCurrent() {
    return new LinkedHashSet<>(pomFiles);
  }

  /**
   * @param file
   */
  public void forcePomFile(IFile file) {
    pomFiles.add(file);
  }

}
