/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Arrays;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

@SuppressWarnings("restriction")
public class JavaConfigurationTest extends AbstractMavenProjectTestCase {

  @Test
  public void testFileChangeUpdatesJDTSettings() throws CoreException, IOException, InterruptedException {
    ((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
    setAutoBuilding(true);
    File pomFileFS = new File(FileLocator.toFileURL(getClass().getResource("/projects/compilerSettings/pom.xml")).getFile());
    IProject project = importProject(pomFileFS.getAbsolutePath());
    waitForJobsToComplete();
    IJavaProject javaProject = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
    assertEquals("1.8", javaProject.getOption(JavaCore.COMPILER_SOURCE, false));
    IFile pomFileWS = project.getFile("pom.xml");
    byte[] bytes = new byte[(int) pomFileFS.length()];
    try (InputStream stream = pomFileWS.getContents()) {
      stream.read(bytes);
    }
    String contents = new String(bytes);
    contents = contents.replace("1.8", "11");
    pomFileWS.setContents(new ByteArrayInputStream(contents.getBytes()), true, false, null);
    waitForJobsToComplete();
    assertEquals("11", javaProject.getOption(JavaCore.COMPILER_SOURCE, false));
  }

  @Test
  public void testJDTWarnings() throws CoreException, IOException, InterruptedException {
    ((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
    setAutoBuilding(true);
    File pomFileFS = new File(
        FileLocator.toFileURL(getClass().getResource("/projects/compilerWarnings/pom.xml")).getFile());
    waitForJobsToComplete();
    IProject project = importProject(pomFileFS.getAbsolutePath());
    IFile file = project.getFile("src/main/java/A.java");
    project.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();
    IMarker[] findMarkers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    assertArrayEquals(new IMarker[0], findMarkers);
  }

  @Test
  public void testSkipAllTest() throws CoreException, IOException, InterruptedException {
    ((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
    setAutoBuilding(true);
    File pomFileFS = new File(
        FileLocator.toFileURL(getClass().getResource("/projects/skipAllTest/pom.xml")).getFile());
    waitForJobsToComplete();
    IProject project = importProject(pomFileFS.getAbsolutePath());
    waitForJobsToComplete();
    IJavaProject jproject = JavaCore.create(project);
    assertTrue(Arrays.stream(jproject.getRawClasspath()).noneMatch(IClasspathEntry::isTest));
  }

  @Test
  public void testSkipTestCompilation() throws CoreException, IOException, InterruptedException {
    ((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
    setAutoBuilding(true);
    File pomFileFS = new File(
        FileLocator.toFileURL(getClass().getResource("/projects/skipTestCompilation/pom.xml")).getFile());
    waitForJobsToComplete();
    IProject project = importProject(pomFileFS.getAbsolutePath());
    waitForJobsToComplete();
    IJavaProject jproject = JavaCore.create(project);
    assertTrue(Arrays.stream(jproject.getRawClasspath())
                     .noneMatch(cp -> cp.isTest() && cp.getPath().toString().contains("test/java")));
    assertEquals(1, Arrays.stream(jproject.getRawClasspath())
                          .filter(cp -> cp.isTest() && cp.getPath().toString().contains("test/resources"))
                          .count());
  }

  @Test
  public void testSkipTestResources() throws CoreException, IOException, InterruptedException {
    ((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
    setAutoBuilding(true);
    File pomFileFS = new File(
        FileLocator.toFileURL(getClass().getResource("/projects/skipTestResources/pom.xml")).getFile());
    waitForJobsToComplete();
    IProject project = importProject(pomFileFS.getAbsolutePath());
    waitForJobsToComplete();
    IJavaProject jproject = JavaCore.create(project);
    assertTrue(Arrays.stream(jproject.getRawClasspath())
                     .noneMatch(cp -> cp.isTest() && cp.getPath().toString().contains("test/resources")));
    assertEquals(1, Arrays.stream(jproject.getRawClasspath())
                          .filter(cp -> cp.isTest() && cp.getPath().toString().contains("test/java"))
                          .count());
  }
}
