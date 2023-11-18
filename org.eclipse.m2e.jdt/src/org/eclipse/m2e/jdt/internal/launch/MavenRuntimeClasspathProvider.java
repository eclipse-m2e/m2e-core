/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others
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

package org.eclipse.m2e.jdt.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardClasspathProvider;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.jdt.IClassifierClasspathProvider;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.IMavenClassifierManager;
import org.eclipse.m2e.jdt.MavenJdtPlugin;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.jdt.internal.MavenClasspathHelpers;
import org.eclipse.m2e.jdt.internal.ModuleSupport;


public class MavenRuntimeClasspathProvider extends StandardClasspathProvider {

  private static final Logger log = LoggerFactory.getLogger(MavenRuntimeClasspathProvider.class);

  public static final String MAVEN_SOURCEPATH_PROVIDER = "org.eclipse.m2e.launchconfig.sourcepathProvider"; //$NON-NLS-1$

  public static final String MAVEN_CLASSPATH_PROVIDER = "org.eclipse.m2e.launchconfig.classpathProvider"; //$NON-NLS-1$

  private static final String THIS_PROJECT_CLASSIFIER = ""; //$NON-NLS-1$

  public static final String JDT_JUNIT_TEST = "org.eclipse.jdt.junit.launchconfig"; //$NON-NLS-1$

  public static final String JDT_JAVA_APPLICATION = "org.eclipse.jdt.launching.localJavaApplication"; //$NON-NLS-1$

  public static final String JDT_TESTNG_TEST = "org.testng.eclipse.launchconfig"; //$NON-NLS-1$

  private static final String ATTRIBUTE_ORG_ECLIPSE_JDT_JUNIT_TEST_KIND = "org.eclipse.jdt.junit.TEST_KIND"; //$NON-NLS-1$

  private static final String TESTKIND_ORG_ECLIPSE_JDT_JUNIT_LOADER_JUNIT5 = "org.eclipse.jdt.junit.loader.junit5"; //$NON-NLS-1$

  private static final String ARTIFACT_JUNIT_JUPITER_API = "junit-jupiter-api"; //$NON-NLS-1$

  private static final String ARTIFACT_JUNIT_JUPITER_ENGINE = "junit-jupiter-engine"; //$NON-NLS-1$

  private static final String ARTIFACT_JUNIT_PLATFORM_COMMONS = "junit-platform-commons"; //$NON-NLS-1$

  private static final String ARTIFACT_JUNIT_PLATFORM_ENGINE = "junit-platform-engine"; //$NON-NLS-1$

  private static final String ARTIFACT_JUNIT_PLATFORM_LAUNCHER = "junit-platform-launcher"; //$NON-NLS-1$

  private static final String GROUP_ORG_JUNIT_JUPITER = "org.junit.jupiter"; //$NON-NLS-1$

  private static final String GROUP_ORG_JUNIT_PLATFORM = "org.junit.platform"; //$NON-NLS-1$

  private static final String PROPERTY_M2E_DISABLE_ADD_MISSING_J_UNIT5_EXECUTION_DEPENDENCIES = "m2e.disableAddMissingJUnit5ExecutionDependencies";

  private static final Set<String> supportedTypes = new HashSet<>();
  static {
    // not exactly nice, but works with eclipse 3.2, 3.3 and 3.4M3
    supportedTypes.add(MavenRuntimeClasspathProvider.JDT_JAVA_APPLICATION);
    supportedTypes.add(MavenRuntimeClasspathProvider.JDT_JUNIT_TEST);
    supportedTypes.add(MavenRuntimeClasspathProvider.JDT_TESTNG_TEST);
  }

  IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();

