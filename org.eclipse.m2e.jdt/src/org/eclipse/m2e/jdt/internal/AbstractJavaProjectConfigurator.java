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

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import org.apache.maven.execution.MavenSession;
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
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * AbstractJavaProjectConfigurator
 * 
 * @author igor
 */
public abstract class AbstractJavaProjectConfigurator extends AbstractProjectConfigurator {
  private static final Logger log = LoggerFactory.getLogger(AbstractJavaProjectConfigurator.class);

  private static final String GOAL_COMPILE = "compile";

  private static final String GOAL_TESTCOMPILE = "testCompile";

  public static final String COMPILER_PLUGIN_ARTIFACT_ID = "maven-compiler-plugin";

  public static final String COMPILER_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

  protected static final List<String> SOURCES = Arrays.asList("1.1,1.2,1.3,1.4,1.5,1.6,1.7".split(",")); //$NON-NLS-1$ //$NON-NLS-2$

  protected static final List<String> TARGETS = Arrays.asList("1.1,1.2,1.3,1.4,jsr14,1.5,1.6,1.7".split(",")); //$NON-NLS-1$ //$NON-NLS-2$

  private static final String GOAL_RESOURCES = "resources";

  private static final String GOAL_TESTRESOURCES = "testResources";

  private static final String RESOURCES_PLUGIN_ARTIFACT_ID = "maven-resources-plugin";

  private static final String RESOURCES_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

  protected static final LinkedHashMap<String, String> ENVIRONMENTS = new LinkedHashMap<String, String>();

  static {
    ENVIRONMENTS.put("1.1", "JRE-1.1"); //$NON-NLS-1$ //$NON-NLS-2$
    ENVIRONMENTS.put("1.2", "J2SE-1.2"); //$NON-NLS-1$ //$NON-NLS-2$
    ENVIRONMENTS.put("1.3", "J2SE-1.3"); //$NON-NLS-1$ //$NON-NLS-2$
    ENVIRONMENTS.put("1.4", "J2SE-1.4"); //$NON-NLS-1$ //$NON-NLS-2$
    ENVIRONMENTS.put("1.5", "J2SE-1.5"); //$NON-NLS-1$ //$NON-NLS-2$
    ENVIRONMENTS.put("jsr14", "J2SE-1.5"); //$NON-NLS-1$ //$NON-NLS-2$
    ENVIRONMENTS.put("1.6", "JavaSE-1.6"); //$NON-NLS-1$ //$NON-NLS-2$
    ENVIRONMENTS.put("1.7", "JavaSE-1.7"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  protected static final String DEFAULT_COMPILER_LEVEL = "1.4"; //$NON-NLS-1$

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    IProject project = request.getProject();

    monitor.setTaskName(Messages.AbstractJavaProjectConfigurator_task_name + project.getName());

    addJavaNature(project, monitor);

    IJavaProject javaProject = JavaCore.create(project);

    Map<String, String> options = new HashMap<String, String>();

    addJavaProjectOptions(options, request, monitor);

    IClasspathDescriptor classpath = new ClasspathDescriptor(javaProject);

    addProjectSourceFolders(classpath, request, monitor);

    String environmentId = getExecutionEnvironmentId(options);

    addJREClasspathContainer(classpath, environmentId);

    addMavenClasspathContainer(classpath);

    addCustomClasspathEntries(javaProject, classpath);

    invokeJavaProjectConfigurators(classpath, request, monitor);

    // now apply new configuration

    // A single setOptions call erases everything else from an existing settings file.
    // Must invoke setOption individually to preserve previous options. 
    for(Map.Entry<String, String> option : options.entrySet()) {
      javaProject.setOption(option.getKey(), option.getValue());
    }

    MavenProject mavenProject = request.getMavenProject();
    IContainer classesFolder = getFolder(project, mavenProject.getBuild().getOutputDirectory());

    javaProject.setRawClasspath(classpath.getEntries(), classesFolder.getFullPath(), monitor);

    MavenJdtPlugin.getDefault().getBuildpathManager().updateClasspath(project, monitor);
  }

  protected String getExecutionEnvironmentId(Map<String, String> options) {
    return ENVIRONMENTS.get(options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));
  }

