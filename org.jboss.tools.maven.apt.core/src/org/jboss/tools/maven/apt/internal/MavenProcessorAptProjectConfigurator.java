/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.maven.apt.internal;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class MavenProcessorAptProjectConfigurator extends AbstractAptProjectConfigurator {

  static final String PROCESSOR_PLUGIN_GROUP_ID = "org.bsc.maven";

  static final String PROCESSOR_PLUGIN_ARTIFACT_ID = "maven-processor-plugin";
  
  static final String GOAL_PROCESS = "process";

  @Override
  protected AnnotationProcessorConfiguration getAnnotationProcessorConfiguration(
      IMavenProjectFacade mavenProjectFacade, MavenSession mavenSession, IProgressMonitor monitor) throws CoreException {

    for(MojoExecution mojoExecution : mavenProjectFacade.getMojoExecutions(PROCESSOR_PLUGIN_GROUP_ID, 
                                                                           PROCESSOR_PLUGIN_ARTIFACT_ID, 
                                                                           monitor, 
                                                                           GOAL_PROCESS)) {

      File generatedOutputDirectory  = maven.getMojoParameterValue(mavenSession, mojoExecution, "defaultOutputDirectory", File.class);
      
      PluginDependencyResolver dependencyResolver = new PluginDependencyResolver();
      List<File> dependencies = dependencyResolver.getResolvedPluginDependencies(mavenSession, 
                                                                                 mavenProjectFacade.getMavenProject(), 
                                                                                 mojoExecution.getPlugin(), 
                                                                                 monitor);
      
      Map<String, String> options  = maven.getMojoParameterValue(mavenSession, mojoExecution, "optionMap", Map.class);
      
      
      DefaultAnnotationProcessorConfiguration configuration = new DefaultAnnotationProcessorConfiguration();
      configuration.setOutputDirectory(generatedOutputDirectory);
      configuration.setAnnotationProcessingEnabled(true);
      configuration.setDependencies(dependencies);
      configuration.setAnnotationProcessorOptions(options);
      return configuration;
    }

    
    return null;
  }


}
