/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;


public class LaunchingUtils {

  private static final String PROJECT_LOCATION_VARIABLE_NAME = "project_loc";

  /**
   * Substitute any variable
   */
  public static String substituteVar(String s) throws CoreException {
    if(s != null) {
      return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(s);
    }
    return null;
  }

  /**
   * Generate project_loc variable expression for the given project.
   */
  public static String generateProjectLocationVariableExpression(IProject project) {
    return VariablesPlugin.getDefault().getStringVariableManager()
        .generateVariableExpression(PROJECT_LOCATION_VARIABLE_NAME, project.getName());
  }
}
