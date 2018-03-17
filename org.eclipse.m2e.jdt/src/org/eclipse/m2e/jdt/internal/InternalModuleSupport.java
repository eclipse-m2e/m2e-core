/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.provisional.JavaModelAccess;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.AutomaticModuleNaming;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IModuleReference;
import org.eclipse.jdt.internal.launching.RuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;


/**
 * Helper for Java Module Support relying on new JDT classes
 *
 * @author Fred Bricon
 * @since 1.8.2
 */
@Deprecated
@SuppressWarnings("restriction")
class InternalModuleSupport {

  private static final Logger log = LoggerFactory.getLogger(InternalModuleSupport.class);

  /**
   * This is a copy of the constant of org.eclipse.jdt.launching.IRuntimeClasspathEntry.PATCH_MODULE. Having this copy
   * allows to compile and run with 4.7.1a
   */
  private static final int PATCH_MODULE = 6;

  /**
   * Sets <code>module</code flag to <code>true</code> to classpath dependencies declared in module-info.java
   * 
   * @param facade    a Maven facade project
   * @param classpath a classpath descriptor
   * @param monitor   a progress monitor
   */
  public static void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    IJavaProject javaProject = JavaCore.create(facade.getProject());
    if(javaProject == null || !javaProject.exists()) {
      return;
    }

    if(monitor == null) {
      monitor = new NullProgressMonitor();
    }
    Set<String> requiredModules = new LinkedHashSet<>(getRequiredModules(javaProject, monitor));

    if(requiredModules.isEmpty() || classpath.getEntryDescriptors().isEmpty() || monitor.isCanceled()) {
      return;
    }
    List<IClasspathEntryDescriptor> entryDescriptors = classpath.getEntryDescriptors();
    Map<String, IClasspathEntryDescriptor> moduleMap = new HashMap<>(entryDescriptors.size());
    Map<IClasspathEntryDescriptor, String> descriptorsMap = new HashMap<>(entryDescriptors.size());

    for(IClasspathEntryDescriptor entry : entryDescriptors) {
      if(monitor.isCanceled()) {
        return;
      }
      String moduleName = getModuleName(entry.getEntryKind(), entry.getPath(), monitor);
      moduleMap.put(moduleName, entry);//potentially suppresses duplicate entries from the same workspace project, with different classifiers
      descriptorsMap.put(entry, moduleName);
    }

    Set<String> visitedModules = new HashSet<>(entryDescriptors.size());
    collectTransitiveRequiredModules(requiredModules, visitedModules, moduleMap, monitor);

    if(monitor.isCanceled()) {
      return;
    }