  @Override
  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(final ILaunchConfiguration configuration)
      throws CoreException {
    boolean isModular = JavaRuntime.isModularConfiguration(configuration);
    boolean useDefault = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
    if(useDefault) {
      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
      IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(configuration);
      IRuntimeClasspathEntry projectEntry;
      if(isModular) {
        projectEntry = ModuleSupport.newModularProjectRuntimeClasspathEntry(javaProject);
      } else {
        projectEntry = JavaRuntime.newProjectRuntimeClasspathEntry(javaProject);
      }
      IRuntimeClasspathEntry mavenEntry = JavaRuntime.newRuntimeContainerClasspathEntry(
          IPath.fromOSString(IClasspathManager.CONTAINER_ID), IRuntimeClasspathEntry.USER_CLASSES);

      final List<IRuntimeClasspathEntry> entries = new ArrayList<>();
      if(jreEntry != null) {
        entries.add(jreEntry);
      }
      entries.add(projectEntry);
      entries.add(mavenEntry);

      entries.addAll(Arrays.stream(JavaRuntime.computeUnresolvedRuntimeDependencies(javaProject, true))
          .filter(e -> e.getType() == IRuntimeClasspathEntry.ARCHIVE).collect(Collectors.toList()));

      return entries.toArray(new IRuntimeClasspathEntry[0]);
    }
    // recover persisted classpath
    if(isModular) {
      IRuntimeClasspathEntry[] runtimeModulePaths = recoverRuntimePath(configuration,
          IJavaLaunchConfigurationConstants.ATTR_MODULEPATH);
      IRuntimeClasspathEntry[] runtimeClasspaths = recoverRuntimePath(configuration,
          IJavaLaunchConfigurationConstants.ATTR_CLASSPATH);
      IRuntimeClasspathEntry[] result = Arrays.copyOf(runtimeModulePaths,
          runtimeModulePaths.length + runtimeClasspaths.length);
      System.arraycopy(runtimeClasspaths, 0, result, runtimeModulePaths.length, runtimeClasspaths.length);
      return result;
    }

    return recoverRuntimePath(configuration, IJavaLaunchConfigurationConstants.ATTR_CLASSPATH);
  }

  @Override
  public IRuntimeClasspathEntry[] resolveClasspath(final IRuntimeClasspathEntry[] entries,
      final ILaunchConfiguration configuration) throws CoreException {
    IProgressMonitor monitor = new NullProgressMonitor(); // XXX
    IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
    IMavenProjectFacade projectFacade = Adapters.adapt(javaProject.getProject(), IMavenProjectFacade.class);
    IMavenExecutionContext context;
    if(projectFacade == null) {
      context = IMavenExecutionContext.getThreadContext().orElseGet(MavenPlugin.getMaven()::createExecutionContext);
    } else {
      context = projectFacade.createExecutionContext();
    }
    return context.execute((ctx, monitor1) -> resolveClasspath0(entries, configuration, monitor1), monitor);
  }

  IRuntimeClasspathEntry[] resolveClasspath0(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration,
      IProgressMonitor monitor) throws CoreException {
    IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);

    boolean isModularConfiguration = JavaRuntime.isModularConfiguration(configuration);

