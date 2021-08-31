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
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;


/**
 * MojoExecutionBuildParticipant
 *
 * @author igor
 */
public class MojoExecutionBuildParticipant extends AbstractBuildParticipant2 {

  private final MojoExecution execution;

  private final boolean runOnIncremental;

  private final boolean runOnConfiguration;

  public MojoExecutionBuildParticipant(MojoExecution execution, boolean runOnIncremental) {
    this(execution, runOnIncremental, false);
  }

  public MojoExecutionBuildParticipant(MojoExecution execution, boolean runOnIncremental, boolean runOnConfiguration) {
    this.execution = execution;
    this.runOnIncremental = runOnIncremental;
    this.runOnConfiguration = runOnConfiguration;
  }

  @Override
  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    if(appliesToBuildKind(kind)) {
      IMaven maven = MavenPlugin.getMaven();

      maven.execute(getMavenProjectFacade().getMavenProject(), getMojoExecution(), monitor);
    }
    return null;
  }

  public boolean appliesToBuildKind(int kind) {
    if(PRECONFIGURE_BUILD == kind) {
      return runOnConfiguration;
    }
    if(INCREMENTAL_BUILD == kind || AUTO_BUILD == kind) {
      return runOnIncremental;
    }
    if(FULL_BUILD == kind || CLEAN_BUILD == kind) {
      return true;
    }
    return false;
  }

  public MojoExecution getMojoExecution() {
    return execution;
  }

}
