/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;


/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IClasspathManager {

  /**
   * Maven Dependencies classpath container id
   */
  public static final String CONTAINER_ID = "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER"; //$NON-NLS-1$

  /**
   * Name of IClasspathEntry attribute that keeps groupId of corresponding Maven artifact.
   */
  public static final String GROUP_ID_ATTRIBUTE = "maven.groupId"; //$NON-NLS-1$

  /**
   * Name of IClasspathEntry attribute that keeps artifactId of corresponding Maven artifact.
   */
  public static final String ARTIFACT_ID_ATTRIBUTE = "maven.artifactId"; //$NON-NLS-1$

  /**
   * Name of IClasspathEntry attribute that keeps version of corresponding Maven artifact.
   */
  public static final String VERSION_ATTRIBUTE = "maven.version"; //$NON-NLS-1$

  /**
   * Name of IClasspathEntry attribute that keeps classified corresponding Maven artifact.
   */
  public static final String CLASSIFIER_ATTRIBUTE = "maven.classifier"; //$NON-NLS-1$

  /**
   * Name of IClasspathEntry attribute that keeps scope corresponding Maven artifact.
   */
  public static final String SCOPE_ATTRIBUTE = "maven.scope"; //$NON-NLS-1$

  /**
   * @see IClasspathEntryDescriptor#setPomDerived(boolean)
   * @since 1.1
   */
  public static final String POMDERIVED_ATTRIBUTE = "maven.pomderived"; //$NON-NLS-1$

  /**
   * Name of IClasspathEntry attribute that is set to {@code true} for entries that correspond to optional Maven
   * dependency.
   * 
   * @since 1.5
   */
  public static final String OPTIONALDEPENDENCY_ATTRIBUTE = "maven.optionaldependency"; //$NON-NLS-1$

  /**
   * Name of IClasspathEntry attribute that is used to mark test sources and dependencies by jdt.core. Same as
   * org.eclipse.jdt.core.IClasspathAttribute.TEST, copied here to allow running with older jdt.core version.
   * 
   * @since 1.9
   */
  public static final String TEST_ATTRIBUTE = "test";

  /**
   * Name of IClasspathEntry attribute that is to limit the imported code of project by jdt.core. Same as
   * org.eclipse.jdt.core.IClasspathAttribute.WITHOUT_TEST_CODE, copied here to allow running with older jdt.core
   * version.
   * 
   * @since 1.9
   */
  public static final String WITHOUT_TEST_CODE = "without_test_code";

  /**
   * Maven dependency resolution scope constant indicating test scope.
   */
  public static final int CLASSPATH_TEST = 0;

  /**
   * Maven dependency resolution scope constant indicating runtime scope.
   */
  public static final int CLASSPATH_RUNTIME = 1;

  /**
   * Maven dependency resolution scope constant indicating default scope. test is the widest possible scope, and this is
   * what we need by default
   */
  public static final int CLASSPATH_DEFAULT = CLASSPATH_TEST;

  /**
   * Request download of sources and/or javadoc from Maven repositories by a background job (asynchronous execution).
   * After download has completed, sources and/or javadoc jar file will be attached to the corresponding classpath
   * entry.
   */
  public void scheduleDownload(IPackageFragmentRoot fragment, boolean downloadSources, boolean downloadJavadoc);

  /**
   * Request download of sources and/or javadoc from Maven repositories by a background job for all classpath entries of
   * the project (asynchronous execution). After download has completed, sources and/or javadoc jar file will be
   * attached to the corresponding classpath entries.
   */
  public void scheduleDownload(IProject project, boolean downloadSources, boolean downloadJavadoc);

  /**
   * Calculates and returns Maven classpath of the project.
   * 
   * @param project the project to calculate classpath for
   * @param scope one of CLASPATH_* constants, that specifies Maven dependency resolution scope for the classpath
   * @param uniquePaths enforce (true) or not to enforce (false) uniqueness of classpath entries paths.
   * @param monitor progress monitor
   * @return Maven classpath of the project
   */
  public IClasspathEntry[] getClasspath(IProject project, int scope, boolean uniquePaths, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Calculates Maven classpath of the project using default dependency resolution scope and updates contents of Maven
   * Dependencies classpath container.
   */
  public void updateClasspath(IProject project, IProgressMonitor monitor);

}
