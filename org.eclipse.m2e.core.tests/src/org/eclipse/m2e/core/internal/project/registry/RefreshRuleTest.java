/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.junit.After;
import org.junit.Test;


/**
 * Tests that {@link ProjectRegistryManager#refresh(java.util.Collection, org.eclipse.core.runtime.IProgressMonitor)}
 * releases the workspace rule also when it cannot get it.
 */
public class RefreshRuleTest {

  private final IProgressMonitor monitor = new NullProgressMonitor();

  private final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("refreshRuleTest");

  @After
  public void deleteProject() throws CoreException {
    project.delete(true, monitor);
  }

  @Test
  public void testRefreshInsideProjectRuleKeepsNoRule() throws Exception {
    project.create(monitor);
    project.open(monitor);
    ProjectRegistryManager manager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();

    boolean released = false;
    try {
      Job.getJobManager().beginRule(project, monitor);
      // the workspace root is not inside the project rule, so refresh cannot get it
      try {
        manager.refresh(List.of(project.getFile("pom.xml")), monitor);
        fail("refresh must not get the workspace root inside a project rule");
      } catch(IllegalArgumentException expected) {
        // expected
      }
      // fails with "does not match most recent begin: R/" when refresh kept the root rule
      Job.getJobManager().endRule(project);
      released = true;
    } finally {
      if(!released) {
        // release what this thread still holds, so that the next tests can run
        endRuleQuietly(ResourcesPlugin.getWorkspace().getRoot());
        endRuleQuietly(project);
      }
    }
  }

  private static void endRuleQuietly(ISchedulingRule rule) {
    try {
      Job.getJobManager().endRule(rule);
    } catch(IllegalArgumentException e) {
      // the thread does not hold this rule
    }
  }
}
