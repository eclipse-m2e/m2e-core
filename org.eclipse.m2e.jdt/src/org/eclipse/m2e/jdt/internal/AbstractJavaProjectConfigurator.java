/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype, Inc. and others.
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

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.eclipse.m2e.jdt.JreSystemVersion;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * AbstractJavaProjectConfigurator
 *
 * @author igor
 */
public abstract class AbstractJavaProjectConfigurator extends AbstractProjectConfigurator
    implements IJavaProjectConfigurator {

  private static final IPath[] DEFAULT_INCLUSIONS = new IPath[0];

  private static final Logger log = LoggerFactory.getLogger(AbstractJavaProjectConfigurator.class);

  private static final String GOAL_COMPILE = "compile";

  private static final String GOAL_TESTCOMPILE = "testCompile";

  public static final String COMPILER_PLUGIN_ARTIFACT_ID = "maven-compiler-plugin";

  public static final String COMPILER_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

  protected static final List<String> RELEASES;

  protected static final List<String> SOURCES;

  protected static final List<String> TARGETS;

  private static final String GOAL_RESOURCES = "resources";

  private static final String GOAL_TESTRESOURCES = "testResources";

  private static final String RESOURCES_PLUGIN_ARTIFACT_ID = "maven-resources-plugin";

  private static final String RESOURCES_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

  protected static final LinkedHashMap<String, String> ENVIRONMENTS = new LinkedHashMap<>();

  static {
    Set<String> supportedExecutionEnvironmentTypes = Set.of("JRE", "J2SE", "JavaSE");

    List<String> sources = new ArrayList<>();

    List<String> targets = new ArrayList<>();
    //Special case
    targets.add("jsr14"); //$NON-NLS-1$
    ENVIRONMENTS.put("jsr14", "J2SE-1.5"); //$NON-NLS-1$ //$NON-NLS-2$

    List<String> releases = new ArrayList<>(List.of("6", "7", "8"));

    for(var ee : JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments()) {
      var eeId = ee.getId();
      if(supportedExecutionEnvironmentTypes.stream().filter(type -> eeId.startsWith(type)).findAny().isEmpty()) {
        continue;
      }
      var compliance = ee.getComplianceOptions().get(JavaCore.COMPILER_COMPLIANCE);
      if(compliance != null) {
        sources.add(compliance);
        targets.add(compliance);
        if(JavaCore.ENABLED.equals(ee.getComplianceOptions().get(JavaCore.COMPILER_RELEASE))) {
          releases.add(compliance);
        }
        ENVIRONMENTS.put(compliance, eeId);
      }
    }

    SOURCES = Collections.unmodifiableList(sources);
    TARGETS = Collections.unmodifiableList(targets);
    RELEASES = Collections.unmodifiableList(releases);
  }

  protected static final String DEFAULT_COMPILER_LEVEL = "1.5"; //$NON-NLS-1$

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    IProject project = request.mavenProjectFacade().getProject();

    monitor.setTaskName(Messages.AbstractJavaProjectConfigurator_task_name + project.getName());

    addJavaNature(project, monitor);

    IJavaProject javaProject = JavaCore.create(project);

    Map<String, String> options = new HashMap<>();

    addJavaProjectOptions(options, request, monitor);

    IClasspathDescriptor classpath = new ClasspathDescriptor(javaProject);

    addProjectSourceFolders(classpath, options, request, monitor);

    String executionEnvironmentId = getExecutionEnvironmentId(options);
    addJREClasspathContainer(classpath, executionEnvironmentId);

    addMavenClasspathContainer(classpath);

    addCustomClasspathEntries(javaProject, classpath);

    IContainer classesFolder = getOutputLocation(request, project);

    // allow module resolution in invokeJavaProjectConfigurators step
    javaProject.setRawClasspath(classpath.getEntries(), classesFolder.getFullPath(), monitor);

    invokeJavaProjectConfigurators(classpath, request, monitor);

    // now apply new configuration

    // A single setOptions call erases everything else from an existing settings file.
    // Must invoke setOption individually to preserve previous options.
    for(Map.Entry<String, String> option : options.entrySet()) {
      javaProject.setOption(option.getKey(), option.getValue());
    }

    javaProject.setRawClasspath(classpath.getEntries(), classesFolder.getFullPath(), monitor);

    MavenJdtPlugin.getDefault().getBuildpathManager().updateClasspath(project, monitor);
  }

  @SuppressWarnings("unused")
  protected IContainer getOutputLocation(ProjectConfigurationRequest request, IProject project) throws CoreException {
    MavenProject mavenProject = request.mavenProject();
    return getFolder(project, mavenProject.getBuild().getOutputDirectory());
  }

  protected String getExecutionEnvironmentId(Map<String, String> options) {
    return ENVIRONMENTS.get(options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));
  }

  protected void addJavaNature(IProject project, IProgressMonitor monitor) throws CoreException {
    addNature(project, JavaCore.NATURE_ID, monitor);
  }

  protected void addCustomClasspathEntries(IJavaProject javaProject, IClasspathDescriptor classpath) {
  }

  protected void invokeJavaProjectConfigurators(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      final IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = request.mavenProjectFacade();
    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(facade);
    if(lifecycleMapping == null) {
      return;
    }
    for(AbstractProjectConfigurator configurator : lifecycleMapping.getProjectConfigurators(facade, monitor)) {
      if(configurator instanceof IJavaProjectConfigurator javaConfigurator) {
        javaConfigurator.configureRawClasspath(request, classpath, monitor);
      }
    }
  }

  protected void addJREClasspathContainer(IClasspathDescriptor classpath, String environmentId) {

    IClasspathEntry cpe;
    IExecutionEnvironment executionEnvironment = getExecutionEnvironment(environmentId);
    if(executionEnvironment == null) {
      cpe = JavaRuntime.getDefaultJREContainerEntry();
    } else {
      IPath containerPath = JavaRuntime.newJREContainerPath(executionEnvironment);
      cpe = JavaCore.newContainerEntry(containerPath);
    }

    IClasspathEntryDescriptor cped = classpath
        .replaceEntry(descriptor -> JavaRuntime.JRE_CONTAINER.equals(descriptor.getPath().segment(0)), cpe);

    if(cped == null) {
      classpath.addEntry(cpe);
    }
  }

  private IExecutionEnvironment getExecutionEnvironment(String environmentId) {
    if(environmentId == null
        || JreSystemVersion.WORKSPACE_DEFAULT == MavenJdtPlugin.getDefault().getJreSystemVersion()) {
      return null;
    }
    IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
    IExecutionEnvironment environment = manager.getEnvironment(environmentId);
    if(environment != null && environment.getCompatibleVMs().length > 0) {
      return environment;
    }
    log.error("Failed to find a compatible VM for environment id '{}', falling back to workspace default",
        environmentId);
    return null;
  }

  protected void addMavenClasspathContainer(IClasspathDescriptor classpath) {
    List<IClasspathEntryDescriptor> descriptors = classpath.getEntryDescriptors();
    List<IAccessRule> accessRules = new ArrayList<>();
    boolean isExported = false;
    for(IClasspathEntryDescriptor descriptor : descriptors) {
      if(MavenClasspathHelpers.isMaven2ClasspathContainer(descriptor.getPath())) {
        isExported = descriptor.isExported();
        List<IAccessRule> previousAccessRules = descriptor.getAccessRules();
        if(previousAccessRules != null) {
          accessRules.addAll(previousAccessRules);
        }
        break;
      }
    }

    IClasspathEntry cpe = MavenClasspathHelpers.getDefaultContainerEntry();
    // add new entry without removing existing entries first, see bug398121
    IClasspathEntryDescriptor entryDescriptor = classpath.addEntry(cpe);
    entryDescriptor.setExported(isExported);
    for(IAccessRule accessRule : accessRules) {
      entryDescriptor.addAccessRule(accessRule);
    }
  }

  protected void addProjectSourceFolders(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    addProjectSourceFolders(classpath, new HashMap<>(), request, monitor);
  }

  protected void addProjectSourceFolders(IClasspathDescriptor classpath, Map<String, String> options,
      ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    SubMonitor mon = SubMonitor.convert(monitor, 6);
    try {
      IProject project = request.mavenProjectFacade().getProject();
      MavenProject mavenProject = request.mavenProject();
      IMavenProjectFacade projectFacade = request.mavenProjectFacade();

      IFolder classes = getFolder(project, mavenProject.getBuild().getOutputDirectory());
      IFolder testClasses = getFolder(project, mavenProject.getBuild().getTestOutputDirectory());

      M2EUtils.createFolder(classes, true, mon.newChild(1));
      M2EUtils.createFolder(testClasses, true, mon.newChild(1));

      IPath[] inclusion = new IPath[0];
      IPath[] exclusion = new IPath[0];

      IPath[] inclusionTest = new IPath[0];
      IPath[] exclusionTest = new IPath[0];

      String mainSourceEncoding = null;
      String testSourceEncoding = null;

      String mainResourcesEncoding = null;
      String testResourcesEncoding = null;

      List<Boolean> isTestCompilationSkipped = new ArrayList<>();
      List<Boolean> isTestResourcesSkipped = new ArrayList<>();

      List<MojoExecution> executions = getCompilerMojoExecutions(request, mon.newChild(1));
      for(MojoExecution compile : executions) {
        if(isCompileExecution(compile, mavenProject, options, monitor)) {
          mainSourceEncoding = maven.getMojoParameterValue(mavenProject, compile, "encoding", String.class, monitor); //$NON-NLS-1$
          try {
            inclusion = toPaths(
                maven.getMojoParameterValue(mavenProject, compile, "includes", String[].class, monitor)); //$NON-NLS-1$
          } catch(CoreException ex) {
            log.error("Failed to determine compiler inclusions, assuming defaults", ex);
          }
          try {
            exclusion = toPaths(
                maven.getMojoParameterValue(mavenProject, compile, "excludes", String[].class, monitor)); //$NON-NLS-1$
          } catch(CoreException ex) {
            log.error("Failed to determine compiler exclusions, assuming defaults", ex);
          }
        }
      }

      for(MojoExecution compile : executions) {
        if(isTestCompileExecution(compile, mavenProject, options, monitor)) {
          testSourceEncoding = maven.getMojoParameterValue(mavenProject, compile, "encoding", String.class, monitor); //$NON-NLS-1$
          try {
            inclusionTest = toPaths(
                maven.getMojoParameterValue(mavenProject, compile, "testIncludes", String[].class, monitor)); //$NON-NLS-1$
          } catch(CoreException ex) {
            log.error("Failed to determine compiler test inclusions, assuming defaults", ex);
          }
          try {
            exclusionTest = toPaths(
                maven.getMojoParameterValue(mavenProject, compile, "testExcludes", String[].class, monitor)); //$NON-NLS-1$
          } catch(CoreException ex) {
            log.error("Failed to determine compiler test exclusions, assuming defaults", ex);
          }
          try {
            isTestCompilationSkipped
                .add(maven.getMojoParameterValue(mavenProject, compile, "skip", Boolean.class, monitor));
          } catch(Exception ex) {
            isTestCompilationSkipped.add(Boolean.FALSE);
          }
        }
      }

      for(MojoExecution resources : projectFacade.getMojoExecutions(RESOURCES_PLUGIN_GROUP_ID,
          RESOURCES_PLUGIN_ARTIFACT_ID, mon.newChild(1), GOAL_RESOURCES)) {
        mainResourcesEncoding = maven.getMojoParameterValue(mavenProject, resources, "encoding", String.class, monitor); //$NON-NLS-1$
      }

      for(MojoExecution resources : projectFacade.getMojoExecutions(RESOURCES_PLUGIN_GROUP_ID,
          RESOURCES_PLUGIN_ARTIFACT_ID, mon.newChild(1), GOAL_TESTRESOURCES)) {
        testResourcesEncoding = maven.getMojoParameterValue(mavenProject, resources, "encoding", String.class, monitor); //$NON-NLS-1$
        try {
          isTestResourcesSkipped
              .add(maven.getMojoParameterValue(mavenProject, resources, "skip", Boolean.class, monitor));
        } catch(Exception ex) {
          isTestResourcesSkipped.add(Boolean.FALSE);
        }
      }
      addSourceDirs(classpath, project, mavenProject.getCompileSourceRoots(), classes.getFullPath(), inclusion,
          exclusion, mainSourceEncoding, mon.newChild(1), false);
      addResourceDirs(classpath, project, mavenProject, mavenProject.getBuild().getResources(), classes.getFullPath(),
          mainResourcesEncoding, mon.newChild(1), false);

      //If the project properties contain m2e.disableTestClasspathFlag=true, then the test flag must not be set
      boolean addTestFlag = !MavenClasspathHelpers.hasTestFlagDisabled(mavenProject);
      if(isTestCompilationSkipped.isEmpty() || !isTestCompilationSkipped.stream().allMatch(Boolean.TRUE::equals)) {
        addSourceDirs(classpath, project, mavenProject.getTestCompileSourceRoots(), testClasses.getFullPath(),
            inclusionTest, exclusionTest, testSourceEncoding, mon.newChild(1), addTestFlag);
      }
      if(isTestResourcesSkipped.isEmpty() || !isTestResourcesSkipped.stream().allMatch(Boolean.TRUE::equals)) {
        addResourceDirs(classpath, project, mavenProject, mavenProject.getBuild().getTestResources(),
            testClasses.getFullPath(), testResourcesEncoding, mon.newChild(1), addTestFlag);
      }
    } finally {
      mon.done();
    }
  }

  protected boolean isTestCompileExecution(MojoExecution execution, MavenProject mavenProject,
      Map<String, String> options, IProgressMonitor monitor) throws CoreException {
    return GOAL_TESTCOMPILE.equals(execution.getGoal()) && isCompliant(execution, mavenProject, options, monitor);
  }

  protected boolean isCompileExecution(MojoExecution execution, MavenProject mavenProject, Map<String, String> options,
      IProgressMonitor monitor) throws CoreException {
    return GOAL_COMPILE.equals(execution.getGoal()) && isCompliant(execution, mavenProject, options, monitor);
  }

  private boolean isCompliant(MojoExecution execution, MavenProject mavenProject, Map<String, String> options,
      IProgressMonitor monitor) throws CoreException {
    String release = maven.getMojoParameterValue(mavenProject, execution, "release", String.class, monitor); //$NON-NLS-1$
    if(release != null && !sanitizeJavaVersion(release).equals(options.get(JavaCore.COMPILER_COMPLIANCE))) {
      return false;
    }
    if(release == null) {
      String source = maven.getMojoParameterValue(mavenProject, execution, "source", String.class, monitor); //$NON-NLS-1$
      if(source != null && !sanitizeJavaVersion(source).equals(options.get(JavaCore.COMPILER_SOURCE))) {
        return false;
      }
      String target = maven.getMojoParameterValue(mavenProject, execution, "target", String.class, monitor); //$NON-NLS-1$
      if(target != null
          && !sanitizeJavaVersion(target).equals(options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM))) {
        return false;
      }
    }
    return true;
  }

  private IPath[] toPaths(String[] values) {
    if(values == null) {
      return new IPath[0];
    }
    IPath[] paths = new IPath[values.length];
    for(int i = 0; i < values.length; i++ ) {
      if(values[i] != null && !"".equals(values[i].trim())) {
        paths[i] = new Path(values[i]);
      }
    }
    return paths;
  }

  protected void addSourceDirs(IClasspathDescriptor classpath, IProject project, List<String> sourceRoots,
      IPath outputPath, IPath[] inclusion, IPath[] exclusion, String sourceEncoding, IProgressMonitor monitor,
      boolean addTestFlag) throws CoreException {

    for(String sourceRoot : sourceRoots) {
      IFolder sourceFolder = getFolder(project, sourceRoot);

      if(sourceFolder == null) {
        // this cannot actually happen, unless I misunderstand how project.getFolder works
        continue;
      }

      // be extra nice to less perfectly written maven plugins, which contribute compile source root to the model
      // but do not use BuildContext to tell as about the actual resources
      sourceFolder.refreshLocal(IResource.DEPTH_ZERO, monitor);

      if(sourceFolder.exists() && !sourceFolder.getProject().equals(project)) {
        // source folders outside of ${project.basedir} are not supported
        continue;
      }

      // Set folder encoding (null = platform/container default)
      if(sourceFolder.exists()) {
        sourceFolder.setDefaultCharset(sourceEncoding, monitor);
      }

      IClasspathEntryDescriptor enclosing = getEnclosingEntryDescriptor(classpath, sourceFolder.getFullPath());
      if(enclosing == null || getEntryDescriptor(classpath, sourceFolder.getFullPath()) != null) {
        log.info("Adding source folder " + sourceFolder.getFullPath());

        // source folder entries are created even when corresponding resources do not actually exist in workspace
        // to keep JDT from complaining too loudly about non-existing folders,
        // all source entries are marked as generated (a.k.a. optional)
        IClasspathEntryDescriptor descriptor = classpath.addSourceEntry(sourceFolder.getFullPath(), outputPath,
            inclusion, exclusion, true /*generated*/);
        descriptor.setClasspathAttribute(IClasspathManager.TEST_ATTRIBUTE, addTestFlag ? "true" : null);
      } else {
        log.info("Not adding source folder " + sourceFolder.getFullPath() + " because it overlaps with "
            + enclosing.getPath());
      }
    }

  }

  private IClasspathEntryDescriptor getEnclosingEntryDescriptor(IClasspathDescriptor classpath, IPath fullPath) {
    for(IClasspathEntryDescriptor cped : classpath.getEntryDescriptors()) {
      if(cped.getPath().isPrefixOf(fullPath)) {
        return cped;
      }
    }
    return null;
  }

  private IClasspathEntryDescriptor getEntryDescriptor(IClasspathDescriptor classpath, IPath fullPath) {
    for(IClasspathEntryDescriptor cped : classpath.getEntryDescriptors()) {
      if(cped.getPath().equals(fullPath)) {
        return cped;
      }
    }
    return null;
  }

  private void addResourceDirs(IClasspathDescriptor classpath, IProject project, MavenProject mavenProject,
      List<Resource> resources, IPath outputPath, String resourceEncoding, IProgressMonitor monitor,
      boolean addTestFlag) throws CoreException {

    for(Resource resource : resources) {
      String directory = resource.getDirectory();
      if(directory == null) {
        continue;
      }
      File resourceDirectory = new File(directory);
      IPath relativePath = getProjectRelativePath(project, directory);
      IResource r = project.findMember(relativePath);
      if(r == project) {
        /*
         * Workaround for the Java Model Exception:
         *   Cannot nest output folder 'xxx/src/main/resources' inside output folder 'xxx'
         * when pom.xml have something like this:
         *
         * <build>
         *   <resources>
         *     <resource>
         *       <directory>${basedir}</directory>
         *       <targetPath>META-INF</targetPath>
         *       <includes>
         *         <include>LICENSE</include>
         *       </includes>
         *     </resource>
         */
        log.error("Skipping resource folder " + r.getFullPath());
        return;
      }
      if(r == null) {
        //this means the resources does not exits (yet) but might be created later on!
        r = project.getFolder(relativePath);
      }
      if(project.equals(r.getProject())) {
        IPath path = r.getFullPath();
        IClasspathEntryDescriptor enclosing = getEnclosingEntryDescriptor(classpath, path);
        if(enclosing != null && overlapsWithSourceFolder(path, project, mavenProject)) {
          configureOverlapWithSource(classpath, enclosing, path);
        } else if(overlapsWithOtherResourceFolder(path, project, mavenProject)) {
          // skip adding resource folders that are included by other resource folders
          log.info("Skipping resource folder " + path + " since it's contained by another resource folder");
        } else {
          addResourceFolder(classpath, path, outputPath, addTestFlag);
        }
        // Set folder encoding (null = platform default)
        IFolder resourceFolder = project.getFolder(relativePath);
        if(resourceFolder.exists()) {
          resourceFolder.setDefaultCharset(resourceEncoding, monitor);
        }
      } else {
        log.info("Not adding resources folder " + resourceDirectory.getAbsolutePath());
      }
    }
  }

  private void addResourceFolder(IClasspathDescriptor classpath, IPath resourceFolder, IPath outputPath,
      boolean addTestFlag) {
    log.info("Adding resource folder " + resourceFolder);
    IClasspathEntryDescriptor descriptor = classpath.addSourceEntry(resourceFolder, outputPath, DEFAULT_INCLUSIONS,
        new IPath[] {new Path("**")}, false /*optional*/);
    descriptor.setClasspathAttribute(IClasspathManager.TEST_ATTRIBUTE, addTestFlag ? "true" : null);
    descriptor.setClasspathAttribute(IClasspathAttribute.OPTIONAL, "true"); //$NON-NLS-1$
  }

  private void configureOverlapWithSource(IClasspathDescriptor classpath, IClasspathEntryDescriptor enclosing,
      IPath resourceFolder) {
    // resources and sources folders overlap. make sure JDT only processes java sources.
    log.info("Resources folder " + resourceFolder + " overlaps with sources folder " + enclosing.getPath());
    enclosing.addInclusionPattern(new Path("**/*.java"));
    enclosing.removeExclusionPattern(new Path("**"));
    classpath.touchEntry(resourceFolder);
  }

  private boolean overlapsWithSourceFolder(IPath path, IProject project, MavenProject mavenProject) {
    IPath relPath = path.makeRelativeTo(project.getFullPath());
    List<String> compile = mavenProject.getCompileSourceRoots();
    List<String> test = mavenProject.getTestCompileSourceRoots();
    return isContained(relPath, project, getSourceFolders(project, compile))
        || isContained(relPath, project, getSourceFolders(project, test));
  }

  private boolean overlapsWithOtherResourceFolder(IPath path, IProject project, MavenProject mavenProject) {
    IPath relPath = path.makeRelativeTo(project.getFullPath());
    return isContained(relPath, project, getOtherResourceFolders(project, mavenProject.getResources(), relPath))
        || isContained(relPath, project, getOtherResourceFolders(project, mavenProject.getTestResources(), relPath));
  }

  private IPath[] getSourceFolders(IProject project, List<String> sources) {
    List<IPath> paths = new ArrayList<>();
    for(String source : sources) {
      paths.add(getProjectRelativePath(project, source));
    }
    return paths.toArray(new IPath[paths.size()]);
  }

  private IPath[] getOtherResourceFolders(IProject project, List<Resource> resources, IPath curPath) {
    List<IPath> paths = new ArrayList<>();
    for(Resource res : resources) {
      IPath path = getProjectRelativePath(project, res.getDirectory());
      if(!path.equals(curPath)) {
        paths.add(path);
      }
    }
    return paths.toArray(new IPath[paths.size()]);
  }

  private boolean isContained(IPath path, IProject project, IPath[] otherPaths) {
    for(IPath otherPath : otherPaths) {
      if(otherPath.isPrefixOf(path)) {
        return true;
      }
    }
    return false;
  }

  protected void addJavaProjectOptions(Map<String, String> options, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    String source = null, target = null;

    //New release flag in JDK 9. See http://mail.openjdk.java.net/pipermail/jdk9-dev/2015-July/002414.html
    String release = null;

    boolean generateParameters = false;

    boolean enablePreviewFeatures = false;

    for(MojoExecution execution : getCompilerMojoExecutions(request, monitor)) {
      release = getCompilerLevel(request.mavenProject(), execution, "release", release, RELEASES, monitor);
      //XXX ignoring testRelease option, since JDT doesn't support main/test classpath separation - yet
      source = getCompilerLevel(request.mavenProject(), execution, "source", source, SOURCES, monitor); //$NON-NLS-1$
      target = getCompilerLevel(request.mavenProject(), execution, "target", target, TARGETS, monitor); //$NON-NLS-1$
      generateParameters = generateParameters || isGenerateParameters(request.mavenProject(), execution, monitor);
      enablePreviewFeatures = enablePreviewFeatures
          || isEnablePreviewFeatures(request.mavenProject(), execution, monitor);

      // process -err:+deprecation , -warn:-serial ...
      for(Object o : maven.getMojoParameterValue(request.mavenProject(), execution, "compilerArgs", List.class,
          monitor)) {
        if(o instanceof String compilerArg) {
          boolean err = false/*, warn = false*/;
          String[] settings = new String[0];
          if(compilerArg.startsWith("-warn:")) {
            //warn = true;
            settings = compilerArg.substring("-warn:".length()).split(",");
          } else if(compilerArg.startsWith("-err:")) {
            err = true;
            settings = compilerArg.substring("-err:".length()).split(",");
          }
          for(String cliSetting : settings) {
            if(cliSetting.length() >= 2) {
              String severity = cliSetting.charAt(0) == '-' ? JavaCore.IGNORE
                  : (err ? JavaCore.ERROR : JavaCore.WARNING);
              cliSetting = Character.isLetter(cliSetting.charAt(0)) ? cliSetting : cliSetting.substring(1);
              String option = toCompilerOption(cliSetting);
              if(option != null) {
                options.put(option, severity);
              }
            }
          }
        }
      }
    }

    if(release != null) {
      source = release;
      target = release;
    } else {
      if(source == null) {
        source = getDefaultSourceLevel();
        log.warn("Could not determine source level, using default " + source);
      }

      if(target == null) {
        target = getDefaultTargetLevel(source);
        log.warn("Could not determine target level, using default " + target);
      }

    }

    // While "5" and "6" ... are valid synonyms for Java 5, Java 6 ... source/target,
    // Eclipse expects the values 1.5 and 1.6 and so on.
    source = sanitizeJavaVersion(source);
    target = sanitizeJavaVersion(target);

    options.put(JavaCore.COMPILER_SOURCE, source);
    options.put(JavaCore.COMPILER_COMPLIANCE, source);
    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
    options.put(JavaCore.COMPILER_RELEASE, (release == null) ? JavaCore.DISABLED : JavaCore.ENABLED);
    if(generateParameters) {
      options.put(JavaCore.COMPILER_CODEGEN_METHOD_PARAMETERS_ATTR, JavaCore.GENERATE);
    }
    // 360962 keep forbidden_reference severity set by the user
    IJavaProject jp = JavaCore.create(request.mavenProjectFacade().getProject());
    if(jp != null && jp.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, false) == null) {
      options.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, JavaCore.WARNING);
    }
    options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES,
        enablePreviewFeatures ? JavaCore.ENABLED : JavaCore.DISABLED);
    //preview features are enabled on purpose, so keep JDT quiet about it, unless specifically overridden by the user
    if(jp != null && jp.getOption(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, false) == null) {
      options.put(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, JavaCore.IGNORE);
    }

  }

  /**
   * @param problemSettingName
   * @return the compiler option (usually defined in {@link JavaCore}) for the given CLI problem setting. or
   *         {@code null} if none found;
   */
  private String toCompilerOption(String problemSettingName) {
    return switch(problemSettingName) {
      case "serial" -> JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION;
      default -> null;
    };
  }

  private boolean isGenerateParameters(MavenProject mavenProject, MojoExecution execution, IProgressMonitor monitor) {
    Boolean generateParameters = null;
    //1st, check the parameters option
    try {
      generateParameters = maven.getMojoParameterValue(mavenProject, execution, "parameters", Boolean.class, monitor);//$NON-NLS-1$
    } catch(Exception ex) {
      //ignore
    }

    //2nd, check the parameters flag in the compilerArgs list
    if(!Boolean.TRUE.equals(generateParameters)) {
      try {
        List<?> args = maven.getMojoParameterValue(mavenProject, execution, "compilerArgs", List.class, monitor);//$NON-NLS-1$
        if(args != null) {
          generateParameters = args.contains(JavaSettingsUtils.PARAMETERS_JVM_FLAG);
        }
      } catch(Exception ex) {
        //ignore
      }
    }

    //3rd, check the parameters flag in the compilerArgument String
    if(!Boolean.TRUE.equals(generateParameters)) {
      try {
        String compilerArgument = maven.getMojoParameterValue(mavenProject, execution, "compilerArgument", String.class, //$NON-NLS-1$
            monitor);
        if(compilerArgument != null) {
          generateParameters = compilerArgument.contains(JavaSettingsUtils.PARAMETERS_JVM_FLAG);
        }
      } catch(CoreException ex) {
        //ignore
      }
    }
    //Let's ignore the <compilerArguments> Map, deprecated since maven-compiler-plugin 3.1 (in 2014).
    return Boolean.TRUE.equals(generateParameters);
  }

  private boolean isEnablePreviewFeatures(MavenProject mavenProject, MojoExecution execution,
      IProgressMonitor monitor) {
    //1st, check the --enable-preview flag in the compilerArgs list
    try {
      List<?> args = maven.getMojoParameterValue(mavenProject, execution, "compilerArgs", List.class, monitor);//$NON-NLS-1$
      if(args != null && args.contains(JavaSettingsUtils.ENABLE_PREVIEW_JVM_FLAG)) {
        return true;
      }
    } catch(Exception ex) {
      //ignore
    }

    //2nd, check the --enable-preview flag in the compilerArgument String
    try {
      String compilerArgument = maven.getMojoParameterValue(mavenProject, execution, "compilerArgument", String.class, //$NON-NLS-1$
          monitor);
      if(compilerArgument != null && compilerArgument.contains(JavaSettingsUtils.ENABLE_PREVIEW_JVM_FLAG)) {
        return true;
      }
    } catch(CoreException ex) {
      //ignore
    }
    return false;
  }

  private String sanitizeJavaVersion(String version) {
    if(version == null) {
      return null;
    }
    return switch(version) {
      case "5", "6", "7", "8" -> "1." + version;
      default -> {
        if(version.startsWith("1.")) {
          String subVersion = version.substring(2);
          if(Integer.parseInt(subVersion) > 8) {
            yield subVersion;
          }
        }
        yield version;
      }
    };
  }

  protected String getDefaultTargetLevel(String source) {
    return DEFAULT_COMPILER_LEVEL;
  }

  protected String getDefaultSourceLevel() {
    return DEFAULT_COMPILER_LEVEL;
  }

  protected List<MojoExecution> getCompilerMojoExecutions(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    return request.mavenProjectFacade().getMojoExecutions(COMPILER_PLUGIN_GROUP_ID, COMPILER_PLUGIN_ARTIFACT_ID,
        monitor, GOAL_COMPILE, GOAL_TESTCOMPILE);
  }

  private String getCompilerLevel(MavenProject mavenProject, MojoExecution execution, String parameter,
      String prevVersion, List<String> supportedVersions, IProgressMonitor monitor) {
    try {
      String version = maven.getMojoParameterValue(mavenProject, execution, parameter, String.class, monitor);
      if(version == null) {
        return prevVersion;
      }
      if(!"release".equals(parameter)) {
        // While "5" and "6" ... are valid synonyms for Java 5, Java 6 ... source/target,
        // Eclipse expects the values 1.5 and 1.6 and so on.
        version = sanitizeJavaVersion(version);
      }
      if(!supportedVersions.isEmpty() && !supportedVersions.contains(version)) {
        // supported versions are sorted, so last === highest
        String highestVersion = supportedVersions.get(supportedVersions.size() - 1);
        try {
          if(Version.valueOf(highestVersion).compareTo(Version.valueOf(version)) < 0) {
            // project (javac) version is probably not yet supported by JDT, 
            // so return the highest available version available at that time
            return highestVersion;
          }
        } catch(Exception malformedVersion) {
          //Ignore malformed version
        }
        return prevVersion;
      }
      return JavaCore.compareJavaVersions(prevVersion, version) < 0 ? version : prevVersion;

    } catch(CoreException ex) {
      log.error("Failed to determine compiler " + parameter + " setting, assuming default", ex);
    }
    return prevVersion;
  }

  @Override
  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    super.unconfigure(request, monitor);
    removeMavenClasspathContainer(request.mavenProjectFacade().getProject());
  }

  private void removeMavenClasspathContainer(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      // remove classpatch container from JavaProject
      ArrayList<IClasspathEntry> newEntries = new ArrayList<>();
      for(IClasspathEntry entry : javaProject.getRawClasspath()) {
        if(!MavenClasspathHelpers.isMaven2ClasspathContainer(entry.getPath())) {
          newEntries.add(entry);
        }
      }
      javaProject.setRawClasspath(newEntries.toArray(new IClasspathEntry[newEntries.size()]), null);
    }
  }

  protected IFolder getFolder(IProject project, String absolutePath) {
    if(project.getLocation().makeAbsolute().equals(Path.fromOSString(absolutePath))) {
      return project.getFolder(project.getLocation());
    }
    return project.getFolder(getProjectRelativePath(project, absolutePath));
  }

  protected IPath getProjectRelativePath(IProject project, String absolutePath) {
    File basedir = project.getLocation().toFile();
    String relative;
    if(absolutePath.equals(basedir.getAbsolutePath())) {
      relative = "."; //$NON-NLS-1$
    } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
      relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
    } else {
      relative = absolutePath;
    }
    return new Path(relative.replace('\\', '/'));
  }

  /**
   * get all the arguments provided to the compiler for the provided {@link MojoExecution}
   * 
   * @param mavenProject the current maven project
   * @param execution the plugin execution
   * @param monitor the progress monitor
   * @return the arguments
   */
  private List<String> getCompilerArguments(MavenProject mavenProject, MojoExecution execution,
      IProgressMonitor monitor) {

    List<String> arguments = new ArrayList<>();

    //1st, get the arguments in the compilerArgs list
    try {
      List<?> args = maven.getMojoParameterValue(mavenProject, execution, "compilerArgs", List.class, monitor);//$NON-NLS-1$
      if(args != null) {//$NON-NLS-1$
        args.stream().filter(a -> a != null).forEach(a -> arguments.add(a.toString()));
      }
    } catch(Exception ex) {
      //ignore
    }

    // Let's ignore the <compilerArgument>, As the maven-compiler-plugin doc states all jpms arguments
    // are in fact two arguments so no complete jpms argument can be provided using compilerArgument

    // Let's ignore the <compilerArguments> Map, deprecated since maven-compiler-plugin 3.1 (in 2014).

    // Let's ignore the <testCompilerArguments> Map because it don't suppport double dashed arguments

    return arguments;
  }

  /**
   * get all the arguments provided to the compiler
   * 
   * @param facade
   * @param monitor
   * @return the arguments
   * @throws CoreException
   */
  private List<String> getCompilerArguments(IMavenProjectFacade facade, Map<String, String> options,
      IProgressMonitor monitor) throws CoreException {
    List<String> compilerArgs = new ArrayList<>();

    List<MojoExecution> executions = facade.getMojoExecutions(COMPILER_PLUGIN_GROUP_ID, COMPILER_PLUGIN_ARTIFACT_ID,
        monitor, GOAL_COMPILE, GOAL_TESTCOMPILE);

    MavenProject mavenProject = facade.getMavenProject();

    //facade.getProject().get
    for(MojoExecution compile : executions) {
      if(isCompileExecution(compile, mavenProject, options, monitor)
          || isTestCompileExecution(compile, mavenProject, options, monitor)) {
        List<String> args = getCompilerArguments(mavenProject, compile, monitor);
        if(args != null) {
          compilerArgs.addAll(args);
        }
      }
    }

    return compilerArgs;
  }

  @Override
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor) {
    ModuleSupport.configureClasspath(facade, classpath, monitor);
  }

  @Override
  public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {

    IMavenProjectFacade facade = request.mavenProjectFacade();
    IJavaProject javaProject = JavaCore.create(facade.getProject());
    if(javaProject == null || !javaProject.exists() || classpath == null) {
      return;
    }

    List<String> compilerArgs = getCompilerArguments(facade, javaProject.getOptions(true), monitor);

    ModuleSupport.configureRawClasspath(request, classpath, monitor, compilerArgs);
  }
}
