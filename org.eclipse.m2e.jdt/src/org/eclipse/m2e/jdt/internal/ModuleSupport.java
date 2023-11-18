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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.AutomaticModuleNaming;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.core.JrtPackageFragmentRoot;
import org.eclipse.jdt.internal.launching.RuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;


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
        // ignore jsr14
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
    if(classpathProperty == IRuntimeClasspathEntry.MODULE_PATH
        && !folder.exists(IPath.fromOSString("module-info.class"))) {
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

  /**
   * Updates M2E container and JRE container with the jpms arguments provided to the maven-compiler-plugin. The dispatch
   * between M2E and JRE containers is done using the targeted module. If the JRE container contains the targeted module
   * then the jpms argument will be attached to it else it will be attached to the M2E container
   * 
   * @param request
   * @param classpath
   * @param monitor
   * @param compilerArgs
   */
  public static void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor, List<String> compilerArgs) {

    IMavenProjectFacade facade = request.mavenProjectFacade();

    IJavaProject javaProject = JavaCore.create(facade.getProject());
    if(javaProject == null || !javaProject.exists() || classpath == null) {
      return;
    }

    Optional<JpmsArgs> jpmsArgs = JpmsArgs.computeFromArgs(compilerArgs);

    if(jpmsArgs.isEmpty()) {
      return;
    }

    IClasspathEntry m2eEntry = findMavenContainerEntry(classpath);
    IClasspathEntry jreEntry = findJreContainerEntry(classpath);

    IClasspathEntryDescriptor m2eEntryDescriptor = new ClasspathEntryDescriptor(m2eEntry);
    IClasspathEntryDescriptor jreEntryDescriptor = new ClasspathEntryDescriptor(jreEntry);

    // list all the modules managed in the jre container
    final List<String> availableModules = getRootModules(javaProject, jreEntry);
    log.debug("Found {} modules managed by the JRE Container", availableModules.size());

    // iterate and add attributes to the matching container jre/m2e
    for(JpmsArgType argType : JpmsArgType.values()) {
      List<JpmsArgValue> values = argType.getFromArgs(jpmsArgs.get());

      if(values != null && !values.isEmpty()) {

        // dispatch jpms args between jre and m2e container
        Map<Boolean, List<JpmsArgValue>> partitioned = values.stream()
            .collect(Collectors.partitioningBy(v -> availableModules.contains(v.getModule())));

        List<JpmsArgValue> jreValues = partitioned.get(true);
        List<JpmsArgValue> m2eValues = partitioned.get(false);

        jreEntryDescriptor.setClasspathAttribute(argType.getEclipseArgumentName(), null);
        if(jreValues != null && !jreValues.isEmpty()) {

          String valuesAsString = jreValues.stream().map(JpmsArgValue::getValue).distinct()
              .collect(Collectors.joining(JpmsArgs.VALUE_SEPARATOR));

          jreEntryDescriptor.setClasspathAttribute(IClasspathAttribute.MODULE, "true");
          jreEntryDescriptor.setClasspathAttribute(argType.getEclipseArgumentName(), valuesAsString);
        }

        m2eEntryDescriptor.setClasspathAttribute(argType.getEclipseArgumentName(), null);
        if(m2eValues != null && !m2eValues.isEmpty()) {

          String valuesAsString = m2eValues.stream().map(JpmsArgValue::getValue).distinct()
              .collect(Collectors.joining(JpmsArgs.VALUE_SEPARATOR));

          //m2eEntryDescriptor.setClasspathAttribute(IClasspathAttribute.MODULE, "true");
          m2eEntryDescriptor.setClasspathAttribute(argType.getEclipseArgumentName(), valuesAsString);
        }
      }
    }

    classpath.replaceEntry(descriptor -> IClasspathManager.CONTAINER_ID.equals(descriptor.getPath().segment(0)),
        m2eEntryDescriptor.toClasspathEntry());
    classpath.replaceEntry(descriptor -> JavaRuntime.JRE_CONTAINER.equals(descriptor.getPath().segment(0)),
        jreEntryDescriptor.toClasspathEntry());

  }

  private static IClasspathEntry findContainerEntry(IClasspathDescriptor classpath,
      Predicate<IClasspathEntry> predicate) {
    // when .classpath file is deleted we need to search those entries directly in non commited classpath
    for(IClasspathEntry entry : classpath.getEntries()) {
      if(predicate.test(entry)) {
        return entry;
      }
    }
    return null;
  }

  private static IClasspathEntry findMavenContainerEntry(IClasspathDescriptor classpath) {
    return findContainerEntry(classpath, (e) -> MavenClasspathHelpers.isMaven2ClasspathContainer(e.getPath()));
  }

  private static IClasspathEntry findJreContainerEntry(IClasspathDescriptor classpath) {
    return findContainerEntry(classpath, (e) -> MavenClasspathHelpers.isJREClasspathContainer(e.getPath()));
  }

  private static List<String> getRootModules(IJavaProject javaProject, IClasspathEntry entry) {

    Set<String> result = new HashSet<>();

    try {
      for(IClasspathEntry entry2 : javaProject.getResolvedClasspath(false)) {
        IPackageFragmentRoot[] fAllSystemRoots = javaProject.findUnfilteredPackageFragmentRoots(entry2);
        for(IPackageFragmentRoot pfr : fAllSystemRoots) {

          if(pfr instanceof JrtPackageFragmentRoot) {
            String moduleName = pfr.getElementName();
            IModuleDescription module = pfr.getModuleDescription();// javaProject.findModule(moduleName, null);

            if(module == null) {
              continue;
            }

            result.add(moduleName);
          }
        }
      }

      return new ArrayList<>(result);
    } catch(JavaModelException ex) {
      log.error(ex.getMessage(), ex);
      return Collections.emptyList();
    }
  }

  private static class JpmsArgValue {

    private final String module;

    private final String value;

    /**
     * @param module
     * @param value
     */
    public JpmsArgValue(String module, String value) {
      super();
      this.module = module;
      this.value = value;
    }

    /**
     * @return Returns the module.
     */
    public String getModule() {
      return this.module;
    }

    /**
     * @return Returns the value.
     */
    public String getValue() {
      return this.value;
    }

  }

  private static class JpmsArgs {

    public static final String VALUE_SEPARATOR = ":";

    private final List<JpmsArgValue> addExports = new ArrayList<>();

    private final List<JpmsArgValue> addOpens = new ArrayList<>();

    // does not seem supported by jdt
    //private final List<JpmsArgValue> addModules = new ArrayList<>();

    private final List<JpmsArgValue> addReads = new ArrayList<>();

    private final List<JpmsArgValue> patchModule = new ArrayList<>();

    protected static Optional<JpmsArgs> computeFromArgs(List<String> args) {
      JpmsArgs jpmsArgs = null;

      if(args != null) {
        ListIterator<String> it = args.listIterator();

        while(it.hasNext()) {
          String argumentName = it.next();
          JpmsArgType argType = JpmsArgType.valueFromArgumentName(argumentName);

          if(argType == null) {
            continue;
          }

          String value = null;
          boolean isNextArg = false;
          if(argType.getArgumentName().equalsIgnoreCase(argumentName)) {
            // full argument match
            if(it.hasNext()) {
              value = it.next();
              isNextArg = true;
            }
          } else {
            // argument with delimiter match
            String argumentWithDelimiter = argType.getArgumentName().toLowerCase() + JpmsArgType.argumentDelimiter;
            if(argumentName.toLowerCase().startsWith(argumentWithDelimiter)) {
              value = argumentName.substring(argumentWithDelimiter.length());
            }
          }
          JpmsArgValue argValue = argType.parse(value);

          if(argValue == null) {
            if(isNextArg) {
              it.previous();
            }
            continue;
          }

          if(jpmsArgs == null) {
            jpmsArgs = new JpmsArgs();
          }
          argType.addToArgs(jpmsArgs, argValue);
        }
      }

      return Optional.ofNullable(jpmsArgs);
    }

    /**
     * @return Returns the exports.
     */
    public List<JpmsArgValue> getAddExports() {
      return this.addExports;
    }

    public boolean toAddExports(JpmsArgValue addExport) {
      return this.addExports.add(addExport);
    }

    /**
     * @return Returns the opens.
     */
    public List<JpmsArgValue> getAddOpens() {
      return this.addOpens;
    }

    public boolean toAddOpens(JpmsArgValue addOpen) {
      return this.addOpens.add(addOpen);
    }

//    /**
//     * @return Returns the modules.
//     */
//    public List<JpmsArgValue> getAddModules() {
//      return this.addModules;
//    }
//
//    public boolean toAddModules(JpmsArgValue addModule) {
//      return this.addModules.add(addModule);
//    }

    /**
     * @return Returns the reads.
     */
    public List<JpmsArgValue> getAddReads() {
      return this.addReads;
    }

    public boolean toAddReads(JpmsArgValue addRead) {
      return this.addReads.add(addRead);
    }

    /**
     * @return Returns the patched modules.
     */
    public List<JpmsArgValue> getPatchModule() {
      return this.patchModule;
    }

    public boolean toPatchModule(JpmsArgValue patchModule) {
      return this.patchModule.add(patchModule);
    }
  }

  protected enum JpmsArgType {
    // @formatter:off
    AddExports("--add-exports", IClasspathAttribute.ADD_EXPORTS,
        Pattern.compile("^(([A-Za-z0-9\\.$_]*)/[A-Za-z0-9\\.$_]*=[A-Za-z0-9\\.$_-]*)$"), (a, v) -> a.toAddExports(v),
        JpmsArgs::getAddExports),

    AddOpens("--add-opens", IClasspathAttribute.ADD_OPENS,
        Pattern.compile("^(([A-Za-z0-9\\.$_]*)/[A-Za-z0-9\\.$_]*=[A-Za-z0-9\\.$_-]*)$"), (a, v) -> a.toAddOpens(v),
        JpmsArgs::getAddOpens),

//    AddModules("--add-modules", IClasspathAttribute."add-modules", //Not supported 
//        Pattern.compile("^(()[A-Za-z0-9\\.$_\\-,]*)$"),
//        (a, v) -> a.toAddModules(v),
//        (a) -> a.getAddModules()), 

    AddReads("--add-reads", IClasspathAttribute.ADD_READS,
        Pattern.compile("^(([A-Za-z0-9\\.$_]*)=[A-Za-z0-9\\.$_-]*)$"), (a, v) -> a.toAddReads(v),
        JpmsArgs::getAddReads),

    PatchModule("--patch-module", IClasspathAttribute.PATCH_MODULE, Pattern.compile("^(([A-Za-z0-9\\.$_]*)=.*)$"), // maybe .* for jar name is too permissive
        (a, v) -> a.toPatchModule(v), JpmsArgs::getPatchModule);
    // @formatter:on

    private String argumentName;

    private String eclipseArgumentName;

    private Pattern extractPattern;

    private BiFunction<JpmsArgs, JpmsArgValue, Boolean> addFunction;

    private Function<JpmsArgs, List<JpmsArgValue>> getFunction;

    public static final String argumentDelimiter = "=";

    /**
     * Jpms argument type enum constructor
     * 
     * @param argumentName the argument name to search in the compiler arguments
     * @param eclipseArgumentName the attribute name to use in the eclipse container
     * @param extractPattern this pattern must validate the value and extract 2 groups, first the value and second the
     *          targeted module name
     * @param addFunction function used to populate the {@link JpmsArgs}
     * @param getFunction function used to get {@link JpmsArgValue} list from the {@link JpmsArgs}
     */
    JpmsArgType(String argumentName, String eclipseArgumentName, Pattern extractPattern,
        BiFunction<JpmsArgs, JpmsArgValue, Boolean> addFunction, Function<JpmsArgs, List<JpmsArgValue>> getFunction) {
      this.argumentName = argumentName;
      this.eclipseArgumentName = eclipseArgumentName;
      this.extractPattern = extractPattern;
      this.addFunction = addFunction;
      this.getFunction = getFunction;
    }

    /**
     * Parse a Jpms argument value provided to the maven compiler into a {@link JpmsArgValue}
     * 
     * @param value
     * @return the parsed value
     */
    public JpmsArgValue parse(String value) {
      if(value == null) {
        return null;
      }

      String trimed = value.trim();
      Matcher matcher = extractPattern.matcher(trimed);

      if(matcher.matches()) {
        String argValue = matcher.group(1);
        String module = matcher.group(2);
        return new JpmsArgValue(module, argValue);
      }

      return null;
    }

    public boolean addToArgs(JpmsArgs args, JpmsArgValue value) {
      return this.addFunction.apply(args, value);
    }

    public List<JpmsArgValue> getFromArgs(JpmsArgs args) {
      return this.getFunction.apply(args);
    }

    /**
     * Provided an jpms compiler argument return an {@link JpmsArgType}
     * 
     * @param the argument name
     * @return
     */
    static JpmsArgType valueFromArgumentName(String argumentName) {
      if(argumentName == null) {
        return null;
      }

      return Arrays.stream(JpmsArgType.values()).filter(at -> at.getArgumentName().equalsIgnoreCase(argumentName))
          .findFirst()
          .or(() -> Arrays.stream(JpmsArgType.values())
              .filter(
                  at -> argumentName.toLowerCase().startsWith(at.getArgumentName().toLowerCase() + argumentDelimiter))
              .findFirst())
          .orElse(null);
    }

    /**
     * @return Returns the argumentName.
     */
    public String getArgumentName() {
      return this.argumentName;
    }

    /**
     * @return Returns the eclipseArgumentName.
     */
    public String getEclipseArgumentName() {
      return this.eclipseArgumentName;
    }

  }
}
