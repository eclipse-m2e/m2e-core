/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;


/**
 * Utility class related to Java project settings
 *
 * @author Fred Bricon
 */
public class JavaSettingsUtils {

  public static final String ENABLE_PREVIEW_JVM_FLAG = "--enable-preview";

  public static final String PARAMETERS_JVM_FLAG = "-parameters";

  private JavaSettingsUtils() {
    //No public instanciation
  }

  /**
   * Checks if the given {@link IJavaProject} has preview features enabled.
   * 
   * @param project the {@link IJavaProject}
   * @return <code>true</code> if the project preferences have JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES=enabled,
   *         <code>false</code> otherwise.
   */
  public static boolean hasPreviewFeatures(IJavaProject project) {
    if(project == null) {
      return false;
    }
    return JavaCore.ENABLED.equals(project.getOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, true));
  }
}
