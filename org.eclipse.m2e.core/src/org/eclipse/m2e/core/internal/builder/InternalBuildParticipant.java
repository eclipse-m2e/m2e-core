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

package org.eclipse.m2e.core.internal.builder;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenSession;

import org.sonatype.plexus.build.incremental.BuildContext;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


public abstract class InternalBuildParticipant {

  private IMavenProjectFacade facade;

  private DeltaProvider getDeltaCallback;

  private MavenSession session;

  private BuildContext buildContext;

  protected IMavenProjectFacade getMavenProjectFacade() {
    return facade;
  }

  void setMavenProjectFacade(IMavenProjectFacade facade) {
    this.facade = facade;
  }

  protected IResourceDelta getDelta(IProject project) {
    return getDeltaCallback.getDelta(project);
  }

  void setGetDeltaCallback(DeltaProvider getDeltaCallback) {
    this.getDeltaCallback = getDeltaCallback;
  }

  protected MavenSession getSession() {
    return session;
  }

  void setSession(MavenSession session) {
    this.session = session;
  }

  public abstract Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception;

  @SuppressWarnings("unused")
  public void clean(IProgressMonitor monitor) throws CoreException {
    // default implementation does nothing
  }

  public abstract boolean callOnEmptyDelta();

  void setBuildContext(BuildContext buildContext) {
    this.buildContext = buildContext;
  }

  protected BuildContext getBuildContext() {
    return buildContext;
  }
}
