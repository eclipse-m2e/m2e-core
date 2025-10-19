/********************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.internal.maven.compat;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * Facade for {@link LifecycleExecutionPlanCalculator} to avoid direct usage that might change in Maven 4.
 * This facade wraps the calculator and provides delegate methods for the functionality m2e actually uses.
 */
public class LifecycleExecutionPlanCalculatorFacade {

  private final LifecycleExecutionPlanCalculator delegate;

  public LifecycleExecutionPlanCalculatorFacade(LifecycleExecutionPlanCalculator delegate) {
    this.delegate = delegate;
  }

  /**
   * Sets up the MojoExecution with its configuration from the project's POM.
   * 
   * @param session the Maven session
   * @param project the Maven project
   * @param mojoExecution the mojo execution to setup
   * @throws Exception if setup fails
   */
  public void setupMojoExecution(MavenSession session, MavenProject project, MojoExecution mojoExecution)
      throws Exception {
    delegate.setupMojoExecution(session, project, mojoExecution);
  }
}
