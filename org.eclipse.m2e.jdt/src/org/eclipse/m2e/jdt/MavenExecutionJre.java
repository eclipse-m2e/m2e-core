/********************************************************************************
 * Copyright (c) 2025, 2025 715508 and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   715508 - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.jdt;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * Wrapper around the JRE version suitable for executing Maven steps (i.e. plugin goals or test execution). This is not
 * necessarily the same as the JRE as Eclipse's project JRE (which reflects the target runtime).
 */
public class MavenExecutionJre {

  private final String executionJreVersionRange;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MavenExecutionJre.class);

  private static final String GOAL_ENFORCE = "enforce"; //$NON-NLS-1$

  private static final String ENFORCER_PLUGIN_ARTIFACT_ID = "maven-enforcer-plugin"; //$NON-NLS-1$

  private static final String ENFORCER_PLUGIN_GROUP_ID = "org.apache.maven.plugins"; //$NON-NLS-1$

  private static final ArtifactVersion DEFAULT_JAVA_VERSION = new DefaultArtifactVersion("0.0.0"); //$NON-NLS-1$

  public static Optional<MavenExecutionJre> forProject(IMavenProjectFacade project, IProgressMonitor monitor)
      throws CoreException {
    Optional<String> executionJreVersionRange = readEnforcedVersion(project, monitor);
    return executionJreVersionRange.map(MavenExecutionJre::new);
  }

  private MavenExecutionJre(String executionJreVersionRange) {
    this.executionJreVersionRange = executionJreVersionRange;
  }

  private static Optional<String> readEnforcedVersion(IMavenProjectFacade project, IProgressMonitor monitor)
      throws CoreException {
    List<MojoExecution> mojoExecutions = project.getMojoExecutions(ENFORCER_PLUGIN_GROUP_ID,
        ENFORCER_PLUGIN_ARTIFACT_ID, monitor, GOAL_ENFORCE);
    for(MojoExecution mojoExecution : mojoExecutions) {
      Optional<String> version = getRequiredJavaVersionFromEnforcerRule(project.getMavenProject(monitor), mojoExecution,
          monitor);
      if(version.isPresent()) {
        return version;
      }
    }
    log.debug("No 'requireJavaVersion' rule found in maven-enforcer-plugin executions for project {}",
        project.getProject().getName());
    return Optional.empty();
  }

  private static Optional<String> getRequiredJavaVersionFromEnforcerRule(MavenProject mavenProject,
      MojoExecution mojoExecution, IProgressMonitor monitor) throws CoreException {
    // https://maven.apache.org/enforcer/enforcer-rules/requireJavaVersion.html
    List<String> parameter = List.of("rules", "requireJavaVersion", "version");
    @SuppressWarnings("restriction")
    String version = ((org.eclipse.m2e.core.internal.embedder.MavenImpl) MavenPlugin.getMaven())
        .getMojoParameterValue(mavenProject, mojoExecution, parameter, String.class, monitor);
    if(version == null) {
      return Optional.empty();
    }
    // normalize version (https://issues.apache.org/jira/browse/MENFORCER-440)
    if("8".equals(version)) {
      version = "1.8";
    }
    return Optional.of(version);
  }

  public String getExecutionJreVersionRange() {
    return executionJreVersionRange;
  }

  public Optional<IVMInstall> getBestMatchingVM() {
    return getBestMatchingVM(executionJreVersionRange);
  }

  /**
   * The returned ID can be used e.g. as value for {@link IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH}.
   * 
   * @return the best matching JRE ID, or empty if no matching JRE could be found
   */
  public Optional<String> getBestMatchingJreContainerPath() {
    return getBestMatchingVM().map(vm -> JavaRuntime.newJREContainerPath(vm).toPortableString());
  }

  public static Optional<IVMInstall> getBestMatchingVM(String requiredVersion) {
    try {
      VersionRange versionRange = VersionRange.createFromVersionSpec(requiredVersion);
      // find all matching JVMs (sorted by version)
      List<IVMInstall> matchingJREs = getAllMatchingJREs(versionRange);
      if(matchingJREs.isEmpty()) {
        return Optional.empty();
      }

      // for ranges with only lower bound or just a recommended version:
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
        // pick nearest major version
        int nearestMajorVersion = getJREVersion(matchingJREs.getLast()).getMajorVersion();
        // with highest minor/patch version
        return matchingJREs.stream().filter(jre -> getJREVersion(jre).getMajorVersion() == nearestMajorVersion)
            .findFirst();
      }
      // otherwise use highest matching version
      return Optional.of(matchingJREs.getFirst());
    } catch(InvalidVersionSpecificationException ex) {
      log.warn("Invalid version range", ex);
    }
    return Optional.empty();
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

  private static ArtifactVersion getJREVersion(IVMInstall jre) {
    if(jre instanceof IVMInstall2 jre2) {
      String version = jre2.getJavaVersion();
      if(version != null) {
        return new DefaultArtifactVersion(version);
      }
    }
    return DEFAULT_JAVA_VERSION;
  }
}
