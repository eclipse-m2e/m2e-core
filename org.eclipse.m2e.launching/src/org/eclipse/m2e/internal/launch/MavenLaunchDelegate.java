/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype, Inc.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.internal.launch.MavenRuntimeLaunchSupport.VMArguments;


public class MavenLaunchDelegate extends JavaLaunchDelegate implements MavenLaunchConstants {
  private static final Logger log = LoggerFactory.getLogger(MavenLaunchDelegate.class);

  private static final ILog ECLIPSE_LOG = Platform.getLog(MavenLaunchDelegate.class);

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

      log.info("Run build in \"{}\":", getWorkingDirectory(configuration)); //$NON-NLS-1$
      log.info(" mvn {}", getProgramArguments(configuration)); //$NON-NLS-1$

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

  public static File getPomDirectory(ILaunchConfiguration configuration) {
    if(configuration == null) {
      return null;
    }
    // set associated project name (if there is some)
    String pomDir;
    try {
      pomDir = configuration.getAttribute(MavenLaunchConstants.ATTR_POM_DIR, "");
    } catch(CoreException ex) {
      log.warn("Failed to retrieve attribute '{}' from launch configuration {}", MavenLaunchConstants.ATTR_POM_DIR,
          configuration.getName());
      return null;
    }
    try {
      return new File(LaunchingUtils.substituteVar(pomDir));
    } catch(CoreException e) {
      log.debug("Cannot substitute vars in {}", pomDir, e);
      return null;
    }
  }

  public static Optional<IContainer> getContainer(File file) {
    // try to retrieve associated Eclipse project
    return Optional.ofNullable(file).map(f -> IPath.fromOSString(f.toString()))
        .map(ResourcesPlugin.getWorkspace().getRoot()::getContainerForLocation);
  }