    descriptorsMap.forEach((entry, module) -> {
      if(requiredModules.contains(module)) {
        entry.setClasspathAttribute(IClasspathAttribute.MODULE, Boolean.TRUE.toString());
      }
    });
  }

  private static void collectTransitiveRequiredModules(Set<String> requiredModules, Set<String> visitedModules,
      Map<String, IClasspathEntryDescriptor> moduleMap, IProgressMonitor monitor) throws JavaModelException {
    if(monitor.isCanceled() || requiredModules.isEmpty()) {
      return;
    }
    Set<String> transitiveModules = new LinkedHashSet<>();
    for(String req : requiredModules) {
      if(visitedModules.contains(req)) {
        //already checked that module
        continue;
      }
      Set<String> modules = getRequiredModules(moduleMap.get(req), monitor);
      transitiveModules.addAll(modules);
      visitedModules.add(req);
    }
    transitiveModules.removeAll(visitedModules);
    if(!transitiveModules.isEmpty()) {
      requiredModules.addAll(transitiveModules);
      collectTransitiveRequiredModules(transitiveModules, visitedModules, moduleMap, monitor);
    }
  }

  private static Set<String> getRequiredModules(IClasspathEntryDescriptor entry, IProgressMonitor monitor)
      throws JavaModelException {
    if(entry != null && !monitor.isCanceled()) {
      if(IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        return getRequiredModules(entry.getPath().toFile());
      } else if(IClasspathEntry.CPE_PROJECT == entry.getEntryKind()) {
        return getRequiredModules(getJavaProject(entry.getPath()), monitor);
      }
    }
    return Collections.emptySet();
  }

  public static Set<String> getRequiredModules(IJavaProject project, IProgressMonitor monitor)
      throws JavaModelException {
    IModuleDescription moduleDescription = project.getModuleDescription();
    if(moduleDescription != null) {
      String[] reqModules = JavaModelAccess.getRequiredModules(moduleDescription);
      return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(reqModules)));
    }
    return Collections.emptySet();
  }

  private static Set<String> getRequiredModules(File file) {
    if(!file.isFile()) {
      return Collections.emptySet();
    }
    try (ZipFile zipFile = new ZipFile(file)) {
      IModule module = null;
      ClassFileReader reader = ClassFileReader.read(zipFile, IModule.MODULE_INFO_CLASS);
      if(reader != null) {
        module = reader.getModuleDeclaration();
        if(module != null) {
          IModuleReference[] moduleRefs = module.requires();
          if(moduleRefs != null) {
            return Stream.of(moduleRefs).map(m -> new String(m.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
          }
        }
      }
    } catch(ClassFormatException | IOException ex) {
      log.error(ex.getMessage(), ex);
    }
    return Collections.emptySet();
  }

  public static String getModuleName(int entryKind, IPath entryPath, IProgressMonitor monitor) {
    String module = null;
    if(entryPath != null) {
      if(IClasspathEntry.CPE_LIBRARY == entryKind) {
        module = getModuleName(entryPath.toFile());
      } else if(IClasspathEntry.CPE_PROJECT == entryKind) {
        module = getModuleName(getJavaProject(entryPath), monitor);
      }
    }
    return module;
  }

  private static String getModuleName(IJavaProject project, IProgressMonitor monitor) {
    String module = null;
    if(project != null) {
      try {
        if(project.getModuleDescription() == null) {
          String buildName = null;
          IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project.getProject());
          if(facade != null) {
            MavenProject mavenProject = facade.getMavenProject(monitor);
            if(mavenProject != null) {
              buildName = mavenProject.getBuild().getFinalName();
            }
          }
          if(buildName == null || buildName.isEmpty()) {
            buildName = project.getElementName();
          }
          module = new String(AutomaticModuleNaming.determineAutomaticModuleName(buildName, false, null));
        } else {
          module = project.getModuleDescription().getElementName();
        }
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    return module;
  }

  private static IJavaProject getJavaProject(IPath projectPath) {
    if(projectPath == null || projectPath.isEmpty()) {
      return null;
    }
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = root.getProject(projectPath.lastSegment());
    if(project.isAccessible()) {
      return JavaCore.create(project);
    }
    return null;
  }

  private static String getModuleName(File file) {
    if(!file.isFile()) {
      return null;
    }

    char[] moduleName = null;
    try (ZipFile zipFile = new ZipFile(file)) {
      IModule module = null;
      ClassFileReader reader = ClassFileReader.read(zipFile, IModule.MODULE_INFO_CLASS);
      if(reader != null) {
        module = reader.getModuleDeclaration();
        if(module != null) {
          moduleName = module.name();
        }
      }
    } catch(ClassFormatException | IOException ex) {
      log.error(ex.getMessage(), ex);
    }
    if(moduleName == null) {
      moduleName = AutomaticModuleNaming.determineAutomaticModuleName(file.getAbsolutePath());
    }
    return new String(moduleName);
  }

  public static boolean isModuleEntry(IClasspathEntry entry) {
    return Arrays.stream(entry.getExtraAttributes())
        .anyMatch(p -> IClasspathAttribute.MODULE.equals(p.getName()) && "true".equals(p.getValue()));
  }

  public static int determineModularClasspathProperty(IClasspathEntry entry) {
    return isModuleEntry(entry) ? IRuntimeClasspathEntry.MODULE_PATH : IRuntimeClasspathEntry.CLASS_PATH;
  }

  public static IRuntimeClasspathEntry createRuntimeClasspathEntry(IFolder folder, int classpathProperty,
      IProject project) {
    if(classpathProperty == IRuntimeClasspathEntry.MODULE_PATH && !folder.exists(new Path("module-info.class"))) {
      classpathProperty = PATCH_MODULE;
    }
    IRuntimeClasspathEntry newArchiveRuntimeClasspathEntry = JavaRuntime
        .newArchiveRuntimeClasspathEntry(folder.getFullPath(), classpathProperty);
    if(classpathProperty == PATCH_MODULE) {
      ((RuntimeClasspathEntry) newArchiveRuntimeClasspathEntry).setJavaProject(JavaCore.create(project));
    }
    return newArchiveRuntimeClasspathEntry;
  }

  public static int determineClasspathPropertyForMainProject(boolean isModularConfiguration, IJavaProject javaProject) {
    if(!isModularConfiguration) {
      return IRuntimeClasspathEntry.USER_CLASSES;
    } else if(!JavaRuntime.isModularProject(javaProject)) {
      return IRuntimeClasspathEntry.CLASS_PATH;
    } else {
      return IRuntimeClasspathEntry.MODULE_PATH;
    }
  }

  public static boolean isModularConfiguration(ILaunchConfiguration configuration) {
    return JavaRuntime.isModularConfiguration(configuration);
  }

  public static IRuntimeClasspathEntry newModularProjectRuntimeClasspathEntry(IJavaProject javaProject) {
    return JavaRuntime.newProjectRuntimeClasspathEntry(javaProject,
        JavaRuntime.isModularProject(javaProject) ? IRuntimeClasspathEntry.MODULE_PATH
            : IRuntimeClasspathEntry.CLASS_PATH);
  }

}
