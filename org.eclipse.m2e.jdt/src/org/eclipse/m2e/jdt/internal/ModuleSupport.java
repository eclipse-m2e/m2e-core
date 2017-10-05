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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

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

  static {
    boolean isModuleSupportAvailable = false;
    try {
      Class.forName("org.eclipse.jdt.core.IModuleDescription");
      isModuleSupportAvailable = true;
    } catch(ClassNotFoundException ignored) {
    }
    IS_MODULE_SUPPORT_AVAILABLE = isModuleSupportAvailable;
  }

  /**
   * Sets <code>module</code flag to <code>true</code> to classpath dependencies declared in module-info.java
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
}
