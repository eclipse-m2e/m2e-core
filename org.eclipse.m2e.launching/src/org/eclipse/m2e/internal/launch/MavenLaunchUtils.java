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

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.osgi.util.NLS;

import com.ibm.icu.util.StringTokenizer;

/**
 * MavenLaunchUtils
 * 
 * @author Igor Fedorenko
 */
public class MavenLaunchUtils {

  public static MavenRuntime getMavenRuntime(ILaunchConfiguration configuration) throws CoreException {
    MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
    String location = configuration.getAttribute(MavenLaunchConstants.ATTR_RUNTIME, ""); //$NON-NLS-1$
    MavenRuntime runtime = runtimeManager.getRuntime(location);
    if(runtime == null) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
          NLS.bind(Messages.MavenLaunchUtils_error_no_maven_install, location), null));
    }
    return runtime;
  }

  public static String getCliResolver(MavenRuntime runtime) throws CoreException {
    String jarname;
    String runtimeVersion = runtime.getVersion();
    if (runtimeVersion.startsWith("3.")) { //$NON-NLS-1$
      jarname = "org.eclipse.m2e.cliresolver30.jar"; //$NON-NLS-1$
    } else {
      jarname = "org.eclipse.m2e.cliresolver.jar"; //$NON-NLS-1$
    }
    URL url = MavenLaunchPlugin.getDefault().getBundle().getEntry(jarname);
    try {
      URL fileURL = FileLocator.toFileURL(url);
      // MNGECLIPSE-804 workaround for spaces in the original path
      URI fileURI = new URI(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPath(), fileURL.getQuery());
      return new File(fileURI).getCanonicalPath();
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
  }

  public static void addUserComponents(ILaunchConfiguration configuration, IMavenLauncherConfiguration collector)
      throws CoreException {
    @SuppressWarnings("unchecked")
    List<String> list = configuration.getAttribute(MavenLaunchConstants.ATTR_FORCED_COMPONENTS_LIST, new ArrayList());
    if(list == null) {
      return;
    }

    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    IMaven maven = MavenPlugin.getDefault().getMaven();
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
          MavenLogger.log("Artifact not found " + name, ex);
        }
      }
    }
  }
}
