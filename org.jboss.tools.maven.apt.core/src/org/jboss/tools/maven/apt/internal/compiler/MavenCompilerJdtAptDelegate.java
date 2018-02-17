/*******************************************************************************
 * Copyright (c) 2012-2018 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.jboss.tools.maven.apt.internal.compiler;

import static org.jboss.tools.maven.apt.internal.utils.ProjectUtils.extractProcessorOptions;
import static org.jboss.tools.maven.apt.internal.utils.ProjectUtils.isValidOptionName;
import static org.jboss.tools.maven.apt.internal.utils.ProjectUtils.parseProcessorOptions;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.tools.maven.apt.internal.AbstractAptConfiguratorDelegate;
import org.jboss.tools.maven.apt.internal.AnnotationProcessorConfiguration;
import org.jboss.tools.maven.apt.internal.DefaultAnnotationProcessorConfiguration;
import org.jboss.tools.maven.apt.internal.IMavenAptConstants;
import org.jboss.tools.maven.apt.internal.Messages;
import org.jboss.tools.maven.apt.internal.processor.MavenProcessorJdtAptDelegate;
import org.jboss.tools.maven.apt.internal.utils.PluginDependencyResolver;
import org.jboss.tools.maven.apt.internal.utils.ProjectUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * MavenCompilerExecutionDelegate
 *
 * @author Fred Bricon
 */
public class MavenCompilerJdtAptDelegate extends AbstractAptConfiguratorDelegate {

  /**
   * The <code>groupId</code> of the <a href="http://maven.apache.org/plugins/maven-compiler-plugin/">Maven Compiler
   * Plugin</a>.
   */
  private static final String COMPILER_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

  /**
   * The <code>artifactId</code> of the <a href="http://maven.apache.org/plugins/maven-compiler-plugin/">Maven Compiler
   * Plugin</a>.
   */
  private static final String COMPILER_PLUGIN_ARTIFACT_ID = "maven-compiler-plugin";

  /**
   * The name of the <a href="http://maven.apache.org/plugins/maven-compiler-plugin/">Maven Compiler Plugin</a>'s
   * "compile" goal.
   */
  private static final String GOAL_COMPILE = "compile";
  
  static final String OUTPUT_DIRECTORY_PARAMETER = "generatedSourcesDirectory";

  /**
   * The name of the <a href="http://maven.apache.org/plugins/maven-compiler-plugin/">Maven Compiler Plugin</a>'s
   * "testCompile" goal.
   */
  private static final String GOAL_TEST_COMPILE = "testCompile";

  static final String TEST_OUTPUT_DIRECTORY_PARAMETER = "generatedTestSourcesDirectory";

  protected IMavenMarkerManager markerManager;

  /**
   * @param markerManager
   */
  public MavenCompilerJdtAptDelegate(IMavenMarkerManager markerManager) {
    this.markerManager = markerManager;
  }

  /**
   * Ignore this configurator if maven-processor-plugin is also active.
   */
  @Override
  public boolean isIgnored(IProgressMonitor monitor) {
    try {
      return !mavenFacade.getMojoExecutions(MavenProcessorJdtAptDelegate.PROCESSOR_PLUGIN_GROUP_ID,
          MavenProcessorJdtAptDelegate.PROCESSOR_PLUGIN_ARTIFACT_ID, monitor, MavenProcessorJdtAptDelegate.GOAL_PROCESS)
          .isEmpty();
    } catch(CoreException ex) {
      //Ohoh!
      ex.printStackTrace();
    }
    return true;
  }