  @Override
  public IVMInstall getVMInstall(ILaunchConfiguration configuration) throws CoreException {
    // use the Maven JVM if nothing explicitly configured in the launch configuration
    File pomDirectory = getPomDirectory(configuration);
    if(!configuration.hasAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH)) {
      String requiredJavaVersion = readEnforcedJavaVersion(pomDirectory, monitor);
      if(requiredJavaVersion != null) {
        IVMInstall jre = getBestMatchingVM(requiredJavaVersion);
        if(jre != null) {
          return jre;
        }
      }
    }
    Optional<IProject> project = getContainer(pomDirectory).map(IContainer::getProject)
        .filter(p -> JavaCore.create(p).exists());
    if(project.isPresent()) {
      // Set the project name so that super.getVMInstall() called below, can find the JDT-Compiler JDK 
      ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
      workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.get().getName());
      configuration = workingCopy;
    }
    return super.getVMInstall(configuration);
  }

  public static String readEnforcedJavaVersion(File pomDirectory, IProgressMonitor monitor) {
    try {
      Optional<IContainer> container = getContainer(pomDirectory);
      if(container.isPresent()) {
        IPath pomPath = IPath.fromOSString(IMavenConstants.POM_FILE_NAME);
        if(container.get().exists(pomPath)) {
          IFile pomFile = container.get().getFile(pomPath);
          IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
          IMavenProjectFacade mavenProject = projectManager.create(pomFile, true, new NullProgressMonitor());
          if(mavenProject != null) {
            return readEnforcedVersion(mavenProject, monitor);
          }
        }
      }
      //TODO: handle the case if the pomDirectory points to a project not in the workspace. Then load the bare project.
    } catch(CoreException ex) {
      logEnforcedJavaVersionCalculationError(ex);
    }
    return null;
  }

  private static final String GOAL_ENFORCE = "enforce"; //$NON-NLS-1$

  private static final String ENFORCER_PLUGIN_ARTIFACT_ID = "maven-enforcer-plugin"; //$NON-NLS-1$

  private static final String ENFORCER_PLUGIN_GROUP_ID = "org.apache.maven.plugins"; //$NON-NLS-1$

  private static String readEnforcedVersion(IMavenProjectFacade project, IProgressMonitor monitor)
      throws CoreException {
    List<MojoExecution> mojoExecutions = project.getMojoExecutions(ENFORCER_PLUGIN_GROUP_ID,
        ENFORCER_PLUGIN_ARTIFACT_ID, monitor, GOAL_ENFORCE);
    for(MojoExecution mojoExecution : mojoExecutions) {
      String version = getRequiredJavaVersionFromEnforcerRule(project.getMavenProject(monitor), mojoExecution, monitor);
      if(version != null) {
        return version;
      }
    }
    return null;
  }

  private static String getRequiredJavaVersionFromEnforcerRule(MavenProject mavenProject, MojoExecution mojoExecution,
      IProgressMonitor monitor) throws CoreException {
    // https://maven.apache.org/enforcer/enforcer-rules/requireJavaVersion.html
    List<String> parameter = List.of("rules", "requireJavaVersion", "version");
    @SuppressWarnings("restriction")
    String version = ((org.eclipse.m2e.core.internal.embedder.MavenImpl) MavenPlugin.getMaven())
        .getMojoParameterValue(mavenProject, mojoExecution, parameter, String.class, monitor);
    if(version == null) {
      return null;
    }
    // normalize version (https://issues.apache.org/jira/browse/MENFORCER-440)
    if("8".equals(version)) {
      version = "1.8";
    }
    return version;
  }

  private static void logEnforcedJavaVersionCalculationError(Throwable e) {
    ECLIPSE_LOG.error(
        "Failed to determine required Java version from maven-enforcer-plugin configuration, assuming default", e);
  }

  public static IVMInstall getBestMatchingVM(String requiredVersion) {
    try {
      VersionRange versionRange = VersionRange.createFromVersionSpec(requiredVersion);
      // find all matching JVMs (sorted by version)
      List<IVMInstall> matchingJREs = getAllMatchingJREs(versionRange);

      // for ranges with only lower bound or just a recommended version pick newest version having equal major version
      ArtifactVersion mainVersion;
      if(versionRange.getRecommendedVersion() != null) {
        mainVersion = versionRange.getRecommendedVersion();
      } else if(versionRange.getRestrictions().size() == 1
          && versionRange.getRestrictions().get(0).getUpperBound() == null) {
        mainVersion = versionRange.getRestrictions().get(0).getLowerBound();
      } else {
        mainVersion = null;
      }
      if(mainVersion != null) {
        return matchingJREs.stream()
            .filter(jre -> getJREVersion(jre).getMajorVersion() == mainVersion.getMajorVersion()).findFirst()
            .orElse(null);
      }
      return !matchingJREs.isEmpty() ? matchingJREs.get(0) : null;
    } catch(InvalidVersionSpecificationException ex) {
      log.warn("Invalid version range", ex);
    }
    return null;
  }

  private static List<IVMInstall> getAllMatchingJREs(VersionRange versionRange) {
    // find all matching JVMs and sort by their version (highest first)
    SortedMap<ArtifactVersion, IVMInstall> installedJREsByVersion = new TreeMap<>(Comparator.reverseOrder());

    for(IVMInstallType vmType : JavaRuntime.getVMInstallTypes()) {
      for(IVMInstall vm : vmType.getVMInstalls()) {
        if(satisfiesVersionRange(vm, versionRange)) {
          ArtifactVersion jreVersion = getJREVersion(vm);
          if(jreVersion != DEFAULT_JAVA_VERSION) {
            installedJREsByVersion.put(jreVersion, vm);
          } else {
            log.debug("Skipping IVMInstall '{}' from type {} as not implementing IVMInstall2", vm.getName(),
                vmType.getName());
          }
        }
      }
    }
    return List.copyOf(installedJREsByVersion.values());
  }

  private static boolean satisfiesVersionRange(IVMInstall jre, VersionRange versionRange) {
    ArtifactVersion jreVersion = getJREVersion(jre);
    if(versionRange.getRecommendedVersion() != null) {
      return jreVersion.compareTo(versionRange.getRecommendedVersion()) >= 0;
    }
    return versionRange.containsVersion(jreVersion);
  }

  private static final ArtifactVersion DEFAULT_JAVA_VERSION = new DefaultArtifactVersion("0.0.0");

  private static ArtifactVersion getJREVersion(IVMInstall jre) {
    if(jre instanceof IVMInstall2 jre2) {
      String version = jre2.getJavaVersion();
      if(version != null) {
        return new DefaultArtifactVersion(version);
      }
    }
    return DEFAULT_JAVA_VERSION;
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
    return preferencesService.getBoolean(ANSI_SUPPORT_QUALIFIER, ANSI_SUPPORT_KEY, true, null);
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
        String msg = NLS.bind(Messages.MavenLaunchDelegate_error_cannot_read_jvmConfig, jvmConfig.getAbsolutePath());
        throw new CoreException(Status.error(msg, ex));
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
