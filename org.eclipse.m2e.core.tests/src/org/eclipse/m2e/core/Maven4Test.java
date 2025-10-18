/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.After;
import org.junit.Test;

/**
 * Test case for Maven 4 support in m2e.
 * 
 * This test demonstrates that m2e currently does NOT support Maven 4 features.
 * As Maven 4 support is implemented, this test can be enhanced to verify:
 * - Model version 4.1.0 parsing
 * - Subprojects support (instead of modules)
 * - Inheritance of groupId and version
 * - Build/Consumer POM split
 * 
 * Based on real-world usage from https://github.com/jline/jline3
 * 
 * @see https://maven.apache.org/whatsnewinmaven4.html
 */
public class Maven4Test extends AbstractMavenProjectTestCase {

  @After
  public void clearWorkspace() throws Exception {
    for(IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      p.delete(true, null);
    }
  }

  /**
   * Test Maven 4 project with model version 4.1.0 and subprojects.
   * 
   * This test attempts to import a Maven 4 project that uses:
   * - Model version 4.1.0
   * - Subprojects instead of modules
   * - Inheritance of groupId and version from parent
   * 
   * Expected behavior:
   * - With current m2e (Maven 3 only): This test is expected to fail or show errors
   * - With Maven 4 support: This test should pass and projects should be imported correctly
   */
  @Test
  public void testMaven4BasicProject() throws Exception {
    // Get the Maven 4 test project
    File sourceDirectory = new File(
        FileLocator.toFileURL(getClass().getResource("/resources/projects/HelloMaven4")).toURI());
    
    assertNotNull("HelloMaven4 project directory should exist", sourceDirectory);
    assertTrue("HelloMaven4 project directory should be a directory", sourceDirectory.isDirectory());
    
    File parentPom = new File(sourceDirectory, "pom.xml");
    assertTrue("Parent pom.xml should exist", parentPom.exists());
    
    // Attempt to import the Maven 4 project
    // Note: This will likely fail with current m2e since it doesn't support Maven 4
    try {
      IProject project = importProject("resources/projects/HelloMaven4/pom.xml");
      
      // If we get here, the project was imported (unexpected with current m2e)
      assertNotNull("Parent project should be imported", project);
      
      // Check if subprojects are recognized
      // Note: With Maven 4 support, these should be imported as child projects
      IProject helloCore = ResourcesPlugin.getWorkspace().getRoot().getProject("hello-core");
      IProject helloApp = ResourcesPlugin.getWorkspace().getRoot().getProject("hello-app");
      
      // These assertions will help verify Maven 4 support when implemented
      // For now, they document what should work with Maven 4
      assertNotNull("hello-core subproject should be recognized", helloCore);
      assertNotNull("hello-app subproject should be recognized", helloApp);
      
    } catch (Exception e) {
      // Expected with current m2e - Maven 4 is not yet supported
      // This documents that m2e needs Maven 4 support
      System.err.println("Expected failure with Maven 3 based m2e: " + e.getMessage());
      
      // For now, we'll let this exception indicate Maven 4 is not supported
      // When Maven 4 support is added, this test should pass without exceptions
      fail("Maven 4 project import failed (expected with current m2e): " + e.getMessage());
    }
  }
  
  /**
   * Test that Maven 4 model version 4.1.0 is recognized.
   * 
   * This is a more basic test that just checks if the model version is parsed correctly.
   * When Maven 4 support is added, this should pass.
   */
  @Test
  public void testMaven4ModelVersion() throws Exception {
    File sourceDirectory = new File(
        FileLocator.toFileURL(getClass().getResource("/resources/projects/HelloMaven4")).toURI());
    File parentPom = new File(sourceDirectory, "pom.xml");
    
    assertTrue("Parent pom.xml should exist", parentPom.exists());
    
    // Try to read the POM
    // With Maven 4 support, this should recognize the 4.1.0 model version
    try {
      // This will likely fail with current Maven 3 based implementation
      // as it doesn't recognize model version 4.1.0
      @SuppressWarnings("unused")
      IProject project = importProject("resources/projects/HelloMaven4/pom.xml");
      
      // If we get here, Maven 4 model version was recognized
      // This indicates Maven 4 support is working
      
    } catch (Exception e) {
      // Expected with current m2e
      System.err.println("Maven 4 model version not supported yet: " + e.getMessage());
      fail("Maven 4 model version 4.1.0 is not supported (expected with current m2e): " + e.getMessage());
    }
  }
}
