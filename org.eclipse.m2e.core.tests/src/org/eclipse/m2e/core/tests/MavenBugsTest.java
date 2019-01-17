/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(child);
        MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
        assertEquals("bar", mavenProject.getProperties().get("foo"));
        String content = IOUtil.toString(parent.getFile("pom.xml").getContents()).replaceAll("bar", "lol");
        parent.getFile("pom.xml").setContents(new ByteArrayInputStream(content.getBytes()), true, false, null);
        MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(child, monitor);
        waitForJobsToComplete();
        mavenProject = facade.getMavenProject(monitor);
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
