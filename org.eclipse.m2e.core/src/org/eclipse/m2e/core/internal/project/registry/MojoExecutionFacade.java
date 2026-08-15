/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMojoExecutionFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * Default {@link IMojoExecutionFacade} implementation simply wrapping a Maven-core {@link MojoExecution} and the
 * {@link IMavenProjectFacade} it belongs to.
 */
@SuppressWarnings("deprecation")
public class MojoExecutionFacade implements IMojoExecutionFacade {

  private final IMavenProjectFacade projectFacade;

  private final MojoExecution mojoExecution;

  private final MojoExecutionKey key;

  public MojoExecutionFacade(IMavenProjectFacade projectFacade, MojoExecution mojoExecution) {
    this.projectFacade = projectFacade;
    this.mojoExecution = mojoExecution;
    this.key = new MojoExecutionKey(mojoExecution);
  }

  @Override
  public MojoExecutionKey getKey() {
    return key;
  }

  @Override
  public <T> T getMojoParameterValue(String parameter, Class<T> asType, IProgressMonitor monitor)
      throws CoreException {
    return projectFacade.getMojoParameterValue(mojoExecution, parameter, asType, monitor);
  }

  @Override
  public String toString() {
    return key.toString();
  }

}
