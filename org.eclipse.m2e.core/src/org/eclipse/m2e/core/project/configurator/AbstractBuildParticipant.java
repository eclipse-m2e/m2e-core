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

package org.eclipse.m2e.core.project.configurator;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
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
   * Build kind constant indicating a full build request. Value is guaranteed to match
   * {@link IncrementalProjectBuilder#FULL_BUILD}.
   *
   * @see IncrementalProjectBuilder#FULL_BUILD
   * @since 1.1
   */
  public static final int FULL_BUILD = IncrementalProjectBuilder.FULL_BUILD;

  /**
   * Build kind constant indicating an automatic build request. Value is guaranteed to match
   * {@link IncrementalProjectBuilder#AUTO_BUILD}.
   *
   * @see IncrementalProjectBuilder#AUTO_BUILD
   * @since 1.1
   */
  public static final int AUTO_BUILD = IncrementalProjectBuilder.AUTO_BUILD;

  /**
   * Build kind constant indicating an incremental build request. Value is guaranteed to match
   * {@link IncrementalProjectBuilder#INCREMENTAL_BUILD}.
   *
   * @see IncrementalProjectBuilder#INCREMENTAL_BUILD
   * @since 1.1
   */
  public static final int INCREMENTAL_BUILD = IncrementalProjectBuilder.INCREMENTAL_BUILD;

  /**
   * Build kind constant indicating a clean build request. Value is guaranteed to match
   * {@link IncrementalProjectBuilder#CLEAN_BUILD}.
   *
   * @see IncrementalProjectBuilder#CLEAN_BUILD
   * @since 1.1
   */
  public static final int CLEAN_BUILD = IncrementalProjectBuilder.CLEAN_BUILD;

  /**
   * This method is called during workspace full or incremental build.
   *
   * @param kind the kind of build being requested, {@link #FULL_BUILD}, {@link #AUTO_BUILD} or
   *          {@link #INCREMENTAL_BUILD}
   * @noreference this method is not intended to be called by the clients.
   */
  @Override
  public abstract Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception;

  @Override
  public boolean callOnEmptyDelta() {
    return false;
  }

  /**
   * This method is called during workspace clean build.
   */
  @Override
  @SuppressWarnings("unused")
  public void clean(IProgressMonitor monitor) throws CoreException {
    // default implementation does nothing
  }

  @Override
  protected IMavenProjectFacade getMavenProjectFacade() {
    return super.getMavenProjectFacade();
  }

  @Override
  protected IResourceDelta getDelta(IProject project) {
    return super.getDelta(project);
  }

  @Override
  protected MavenSession getSession() {
    return super.getSession();
  }

  @Override
  protected BuildContext getBuildContext() {
    return super.getBuildContext();
  }
}
