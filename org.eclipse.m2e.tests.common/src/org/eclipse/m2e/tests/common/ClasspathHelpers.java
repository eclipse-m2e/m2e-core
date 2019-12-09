/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.common;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;


/**
 * @since 1.1
 */
public class ClasspathHelpers {

  /**
   * Returns classpath entry with given path. Throws AssertionError if no such entry.
   */
  public static IClasspathEntry getClasspathEntry(IClasspathEntry[] cp, IPath path) {
    for(IClasspathEntry cpe : cp) {
      if(path.equals(cpe.getPath())) {
        return cpe;
      }
    }
    Assert.fail("Missing classpath entry " + path);
    return null;
  }

  public static IClasspathEntry getClasspathEntry(IClasspathEntry[] cp, String path) {
    return getClasspathEntry(cp, new Path(path));
  }

  /**
   * Asserts that classpath has one and only one entry with given first path segments.
   */
  public static void assertClasspathEntry(IClasspathEntry[] cp, String... segments) {
    int count = 0;
    for(IClasspathEntry cpe : cp) {
      if(startsWith(cpe.getPath(), segments)) {
        count++ ;
      }
    }
    Assert.assertEquals("Unexpected classpath with prefix " + Arrays.toString(segments), 1, count);
  }

  private static boolean startsWith(IPath path, String[] segments) {
    if(path.segmentCount() < segments.length) {
      return false;
    }
    for(int i = 0; i < segments.length; i++ ) {
      if(!segments[i].equals(path.segment(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Asserts that classpath has one and only one entry with given path.
   */
  public static void assertClasspathEntry(IClasspathEntry[] cp, IPath path) {
    int count = 0;
    for(IClasspathEntry cpe : cp) {
      if(cpe.getPath().equals(path)) {
        count++ ;
      }
    }
    Assert.assertEquals("Number of classpath entries with path " + path, 1, count);
  }

  /**
   * Asserts that classpath matches specified path regex patterns.
   */
  public static void assertClasspath(String[] expectedPathPatterns, IClasspathEntry[] cp) {
    boolean matches = false;
    if(expectedPathPatterns.length == cp.length) {
      matches = true;
      for(int i = 0; i < expectedPathPatterns.length; i++ ) {
        if(!Pattern.matches(expectedPathPatterns[i], cp[i].getPath().toPortableString())) {
          matches = false;
          break;
        }
      }
    }
    if(!matches) {
      // pretty format and fail
      StringBuilder sb_expected = new StringBuilder();
      for(String expected : expectedPathPatterns) {
        sb_expected.append(expected).append("\n");
      }
      StringBuilder sb_actual = new StringBuilder();
      for(IClasspathEntry cpe : cp) {
        sb_actual.append(cpe.getPath().toPortableString()).append("\n");
      }
      Assert.assertEquals("Unexpected classpath", sb_expected.toString(), sb_actual.toString());
    }
  }

  /**
   * @since 1.6
   */
  public static void assertClasspath(IProject project, String... expectedPatterns) throws CoreException {
    IJavaProject javaProject = JavaCore.create(project);
    Assert.assertNotNull("Is a Java project", javaProject);
    assertClasspath(expectedPatterns, javaProject.getRawClasspath());
  }

  public static IClasspathAttribute getClasspathAttribute(IClasspathEntry entry, String attributeName) {
    for(IClasspathAttribute a : entry.getExtraAttributes()) {
      if(attributeName.equals(a.getName())) {
        return a;
      }
    }
    return null;
  }
}
