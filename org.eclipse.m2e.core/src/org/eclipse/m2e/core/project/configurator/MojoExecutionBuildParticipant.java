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

  public MojoExecutionBuildParticipant(MojoExecution execution, boolean runOnIncremental) {
    this.execution = execution;
    this.runOnIncremental = runOnIncremental;
  }

  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    if(appliesToBuildKind(kind)) {
      IMaven maven = MavenPlugin.getMaven();

      maven.execute(getSession(), getMojoExecution(), monitor);
    }
    return null;
  }

  public boolean appliesToBuildKind(int kind) {
    if(FULL_BUILD == kind || CLEAN_BUILD == kind) {
      return true;
    }
    if(PRECONFIGURE_BUILD == kind && runOnIncremental) {
      return true;
    }
    if(INCREMENTAL_BUILD == kind) {
      return runOnIncremental;
    }
    return false;
  }

  public MojoExecution getMojoExecution() {
    return execution;
  }

}
