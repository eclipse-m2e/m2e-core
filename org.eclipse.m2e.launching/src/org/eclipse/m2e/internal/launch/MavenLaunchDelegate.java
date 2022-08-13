/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc.
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

import static org.eclipse.m2e.internal.launch.MavenLaunchUtils.quote;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.internal.launch.MavenRuntimeLaunchSupport.VMArguments;


public class MavenLaunchDelegate extends JavaLaunchDelegate implements MavenLaunchConstants {
  static final Logger log = LoggerFactory.getLogger(MavenLaunchDelegate.class);

  private static final String LAUNCHER_TYPE = "org.codehaus.classworlds.Launcher"; //$NON-NLS-1$

  //classworlds 2.0
  private static final String LAUNCHER_TYPE3 = "org.codehaus.plexus.classworlds.launcher.Launcher"; //$NON-NLS-1$

  private static final String ANSI_SUPPORT_QUALIFIER = "org.eclipse.ui.console"; //$NON-NLS-1$

  private static final String ANSI_SUPPORT_KEY = "ANSI_support_enabled"; //$NON-NLS-1$

  private static final VersionRange MAVEN_33PLUS_RUNTIMES;

  static {
    VersionRange mvn33PlusRange;
    try {
      mvn33PlusRange = VersionRange.createFromVersionSpec("[3.3,)");
    } catch(InvalidVersionSpecificationException O_o) {
      mvn33PlusRange = null;
    }
    MAVEN_33PLUS_RUNTIMES = mvn33PlusRange;
  }

  private ILaunch launch;

  private IProgressMonitor monitor;

  private String programArguments;

  private MavenRuntimeLaunchSupport launchSupport;

  private MavenLaunchExtensionsSupport extensionsSupport;

  private IPreferencesService preferencesService;

  public MavenLaunchDelegate() {
    allowAdvancedSourcelookup();
  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    this.launch = launch;
    this.monitor = monitor;
    this.programArguments = null;

    try {
      this.launchSupport = MavenRuntimeLaunchSupport.create(configuration, monitor);
      this.extensionsSupport = MavenLaunchExtensionsSupport.create(configuration, launch);
      this.preferencesService = Platform.getPreferencesService();

      log.info("" + getWorkingDirectory(configuration)); //$NON-NLS-1$
      log.info(" mvn" + getProgramArguments(configuration)); //$NON-NLS-1$

      extensionsSupport.configureSourceLookup(configuration, launch, monitor);

      super.launch(configuration, mode, launch, monitor);
    } finally {
      this.launch = null;
      this.monitor = null;
      this.launchSupport = null;
      this.extensionsSupport = null;
      this.preferencesService = null;
    }
  }

  @Override
  protected boolean saveBeforeLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
      throws CoreException {
    if(configuration.getAttribute(ATTR_SAVE_BEFORE_LAUNCH, true)) {
      return super.saveBeforeLaunch(configuration, mode, monitor);
    }
    return true;
  }

  @Override
  public IVMRunner getVMRunner(final ILaunchConfiguration configuration, String mode) throws CoreException {
    return launchSupport.decorateVMRunner(super.getVMRunner(configuration, mode));
  }

  @Override
  public String getMainTypeName(ILaunchConfiguration configuration) {
    return launchSupport.getVersion().startsWith("3.") ? LAUNCHER_TYPE3 : LAUNCHER_TYPE; //$NON-NLS-1$
  }

  @Override
  public String[] getClasspath(ILaunchConfiguration configuration) {
    List<String> cp = launchSupport.getBootClasspath();
    return cp.toArray(String[]::new);
  }

  @Override
  public String[][] getClasspathAndModulepath(ILaunchConfiguration configuration) {
    String[][] paths = new String[2][];
    paths[0] = getClasspath(configuration);
    return paths;
  }

