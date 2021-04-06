/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *      Metron, Inc. - support for provides/uses directives
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.AutomaticModuleNaming;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.launching.RuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;


/**
 * Helper for Java Module Support
 *
 * @author Fred Bricon
 * @since 1.8.2
 */
@SuppressWarnings("restriction")
public class ModuleSupport {

  public static final String MODULE_INFO_JAVA = "module-info.java";

  private static final Logger log = LoggerFactory.getLogger(ModuleSupport.class);

  /**
   * Sets <code>module</code> flag to <code>true</code> to classpath dependencies declared in module-info.java
   *
   * @param facade a Maven facade project
   * @param classpath a classpath descriptor
   * @param monitor a progress monitor
   */
  public static void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath,
      IProgressMonitor monitor) {
    IJavaProject javaProject = JavaCore.create(facade.getProject());
    if(javaProject == null || !javaProject.exists() || classpath == null) {
      return;
    }

    int targetCompliance = 8;
    String option = javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);
    if(option != null) {
      if(option.startsWith("1.")) {
        option = option.substring("1.".length());
      }
      try {
        targetCompliance = Integer.parseInt(option);
      } catch(NumberFormatException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    if(targetCompliance < 9) {
      return;
    }

    if(monitor == null) {
      monitor = new NullProgressMonitor();
    }

    InternalModuleInfo moduleInfo = getModuleInfo(javaProject, monitor);
    if(moduleInfo == null) {
      return;
    }

    Map<String, InternalModuleInfo> entryModuleInfos = new LinkedHashMap<>();
    Map<String, IClasspathEntryDescriptor> entryDescriptors = new LinkedHashMap<>();
    for(IClasspathEntryDescriptor entryDescriptor : classpath.getEntryDescriptors()) {
      if(monitor.isCanceled()) {
        return;
      }
      InternalModuleInfo entryModuleInfo = getModuleInfo(entryDescriptor, monitor, targetCompliance);
      if(entryModuleInfo != null) {
        entryModuleInfos.put(entryModuleInfo.name, entryModuleInfo);//potentially suppresses duplicate entries from the same workspace project, with different classifiers
        entryDescriptors.put(entryModuleInfo.name, entryDescriptor);
      }
    }

    Set<String> neededModuleNames = collectModulesNeededTransitively(moduleInfo, entryModuleInfos);
    if(monitor.isCanceled()) {
      return;
    }

    entryDescriptors.forEach((entryModuleName, entry) -> {
      if(neededModuleNames.contains(entryModuleName)) {
        entry.setClasspathAttribute(IClasspathAttribute.MODULE, Boolean.TRUE.toString());
      }
    });
  }

  private static Set<String> collectModulesNeededTransitively(InternalModuleInfo module,
      Map<String, InternalModuleInfo> classpathModules) {
    Set<String> result = new LinkedHashSet<>();
    Function<InternalModuleInfo, Set<String>> neededModulesLookup = createNeededModulesLookup(classpathModules);
    Set<String> todo = neededModulesLookup.apply(module);
    while(!todo.isEmpty()) {
      Set<String> todoNext = new LinkedHashSet<>();
      for(String neededModuleName : todo) {
        if(result.add(neededModuleName)) {
          InternalModuleInfo neededModule = classpathModules.get(neededModuleName);
          todoNext.addAll(neededModulesLookup.apply(neededModule));
        } else {
          //already checked that module
        }
      }
      todo = todoNext;
    }
    return result;
  }

  /**
   * Returns a function that takes a {@link ModuleInfo}, and looks up the names of the modules needed by the given
   * module -- including modules it requires, and also modules that provide services it uses.
   */
  private static Function<InternalModuleInfo, Set<String>> createNeededModulesLookup(
      Map<String, InternalModuleInfo> classpathModules) {
    Map<String, Set<String>> providersByServiceName = new LinkedHashMap<>();
    for(InternalModuleInfo classpathModule : classpathModules.values()) {
      for(String serviceName : classpathModule.providedServiceNames) {
        providersByServiceName.computeIfAbsent(serviceName, k -> new LinkedHashSet<>()).add(classpathModule.name);
      }
    }
    return (module) -> {
      if(module != null) {
        Set<String> result = new LinkedHashSet<>();
        result.addAll(module.requiredModuleNames);
        for(String serviceName : module.usedServiceNames) {
          Set<String> providerNames = providersByServiceName.getOrDefault(serviceName, Collections.emptySet());
          result.addAll(providerNames);
        }
        return result;
      }
      return Collections.emptySet();
    };
  }

  private static InternalModuleInfo getModuleInfo(IClasspathEntryDescriptor entry, IProgressMonitor monitor,
      int targetCompliance) {
    if(entry != null && !monitor.isCanceled()) {
      if(IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        return getModuleInfo(entry.getPath().toFile(), targetCompliance);
      } else if(IClasspathEntry.CPE_PROJECT == entry.getEntryKind()) {
        return getModuleInfo(getJavaProject(entry.getPath()), monitor);
      }
    }
    return null;
  }

  static InternalModuleInfo getModuleInfo(IJavaProject project, IProgressMonitor monitor) {
    if(project != null) {
      try {
        IModuleDescription moduleDescription = project.getModuleDescription();
        if(moduleDescription != null) {
          return InternalModuleInfo.fromDescription(moduleDescription);
        }

        String buildName = null;
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project.getProject());
        if(facade != null) {
          buildName = facade.getFinalName();
        }
        if(buildName == null || buildName.isEmpty()) {
          buildName = project.getElementName();
        }
        String moduleName = new String(AutomaticModuleNaming.determineAutomaticModuleName(buildName, false, null));
        return InternalModuleInfo.withAutomaticName(moduleName);

      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    return null;
  }

  private static InternalModuleInfo getModuleInfo(File file, int targetCompliance) {
    if(!file.isFile()) {
      return null;
    }
    try (JarFile jar = new JarFile(file, false)) {
      Manifest manifest = jar.getManifest();
      boolean isMultiRelease = false;
      if(manifest != null) {
        isMultiRelease = "true".equalsIgnoreCase(manifest.getMainAttributes().getValue("Multi-Release"));
      }
      int compliance = isMultiRelease ? targetCompliance : 8;
      for(int i = compliance; i >= 8; i-- ) {
        String filename;
        if(i == 8) {
          // 8 represents unversioned module-info.class
          filename = IModule.MODULE_INFO_CLASS;
        } else {
          filename = "META-INF/versions/" + i + "/" + IModule.MODULE_INFO_CLASS;
        }
        ClassFileReader reader = ClassFileReader.read(jar, filename);
        if(reader != null) {
          IModule module = reader.getModuleDeclaration();
          if(module != null) {
            return InternalModuleInfo.fromDeclaration(module);
          }
        }
      }
      if(manifest != null) {
        // optimization: we already have the manifest, so directly check for Automatic-Module-Name
        // rather than using AutomaticModuleNaming.determineAutomaticModuleName(String)
        String automaticModuleName = manifest.getMainAttributes().getValue("Automatic-Module-Name");
        if(automaticModuleName != null) {
          return InternalModuleInfo.withAutomaticName(automaticModuleName);
        }
      }
    } catch(ClassFormatException | IOException ex) {
      log.error(ex.getMessage(), ex);
    }
    return InternalModuleInfo.withAutomaticNameFromFile(file);
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
      classpathProperty = IRuntimeClasspathEntry.PATCH_MODULE;
    }
    IRuntimeClasspathEntry newArchiveRuntimeClasspathEntry = JavaRuntime
        .newArchiveRuntimeClasspathEntry(folder.getFullPath(), classpathProperty);
    if(classpathProperty == IRuntimeClasspathEntry.PATCH_MODULE) {
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

  public static IRuntimeClasspathEntry newModularProjectRuntimeClasspathEntry(IJavaProject javaProject) {
    return JavaRuntime.newProjectRuntimeClasspathEntry(javaProject,
        JavaRuntime.isModularProject(javaProject) ? IRuntimeClasspathEntry.MODULE_PATH
            : IRuntimeClasspathEntry.CLASS_PATH);
  }

  public static boolean isMavenJavaProject(IProject project) {
    try {
      return project != null && project.isOpen() && project.hasNature(IMavenConstants.NATURE_ID)
          && project.hasNature(JavaCore.NATURE_ID);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
    return false;
  }
}