    int scope = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_EXCLUDE_TEST_CODE, false)
        ? IClasspathManager.CLASSPATH_RUNTIME
        : IClasspathManager.CLASSPATH_TEST;

    Set<IRuntimeClasspathEntry> all = new LinkedHashSet<>(entries.length);
    for(IRuntimeClasspathEntry entry : entries) {
      if(entry.getType() == IRuntimeClasspathEntry.CONTAINER
          && MavenClasspathHelpers.isMaven2ClasspathContainer(entry.getPath())) {
        addMavenClasspathEntries(all, entry, configuration, scope, monitor, isModularConfiguration);
      } else if(entry.getType() == IRuntimeClasspathEntry.PROJECT) {
        if(javaProject.getPath().equals(entry.getPath())) {
          addProjectEntries(all, entry.getPath(), scope, THIS_PROJECT_CLASSIFIER, configuration, monitor,
              ModuleSupport.determineClasspathPropertyForMainProject(isModularConfiguration, javaProject));
        } else {
          addStandardClasspathEntries(all, entry, configuration);
        }
      } else {
        addStandardClasspathEntries(all, entry, configuration);
      }
    }
    return all.toArray(new IRuntimeClasspathEntry[all.size()]);
  }

  private void addStandardClasspathEntries(Set<IRuntimeClasspathEntry> all, IRuntimeClasspathEntry entry,
      ILaunchConfiguration configuration) throws CoreException {
    IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveRuntimeClasspathEntry(entry, configuration);
    Collections.addAll(all, resolved);
  }

  private void addMavenClasspathEntries(Set<IRuntimeClasspathEntry> resolved,
      IRuntimeClasspathEntry runtimeClasspathEntry, ILaunchConfiguration configuration, int scope,
      IProgressMonitor monitor, boolean isModularConfiguration) throws CoreException {
    IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
    MavenJdtPlugin plugin = MavenJdtPlugin.getDefault();
    IClasspathManager buildpathManager = plugin.getBuildpathManager();
    IClasspathEntry[] cp = buildpathManager.getClasspath(javaProject.getProject(), scope, false, monitor);
    for(IClasspathEntry entry : cp) {
      int classpathProperty = isModularConfiguration ? ModuleSupport.determineModularClasspathProperty(entry)
          : IRuntimeClasspathEntry.USER_CLASSES;
      switch(entry.getEntryKind()) {
        case IClasspathEntry.CPE_PROJECT:
          addProjectEntries(resolved, entry.getPath(), scope, getArtifactClassifier(entry), configuration, monitor,
              classpathProperty);
          break;
        case IClasspathEntry.CPE_LIBRARY:
          resolved.add(JavaRuntime.newArchiveRuntimeClasspathEntry(entry.getPath(), classpathProperty));
          break;
//        case IClasspathEntry.CPE_SOURCE:
//          resolved.add(newSourceClasspathEntry(javaProject, cp[i]));
//          break;
      }
    }

    if(scope == IClasspathManager.CLASSPATH_TEST && TESTKIND_ORG_ECLIPSE_JDT_JUNIT_LOADER_JUNIT5
        .equals(configuration.getAttribute(ATTRIBUTE_ORG_ECLIPSE_JDT_JUNIT_TEST_KIND, ""))) {
      addMissingJUnit5ExecutionDependencies(resolved, monitor, javaProject);
    }
  }

  private void addMissingJUnit5ExecutionDependencies(Set<IRuntimeClasspathEntry> resolved, IProgressMonitor monitor,
      IJavaProject javaProject) throws CoreException {
    IMavenProjectFacade facade = projectManager.create(javaProject.getProject(), monitor);
    MavenProject mavenProject = facade.getMavenProject(monitor);
    if(Boolean.parseBoolean(mavenProject.getProperties()
        .getProperty(PROPERTY_M2E_DISABLE_ADD_MISSING_J_UNIT5_EXECUTION_DEPENDENCIES, "false"))) { //$NON-NLS-1$
      return;
    }
    Artifact platformLauncherArtifact = null;
    Artifact platformEngineArtifact = null;
    Artifact jupiterEngineArtifact = null;
    Artifact platformCommonsArtifact = null;
    Artifact jupiterApiArtifact = null;
    for(Artifact a : mavenProject.getArtifacts()) {
      if(BuildPathManager.SCOPE_FILTER_TEST.include(a) && a.getArtifactHandler().isAddedToClasspath()) {
        String groupId = a.getGroupId();
        String artifactId = a.getArtifactId();
        if(GROUP_ORG_JUNIT_PLATFORM.equals(groupId)) {
          if(ARTIFACT_JUNIT_PLATFORM_LAUNCHER.equals(artifactId)) {
            platformLauncherArtifact = a;
          } else if(ARTIFACT_JUNIT_PLATFORM_ENGINE.equals(artifactId)) {
            platformEngineArtifact = a;
          } else if(ARTIFACT_JUNIT_PLATFORM_COMMONS.equals(artifactId)) {
            platformCommonsArtifact = a;
          }
        } else if(GROUP_ORG_JUNIT_JUPITER.equals(groupId)) {
          if(ARTIFACT_JUNIT_JUPITER_ENGINE.equals(artifactId)) {
            jupiterEngineArtifact = a;
          } else if(ARTIFACT_JUNIT_JUPITER_API.equals(artifactId)) {
            jupiterApiArtifact = a;
          }
        }
      }
    }
    // even junit-jupiter-api depends on junit-platform-commons, so it should always be present
    if(platformCommonsArtifact != null && platformLauncherArtifact == null) {
      addResolvedJUnit5Dependency(resolved, GROUP_ORG_JUNIT_PLATFORM, platformCommonsArtifact.getVersion(),
          ARTIFACT_JUNIT_PLATFORM_LAUNCHER, mavenProject, monitor);
    }
    // required for junit-platform-launcher, but might be already present if pom contains engine
    if(platformCommonsArtifact != null && platformEngineArtifact == null) {
      addResolvedJUnit5Dependency(resolved, GROUP_ORG_JUNIT_PLATFORM, platformCommonsArtifact.getVersion(),
          ARTIFACT_JUNIT_PLATFORM_ENGINE, mavenProject, monitor);
    }
    // engine might be automagically added by surefire, so we add it, too
    if(jupiterApiArtifact != null && jupiterEngineArtifact == null) {
      addResolvedJUnit5Dependency(resolved, GROUP_ORG_JUNIT_JUPITER, jupiterApiArtifact.getVersion(),
          ARTIFACT_JUNIT_JUPITER_ENGINE, mavenProject, monitor);
    }
  }

  private void addResolvedJUnit5Dependency(Set<IRuntimeClasspathEntry> resolved, String groupId, String version,
      String artifactId, MavenProject mavenProject, IProgressMonitor monitor) {
    try {
      File file = MavenPlugin.getMaven()
          .resolve(groupId, artifactId, version, "jar", null, mavenProject.getRemoteArtifactRepositories(), monitor) //$NON-NLS-1$
          .getFile();
      if(file != null) {
        resolved.add(JavaRuntime.newArchiveRuntimeClasspathEntry(IPath.fromOSString(file.getAbsolutePath()),
            IRuntimeClasspathEntry.USER_CLASSES));
      }
    } catch(CoreException ex) {
      log.error("Could not resolve JUnit5 dependency " + groupId + ":" + artifactId + ":" + version, ex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
  }

  protected void addProjectEntries(Set<IRuntimeClasspathEntry> resolved, IPath path, int scope, String classifier,
      ILaunchConfiguration launchConfiguration, final IProgressMonitor monitor, int classpathProperty)
      throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = root.getProject(path.segment(0));

    IMavenProjectFacade projectFacade = projectManager.create(project, monitor);
    if(projectFacade == null) {
      return;
    }

    IProjectConfiguration configuration = projectFacade.getConfiguration();
    if(configuration == null) {
      return;
    }

    IJavaProject javaProject = JavaCore.create(project);

    boolean projectResolved = false;

    for(IClasspathEntry entry : javaProject.getRawClasspath()) {
      IRuntimeClasspathEntry rce = null;
      switch(entry.getEntryKind()) {
        case IClasspathEntry.CPE_SOURCE:
          if(!projectResolved) {

            IMavenClassifierManager mavenClassifierManager = MavenJdtPlugin.getDefault().getMavenClassifierManager();
            IClassifierClasspathProvider classifierClasspathProvider = mavenClassifierManager
                .getClassifierClasspathProvider(projectFacade, classifier);

            if(IClasspathManager.CLASSPATH_TEST == scope) {
              classifierClasspathProvider.setTestClasspath(resolved, projectFacade, monitor, classpathProperty);
            } else {
              classifierClasspathProvider.setRuntimeClasspath(resolved, projectFacade, monitor, classpathProperty);
            }

            projectResolved = true;
          }
          break;
        case IClasspathEntry.CPE_CONTAINER:
          IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), javaProject);
          if(container != null && !MavenClasspathHelpers.isMaven2ClasspathContainer(entry.getPath())) {
            switch(container.getKind()) {
              case IClasspathContainer.K_APPLICATION:
                rce = JavaRuntime.newRuntimeContainerClasspathEntry(container.getPath(),
                    IRuntimeClasspathEntry.USER_CLASSES, javaProject);
                break;
//                case IClasspathContainer.K_DEFAULT_SYSTEM:
//                  unresolved.add(JavaRuntime.newRuntimeContainerClasspathEntry(container.getPath(), IRuntimeClasspathEntry.STANDARD_CLASSES, javaProject));
//                  break;
//                case IClasspathContainer.K_SYSTEM:
//                  unresolved.add(JavaRuntime.newRuntimeContainerClasspathEntry(container.getPath(), IRuntimeClasspathEntry.BOOTSTRAP_CLASSES, javaProject));
//                  break;
            }
          }
          break;
        case IClasspathEntry.CPE_LIBRARY:
          rce = JavaRuntime.newArchiveRuntimeClasspathEntry(entry.getPath(),
              classpathProperty == IRuntimeClasspathEntry.USER_CLASSES ? IRuntimeClasspathEntry.USER_CLASSES
                  : ModuleSupport.determineModularClasspathProperty(entry));
          break;
        case IClasspathEntry.CPE_VARIABLE:
          if(!JavaRuntime.JRELIB_VARIABLE.equals(entry.getPath().segment(0))) {
            rce = JavaRuntime.newVariableRuntimeClasspathEntry(entry.getPath());
          }
          break;
        case IClasspathEntry.CPE_PROJECT:
          IProject res = root.getProject(entry.getPath().segment(0));
          if(res != null) {
            IJavaProject otherProject = JavaCore.create(res);
            if(otherProject != null) {
              rce = JavaRuntime.newDefaultProjectClasspathEntry(otherProject);
            }
          }
          break;
        default:
          break;
      }
      if(rce != null) {
        addStandardClasspathEntries(resolved, rce, launchConfiguration);
      }
    }
  }

  public static boolean isSupportedType(String id) {
    return supportedTypes.contains(id);
  }

  public static void enable(ILaunchConfiguration config) throws CoreException {
    if(config instanceof ILaunchConfigurationWorkingCopy wc) {
      enable(wc);
    } else {
      ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
      enable(wc);
      wc.doSave();
    }
  }

  private static void enable(ILaunchConfigurationWorkingCopy wc) {
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, MAVEN_CLASSPATH_PROVIDER);
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, MAVEN_SOURCEPATH_PROVIDER);
  }

  public static void disable(ILaunchConfiguration config) throws CoreException {
    ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, (String) null);
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String) null);
    wc.doSave();
  }

  private static String getArtifactClassifier(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    for(IClasspathAttribute attribute : attributes) {
      if(IClasspathManager.CLASSIFIER_ATTRIBUTE.equals(attribute.getName())) {
        return attribute.getValue();
      }
    }
    return null;
  }

  public static void enable(IProject project) throws CoreException {
    for(ILaunchConfiguration config : getLaunchConfiguration(project)) {
      if(isSupportedType(config.getType().getIdentifier())) {
        enable(config);
      }
    }
  }

  public static void disable(IProject project) throws CoreException {
    for(ILaunchConfiguration config : getLaunchConfiguration(project)) {
      if(isSupportedType(config.getType().getIdentifier())) {
        disable(config);
      }
    }
  }

  private static List<ILaunchConfiguration> getLaunchConfiguration(IProject project) throws CoreException {
    ArrayList<ILaunchConfiguration> result = new ArrayList<>();
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfiguration[] configurations = launchManager.getLaunchConfigurations();
    for(ILaunchConfiguration config : configurations) {
      String projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
      if(project.getName().equals(projectName)) {
        result.add(config);
      }
    }
    return result;
  }

}
