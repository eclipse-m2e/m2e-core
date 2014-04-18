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
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.artifact.Artifact;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * MavenLaunchUtils
 * 
 * @author Igor Fedorenko
 */
public class MavenLaunchUtils {
  private static final Logger log = LoggerFactory.getLogger(MavenLaunchUtils.class);

  public static MavenRuntime getMavenRuntime(ILaunchConfiguration configuration) throws CoreException {
    MavenRuntimeManager runtimeManager = MavenPlugin.getMavenRuntimeManager();
    String name = configuration.getAttribute(MavenLaunchConstants.ATTR_RUNTIME, ""); //$NON-NLS-1$
    MavenRuntime runtime = runtimeManager.getRuntimeByName(name);
    if(runtime == null) {
      throw new CoreException(new Status(IStatus.ERROR, MavenLaunchConstants.PLUGIN_ID, -1, //
          NLS.bind(Messages.MavenLaunchUtils_error_no_maven_install, name), null));
    }
    return runtime;
  }

  public static String getCliResolver(MavenRuntime runtime) throws CoreException {
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

  public static void addUserComponents(ILaunchConfiguration configuration, IMavenLauncherConfiguration collector)
      throws CoreException {
    List<String> list = configuration.getAttribute(MavenLaunchConstants.ATTR_FORCED_COMPONENTS_LIST,
        new ArrayList<String>());
    if(list == null) {
      return;
    }

    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    IMaven maven = MavenPlugin.getMaven();
    for(String gav : list) {
      // groupId:artifactId:version
      StringTokenizer st = new StringTokenizer(gav, ":"); //$NON-NLS-1$
      String groupId = st.nextToken();
      String artifactId = st.nextToken();
      String version = st.nextToken();

      IMavenProjectFacade facade = projectManager.getMavenProject(groupId, artifactId, version);

      if(facade != null) {
        collector.addProjectEntry(facade);
      } else {
        String name = groupId + ":" + artifactId + ":" + version; //$NON-NLS-1$ //$NON-NLS-2$
        try {
          Artifact artifact = maven.resolve(groupId, artifactId, version, "jar", null, null, null); //$NON-NLS-1$
          File file = artifact.getFile();
          if(file != null) {
            collector.addArchiveEntry(file.getAbsolutePath());
          }
        } catch(CoreException ex) {
          log.error("Artifact not found " + name, ex);
        }
      }
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
