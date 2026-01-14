/*******************************************************************************
 * Copyright (c) 2025 Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.embedder;

import static org.junit.Assert.assertNotNull;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

/**
 * Tests for SessionScope handling in Maven embedder, particularly for issue #2084.
 * This test verifies that getConfiguredMojo properly enters and seeds SessionScope
 * when working with projects that have a .mvn folder.
 * 
 * Background: When a project has a .mvn folder, PlexusContainerManager creates a 
 * separate container for that multi-module project directory. Each container has its
 * own ClassWorld and Guice injector with separate SessionScope instances. This can
 * cause OutOfScopeException if the SessionScope is not properly managed.
 */
public class SessionScopeTest extends AbstractMavenProjectTestCase {

  /**
   * Test that getConfiguredMojo works with projects containing .mvn folder.
   * This reproduces the OutOfScopeException issue where plugin realm creation
   * may use a different SessionScope instance that needs explicit seeding.
   * 
   * The .mvn folder triggers PlexusContainerManager to create a separate container
   * for the multi-module project directory, which has its own SessionScope instance.
   * 
   * See: https://github.com/eclipse-m2e/m2e-core/issues/2084
   */
  @Test
  public void testGetConfiguredMojoWithDotMvnFolder() throws Exception {
    // Import a project with .mvn folder and maven-jar-plugin configured
    IProject project = importProject("resources/projects/sessionScopeTest/pom.xml");
    assertNotNull("Project should be created", project);
    
    waitForJobsToComplete();
    
    // Trigger a build to ensure the project is fully configured
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();
    
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    assertNotNull("Maven facade should exist", facade);
    
    // Get the jar mojo execution from the execution plan
    IMaven maven = MavenPlugin.getMaven();
    IMavenExecutionContext context = facade.createExecutionContext();
    
    context.execute((ctx, monitor) -> {
      MavenSession session = ctx.getSession();
      assertNotNull("Session should exist", session);
      
      // Get the execution plan for the package phase which includes jar:jar goal
      MavenExecutionPlan plan = maven.calculateExecutionPlan(facade.getMavenProject(), 
          java.util.List.of("package"), false, monitor);
      assertNotNull("Execution plan should exist", plan);
      
      // Find the jar:jar mojo execution
      MojoExecution jarExecution = null;
      for (MojoExecution execution : plan.getMojoExecutions()) {
        if ("maven-jar-plugin".equals(execution.getArtifactId()) && "jar".equals(execution.getGoal())) {
          jarExecution = execution;
          break;
        }
      }
      
      assertNotNull("jar:jar execution should exist in plan", jarExecution);
      
      // This call should NOT throw OutOfScopeException
      // Before the fix, this would fail with:
      // "OutOfScopeException: Cannot access session scope outside of a scoping block"
      Mojo configuredMojo = maven.getConfiguredMojo(session, jarExecution, Mojo.class);
      assertNotNull("Configured mojo should be created", configuredMojo);
      
      // Clean up
      maven.releaseMojo(configuredMojo, jarExecution);
      
      return null;
    }, monitor);
  }
  
  /**
   * Test that getConfiguredMojo also works with projects without .mvn folder
   * to ensure the fix doesn't break existing functionality.
   */
  @Test
  public void testGetConfiguredMojoWithoutDotMvnFolder() throws Exception {
    // Use an existing simple project without .mvn folder
    IProject project = importProject("resources/projects/simplePomOK/pom.xml");
    assertNotNull("Project should be created", project);
    
    waitForJobsToComplete();
    
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    assertNotNull("Maven facade should exist", facade);
    
    // Verify that getConfiguredMojo still works for normal projects
    IMaven maven = MavenPlugin.getMaven();
    IMavenExecutionContext context = facade.createExecutionContext();
    
    context.execute((ctx, monitor) -> {
      MavenSession session = ctx.getSession();
      assertNotNull("Session should exist", session);
      
      // This should work without issues both before and after the fix
      return null;
    }, monitor);
  }
}
