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

package org.eclipse.m2e.core.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;


/**
 * Project Scanner
 *
 * @author Eugene Kuleshov
 */
public abstract class AbstractProjectScanner<T extends MavenProjectInfo> {

  private final List<T> projects = new ArrayList<>();

  private final List<Throwable> errors = new ArrayList<>();

  /**
   * Returns <code>List</code> of {@link MavenProjectInfo}
   */
  public List<T> getProjects() {
    return projects;
  }

  /**
   * Returns <code>List</code> of <code>Exception</code>
   */
  public List<Throwable> getErrors() {
    return this.errors;
  }

  protected void addProject(T mavenProjectInfo) {
    projects.add(mavenProjectInfo);
  }

  protected void addError(Throwable exception) {
    errors.add(exception);
  }

  public abstract String getDescription();

  /**
   * Execute. Monitor cancellation will propagate as a thrown {@link OperationCanceledException}, InterruptedException
   * is not an expected exception and remains for compatibility reasons and to handle unexpected thread interruptions.
   * 
   * @param monitor a progress monitor for progress and cancellation, may be null
   * @throws InterruptedException an unexpected thread interruption occurred
   */
  public abstract void run(IProgressMonitor monitor) throws InterruptedException;
}