  protected void addJavaNature(IProject project, IProgressMonitor monitor) throws CoreException {
    addNature(project, JavaCore.NATURE_ID, monitor);
  }

  protected void addCustomClasspathEntries(IJavaProject javaProject, IClasspathDescriptor classpath)
      throws JavaModelException {
    //Preserve existing libraries and classpath order (sort of) 
    // as other containers would have been added AFTER the JRE and M2 ones anyway 
    IClasspathEntry[] cpEntries = javaProject.getRawClasspath();
    if(cpEntries != null && cpEntries.length > 0) {
      for(IClasspathEntry entry : cpEntries) {
        if(IClasspathEntry.CPE_CONTAINER == entry.getEntryKind()
            && !JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0))
            && !MavenClasspathHelpers.isMaven2ClasspathContainer(entry.getPath())) {
          classpath.addEntry(entry);
        }
      }
    }
  }

  protected void invokeJavaProjectConfigurators(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      final IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = request.getMavenProjectFacade();
    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(facade);
    if(lifecycleMapping == null) {
      return;
    }
    for(AbstractProjectConfigurator configurator : lifecycleMapping.getProjectConfigurators(facade, monitor)) {
      if(configurator instanceof IJavaProjectConfigurator) {
        ((IJavaProjectConfigurator) configurator).configureRawClasspath(request, classpath, monitor);
      }
    }
  }

  protected void addJREClasspathContainer(IClasspathDescriptor classpath, String environmentId) {
    // remove existing JRE entry
    classpath.removeEntry(new ClasspathDescriptor.EntryFilter() {
      public boolean accept(IClasspathEntryDescriptor descriptor) {
        return JavaRuntime.JRE_CONTAINER.equals(descriptor.getPath().segment(0));
      }
    });

    IClasspathEntry cpe;
    IExecutionEnvironment executionEnvironment = getExecutionEnvironment(environmentId);
    if(executionEnvironment == null) {
      cpe = JavaRuntime.getDefaultJREContainerEntry();
    } else {
      IPath containerPath = JavaRuntime.newJREContainerPath(executionEnvironment);
      cpe = JavaCore.newContainerEntry(containerPath);
    }

    classpath.addEntry(cpe);
  }

  private IExecutionEnvironment getExecutionEnvironment(String environmentId) {
    IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
    for(IExecutionEnvironment environment : manager.getExecutionEnvironments()) {
      if(environment.getId().equals(environmentId)) {
        return environment;
      }
    }
    return null;
  }

  protected void addMavenClasspathContainer(IClasspathDescriptor classpath) {
    // remove any old maven classpath container entries
    classpath.removeEntry(new ClasspathDescriptor.EntryFilter() {
      public boolean accept(IClasspathEntryDescriptor entry) {
        return MavenClasspathHelpers.isMaven2ClasspathContainer(entry.getPath());
      }
    });

    // add new entry
    IClasspathEntry cpe = MavenClasspathHelpers.getDefaultContainerEntry();
    classpath.addEntry(cpe);
  }

  protected void addProjectSourceFolders(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    SubMonitor mon = SubMonitor.convert(monitor, 6);
    try {
      IProject project = request.getProject();
      MavenProject mavenProject = request.getMavenProject();
      IMavenProjectFacade projectFacade = request.getMavenProjectFacade();

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

      MavenSession mavenSession = request.getMavenSession();

      for(MojoExecution compile : projectFacade.getMojoExecutions(COMPILER_PLUGIN_GROUP_ID,
          COMPILER_PLUGIN_ARTIFACT_ID, mon.newChild(1), GOAL_COMPILE)) {
        mainSourceEncoding = maven.getMojoParameterValue(mavenSession, compile, "encoding", String.class); //$NON-NLS-1$
        try {
          inclusion = toPaths(maven.getMojoParameterValue(request.getMavenSession(), compile,
              "includes", String[].class)); //$NON-NLS-1$
        } catch(CoreException ex) {
          log.error("Failed to determine compiler inclusions, assuming defaults", ex);
        }
        try {
          exclusion = toPaths(maven.getMojoParameterValue(request.getMavenSession(), compile,
              "excludes", String[].class)); //$NON-NLS-1$
        } catch(CoreException ex) {
          log.error("Failed to determine compiler exclusions, assuming defaults", ex);
        }
      }

      for(MojoExecution compile : projectFacade.getMojoExecutions(COMPILER_PLUGIN_GROUP_ID,
          COMPILER_PLUGIN_ARTIFACT_ID, mon.newChild(1), GOAL_TESTCOMPILE)) {
        testSourceEncoding = maven.getMojoParameterValue(mavenSession, compile, "encoding", String.class); //$NON-NLS-1$
        try {
          inclusionTest = toPaths(maven.getMojoParameterValue(request.getMavenSession(), compile,
              "testIncludes", String[].class)); //$NON-NLS-1$
        } catch(CoreException ex) {
          log.error("Failed to determine compiler test inclusions, assuming defaults", ex);
        }
        try {
          exclusionTest = toPaths(maven.getMojoParameterValue(request.getMavenSession(), compile,
              "testExcludes", String[].class)); //$NON-NLS-1$
        } catch(CoreException ex) {
          log.error("Failed to determine compiler test exclusions, assuming defaults", ex);
        }
      }

      for(MojoExecution resources : projectFacade.getMojoExecutions(RESOURCES_PLUGIN_GROUP_ID,
          RESOURCES_PLUGIN_ARTIFACT_ID, mon.newChild(1), GOAL_RESOURCES)) {
        mainResourcesEncoding = maven.getMojoParameterValue(mavenSession, resources, "encoding", String.class); //$NON-NLS-1$
      }

      for(MojoExecution resources : projectFacade.getMojoExecutions(RESOURCES_PLUGIN_GROUP_ID,
          RESOURCES_PLUGIN_ARTIFACT_ID, mon.newChild(1), GOAL_TESTRESOURCES)) {
        testResourcesEncoding = maven.getMojoParameterValue(mavenSession, resources, "encoding", String.class); //$NON-NLS-1$
      }

      addSourceDirs(classpath, project, mavenProject.getCompileSourceRoots(), classes.getFullPath(), inclusion,
          exclusion, mainSourceEncoding, mon.newChild(1));
      addResourceDirs(classpath, project, mavenProject.getBuild().getResources(), classes.getFullPath(),
          mainResourcesEncoding, mon.newChild(1));

      addSourceDirs(classpath, project, mavenProject.getTestCompileSourceRoots(), testClasses.getFullPath(),
          inclusionTest, exclusionTest, testSourceEncoding, mon.newChild(1));
      addResourceDirs(classpath, project, mavenProject.getBuild().getTestResources(), testClasses.getFullPath(),
          testResourcesEncoding, mon.newChild(1));
    } finally {
      mon.done();
    }
  }

  private IPath[] toPaths(String[] values) {
    if(values == null) {
      return new IPath[0];
    }
    IPath[] paths = new IPath[values.length];
    for(int i = 0; i < values.length; i++ ) {
      paths[i] = new Path(values[i]);
    }
    return paths;
  }

  private void addSourceDirs(IClasspathDescriptor classpath, IProject project, List<String> sourceRoots,
      IPath outputPath, IPath[] inclusion, IPath[] exclusion, String sourceEncoding, IProgressMonitor monitor) throws CoreException {
    
    for(String sourceRoot : sourceRoots) {
      IFolder sourceFolder = getFolder(project, sourceRoot);
      if(sourceFolder != null && sourceFolder.exists() && sourceFolder.getProject().equals(project)) {
        IClasspathEntryDescriptor cped = getEnclosingEntryDescriptor(classpath, sourceFolder.getFullPath());
        if(cped == null) {
          log.info("Adding source folder " + sourceFolder.getFullPath());
          classpath.addSourceEntry(sourceFolder.getFullPath(), outputPath, inclusion, exclusion, false);
        } else {
          log.info("Not adding source folder " + sourceFolder.getFullPath() + " because it overlaps with "
              + cped.getPath());
        }
        if(sourceEncoding != null) {
          sourceFolder.setDefaultCharset(sourceEncoding, monitor);
        }
      } else {
        if(sourceFolder != null) {
          classpath.removeEntry(sourceFolder.getFullPath());
        }
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

  private void addResourceDirs(IClasspathDescriptor classpath, IProject project, List<Resource> resources,
      IPath outputPath, String resourceEncoding, IProgressMonitor monitor) throws CoreException {

    for(Resource resource : resources) {
      File resourceDirectory = new File(resource.getDirectory());
      if(resourceDirectory.exists() && resourceDirectory.isDirectory()) {
        IPath relativePath = getProjectRelativePath(project, resource.getDirectory());
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
        } else if(r != null && r.getProject().equals(project)) {
          IClasspathEntryDescriptor cped = getEnclosingEntryDescriptor(classpath, r.getFullPath());
          if(cped == null) {
            log.info("Adding resource folder " + r.getFullPath());
            classpath.addSourceEntry(r.getFullPath(), outputPath, new IPath[0] /*inclusions*/, new IPath[] {new Path(
                "**")} /*exclusion*/, false /*optional*/);
          } else {
            // resources and sources folders overlap. make sure JDT only processes java sources.
            log.info("Resources folder " + r.getFullPath() + " overlaps with sources folder "
                + cped.getPath());
            cped.addInclusionPattern(new Path("**/*.java"));
          }

          // Set folder encoding (null = platform default)
          IFolder resourceFolder = project.getFolder(relativePath);
          resourceFolder.setDefaultCharset(resourceEncoding, monitor);
        } else {
          log.info("Not adding resources folder " + resourceDirectory.getAbsolutePath());
        }
      }
    }
  }

  protected void addJavaProjectOptions(Map<String, String> options, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    MavenSession mavenSession = request.getMavenSession();

    String source = null, target = null;

    for(MojoExecution execution : request.getMavenProjectFacade().getMojoExecutions(COMPILER_PLUGIN_GROUP_ID,
        COMPILER_PLUGIN_ARTIFACT_ID, monitor, GOAL_COMPILE, GOAL_TESTCOMPILE)) {

      source = getCompilerLevel(mavenSession, execution, "source", source, SOURCES); //$NON-NLS-1$
      target = getCompilerLevel(mavenSession, execution, "target", target, TARGETS); //$NON-NLS-1$

    }

    if(source == null) {
      source = DEFAULT_COMPILER_LEVEL;
      log.warn("Could not determine source level, using default " + source);
    }

    if(target == null) {
      target = DEFAULT_COMPILER_LEVEL;
      log.warn("Could not determine target level, using default " + target);
    }

    options.put(JavaCore.COMPILER_SOURCE, source);
    options.put(JavaCore.COMPILER_COMPLIANCE, source);
    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
    options.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, "warning"); //$NON-NLS-1$
  }

  private String getCompilerLevel(MavenSession session, MojoExecution execution, String parameter, String source,
      List<String> levels) {
    int levelIdx = getLevelIndex(source, levels);

    try {
      source = maven.getMojoParameterValue(session, execution, parameter, String.class);
    } catch(CoreException ex) {
      log.error("Failed to determine compiler " + parameter + " setting, assuming default", ex);
    }

    int newLevelIdx = getLevelIndex(source, levels);

    if(newLevelIdx > levelIdx) {
      levelIdx = newLevelIdx;
    }

    if(levelIdx < 0) {
      return DEFAULT_COMPILER_LEVEL;
    }

    return levels.get(levelIdx);
  }

  private int getLevelIndex(String level, List<String> levels) {
    return level != null ? levels.indexOf(level) : -1;
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    super.unconfigure(request, monitor);
    removeMavenClasspathContainer(request.getProject());
  }

  private void removeMavenClasspathContainer(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      // remove classpatch container from JavaProject
      ArrayList<IClasspathEntry> newEntries = new ArrayList<IClasspathEntry>();
      for(IClasspathEntry entry : javaProject.getRawClasspath()) {
        if(!MavenClasspathHelpers.isMaven2ClasspathContainer(entry.getPath())) {
          newEntries.add(entry);
        }
      }
      javaProject.setRawClasspath(newEntries.toArray(new IClasspathEntry[newEntries.size()]), null);
    }
  }

  protected IFolder getFolder(IProject project, String absolutePath) {
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
    return new Path(relative.replace('\\', '/')); //$NON-NLS-1$ //$NON-NLS-2$
  }
}
