/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.AutomaticModuleNaming;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.core.AbstractModule;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;


/**
 * Helper for Java Module Support relying on new JDT classes
 *
 * @author Fred Bricon
 * @since 1.8.2
 */
@Deprecated
@SuppressWarnings("restriction")
class InternalModuleSupport {

  private static final Logger log = LoggerFactory.getLogger(InternalModuleSupport.class);

  /**
   * Sets <code>module</code flag to <code>true</code> to classpath dependencies declared in module-info.java
   * 
   * @param facade a Maven facade project
   * @param classpath a classpath descriptor
   * @param monitor a progress monitor
   */
  public static void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    IJavaProject javaProject = JavaCore.create(facade.getProject());
    if(javaProject == null || !javaProject.exists()) {
      return;
    }
    IModuleDescription moduleDescription = javaProject.getModuleDescription();
    if(!(moduleDescription instanceof AbstractModule)) {
      return;
    }
    AbstractModule module = (AbstractModule) moduleDescription;
    Set<String> requiredModules = Stream.of(module.getRequiredModules()).map(m -> new String(m.name()))
        .collect(Collectors.toSet());
    for(IClasspathEntryDescriptor entry : classpath.getEntryDescriptors()) {
      String moduleName = getModuleName(entry, monitor);
      if(requiredModules.contains(moduleName)) {
        entry.setClasspathAttribute(IClasspathAttribute.MODULE, Boolean.TRUE.toString());
      }
    }
  }

  private static String getModuleName(IClasspathEntryDescriptor entry, IProgressMonitor monitor) {
    String module = null;
    if(IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
      module = getModuleNameFromJar(entry.getPath().toFile());
    } else if(IClasspathEntry.CPE_PROJECT == entry.getEntryKind()) {
      module = getModuleNameFromProject(entry.getPath(), monitor);
    }
    return module;
  }

  private static String getModuleNameFromProject(IPath projectPath, IProgressMonitor monitor) {
    IJavaProject project = getJavaProject(projectPath);
    String module = null;
    if(project != null) {
      try {
        if(project.getModuleDescription() == null) {
          String buildName = null;
          IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project.getProject());
          if(facade != null) {
            MavenProject mavenProject = facade.getMavenProject(monitor);
            if(mavenProject != null) {
              buildName = mavenProject.getBuild().getFinalName();
            }
          }
          if(buildName == null || buildName.isEmpty()) {
            buildName = project.getElementName();
          }
          module = new String(AutomaticModuleNaming.determineAutomaticModuleName(buildName, false, null));
        } else {
          module = project.getModuleDescription().getElementName();
        }
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    return module;
  }

  private static IJavaProject getJavaProject(IPath projectPath) {
    if(projectPath == null || projectPath.isEmpty()) {
      return null;
    }
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = root.getProject(projectPath.lastSegment());
    if(project.isAccessible()) {
      return JavaCore.create(project);
    }
    return null;
  }

  private static String getModuleNameFromJar(File file) {
    if(!file.isFile()) {
      return null;
    }
    char[] moduleName = null;
    try (ZipFile zipFile = new ZipFile(file)) {
      IModule module = null;
      ClassFileReader reader = ClassFileReader.read(zipFile, IModule.MODULE_INFO_CLASS);
      if(reader != null) {
        module = reader.getModuleDeclaration();
        if(module != null) {
          moduleName = module.name();
        }
      }
    } catch(ClassFormatException | IOException ex) {
      log.error(ex.getMessage(), ex);
    }
    if(moduleName == null) {
      moduleName = AutomaticModuleNaming.determineAutomaticModuleName(file.getAbsolutePath());
    }
    return new String(moduleName);
  }

}
