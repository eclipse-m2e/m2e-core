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

package org.jboss.tools.maven.apt.internal.processor;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.jboss.tools.maven.apt.internal.AbstractAptConfiguratorDelegate;
import org.jboss.tools.maven.apt.internal.AnnotationProcessorConfiguration;
import org.jboss.tools.maven.apt.internal.DefaultAnnotationProcessorConfiguration;
import org.jboss.tools.maven.apt.internal.utils.PluginDependencyResolver;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * MavenProcessorJdtAptDelegate
 *
 * @author Fred Bricon
 */
public class MavenProcessorJdtAptDelegate extends AbstractAptConfiguratorDelegate {

  public static final String PROCESSOR_PLUGIN_GROUP_ID = "org.bsc.maven";

  public static final String PROCESSOR_PLUGIN_ARTIFACT_ID = "maven-processor-plugin";

  public static final String GOAL_PROCESS = "process";

  public static final String GOAL_PROCESS_TEST = "process-test";

  static final String SOURCE_DIRECTORY_PARAMETER = "sourceDirectory";

  static final String OUTPUT_DIRECTORY_PARAMETER = "outputDirectory";

  static final String DEFAULT_OUTPUT_DIRECTORY_PARAMETER = "defaultOutputDirectory";

  @Override
  protected AnnotationProcessorConfiguration getAnnotationProcessorConfiguration(IProgressMonitor monitor)
      throws CoreException {

    MojoExecution mojoExecution = getProcessorPluginMojoExecution(mavenFacade, GOAL_PROCESS, monitor);
    if(mojoExecution == null) {
      return null;
    }
    File generatedOutputDirectory = getParameterValue(OUTPUT_DIRECTORY_PARAMETER, File.class, mavenSession,
        mojoExecution);

    PluginDependencyResolver dependencyResolver = new PluginDependencyResolver();
    List<File> dependencies = dependencyResolver.getResolvedPluginDependencies(mavenSession,
        mavenFacade.getMavenProject(), mojoExecution.getPlugin(), monitor);

    @SuppressWarnings("unchecked")
    Map<String, String> options = getParameterValue("optionMap", Map.class, mavenSession, mojoExecution);

    DefaultAnnotationProcessorConfiguration configuration = new DefaultAnnotationProcessorConfiguration();
    configuration.setOutputDirectory(generatedOutputDirectory);
    configuration.setAnnotationProcessingEnabled(true);
    configuration.setDependencies(dependencies);
    configuration.setAnnotationProcessorOptions(options);

    MojoExecution testMojoExecution = getProcessorPluginMojoExecution(mavenFacade, GOAL_PROCESS_TEST, monitor);
    if(testMojoExecution != null) {
      File generatedTestOutputDirectory = getParameterValue(OUTPUT_DIRECTORY_PARAMETER, File.class, mavenSession,
          testMojoExecution);
      configuration.setTestOutputDirectory(generatedTestOutputDirectory);
    }

    return configuration;
  }

  protected MojoExecution getProcessorPluginMojoExecution(IMavenProjectFacade mavenProjectFacade, String goal,
      IProgressMonitor monitor) throws CoreException {
    for(MojoExecution mojoExecution : mavenProjectFacade.getMojoExecutions(PROCESSOR_PLUGIN_GROUP_ID,
        PROCESSOR_PLUGIN_ARTIFACT_ID, monitor, goal)) {
      return mojoExecution;
    }
    return null;
  }

  @Override
  protected <T> T getParameterValue(String parameter, Class<T> asType, MavenSession session,
      MojoExecution mojoExecution) throws CoreException {
    T result = super.getParameterValue(parameter, asType, session, mojoExecution);
    if(OUTPUT_DIRECTORY_PARAMETER.equals(parameter) && (result == null)) {
      return super.getParameterValue(DEFAULT_OUTPUT_DIRECTORY_PARAMETER, asType, session, mojoExecution);
    }
    return result;
  }

}
