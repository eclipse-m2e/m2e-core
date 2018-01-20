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

import org.apache.maven.model.Build;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.internal.ModuleSupport;


/**
 * Base class for {@link IClassifierClasspathProvider} implementations.
 * 
 * @author Fred Bricon
 * @since 1.3
 */
public abstract class AbstractClassifierClasspathProvider
    implements IClassifierClasspathProvider, IExecutableExtension {

  private static final String ATTR_ID = "id";

  private static final String ATTR_NAME = "name";

  private String id;

  private String name;

  /**
   * @throws CoreException
   * @deprecated replaced by
   *             {@link IClassifierClasspathProvider#setTestClasspath(Set, IMavenProjectFacade, IProgressMonitor, int)}
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  public void setTestClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
  }

  /**
   * @throws CoreException
   * @deprecated replaced by
   *             {@link IClassifierClasspathProvider#setRuntimeClasspath(Set, IMavenProjectFacade, IProgressMonitor, int)}
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  public void setRuntimeClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
  }

  @Deprecated
  protected void addTestFolder(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    addTestFolder(runtimeClasspath, mavenProjectFacade, monitor, IRuntimeClasspathEntry.USER_CLASSES);
  }

  @Deprecated
  protected void addMainFolder(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    addMainFolder(runtimeClasspath, mavenProjectFacade, monitor, IRuntimeClasspathEntry.USER_CLASSES);
  }

  @Deprecated
  protected void addFolders(Set<IRuntimeClasspathEntry> runtimeClasspath, IProject project, Set<IPath> folders) {
    addFolders(runtimeClasspath, project, folders, IRuntimeClasspathEntry.USER_CLASSES);
  }

  /**
   * Adds test classes folder to the runtime classpath.
   * 
   * @param requiredModules
   */
  protected void addTestFolder(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) throws CoreException {
    Build build = mavenProjectFacade.getMavenProject(monitor).getBuild();
    final Set<IPath> allTestClasses = new LinkedHashSet<IPath>();
    allTestClasses.add(mavenProjectFacade.getProjectRelativePath(build.getTestOutputDirectory()));
    addFolders(runtimeClasspath, mavenProjectFacade.getProject(), allTestClasses, classpathProperty);
  }

  /**
   * Adds main classes folder to the runtime classpath.
   * 
   * @param classpathProperty
   */
  protected void addMainFolder(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) throws CoreException {
    Build build = mavenProjectFacade.getMavenProject(monitor).getBuild();
    final Set<IPath> allClasses = new LinkedHashSet<IPath>();
    allClasses.add(mavenProjectFacade.getProjectRelativePath(build.getOutputDirectory()));
    addFolders(runtimeClasspath, mavenProjectFacade.getProject(), allClasses, classpathProperty);
  }

  /**
   * Adds a {@link Set} of folder {@link IPath} to the runtime classpath.
   */
  protected void addFolders(Set<IRuntimeClasspathEntry> runtimeClasspath, IProject project, Set<IPath> folders,
      int classpathProperty) {
    for(IPath folder : folders) {
      IResource member = project.findMember(folder); // only returns existing members
      if(member instanceof IFolder) { // must exist and be a folder
        runtimeClasspath.add(ModuleSupport.createRuntimeClasspathEntry((IFolder) member, classpathProperty, project));
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
