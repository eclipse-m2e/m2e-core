/*******************************************************************************
 * Copyright (c) 2026 Aman Poonia and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Aman Poonia - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

public class TestJarWorkspaceResolutionTest extends AbstractMavenProjectTestCase {

  @Test
  public void testJarWorkspaceDependencyResolution() throws Exception {
    // Import two projects:
    // - module-a: produces test-jar containing TestUtil class
    // - module-b: depends on module-a:test-jar and uses TestUtil in main code
    IProject moduleA = importProject("projects/testJarWorkspace/module-a/pom.xml");
    IProject moduleB = importProject("projects/testJarWorkspace/module-b/pom.xml");
    waitForJobsToComplete();

    // Build both projects
    moduleA.build(IncrementalProjectBuilder.FULL_BUILD, null);
    moduleB.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    // Verify module-a has no errors
    assertEquals("Module-A should have no errors", List.of(), findErrorMarkers(moduleA));

    // Verify module-b has no errors (key test - TestUtil should be resolvable)
    assertEquals("Module-B should have no compilation errors", List.of(), findErrorMarkers(moduleB));

    // Verify classpath contains test-classes folder from module-a
    IJavaProject javaProjectB = JavaCore.create(moduleB);
    IClasspathEntry[] classpath = javaProjectB.getRawClasspath();

    // Should have the project dependency on module-a
    Optional<IClasspathEntry> projectEntry = Arrays.stream(classpath)
        .filter(e -> e.getEntryKind() == IClasspathEntry.CPE_PROJECT
            && e.getPath().segment(0).equals("module-a"))
        .findFirst();
    assertTrue("Module-B should have project dependency on module-a", projectEntry.isPresent());

    // Should have the library entry pointing to module-a/target/test-classes
    Optional<IClasspathEntry> testClassesEntry = Arrays.stream(classpath)
        .filter(e -> e.getEntryKind() == IClasspathEntry.CPE_LIBRARY
            && e.getPath().toString().contains("module-a")
            && e.getPath().toString().endsWith("test-classes"))
        .findFirst();
    assertTrue("Module-B should have library entry for module-a/target/test-classes", testClassesEntry.isPresent());

    // Verify source attachment is configured
    if(testClassesEntry.isPresent()) {
      assertNotNull("Test-classes entry should have source attachment",
          testClassesEntry.get().getSourceAttachmentPath());
      assertTrue("Source attachment should point to src/test/java",
          testClassesEntry.get().getSourceAttachmentPath().toString().contains("src/test/java"));
    }
  }

  @Test
  public void testJarWithClassifierWorkspaceDependencyResolution() throws Exception {
    // Test with <classifier>tests</classifier> instead of <type>test-jar</type>
    IProject moduleA = importProject("projects/testJarWorkspace/module-a/pom.xml");
    IProject moduleC = importProject("projects/testJarWorkspace/module-c/pom.xml");
    waitForJobsToComplete();

    moduleA.build(IncrementalProjectBuilder.FULL_BUILD, null);
    moduleC.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    assertEquals("Module-A should have no errors", List.of(), findErrorMarkers(moduleA));
    assertEquals("Module-C should have no compilation errors (classifier=tests)", List.of(),
        findErrorMarkers(moduleC));
  }

  @Test
  public void testRegularDependencyDoesNotGetTestClasses() throws Exception {
    // Negative test: Regular (non-test-jar) dependency should NOT get test-classes folder
    IProject moduleA = importProject("projects/testJarWorkspace/module-a/pom.xml");
    IProject moduleD = importProject("projects/testJarWorkspace/module-d/pom.xml");
    waitForJobsToComplete();

    moduleA.build(IncrementalProjectBuilder.FULL_BUILD, null);
    moduleD.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    IJavaProject javaProjectD = JavaCore.create(moduleD);
    IClasspathEntry[] classpath = javaProjectD.getRawClasspath();

    // Should have project dependency on module-a
    Optional<IClasspathEntry> projectEntry = Arrays.stream(classpath)
        .filter(e -> e.getEntryKind() == IClasspathEntry.CPE_PROJECT
            && e.getPath().segment(0).equals("module-a"))
        .findFirst();
    assertTrue("Module-D should have project dependency on module-a", projectEntry.isPresent());

    // Should NOT have library entry for test-classes (negative assertion)
    boolean hasTestClassesEntry = Arrays.stream(classpath)
        .anyMatch(e -> e.getEntryKind() == IClasspathEntry.CPE_LIBRARY
            && e.getPath().toString().contains("module-a")
            && e.getPath().toString().endsWith("test-classes"));
    assertFalse("Module-D should NOT have test-classes entry for regular dependency", hasTestClassesEntry);

    // Module-D should NOT be able to access TestUtil (main code only sees main classes)
    // This is verified by the absence of compilation errors - if TestUtil were accessible,
    // the comment in module-d would be compilable code
    assertEquals("Module-D should have no errors (doesn't try to use TestUtil)", List.of(),
        findErrorMarkers(moduleD));
  }

  @Test
  public void testTestJarInTestScope() throws Exception {
    // Edge case: test-jar dependency in test scope (common pattern for test utilities)
    IProject moduleA = importProject("projects/testJarWorkspace/module-a/pom.xml");
    IProject moduleE = importProject("projects/testJarWorkspace/module-e/pom.xml");
    waitForJobsToComplete();

    moduleA.build(IncrementalProjectBuilder.FULL_BUILD, null);
    moduleE.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    assertEquals("Module-A should have no errors", List.of(), findErrorMarkers(moduleA));
    assertEquals("Module-E should have no errors (test-jar in test scope)", List.of(),
        findErrorMarkers(moduleE));

    IJavaProject javaProjectE = JavaCore.create(moduleE);
    IClasspathEntry[] classpath = javaProjectE.getRawClasspath();

    // Should have test-classes library entry marked as test
    Optional<IClasspathEntry> testClassesEntry = Arrays.stream(classpath)
        .filter(e -> e.getEntryKind() == IClasspathEntry.CPE_LIBRARY
            && e.getPath().toString().contains("module-a")
            && e.getPath().toString().endsWith("test-classes"))
        .findFirst();
    assertTrue("Module-E should have test-classes entry", testClassesEntry.isPresent());

    // Verify the entry is marked as test-only
    if(testClassesEntry.isPresent()) {
      boolean isTestEntry = Arrays.stream(testClassesEntry.get().getExtraAttributes())
          .anyMatch(attr -> "test".equals(attr.getName()) && "true".equals(attr.getValue()));
      assertTrue("Test-classes entry should be marked with test=true attribute", isTestEntry);
    }
  }

}
