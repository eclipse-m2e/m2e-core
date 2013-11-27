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

import static org.eclipse.m2e.internal.launch.MavenLaunchUtils.quote;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.internal.launch.MavenRuntimeLaunchSupport.VMArguments;


public class MavenLaunchDelegate extends JavaLaunchDelegate implements MavenLaunchConstants {
  static final Logger log = LoggerFactory.getLogger(MavenLaunchDelegate.class);

  private static final String LAUNCHER_TYPE = "org.codehaus.classworlds.Launcher"; //$NON-NLS-1$

  //classworlds 2.0
  private static final String LAUNCHER_TYPE3 = "org.codehaus.plexus.classworlds.launcher.Launcher"; //$NON-NLS-1$

  private ILaunch launch;

  private IProgressMonitor monitor;

  private String programArguments;

  private MavenRuntimeLaunchSupport launchSupport;

  private MavenLaunchExtensionsSupport extensionsSupport;

  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    this.launch = launch;
    this.monitor = monitor;
    this.programArguments = null;

    try {
      this.launchSupport = MavenRuntimeLaunchSupport.create(configuration, launch, monitor);
      this.extensionsSupport = MavenLaunchExtensionsSupport.create(configuration, launch);

      log.info("" + getWorkingDirectory(configuration)); //$NON-NLS-1$
      log.info(" mvn" + getProgramArguments(configuration)); //$NON-NLS-1$

      extensionsSupport.configureSourceLookup(configuration, launch, monitor);

      super.launch(configuration, mode, launch, monitor);
    } finally {
      this.launch = null;
      this.monitor = null;
      this.launchSupport = null;
      this.extensionsSupport = null;
    }
  }

  public IVMRunner getVMRunner(final ILaunchConfiguration configuration, String mode) throws CoreException {
    return launchSupport.decorateVMRunner(super.getVMRunner(configuration, mode));
  }

  public String getMainTypeName(ILaunchConfiguration configuration) {
    return launchSupport.getVersion().startsWith("3.") ? LAUNCHER_TYPE3 : LAUNCHER_TYPE; //$NON-NLS-1$
  }

  public String[] getClasspath(ILaunchConfiguration configuration) {
    List<String> cp = launchSupport.getBootClasspath();
    return cp.toArray(new String[cp.size()]);
  }

  public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
    if(programArguments == null) {
      StringBuilder sb = new StringBuilder();
      sb.append(getProperties(configuration));
      sb.append(" ").append(getPreferences(configuration));
      sb.append(" ").append(getGoals(configuration));

      extensionsSupport.appendProgramArguments(sb, configuration, launch, monitor);

      programArguments = sb.toString();
    }
    return programArguments;
  }

  public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
    VMArguments arguments = launchSupport.getVMArguments();

    extensionsSupport.appendVMArguments(arguments, configuration, launch, monitor);

    // user configured entries
    arguments.append(super.getVMArguments(configuration));

    return arguments.toString();
  }

  protected String getGoals(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS, ""); //$NON-NLS-1$
  }

  public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) {
    return false;
  }

  /**
   * Construct string with properties to pass to JVM as system properties
   */
  private String getProperties(ILaunchConfiguration configuration) {
    StringBuffer sb = new StringBuffer();

    try {
      @SuppressWarnings("unchecked")
      List<String> properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
      for(String property : properties) {
        int n = property.indexOf('=');
        String name = property;
        String value = null;

        if(n > -1) {
          name = property.substring(0, n);
          if(n > 1) {
            value = LaunchingUtils.substituteVar(property.substring(n + 1));
          }
        }

        sb.append(" -D").append(name); //$NON-NLS-1$
        if(value != null) {
          sb.append('=').append(quote(value));
        }
      }
    } catch(CoreException e) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROPERTIES;
      log.error(msg, e);
    }

    try {
      String profiles = configuration.getAttribute(ATTR_PROFILES, (String) null);
      if(profiles != null && profiles.trim().length() > 0) {
        sb.append(" -P").append(profiles.replaceAll("\\s+", ",")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
    } catch(CoreException ex) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROFILES;
      log.error(msg, ex);
    }

    return sb.toString();
  }

  /**
   * Construct string with preferences to pass to JVM as system properties
   */
  private String getPreferences(ILaunchConfiguration configuration) throws CoreException {
    IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();

    StringBuffer sb = new StringBuffer();

    sb.append(" -B"); //$NON-NLS-1$

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_DEBUG_OUTPUT, mavenConfiguration.isDebugOutput())) {
      sb.append(" -X").append(" -e"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    // sb.append(" -D").append(MavenPreferenceConstants.P_DEBUG_OUTPUT).append("=").append(debugOutput);

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_OFFLINE, mavenConfiguration.isOffline())) {
      sb.append(" -o"); //$NON-NLS-1$
    }
    // sb.append(" -D").append(MavenPreferenceConstants.P_OFFLINE).append("=").append(offline);

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_UPDATE_SNAPSHOTS, false)) {
      sb.append(" -U"); //$NON-NLS-1$
    }

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_NON_RECURSIVE, false)) {
      sb.append(" -N"); //$NON-NLS-1$
    }

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_SKIP_TESTS, false)) {
      sb.append(" -Dmaven.test.skip=true -DskipTests"); //$NON-NLS-1$
    }

    int threads = configuration.getAttribute(MavenLaunchConstants.ATTR_THREADS, 1);
    if(threads > 1) {
      sb.append(" --threads ").append(threads);
    }

    String settings = configuration.getAttribute(MavenLaunchConstants.ATTR_USER_SETTINGS, (String) null);
    if(settings == null || settings.trim().length() <= 0) {
      settings = mavenConfiguration.getUserSettingsFile();
      if(settings != null && settings.trim().length() > 0 && !new File(settings.trim()).exists()) {
        settings = null;
      }
    }
    if(settings != null && settings.trim().length() > 0) {
      sb.append(" -s ").append(quote(settings)); //$NON-NLS-1$
    }

    // boolean b = preferenceStore.getBoolean(MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION);
    // sb.append(" -D").append(MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION).append("=").append(b);

    // b = preferenceStore.getBoolean(MavenPreferenceConstants.P_UPDATE_SNAPSHOTS);
    // sb.append(" -D").append(MavenPreferenceConstants.P_UPDATE_SNAPSHOTS).append("=").append(b);

    // String s = preferenceStore.getString(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    // if(s != null && s.trim().length() > 0) {
    //   sb.append(" -D").append(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY).append("=").append(s);
    // }

    return sb.toString();
  }

  static void removeTempFiles(ILaunch launch) {
    MavenRuntimeLaunchSupport.removeTempFiles(launch);
  }
}
