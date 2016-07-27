/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.importer.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.commons.io.FileUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.importer.internal.MavenProjectConfigurator;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.JobHelpers;
import org.eclipse.m2e.tests.common.JobHelpers.IJobMatcher;


public class MavenImporterTest extends AbstractMavenProjectTestCase {

  private File projectDirectory;

  @Before
  public void setUp() throws IOException {
    projectDirectory = new File(Files.createTempDirectory("m2e-tests").toFile(), "example1");
    projectDirectory.mkdirs(); //$NON-NLS-1$ //$NON-NLS-2$
    copyDir(new File("resources/examples/example1"), projectDirectory);

    // Make sure projects don't have Eclipse metadata set
    new File(projectDirectory, ".project").delete();
    new File(projectDirectory, ".classpath").delete();
    new File(projectDirectory, "module1/.project").delete();
    new File(projectDirectory, "module1/.classpath").delete();
  }

  @After
  public void tearDown() throws IOException {
    FileUtils.deleteDirectory(this.projectDirectory.getParentFile());
  }

  @Test
  public void test() throws Exception {
    Set<IProject> newProjects = null;
    SmartImportJob job = new SmartImportJob(projectDirectory, Collections.EMPTY_SET, true, true);

    Map<File, List<ProjectConfigurator>> proposals = job.getImportProposals(monitor);
    Assert.assertEquals("Expected 2 projects to import", 2, proposals.size()); //$NON-NLS-1$
    boolean thymConfiguratorFound = false;
    for(ProjectConfigurator configurator : proposals.values().iterator().next()) {
      if(configurator instanceof MavenProjectConfigurator) {
        thymConfiguratorFound = true;
      }
    }
    Assert.assertTrue("Maven configurator not found while checking directory", thymConfiguratorFound); //$NON-NLS-1$

    // accept proposals
    job.setDirectoriesToImport(proposals.keySet());

    IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
    Set<IProject> beforeImport = new HashSet<>(Arrays.asList(wsRoot.getProjects()));
    job.run(monitor);
    job.join();
    newProjects = new HashSet<>(Arrays.asList(wsRoot.getProjects()));
    newProjects.removeAll(beforeImport);
    Assert.assertEquals("Expected only 2 new projects", 2, newProjects.size()); //$NON-NLS-1$

    JobHelpers.waitForJobs(new IJobMatcher() {
      public boolean matches(Job job) {
        return MavenProjectConfigurator.UPDATE_MAVEN_CONFIGURATION_JOB_NAME.equals(job.getName());
      }
    }, 30_000);

    for(IProject project : newProjects) {
      Assert.assertTrue(
          project.getLocation().toFile().getCanonicalPath().startsWith(projectDirectory.getCanonicalPath()));
      IMavenProjectFacade mavenProject = MavenPlugin.getMavenProjectRegistry().getProject(project);
      Assert.assertNotNull("Project not configured as Maven", mavenProject); //$NON-NLS-1$
    }
  }

  @Test
  public void testRootWithoutPom() throws Exception {
    Set<IProject> newProjects = null;
    // important part here is the "getParentFile()"
    SmartImportJob job = new SmartImportJob(projectDirectory.getParentFile(), Collections.EMPTY_SET, true, true);

    Map<File, List<ProjectConfigurator>> proposals = job.getImportProposals(monitor);
    Assert.assertEquals("Expected 2 projects to import", 2, proposals.size()); //$NON-NLS-1$
    boolean thymConfiguratorFound = false;
    for(ProjectConfigurator configurator : proposals.values().iterator().next()) {
      if(configurator instanceof MavenProjectConfigurator) {
        thymConfiguratorFound = true;
      }
    }
    Assert.assertTrue("Maven configurator not found while checking directory", thymConfiguratorFound); //$NON-NLS-1$

    // accept proposals
    job.setDirectoriesToImport(proposals.keySet());

    IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
    Set<IProject> beforeImport = new HashSet<>(Arrays.asList(wsRoot.getProjects()));
    job.run(monitor);
    job.join();
    newProjects = new HashSet<>(Arrays.asList(wsRoot.getProjects()));
    newProjects.removeAll(beforeImport);
    Assert.assertEquals("Expected only 2 new projects", 2, newProjects.size()); //$NON-NLS-1$

    JobHelpers.waitForJobs(new IJobMatcher() {
      public boolean matches(Job job) {
        return MavenProjectConfigurator.UPDATE_MAVEN_CONFIGURATION_JOB_NAME.equals(job.getName());
      }
    }, 30_000);

    for(IProject project : newProjects) {
      Assert.assertTrue(
          project.getLocation().toFile().getCanonicalPath().startsWith(projectDirectory.getCanonicalPath()));
      IMavenProjectFacade mavenProject = MavenPlugin.getMavenProjectRegistry().getProject(project);
      Assert.assertNotNull("Project not configured as Maven", mavenProject); //$NON-NLS-1$
    }
  }
}
