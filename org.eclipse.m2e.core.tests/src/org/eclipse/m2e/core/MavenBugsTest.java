/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.shared.utils.io.IOUtil;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MavenBugsTest extends AbstractMavenProjectTestCase {

  @After
  public void clearWorkspace() throws Exception {
    for(IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      p.delete(true, null);
    }
  }

  @Test
  public void testMNG6530() throws Exception {
    File sourceDirectory = new File(
        FileLocator.toFileURL(getClass().getResource("/resources/projects/testMNG6530")).toURI());
    File tempDirectory = Files.createTempDirectory(getClass().getSimpleName()).toFile();
    try {
      FileUtils.copyDirectoryStructure(sourceDirectory, tempDirectory);
      List<MavenProjectInfo> toImport = new ArrayList<>(2);
      toImport.add(new MavenProjectInfo("", new File(tempDirectory, "pom.xml"), null, null));
      toImport.add(new MavenProjectInfo("", new File(tempDirectory, "child/pom.xml"), null, null));
      MavenPlugin.getProjectConfigurationManager().importProjects(toImport, new ProjectImportConfiguration(), null,
          new NullProgressMonitor());

      IProject parent = ResourcesPlugin.getWorkspace().getRoot().getProject("testMNG6530");
      IProject child = ResourcesPlugin.getWorkspace().getRoot().getProject("child");
      try {
        IMavenProjectFacade childFacade = MavenPlugin.getMavenProjectRegistry().getProject(child);
        MavenProject mavenProject = childFacade.getMavenProject(new NullProgressMonitor());
        assertEquals("bar", mavenProject.getProperties().get("foo"));
        String content = IOUtil.toString(parent.getFile("pom.xml").getContents()).replaceAll("bar", "lol");
        parent.getFile("pom.xml").setContents(new ByteArrayInputStream(content.getBytes()), true, false, null);
        MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(child, monitor);
        waitForJobsToComplete();
        mavenProject = childFacade.getMavenProject(monitor);
        assertEquals("lol", mavenProject.getProperties().get("foo"));
      } finally {
        parent.delete(true, null);
        child.delete(true, null);
      }
    } finally {
      FileUtils.deleteDirectory(tempDirectory);
    }
  }
}
