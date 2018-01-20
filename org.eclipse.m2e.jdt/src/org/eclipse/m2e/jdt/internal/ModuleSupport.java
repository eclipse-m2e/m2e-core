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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.IClasspathDescriptor;


/**
 * Helper for Java Module Support
 *
 * @author Fred Bricon
 * @since 1.8.2
 */
public class ModuleSupport {

  static final boolean IS_MODULE_SUPPORT_AVAILABLE;

  static final boolean IS_PATCH_MODULE_SUPPORT_AVAILABLE;

  static {
    boolean isModuleSupportAvailable = false;
    boolean isPatchModuleSupportAvailable = false;
    try {
      Class.forName("org.eclipse.jdt.core.IModuleDescription");
      isModuleSupportAvailable = true;
      try {
        IRuntimeClasspathEntry.class.getDeclaredField("PATCH_MODULE");
        isPatchModuleSupportAvailable = true;
      } catch(NoSuchFieldException | SecurityException ignored) {
      }
    } catch(ClassNotFoundException ignored) {
    }

    IS_MODULE_SUPPORT_AVAILABLE = isModuleSupportAvailable;
    IS_PATCH_MODULE_SUPPORT_AVAILABLE = isPatchModuleSupportAvailable;
  }

  /**
   * Sets <code>module</code> flag to <code>true</code> to classpath dependencies declared in module-info.java
   * 
   * @param facade a Maven facade project
   * @param classpath a classpath descriptor
   * @param monitor a progress monitor
   */
  public static void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    if(!IS_MODULE_SUPPORT_AVAILABLE) {
      return;
    }
    InternalModuleSupport.configureClasspath(facade, classpath, monitor);
  }

  public static int determineModularClasspathProperty(IClasspathEntry entry) {
    if(!IS_PATCH_MODULE_SUPPORT_AVAILABLE) {
      return IRuntimeClasspathEntry.USER_CLASSES;
    }
    return InternalModuleSupport.determineModularClasspathProperty(entry);
  }

  public static IRuntimeClasspathEntry createRuntimeClasspathEntry(IFolder folder, int classpathProperty,
      IProject project) {
    if(!IS_PATCH_MODULE_SUPPORT_AVAILABLE) {
      return JavaRuntime.newArchiveRuntimeClasspathEntry(folder.getFullPath());
    }
    return InternalModuleSupport.createRuntimeClasspathEntry(folder, classpathProperty, project);
  }

  public static int determineClasspathPropertyForMainProject(boolean isModularConfiguration, IJavaProject javaProject) {
    if(!IS_PATCH_MODULE_SUPPORT_AVAILABLE) {
      return IRuntimeClasspathEntry.USER_CLASSES;
    }
    return InternalModuleSupport.determineClasspathPropertyForMainProject(isModularConfiguration, javaProject);
  }

  public static boolean isModularConfiguration(ILaunchConfiguration configuration) {
    if(!IS_PATCH_MODULE_SUPPORT_AVAILABLE) {
      return false;
    }
    return InternalModuleSupport.isModularConfiguration(configuration);
  }

  public static IRuntimeClasspathEntry newModularProjectRuntimeClasspathEntry(IJavaProject javaProject) {
    if(!IS_PATCH_MODULE_SUPPORT_AVAILABLE) {
      return JavaRuntime.newProjectRuntimeClasspathEntry(javaProject);
    }
    return InternalModuleSupport.newModularProjectRuntimeClasspathEntry(javaProject);
  }
}
