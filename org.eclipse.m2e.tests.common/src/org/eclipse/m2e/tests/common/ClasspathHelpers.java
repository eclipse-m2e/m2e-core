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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;


/**
 * @since 1.1
 */
public class ClasspathHelpers {

  public static final String JRE_CONTAINER = "org.eclipse.jdt.launching.JRE_CONTAINER.*";

  public static final String M2E_CONTAINER = "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER";

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
    return getClasspathEntry(cp, IPath.fromOSString(path));
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
   * 
   * @return a mapping from pattern to classpath entry in the order they appear in the classpath
   */
  public static Map<String, IClasspathEntry> assertClasspath(String[] expectedPathPatterns, IClasspathEntry[] entries) {
    Map<String, IClasspathEntry> matchingEntry = new LinkedHashMap<>(expectedPathPatterns.length);
    Set<String> unmatched = new LinkedHashSet<>(Arrays.asList(expectedPathPatterns));
    for(IClasspathEntry entry : entries) {
      String path = entry.getPath().toPortableString();
      for(String pattern : expectedPathPatterns) {
        if(Pattern.matches(pattern, path)) {
          matchingEntry.put(pattern, entry);
          unmatched.remove(pattern);
          break;
        }
      }
    }
    if(unmatched.isEmpty()) {
      //everything is fine!
      return matchingEntry;
    }
    // pretty format and fail
    StringBuilder sb_expected = new StringBuilder();
    for(String expected : unmatched) {
      sb_expected.append(expected).append("\n");
    }
    StringBuilder sb_actual = new StringBuilder();
    for(IClasspathEntry cpe : entries) {
      sb_actual.append(cpe.getPath().toPortableString()).append("\n");
    }
    Assert.fail("The following entries could not be matched to the classpath: \n" + sb_expected.toString()
        + "\nThe actual classpath is:\n" + sb_actual.toString());
    return Map.of();
  }

  /**
   * @since 1.6
   */
  public static Map<String, IClasspathEntry> assertClasspath(IProject project, String... expectedPatterns)
      throws CoreException {
    IJavaProject javaProject = JavaCore.create(project);
    Assert.assertNotNull("Is a Java project", javaProject);
    return assertClasspath(expectedPatterns, javaProject.getRawClasspath());
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
