/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

/**
 * Project configurator for maven-resources-plugin
 */
public class MavenResourcesProjectConfigurator extends AbstractProjectConfigurator {
  @Override
  public AbstractBuildParticipant getBuildParticipant(MojoExecution execution) {
    return new MojoExecutionBuildParticipant(execution, true /*runOnIncremental*/);
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    // do nothing
  }
}