  @Override
  public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
    if(programArguments == null) {
      String goals = getGoals(configuration);

      StringBuilder sb = new StringBuilder();
      getProperties(sb, configuration);
      getPreferences(sb, configuration, goals);
      sb.append(" ").append(goals);

      extensionsSupport.appendProgramArguments(sb, configuration, launch, monitor);

      programArguments = sb.toString();
    }
    return programArguments;
  }

  @Override
  public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
    VMArguments arguments = launchSupport.getVMArguments();

    AbstractMavenRuntime runtime = MavenLaunchUtils.getMavenRuntime(configuration);
    appendRuntimeSpecificArguments(runtime.getVersion(), arguments, configuration);

    extensionsSupport.appendVMArguments(arguments, configuration, launch, monitor);

    // user configured entries
    arguments.append(super.getVMArguments(configuration));

    return arguments.toString();
  }

  protected String getGoals(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS, ""); //$NON-NLS-1$
  }

  @Override
  public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) {
    return false;
  }

  /**
   * Construct string with properties to pass to JVM as system properties
   */
  private void getProperties(StringBuilder sb, ILaunchConfiguration configuration) throws CoreException {

    List<String> properties;
    try {
      properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.emptyList());
    } catch(CoreException e) {
      log.error("Exception while getting configuration attribute " + ATTR_PROPERTIES, e);
      throw e;
    }
    for(String property : properties) {
      int n = property.indexOf('=');
      String name = property;
      String value = null;

      if(n > -1) {
        name = property.substring(0, n);
        if(n > 1) {
          String substring = property.substring(n + 1);
          try {
            value = LaunchingUtils.substituteVar(substring);
          } catch(CoreException e) {
            log.debug("Exception while substitute variables in substring " + substring + " using raw value.", e);
            value = substring;
          }
        }
      }

      sb.append(" -D").append(name); //$NON-NLS-1$
      if(value != null) {
        sb.append('=').append(quote(value));
      }
    }

    try {
      String profiles = configuration.getAttribute(ATTR_PROFILES, (String) null);
      if(profiles != null && profiles.trim().length() > 0) {
        sb.append(" -P").append(profiles.replaceAll("\\s+", ",")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
    } catch(CoreException ex) {
      log.error("Exception while getting configuration attribute " + ATTR_PROFILES, ex);
      throw ex;
    }
  }

  /**
   * Construct string with preferences to pass to JVM as system properties
   */
  private void getPreferences(StringBuilder sb, ILaunchConfiguration configuration, String goals) throws CoreException {
    IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_BATCH, true)) {
      sb.append(" -B"); //$NON-NLS-1$
    }

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

    int colors = configuration.getAttribute(MavenLaunchConstants.ATTR_COLOR,
        MavenLaunchConstants.ATTR_COLOR_VALUE_AUTO);
    if(colors == MavenLaunchConstants.ATTR_COLOR_VALUE_AUTO) {
      // In reality we don't want to pass -Dstyle.color=auto to Maven.
      // It tries to detect if the current stdout is a terminal (using something like `isatty`)
      // and for Eclipse that test fails.
      colors = isAnsiProcessingEnabled() ? MavenLaunchConstants.ATTR_COLOR_VALUE_ALWAYS
          : MavenLaunchConstants.ATTR_COLOR_VALUE_NEVER;
    }
    String enableColor = switch(colors) {
      case MavenLaunchConstants.ATTR_COLOR_VALUE_ALWAYS -> "always";
      case MavenLaunchConstants.ATTR_COLOR_VALUE_NEVER -> "never";
      default -> throw new IllegalArgumentException(
          "Unexpected value for" + MavenLaunchConstants.ATTR_COLOR + ": " + colors);
    };
    sb.append(" -Dstyle.color=" + enableColor);

    if(!goals.contains("-gs ")) { //$NON-NLS-1$
      String globalSettings = launchSupport.getSettings();
      if(globalSettings != null && !globalSettings.trim().isEmpty() && !new File(globalSettings.trim()).exists()) {
        globalSettings = null;
      }
      if(globalSettings != null && !globalSettings.trim().isEmpty()) {
        sb.append(" -gs ").append(quote(globalSettings)); //$NON-NLS-1$
      }
    }

    String settings = configuration.getAttribute(MavenLaunchConstants.ATTR_USER_SETTINGS, (String) null);
    settings = LaunchingUtils.substituteVar(settings);
    if(settings == null || settings.trim().isEmpty()) {
      settings = mavenConfiguration.getUserSettingsFile();
      if(settings != null && !settings.trim().isEmpty() && !new File(settings.trim()).exists()) {
        settings = null;
      }
    }
    if(settings != null && !settings.trim().isEmpty()) {
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
  }

  /**
   * Gets the current status of the ANSI processing.
   *
   * @return true if enabled, false if not.
   */
  private boolean isAnsiProcessingEnabled() {
    if(!isAnsiSupportInstalled()) {
      return false;
    }
    return preferencesService.getBoolean(ANSI_SUPPORT_QUALIFIER, ANSI_SUPPORT_KEY, true, null);
  }

  // We can completely remove this test if / when this module will require
  // a version of org.eclipse.ui.console newer than 3.11.300
  private boolean isAnsiSupportInstalled() {
    Bundle consoleBundle = Platform.getBundle("org.eclipse.ui.console");
    if(consoleBundle == null) {
      return false;
    }

    Version currentVer = consoleBundle.getVersion();
    if("qualifier".equals(currentVer.getQualifier())) {
      // In the rare case when someone is running with a dev version of
      // `org.eclipse.ui.console` we "fake" a qualifier with the current time.
      SimpleDateFormat df = new SimpleDateFormat("'v'yyyyMMdd-HHmm", Locale.US);
      String qualifier = df.format(new Date());
      currentVer = new Version(currentVer.getMajor(), currentVer.getMinor(), currentVer.getMicro(), qualifier);
    }

    Version ansiVer = new Version(3, 11, 300, "v20220804-1122");
    return currentVer.compareTo(ansiVer) >= 0;
  }

  /**
   * Not API. Made public for testing purposes.
   */
  public void appendRuntimeSpecificArguments(String runtimeVersion, VMArguments arguments,
      ILaunchConfiguration configuration) throws CoreException {
    if(applies(runtimeVersion)) {
      getArgsFromMvnDir(arguments, configuration);
    }
  }

  private void getArgsFromMvnDir(VMArguments arguments, ILaunchConfiguration configuration) throws CoreException {
    String pomDir = LaunchingUtils.substituteVar(configuration.getAttribute(MavenLaunchConstants.ATTR_POM_DIR, ""));
    if(pomDir.isEmpty()) {
      return;
    }
    File baseDir = findMavenProjectBasedir(new File(pomDir));
    File mvnDir = new File(baseDir, ".mvn");
    File jvmConfig = new File(mvnDir, "jvm.config");
    if(jvmConfig.isFile()) {
      try {
        for(String line : Files.readAllLines(jvmConfig.toPath(), StandardCharsets.UTF_8)) {
          arguments.append(line);
        }
      } catch(IOException ex) {
        IStatus error = new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID,
            NLS.bind(Messages.MavenLaunchDelegate_error_cannot_read_jvmConfig, jvmConfig.getAbsolutePath()), ex);
        throw new CoreException(error);
      }
    }
    arguments.appendProperty("maven.multiModuleProjectDirectory", MavenLaunchUtils.quote(baseDir.getAbsolutePath()));
  }

  //This will likely move to core when we need it
  private File findMavenProjectBasedir(File dir) {
    File folder = dir;
    // loop upwards but stop if root
    while(folder != null && folder.getParentFile() != null) {
      // see if /.mvn exists
      if(new File(folder, ".mvn").isDirectory()) {
        return folder;
      }
      folder = folder.getParentFile();
    }
    return dir;
  }

  private boolean applies(String runtimeVersion) {
    return MAVEN_33PLUS_RUNTIMES.containsVersion(new DefaultArtifactVersion(runtimeVersion));
  }
}
