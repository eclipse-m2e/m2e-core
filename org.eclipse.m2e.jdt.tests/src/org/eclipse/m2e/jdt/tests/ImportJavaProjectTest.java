/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.project.MavenProjectConfigurator;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ImportJavaProjectTest extends AbstractMavenProjectTestCase {

  private File parentproject;

  @Override
@Before
  public void setUp() throws IOException {
    parentproject = createProjectDirectory("projects/parentproject", "parentproject");
  }

  private File createProjectDirectory(String parent, String dir) throws IOException {
	File file = new File(Files.createTempDirectory("m2e-tests").toFile(), dir);
	file.mkdirs();
    copyDir(new File(parent), file);

    // Make sure projects don't have Eclipse metadata set
    new File(file, ".project").delete();
    new File(file, ".classpath").delete();
    new File(file, "app/.project").delete();
    new File(file, "core/.classpath").delete();
    return file;
  }

  @Override
@After
  public void tearDown() throws IOException {
    FileUtils.deleteDirectory(this.parentproject.getParentFile());
  }
 
  @Test
  public void testExecution() throws Exception {
    SmartImportJob job = new SmartImportJob(parentproject, Collections.emptySet(), true, true);
    Map<File, List<ProjectConfigurator>> proposals = job.getImportProposals(monitor);
    Assert.assertEquals("Expected 3 projects to import", 3, proposals.size()); //$NON-NLS-1$
    boolean mavenConfiguratorFound = false;
    for(ProjectConfigurator configurator : proposals.values().iterator().next()) {
      if(configurator instanceof MavenProjectConfigurator) {
        mavenConfiguratorFound = true;
      }
    }
    Assert.assertTrue("Maven configurator not found while checking directory", mavenConfiguratorFound); //$NON-NLS-1$
    // accept proposals
    job.setDirectoriesToImport(proposals.keySet());
    IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
    Set<IProject> beforeImport = new HashSet<>(Arrays.asList(wsRoot.getProjects()));
    job.run(monitor);
    job.join();
    HashSet<IProject> projects = new HashSet<>(Arrays.asList(wsRoot.getProjects()));
    projects.removeAll(beforeImport);
    Assert.assertEquals("Expected only 3 new projects", 3, projects.size()); //$NON-NLS-1$
    for(IProject project : projects) {
      Assert.assertTrue(
          project.getLocation().toFile().getCanonicalPath().startsWith(parentproject.getCanonicalPath()));
      refreshMavenProject(project);
      waitForJobsToComplete();
      IMavenProjectFacade mavenProject = MavenPlugin.getMavenProjectRegistry().getProject(project);
      Assert.assertNotNull("Project not configured as Maven", mavenProject); //$NON-NLS-1$
    }
    IProject core = wsRoot.getProject("core");
    assertTrue(core.exists());
    IJavaProject javaProject = JavaCore.create(core);
    IClasspathEntry[] entries = javaProject.getRawClasspath();
    for (IClasspathEntry entry:entries) {
      if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && "/core/src/main/java".equals(entry.getPath().toString()) ) {
        IPath[] exclusion = entry.getExclusionPatterns();
        assertTrue(exclusion == null || exclusion.length == 0);
      }
    }
  }
}
