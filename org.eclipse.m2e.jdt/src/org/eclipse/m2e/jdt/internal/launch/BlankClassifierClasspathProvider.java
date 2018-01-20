/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal.launch;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.AbstractClassifierClasspathProvider;


/**
 * Classpath provider for the blank (i.e. empty String) classifier, corresponding to the main project for which the
 * Launch configuration applies.
 * 
 * @author Fred Bricon
 * @since 1.3
 */
public class BlankClassifierClasspathProvider extends AbstractClassifierClasspathProvider {

  /**
   * This provider applies to the mavenProjectFacade if the classifier is blank.
   */
  public boolean applies(IMavenProjectFacade mavenProjectFacade, String classifier) {
    return getClassifier().equals(classifier);
  }

  /**
   * @return an empty String
   */
  public String getClassifier() {
    return "";
  }

  /**
   * Adds the main classes folder to the runtime classpath.
   */
  @Override
  public void setRuntimeClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) throws CoreException {
    addMainFolder(runtimeClasspath, mavenProjectFacade, monitor, classpathProperty);
  }

  /**
   * Adds the test classes folder followed by the main classes one to the runtime classpath.
   */
  @Override
  public void setTestClasspath(Set<IRuntimeClasspathEntry> testClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) throws CoreException {
    addTestFolder(testClasspath, mavenProjectFacade, monitor, classpathProperty);
    addMainFolder(testClasspath, mavenProjectFacade, monitor, classpathProperty);
  }

}