  @Override
  protected AnnotationProcessorConfiguration getAnnotationProcessorConfiguration(IProgressMonitor monitor)
      throws CoreException {

    markerManager.deleteMarkers(mavenFacade.getProject(), true, IMavenAptConstants.INVALID_ARGUMENT_MARKER_ID);
    ;
    MavenProject mavenProject = mavenFacade.getMavenProject(monitor);
    File generatedTestOutputDirectory = null;
    for(MojoExecution mojoExecution : mavenFacade.getMojoExecutions(COMPILER_PLUGIN_GROUP_ID,
            COMPILER_PLUGIN_ARTIFACT_ID, monitor, GOAL_TEST_COMPILE)) {
          generatedTestOutputDirectory = maven.getMojoParameterValue(mavenProject, mojoExecution,
              TEST_OUTPUT_DIRECTORY_PARAMETER, File.class, monitor);
    }
    for(MojoExecution mojoExecution : mavenFacade.getMojoExecutions(COMPILER_PLUGIN_GROUP_ID,
        COMPILER_PLUGIN_ARTIFACT_ID, monitor, GOAL_COMPILE)) {
      File generatedOutputDirectory = maven.getMojoParameterValue(mavenProject, mojoExecution,
          OUTPUT_DIRECTORY_PARAMETER, File.class, monitor);

      Map<String, String> options = new HashMap<>();

      @SuppressWarnings("unchecked")
      Map<String, String> compilerArguments = maven.getMojoParameterValue(mavenProject, mojoExecution,
          "compilerArguments", Map.class, monitor);
      options.putAll(extractProcessorOptions(compilerArguments));

      // the single compiler argument takes precedence in maven-compiler-plugin
      String compilerArgument = maven.getMojoParameterValue(mavenProject, mojoExecution, "compilerArgument",
          String.class, monitor);
      options.putAll(parseProcessorOptions(compilerArgument));

      @SuppressWarnings("unchecked")
      List<String> compilerArgs = maven.getMojoParameterValue(mavenProject, mojoExecution, "compilerArgs", List.class,
          monitor);
      options.putAll(ProjectUtils.parseProcessorOptions(compilerArgs));

      sanitizeOptionNames(options.keySet(), mojoExecution);

      boolean isAnnotationProcessingEnabled = (compilerArgument == null) || !compilerArgument.contains("-proc:none");
      if(isAnnotationProcessingEnabled) {
        String proc = maven.getMojoParameterValue(mavenProject, mojoExecution, "proc", String.class, monitor);
        isAnnotationProcessingEnabled = !"none".equals(proc);
      }

      final Dependency[] annotationProcessorPaths = maven.getMojoParameterValue(mavenProject, mojoExecution,
          "annotationProcessorPaths", Dependency[].class, monitor);

      final boolean hasAnnotationProcessorPaths = annotationProcessorPaths.length > 0;

      PluginDependencyResolver dependencyResolver = new PluginDependencyResolver() {
        @Override
        protected Collection<Dependency> getDependencies(Plugin plugin) {
          return hasAnnotationProcessorPaths ? Arrays.asList(annotationProcessorPaths) : super.getDependencies(plugin);
        }
      };

      List<File> dependencies = dependencyResolver.getResolvedPluginDependencies(mavenSession,
          mavenFacade.getMavenProject(), mojoExecution.getPlugin(), monitor);

      DefaultAnnotationProcessorConfiguration configuration = new DefaultAnnotationProcessorConfiguration();
      configuration.setOutputDirectory(generatedOutputDirectory);
      configuration.setAnnotationProcessingEnabled(isAnnotationProcessingEnabled);
      configuration.setAddProjectDependencies(!hasAnnotationProcessorPaths);
      configuration.setDependencies(dependencies);
      configuration.setAnnotationProcessorOptions(options);
      configuration.setTestOutputDirectory(generatedTestOutputDirectory);
      return configuration;
    }

    return null;
  }

  /**
   * 'javac' fails if an option name is not valid. Eclipse APT does not reject such names, so this method drops them
   * from the given set and creates error markers for the POM.
   *
   * @param optionNames
   * @param mojoExecution
   * @throws CoreException
   */
  private void sanitizeOptionNames(Set<String> optionNames, MojoExecution mojoExecution) throws CoreException {
    if(optionNames.isEmpty()) {
      return;
    }
    Iterator<String> iter = optionNames.iterator();
    SourceLocation location = SourceLocationHelper.findLocation(mavenFacade.getMavenProject(new NullProgressMonitor()),
        new MojoExecutionKey(mojoExecution));
    while(iter.hasNext()) {
      String optionName = iter.next();
      if(!isValidOptionName(optionName)) {
        markerManager.addErrorMarker(mavenFacade.getPom(), IMavenAptConstants.INVALID_ARGUMENT_MARKER_ID,
            new MavenProblemInfo(NLS.bind(Messages.ProjectUtils_error_invalid_option_name, optionName), location));

        iter.remove();
      }
    }
  }

}
