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

package org.eclipse.m2e.core.project.configurator;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenSession;

import org.sonatype.plexus.build.incremental.BuildContext;

import org.eclipse.m2e.core.internal.builder.InternalBuildParticipant;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * AbstractMavenBuildParticipant
 * 
 * @author igor
 */
public abstract class AbstractBuildParticipant extends InternalBuildParticipant {

  /**
   * This method is called during workspace full or incremental build.
   */
  public abstract Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception;

  public boolean callOnEmptyDelta() {
    return false;
  }

  /**
   * This method is called during workspace clean build.
   */
  @SuppressWarnings("unused")
  public void clean(IProgressMonitor monitor) throws CoreException {
    // default implementation does nothing
  }

  protected IMavenProjectFacade getMavenProjectFacade() {
    return super.getMavenProjectFacade();
  }

  protected IResourceDelta getDelta(IProject project) {
    return super.getDelta(project);
  }

  protected MavenSession getSession() {
    return super.getSession();
  }
  
  protected BuildContext getBuildContext() {
    return super.getBuildContext();
  }
}
