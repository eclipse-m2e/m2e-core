/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Project Scanner
 *
 * @author Eugene Kuleshov
 */
public abstract class AbstractProjectScanner<T extends MavenProjectInfo> {

  private final List<T> projects = new ArrayList<T>();
  private final List<Throwable> errors = new ArrayList<Throwable>();
  
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

  public abstract void run(IProgressMonitor monitor) throws InterruptedException;
}
