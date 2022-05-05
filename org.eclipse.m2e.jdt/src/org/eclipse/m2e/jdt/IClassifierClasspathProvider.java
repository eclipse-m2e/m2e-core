/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Configures Runtime Classpath for Launch configuration of a given workspace project, depending on its classifier.
 *
 * @author Fred Bricon
 * @since 1.3
 */
public interface IClassifierClasspathProvider {

  /**
   * Checks if this provider applies to the given project / classifier combo.
   */
  boolean applies(IMavenProjectFacade mavenProjectFacade, String classifier);

  /**
   * @return the classifier key this provider applies to. Can be <code>null</code>.
   */
  String getClassifier();

  /**
   * Configures the runtime classpath of the given project.
   */
  void setTestClasspath(Set<IRuntimeClasspathEntry> testClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) throws CoreException;

  /**
   * Configures the runtime classpath of the given project.
   */
  void setRuntimeClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) throws CoreException;

}
