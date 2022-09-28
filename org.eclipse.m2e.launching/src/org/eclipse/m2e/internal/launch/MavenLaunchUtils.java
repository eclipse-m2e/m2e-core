/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
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

import java.util.Collections;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.internal.Bundles;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;
import org.eclipse.m2e.workspace.WorkspaceState;


/**
 * MavenLaunchUtils
 *
 * @author Igor Fedorenko
 */
public class MavenLaunchUtils {

  private MavenLaunchUtils() { // static use only
  }

  public static AbstractMavenRuntime getMavenRuntime(ILaunchConfiguration configuration) throws CoreException {
    MavenRuntimeManagerImpl runtimeManager = MavenPluginActivator.getDefault().getMavenRuntimeManager();
    String name = configuration.getAttribute(MavenLaunchConstants.ATTR_RUNTIME, ""); //$NON-NLS-1$
    AbstractMavenRuntime runtime = runtimeManager.getRuntime(name);
    if(runtime == null) {
      throw new CoreException(Status.error(NLS.bind(Messages.MavenLaunchUtils_error_no_maven_install, name)));
    }
    return runtime;
  }

  public static List<String> getCliResolver(AbstractMavenRuntime runtime) {
    if(runtime.getVersion().startsWith("3.")) { //$NON-NLS-1$
      Bundle m2eWorkspaceCLIBundle = FrameworkUtil.getBundle(WorkspaceState.class);
      return Bundles.getClasspathEntries(m2eWorkspaceCLIBundle);
    }
    return Collections.emptyList(); // unsupported version of maven
  }

  /**
   * @since 1.4
   */
  public static String quote(String string) {
    return string.contains(" ") ? "\"" + string + "\"" : string; //$NON-NLS-1$ //$NON-NLS-2$
  }
}
