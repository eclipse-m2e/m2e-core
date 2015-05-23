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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.actions.MavenLaunchConstants;


public class LaunchingUtils {

  private static Logger log = LoggerFactory.getLogger(LaunchingUtils.class);

  /**
   * Substitute any variable
   */
  public static String substituteVar(String s) throws CoreException {
    if(s == null) {
      return s;
    }
    try {
      return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(s);
    } catch(CoreException e) {
      if(e.getStatus() != null && e.getStatus().matches(IStatus.CANCEL)) {
        throw e;
      }
      log.error("Could not substitute variable {}.", s, e);
      throw new CoreException(new Status(IStatus.ERROR, MavenLaunchConstants.PLUGIN_ID, -1,
          NLS.bind(Messages.MavenLaunchUtils_error_could_not_substitute_variable, s), e));
    }
  }

}
