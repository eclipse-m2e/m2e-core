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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;


/**
 * MavenLaunchUtils
 * 
 * @author Igor Fedorenko
 */
@SuppressWarnings("restriction")
public class MavenLaunchUtils {

  public static AbstractMavenRuntime getMavenRuntime(ILaunchConfiguration configuration) throws CoreException {
    MavenRuntimeManagerImpl runtimeManager = MavenPluginActivator.getDefault().getMavenRuntimeManager();
    String name = configuration.getAttribute(MavenLaunchConstants.ATTR_RUNTIME, ""); //$NON-NLS-1$
    AbstractMavenRuntime runtime = runtimeManager.getRuntime(name);
    if(runtime == null) {
      throw new CoreException(new Status(IStatus.ERROR, MavenLaunchConstants.PLUGIN_ID, -1, //
          NLS.bind(Messages.MavenLaunchUtils_error_no_maven_install, name), null));
    }
    return runtime;
  }

  public static String getCliResolver(AbstractMavenRuntime runtime) throws CoreException {
    String jarname;
    String runtimeVersion = runtime.getVersion();
    if(runtimeVersion.startsWith("3.0")) { //$NON-NLS-1$
      jarname = "org.eclipse.m2e.cliresolver30.jar"; //$NON-NLS-1$
    } else if(runtimeVersion.startsWith("3.")) { //$NON-NLS-1$
      jarname = "org.eclipse.m2e.cliresolver31.jar"; //$NON-NLS-1$
    } else {
      jarname = "org.eclipse.m2e.cliresolver.jar"; //$NON-NLS-1$
    }
    return getBundleEntry(MavenLaunchPlugin.getDefault().getBundle(), jarname);
  }

  /**
   * Returns bundle entry path on local filesystem.
   */
  public static String getBundleEntry(Bundle bundle, String entryPath) throws CoreException {
    URL url = bundle.getEntry(entryPath);
    try {
      URL fileURL = FileLocator.toFileURL(url);
      // MNGECLIPSE-804 workaround for spaces in the original path
      URI fileURI = new URI(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPath(), fileURL.getQuery());
      return new File(fileURI).getCanonicalPath();
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenLaunchConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
  }

  /**
   * @since 1.4
   */
  public static String quote(String string) {
    return string.indexOf(' ') > -1 ? "\"" + string + "\"" : string; //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * @since 1.4
   */
  public static String toPath(List<String> cp) {
    StringBuilder sb = new StringBuilder();
    for(String cpe : cp) {
      if(sb.length() > 0) {
        sb.append(File.pathSeparator);
      }
      sb.append(cpe);
    }
    return sb.toString();
  }

}
