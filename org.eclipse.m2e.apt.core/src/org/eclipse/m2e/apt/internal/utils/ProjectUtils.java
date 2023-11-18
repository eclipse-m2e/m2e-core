/*******************************************************************************
 * Copyright (c) 2012-2019 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.apt.internal.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;

import org.eclipse.m2e.apt.internal.utils.AnnotationServiceLocator.ServiceEntry;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * ProjectUtils
 *
 * @author Fred Bricon
 */
public class ProjectUtils {
  private ProjectUtils() {
  }

  private static final Pattern OPTION_PATTERN = Pattern.compile("-A([^ \\t\"']+)");

  /**
   * Parse a string to extract Annotation Processor options
   */
  public static Map<String, String> parseProcessorOptions(String compilerArgument) {

    if((compilerArgument == null) || compilerArgument.trim().isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> ret = new HashMap<>();

    Matcher matcher = OPTION_PATTERN.matcher(compilerArgument);

    int start = 0;
    while(matcher.find(start)) {
      String argument = matcher.group(1);
      parse(argument, ret);
      start = matcher.end();
    }

    return ret;
  }

  private static void parse(String argument, Map<String, String> results) {
    if(argument == null || argument.isBlank()) {
      return;
    }
    String key;
    String value;
    int optionalEqualsIndex = argument.indexOf('=');
    switch(optionalEqualsIndex) {
      case -1: { // -Akey : ok
        key = argument;
        value = null;
        break;
      }
      case 0: { // -A=value : invalid
        return;
      }
      default: {
        key = argument.substring(0, optionalEqualsIndex);
        if(containsWhitespace(key)) { // -A key = value : invalid
          return;
        }
        value = argument.substring(optionalEqualsIndex + 1, argument.length());
      }
    }
    results.put(key, value);
  }

  public static Map<String, String> parseProcessorOptions(List<String> compilerArgs) {
    if((compilerArgs == null) || compilerArgs.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> options = new HashMap<>();

    for(String arg : compilerArgs) {
      if(arg != null && arg.startsWith("-A")) {
        parse(arg.substring(2), options);
      }
    }
    return options;
  }

  /**
   * Extract Annotation Processor options from a compiler-argument map
   */
  public static Map<String, String> extractProcessorOptions(Map<String, String> compilerArguments) {
    if((compilerArguments == null) || compilerArguments.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> ret = new HashMap<>(compilerArguments.size());

    for(Map.Entry<String, String> argument : compilerArguments.entrySet()) {
      String key = argument.getKey();

      if(key.startsWith("A")) {
        String value = argument.getValue();
        if((value != null) && (value.length() > 0)) {
          ret.put(key.substring(1), value);
        } else {
          ret.put(key.substring(1), null);
        }
      }
    }

    return ret;
  }

  /**
   * Validates that the name of a processor option conforms to the grammar defined by
   * <code>javax.annotation.processing.Processor.getSupportedOptions()</code>.
   *
   * @param optionName
   * @return <code>true</code> if the name conforms to the grammar, <code>false</code> if not.
   */
  public static boolean isValidOptionName(String optionName) {
    if(optionName == null) {
      return false;
    }

    boolean startExpected = true;
    int codePoint;

    for(int i = 0; i < optionName.length(); i += Character.charCount(codePoint)) {
      codePoint = optionName.codePointAt(i);

      if(startExpected) {
        if(!Character.isJavaIdentifierStart(codePoint)) {
          return false;
        }

        startExpected = false;

      } else {
        if(codePoint == '.') {
          startExpected = true;

        } else if(!Character.isJavaIdentifierPart(codePoint)) {
          return false;
        }
      }
    }

    return !startExpected;
  }

  /**
   * Converts the specified relative or absolute {@link File} to a {@link File} that is relative to the base directory
   * of the specified {@link IProject}.
   *
   * @param project the {@link IProject} whose base directory the returned {@link File} should be relative to
   * @param fileToConvert the relative or absolute {@link File} to convert
   * @return a {@link File} that is relative to the base directory of the specified {@link IProject}
   */
  public static File convertToProjectRelativePath(IProject project, File fileToConvert) {
    // Get an absolute version of the specified file
    File absoluteFile = fileToConvert.getAbsoluteFile();
    String absoluteFilePath = absoluteFile.getAbsolutePath();

    // Get a File for the absolute path to the project's directory
    File projectBasedirFile = project.getLocation().toFile().getAbsoluteFile();
    String projectBasedirFilePath = projectBasedirFile.getAbsolutePath();

    // Compute the relative path
    if(absoluteFile.equals(projectBasedirFile)) {
      return new File(".");
    } else if(absoluteFilePath.startsWith(projectBasedirFilePath)) {
      String projectRelativePath = absoluteFilePath.substring(projectBasedirFilePath.length() + 1);
      return new File(projectRelativePath);
    } else {
      return absoluteFile;
    }
  }

  /**
   * @param mavenProjectFacade the {@link IMavenProjectFacade} to get the {@link Artifact}s for
   * @return an ordered {@link List} of the specified {@link IMavenProjectFacade}'s {@link Artifact}s
   */
  public static List<Artifact> getProjectArtifacts(IMavenProjectFacade mavenProjectFacade) {
    /*
     * This method essentially wraps org.apache.maven.project.MavenProject.getArtifacts(),
     * returning a List instead of a Set to indicate that ordering is maintained and important.
     * The set being "wrapped" is actually a LinkedHashSet, which does guarantee a consistent
     * insertion & iteration order.
     */

    Set<Artifact> unorderedArtifacts = mavenProjectFacade.getMavenProject().getArtifacts();
    List<Artifact> orderedArtifacts = new ArrayList<>(unorderedArtifacts.size());
    for(Artifact artifact : unorderedArtifacts) {
      orderedArtifacts.add(artifact);
    }
    return orderedArtifacts;
  }

  /**
   * <p>
   * Filters the specified {@link Artifact}s to those that match the following criteria:
   * </p>
   * <ul>
   * <li>{@link Artifact#isResolved()} is <code>true</code></li>
   * <li>{@link Artifact#getArtifactHandler().getExtension()} equals "jar"</li>
   * <li>{@link Artifact#getScope()} equals {@link Artifact#SCOPE_COMPILE}</li>
   * <li>{@link Artifact#getFile()} returns a {@link File} where {@link File#isFile()} is <code>true</code></li>
   * </ul>
   *
   * @param artifacts the {@link Set} of {@link Artifact}s to filter
   * @return the actual JAR {@link File}s available from the specified {@link Artifact}s
   */
  public static List<File> filterToResolvedJars(List<Artifact> artifacts) {
    ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE_PLUS_RUNTIME);

    // Ensure that this Artifact should be included and it resolves to a File that we can use
    return artifacts.stream().filter(Artifact::isResolved).filter(filter::include).map(Artifact::getFile)
        .filter(ProjectUtils::isJar).collect(Collectors.toList());
  }

  /**
   * Returns <code>true</code> if any of the specified JARs contain a Java 5 or Java 6 annotation processor,
   * <code>false</code> if none of them do.
   *
   * @param resolvedJarArtifacts the JAR {@link File}s to inspect for annotation processors
   * @return <code>true</code> if any of the specified JARs contain a Java 5 or Java 6 annotation processor,
   *         <code>false</code> if none of them do
   */
  public static boolean containsAptProcessors(Collection<File> resolvedJarArtifacts) {
    // Read through all JARs, checking for any APT service entries
    try {
      for(File resolvedJarArtifact : resolvedJarArtifacts) {
        Set<ServiceEntry> aptServiceEntries = AnnotationServiceLocator.getAptServiceEntries(resolvedJarArtifact);
        if(!aptServiceEntries.isEmpty()) {
          return true;
        }
      }
    } catch(IOException e) {
      Platform.getLog(ProjectUtils.class).log(Status.error("Error while reading artifact JARs.", e));
    }
    // No service entries were found
    return false;
  }

  /**
   * Disable JDT APT on this project
   */
  public static void disableApt(IProject project) {
    if(project == null) {
      return;
    }

    IJavaProject javaProject = JavaCore.create(project);
    if((javaProject != null) && AptConfig.isEnabled(javaProject)) {
      AptConfig.setEnabled(javaProject, false);
    }
  }

  public static boolean isJar(File file) {
    return file.isFile() && "jar".equals(IPath.fromOSString(file.getAbsolutePath()).getFileExtension());
  }

  private static boolean containsWhitespace(String seq) {
    return seq != null && !seq.isBlank() && seq.chars().anyMatch(Character::isWhitespace);
  }
}
