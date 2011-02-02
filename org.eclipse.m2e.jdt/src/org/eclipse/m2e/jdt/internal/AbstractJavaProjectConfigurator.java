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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.core.util.Util;
import org.eclipse.m2e.jdt.BuildPathManager;
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

  protected static final List<String> SOURCES = Arrays.asList("1.1,1.2,1.3,1.4,1.5,1.6,1.7".split(",")); //$NON-NLS-1$ //$NON-NLS-2$

  protected static final List<String> TARGETS = Arrays.asList("1.1,1.2,1.3,1.4,jsr14,1.5,1.6,1.7".split(",")); //$NON-NLS-1$ //$NON-NLS-2$

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

    MavenProject mavenProject = getMavenProject(request, monitor);

    Map<String, String> options = new HashMap<String, String>();

    addJavaProjectOptions(options, request, mavenProject, monitor);

    IClasspathDescriptor classpath = new ClasspathDescriptor(javaProject);

    addProjectSourceFolders(classpath, request, mavenProject, monitor);

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
            && !BuildPathManager.isMaven2ClasspathContainer(entry.getPath())) {
          classpath.addEntry(entry);
        }
      }
    }
  }

  @SuppressWarnings("unused")
  protected MavenProject getMavenProject(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    return request.getMavenProject();
  }

  protected void invokeJavaProjectConfigurators(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      final IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = request.getMavenProjectFacade();
    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
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

  private void addJREClasspathContainer(IClasspathDescriptor classpath, String environmentId) {
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

  private void addMavenClasspathContainer(IClasspathDescriptor classpath) {
    // remove any old maven classpath container entries
    classpath.removeEntry(new ClasspathDescriptor.EntryFilter() {
      public boolean accept(IClasspathEntryDescriptor entry) {
        return BuildPathManager.isMaven2ClasspathContainer(entry.getPath());
      }
    });

    // add new entry
    IClasspathEntry cpe = BuildPathManager.getDefaultContainerEntry();
    classpath.addEntry(cpe);
  }

  protected void addProjectSourceFolders(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
    IProject project = request.getProject();

    IFolder classes = getFolder(project, mavenProject.getBuild().getOutputDirectory());
    IFolder testClasses = getFolder(project, mavenProject.getBuild().getTestOutputDirectory());

    Util.createFolder(classes, true);
    Util.createFolder(testClasses, true);

    IPath[] inclusion = new IPath[0];
    IPath[] exclusion = new IPath[0];

    IPath[] inclusionTest = new IPath[0];
    IPath[] exclusionTest = new IPath[0];

    for(Plugin plugin : mavenProject.getBuildPlugins()) {
      if(isJavaCompilerExecution(plugin)) {
        for(PluginExecution execution : plugin.getExecutions()) {
          for(String goal : execution.getGoals()) {
            if("compile".equals(goal)) { //$NON-NLS-1$
              try {
                inclusion = toPaths(maven.getMojoParameterValue("includes", String[].class, request.getMavenSession(), //$NON-NLS-1$
                    plugin, execution, goal));
              } catch(CoreException ex) {
                MavenLogger.log(ex);
                console.logError("Failed to determine compiler inclusions, assuming defaults");
              }
              try {
                exclusion = toPaths(maven.getMojoParameterValue("excludes", String[].class, request.getMavenSession(), //$NON-NLS-1$
                    plugin, execution, goal));
              } catch(CoreException ex) {
                MavenLogger.log(ex);
                console.logError("Failed to determine compiler exclusions, assuming defaults");
              }
            } else if("testCompile".equals(goal)) { //$NON-NLS-1$
              try {
                inclusionTest = toPaths(maven.getMojoParameterValue("testIncludes", String[].class, //$NON-NLS-1$
                    request.getMavenSession(), plugin, execution, goal));
              } catch(CoreException ex) {
                MavenLogger.log(ex);
                console.logError("Failed to determine compiler test inclusions, assuming defaults");
              }
              try {
                exclusionTest = toPaths(maven.getMojoParameterValue("testExcludes", String[].class, //$NON-NLS-1$
                    request.getMavenSession(), plugin, execution, goal));
              } catch(CoreException ex) {
                MavenLogger.log(ex);
                console.logError("Failed to determine compiler test exclusions, assuming defaults");
              }
            }
          }
        }
      }
    }

    addSourceDirs(classpath, project, mavenProject.getCompileSourceRoots(), classes.getFullPath(), inclusion, exclusion);
    addResourceDirs(classpath, project, mavenProject.getBuild().getResources(), classes.getFullPath());

    addSourceDirs(classpath, project, mavenProject.getTestCompileSourceRoots(), testClasses.getFullPath(),
        inclusionTest, exclusionTest);
    addResourceDirs(classpath, project, mavenProject.getBuild().getTestResources(), testClasses.getFullPath());
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
      IPath outputPath, IPath[] inclusion, IPath[] exclusion) throws CoreException {
    for(String sourceRoot : sourceRoots) {
      IFolder sourceFolder = getFolder(project, sourceRoot);

      if(sourceFolder != null && sourceFolder.exists() && sourceFolder.getProject().equals(project)) {
        IClasspathEntryDescriptor cped = getEnclosingEntryDescriptor(classpath, sourceFolder.getFullPath());
        if (cped == null) {
          console.logMessage("Adding source folder " + sourceFolder.getFullPath());
          classpath.addSourceEntry(sourceFolder.getFullPath(), outputPath, inclusion, exclusion, false);
        } else {
          console.logMessage("Not adding source folder " + sourceFolder.getFullPath() + " because it overlaps with " + cped.getPath());
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
      IPath outputPath) throws CoreException {

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
          console.logError("Skipping resource folder " + r.getFullPath());
        } else if(r != null && r.getProject().equals(project)) {
          IClasspathEntryDescriptor cped = getEnclosingEntryDescriptor(classpath, r.getFullPath());
          if(cped == null) {
            console.logMessage("Adding resource folder " + r.getFullPath());
            classpath.addSourceEntry(r.getFullPath(), outputPath, new IPath[0] /*inclusions*/, new IPath[] {new Path(
                "**")} /*exclusion*/, false /*optional*/);
          } else {
            // resources and sources folders overlap. make sure JDT only processes java sources.
            console.logMessage("Resources folder " + r.getFullPath() + " overlaps with sources folder "
                + cped.getPath());
            cped.addInclusionPattern(new Path("**/*.java"));
          }
        } else {
          console.logMessage("Not adding resources folder " + resourceDirectory.getAbsolutePath());
        }
      }
    }
  }  
  

  protected void addJavaProjectOptions(Map<String, String> options, ProjectConfigurationRequest request, MavenProject mavenProject,
      IProgressMonitor monitor) {
    MavenSession mavenSession = request.getMavenSession();

    String source = null, target = null;

    for(Plugin plugin : mavenProject.getBuildPlugins()) {
      if(isJavaCompilerExecution(plugin)) {
        for(PluginExecution execution : plugin.getExecutions()) {
          for(String goal : execution.getGoals()) {
            source = getCompilerLevel(mavenSession, plugin, execution, goal, "source", source, SOURCES); //$NON-NLS-1$
            target = getCompilerLevel(mavenSession, plugin, execution, goal, "target", target, TARGETS); //$NON-NLS-1$
          }
        }
      }
    }

    if(source == null) {
      source = DEFAULT_COMPILER_LEVEL;
      console.logMessage("Could not determine source level, using default " + source);
    }

    if(target == null) {
      target = DEFAULT_COMPILER_LEVEL;
      console.logMessage("Could not determine target level, using default " + target);
    }

    options.put(JavaCore.COMPILER_SOURCE, source);
    options.put(JavaCore.COMPILER_COMPLIANCE, source);
    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
    options.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, "warning"); //$NON-NLS-1$
  }

  private String getCompilerLevel(MavenSession session, Plugin plugin, PluginExecution execution, String goal,
      String parameter, String source, List<String> levels) {

    int levelIdx = getLevelIndex(source, levels);

    try {
      source = maven.getMojoParameterValue(parameter, String.class, session, plugin, execution, goal);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      console.logError("Failed to determine compiler " + parameter + " setting, assuming default");
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

  private boolean isJavaCompilerExecution(Plugin plugin) {
    return isJavaCompilerPlugin(plugin.getGroupId(), plugin.getArtifactId());
  }

  private boolean isJavaCompilerPlugin(String groupId, String artifactId) {
    return "org.apache.maven.plugins".equals(groupId) && "maven-compiler-plugin".equals(artifactId); //$NON-NLS-1$ //$NON-NLS-2$
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
        if(!BuildPathManager.isMaven2ClasspathContainer(entry.getPath())) {
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
