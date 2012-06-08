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
 * Classpath provider for the <code>null</code> classifier. This provider adds the main classes folder to the runtime
 * classpath for both Runtime and Test launch configurations.
 * 
 * @author Fred Bricon
 * @since 1.3
 */
public class NullClassifierClasspathProvider extends AbstractClassifierClasspathProvider {

  /**
   * This provider applies to the mavenProjectFacade if the classifier is <code>null</code>.
   */
  public boolean applies(IMavenProjectFacade mavenProjectFacade, String classifier) {
    return classifier == null;
  }

  /**
   * @return <code>null</code>
   */
  public String getClassifier() {
    return null;
  }

  /**
   * Adds the main classes folder to the runtime classpath.
   */
  @Override
  public void setRuntimeClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    addMainFolder(runtimeClasspath, mavenProjectFacade, monitor);
  }

  /**
   * Adds the main classes folder to the test classpath.
   */
  @Override
  public void setTestClasspath(Set<IRuntimeClasspathEntry> testClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    setRuntimeClasspath(testClasspath, mavenProjectFacade, monitor);
  }

}
