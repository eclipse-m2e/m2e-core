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

package org.eclipse.m2e.core.internal.project;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import org.apache.maven.execution.MavenExecutionRequest;

import org.eclipse.m2e.core.project.MavenUpdateRequest;

/**
 * @author igor
 */
public class DependencyResolutionContext {

  /** Original update request */
  private final MavenUpdateRequest request;

  /** Set of all pom files to resolve */
  private final Set<IFile> pomFiles = new LinkedHashSet<IFile>();

  /** Set of pom files to resolve regardless of their isStale() state */
  private final Set<IFile> forcedPomFiles = new HashSet<IFile>();

  /** The template request for invocations of Maven */
  private MavenExecutionRequest executionRequest;

  public DependencyResolutionContext(MavenUpdateRequest request, MavenExecutionRequest executionRequest) {
    this.request = request;
    this.pomFiles.addAll(request.getPomFiles());
    this.executionRequest = executionRequest;
  }

  public boolean isEmpty() {
    return pomFiles.isEmpty();
  }

  public void forcePomFiles(Set<IFile> pomFiles) {
    this.pomFiles.addAll(pomFiles);
    this.forcedPomFiles.addAll(pomFiles);
  }

  public MavenUpdateRequest getRequest() {
    return request;
  }

  public MavenExecutionRequest getExecutionRequest() {
    return executionRequest;
  }

  public boolean isForce(IFile pom) {
    return request.isForce() || forcedPomFiles.contains(pom);
  }

  public IFile pop() {
    Iterator<IFile> i = pomFiles.iterator();
    IFile pom = i.next();
    i.remove();
    return pom;
  }
}
