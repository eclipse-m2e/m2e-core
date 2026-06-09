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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

@SuppressWarnings("restriction")
public class TestJarWorkspaceResolutionTest extends AbstractMavenProjectTestCase {

  @Test
  public void testJarWorkspaceDependencyResolution() throws Exception {
    // module-a: produces test-jar; module-b: depends on module-a:test-jar in compile scope
    IProject moduleA = importProject("projects/testJarWorkspace/module-a/pom.xml");
    IProject moduleB = importProject("projects/testJarWorkspace/module-b/pom.xml");
    waitForJobsToComplete();

    moduleA.build(IncrementalProjectBuilder.FULL_BUILD, null);
    moduleB.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    assertEquals("Module-A should have no errors", List.of(), findErrorMarkers(moduleA));
    assertEquals("Module-B should have no compilation errors", List.of(), findErrorMarkers(moduleB));

    // The workspace dependency on module-a's test-jar is in the Maven container
    IClasspathContainer containerB = BuildPathManager.getMaven2ClasspathContainer(JavaCore.create(moduleB));
    assertNotNull("Maven container must exist for module-b", containerB);
    IClasspathEntry[] containerEntries = containerB.getClasspathEntries();

    // Should have a project entry for module-a
    Optional<IClasspathEntry> projectEntry = Arrays.stream(containerEntries)
        .filter(e -> e.getEntryKind() == IClasspathEntry.CPE_PROJECT
            && e.getPath().segment(0).equals("module-a"))
        .findFirst();
    assertTrue("Module-B should have project dependency on module-a", projectEntry.isPresent());

    // The project entry must NOT have WITHOUT_TEST_CODE set — that's how test sources are exposed
    IClasspathAttribute withoutTestCode = getClasspathAttribute(projectEntry.get(),
        IClasspathManager.WITHOUT_TEST_CODE);
    assertNull("WITHOUT_TEST_CODE must not be set on test-jar project entry", withoutTestCode);
  }

  @Test
  public void testJarWithClassifierWorkspaceDependencyResolution() throws Exception {
    // <classifier>tests</classifier> instead of <type>test-jar</type>
    IProject moduleA = importProject("projects/testJarWorkspace/module-a/pom.xml");
    IProject moduleC = importProject("projects/testJarWorkspace/module-c/pom.xml");
    waitForJobsToComplete();

    moduleA.build(IncrementalProjectBuilder.FULL_BUILD, null);
    moduleC.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    assertEquals("Module-A should have no errors", List.of(), findErrorMarkers(moduleA));
    assertEquals("Module-C should have no compilation errors (classifier=tests)", List.of(),
        findErrorMarkers(moduleC));

    IClasspathContainer containerC = BuildPathManager.getMaven2ClasspathContainer(JavaCore.create(moduleC));
    assertNotNull("Maven container must exist for module-c", containerC);
    Optional<IClasspathEntry> projectEntry = Arrays.stream(containerC.getClasspathEntries())
        .filter(e -> e.getEntryKind() == IClasspathEntry.CPE_PROJECT
            && e.getPath().segment(0).equals("module-a"))
        .findFirst();
    assertTrue("Module-C should have project dependency on module-a (classifier=tests)", projectEntry.isPresent());
    assertNull("WITHOUT_TEST_CODE must not be set on classifier=tests project entry",
        getClasspathAttribute(projectEntry.get(), IClasspathManager.WITHOUT_TEST_CODE));
  }

  @Test
  public void testRegularDependencyDoesNotGetTestClasses() throws Exception {
    // Regular (non-test-jar) dependency: WITHOUT_TEST_CODE must be set, test sources hidden
    IProject moduleA = importProject("projects/testJarWorkspace/module-a/pom.xml");
    IProject moduleD = importProject("projects/testJarWorkspace/module-d/pom.xml");
    waitForJobsToComplete();

    moduleA.build(IncrementalProjectBuilder.FULL_BUILD, null);
    moduleD.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    IClasspathContainer containerD = BuildPathManager.getMaven2ClasspathContainer(JavaCore.create(moduleD));
    assertNotNull("Maven container must exist for module-d", containerD);
    IClasspathEntry[] containerEntries = containerD.getClasspathEntries();

    Optional<IClasspathEntry> projectEntry = Arrays.stream(containerEntries)
        .filter(e -> e.getEntryKind() == IClasspathEntry.CPE_PROJECT
            && e.getPath().segment(0).equals("module-a"))
        .findFirst();
    assertTrue("Module-D should have project dependency on module-a", projectEntry.isPresent());

    // Regular dependency must have WITHOUT_TEST_CODE=true so test sources stay hidden
    IClasspathAttribute withoutTestCode = getClasspathAttribute(projectEntry.get(),
        IClasspathManager.WITHOUT_TEST_CODE);
    assertTrue("WITHOUT_TEST_CODE must be set for regular dependency",
        withoutTestCode != null && "true".equals(withoutTestCode.getValue()));

    // Should also have no library entries for test-classes in the container
    boolean hasTestClassesEntry = Arrays.stream(containerEntries)
        .anyMatch(e -> e.getEntryKind() == IClasspathEntry.CPE_LIBRARY
            && e.getPath().toString().contains("module-a")
            && e.getPath().toString().endsWith("test-classes"));
    assertFalse("Module-D should NOT have test-classes library entry", hasTestClassesEntry);

    assertEquals("Module-D should have no errors", List.of(), findErrorMarkers(moduleD));
  }

  @Test
  public void testTestJarInTestScope() throws Exception {
    // test-jar dependency in test scope: project entry should have test attribute
    IProject moduleA = importProject("projects/testJarWorkspace/module-a/pom.xml");
    IProject moduleE = importProject("projects/testJarWorkspace/module-e/pom.xml");
    waitForJobsToComplete();

    moduleA.build(IncrementalProjectBuilder.FULL_BUILD, null);
    moduleE.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    assertEquals("Module-A should have no errors", List.of(), findErrorMarkers(moduleA));
    assertEquals("Module-E should have no errors (test-jar in test scope)", List.of(),
        findErrorMarkers(moduleE));

    IClasspathContainer containerE = BuildPathManager.getMaven2ClasspathContainer(JavaCore.create(moduleE));
    assertNotNull("Maven container must exist for module-e", containerE);
    IClasspathEntry[] containerEntries = containerE.getClasspathEntries();

    Optional<IClasspathEntry> projectEntry = Arrays.stream(containerEntries)
        .filter(e -> e.getEntryKind() == IClasspathEntry.CPE_PROJECT
            && e.getPath().segment(0).equals("module-a"))
        .findFirst();
    assertTrue("Module-E should have project dependency on module-a", projectEntry.isPresent());

    // Project entry should be test-only (test scope)
    IClasspathAttribute testAttr = getClasspathAttribute(projectEntry.get(), IClasspathManager.TEST_ATTRIBUTE);
    assertTrue("Test-scope test-jar entry should have test=true attribute",
        testAttr != null && "true".equals(testAttr.getValue()));

    // WITHOUT_TEST_CODE must NOT be set so test sources are accessible
    IClasspathAttribute withoutTestCode = getClasspathAttribute(projectEntry.get(),
        IClasspathManager.WITHOUT_TEST_CODE);
    assertNull("WITHOUT_TEST_CODE must not be set on test-jar project entry", withoutTestCode);
  }

  private static IClasspathAttribute getClasspathAttribute(IClasspathEntry entry, String name) {
    for(IClasspathAttribute attr : entry.getExtraAttributes()) {
      if(name.equals(attr.getName())) {
        return attr;
      }
    }
    return null;
  }
}
