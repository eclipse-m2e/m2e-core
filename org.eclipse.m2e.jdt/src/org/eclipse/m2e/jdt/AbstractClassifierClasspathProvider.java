/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.apache.maven.model.Build;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Base class for {@link IClassifierClasspathProvider} implementations.
 * 
 * @author Fred Bricon
 * @since 1.3
 */
public abstract class AbstractClassifierClasspathProvider implements IClassifierClasspathProvider, IExecutableExtension {

  private static final String ATTR_ID = "id";

  private static final String ATTR_NAME = "name";

  private String id;

  private String name;

  /**
   * @throws CoreException
   */
  public void setTestClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
  }

  /**
   * @throws CoreException
   */
  public void setRuntimeClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
  }

  /**
   * Adds test classes folder to the runtime classpath.
   */
  protected void addTestFolder(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    Build build = mavenProjectFacade.getMavenProject(monitor).getBuild();
    final Set<IPath> allTestClasses = new LinkedHashSet<IPath>();
    allTestClasses.add(mavenProjectFacade.getProjectRelativePath(build.getTestOutputDirectory()));
    addFolders(runtimeClasspath, mavenProjectFacade.getProject(), allTestClasses);
  }

  /**
   * Adds main classes folder to the runtime classpath.
   */
  protected void addMainFolder(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    Build build = mavenProjectFacade.getMavenProject(monitor).getBuild();
    final Set<IPath> allClasses = new LinkedHashSet<IPath>();
    allClasses.add(mavenProjectFacade.getProjectRelativePath(build.getOutputDirectory()));
    addFolders(runtimeClasspath, mavenProjectFacade.getProject(), allClasses);
  }

  /**
   * Adds a {@link Set} of folder {@link IPath} to the runtime classpath.
   */
  protected void addFolders(Set<IRuntimeClasspathEntry> runtimeClasspath, IProject project, Set<IPath> folders) {
    for(IPath folder : folders) {
      IResource member = project.findMember(folder); // only returns existing members
      if(member instanceof IFolder) { // must exist and be a folder
        runtimeClasspath.add(JavaRuntime.newArchiveRuntimeClasspathEntry(member.getFullPath()));
      }
    }
  }

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.id = config.getAttribute(ATTR_ID);
    this.name = config.getAttribute(ATTR_NAME);
  }

  public String toString() {
    return getName();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
