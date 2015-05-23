/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;


public class LaunchingUtils {

  /**
   * Substitute any variable
   */
  public static String substituteVar(String s) throws CoreException {
    if(s != null) {
      return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(s);
    }
    return null;
  }

}
