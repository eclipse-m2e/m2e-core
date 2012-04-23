/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 *     Karl M. Davis (Knowledge Computing Corp.) - initial implementation
 ************************************************************************************/
package org.jboss.tools.maven.apt.internal;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class MavenCompilerAptProjectConfigurator extends AbstractAptProjectConfigurator {

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
  
  @Override
  protected AnnotationProcessorConfiguration getAnnotationProcessorConfiguration(
      IMavenProjectFacade mavenProjectFacade, MavenSession mavenSession, IProgressMonitor monitor) throws CoreException {
    
     for(MojoExecution mojoExecution : mavenProjectFacade.getMojoExecutions(COMPILER_PLUGIN_GROUP_ID,
         COMPILER_PLUGIN_ARTIFACT_ID, monitor, GOAL_COMPILE)) {
       File generatedOutputDirectory  = maven.getMojoParameterValue(mavenSession, mojoExecution, "generatedSourcesDirectory", File.class);
       String compilerArgument  = maven.getMojoParameterValue(mavenSession, mojoExecution, "compilerArgument", String.class);
       
       boolean isAnnotationProcessingEnabled = compilerArgument == null || !compilerArgument.contains("-proc:none");  
       if (isAnnotationProcessingEnabled ) {
         String proc = maven.getMojoParameterValue(mavenSession, mojoExecution, "proc", String.class);
         isAnnotationProcessingEnabled = !"none".equals(proc); 
       }

       PluginDependencyResolver dependencyResolver = new PluginDependencyResolver();
       List<File> dependencies = dependencyResolver.getResolvedPluginDependencies(mavenSession, 
                                                                                  mavenProjectFacade.getMavenProject(), 
                                                                                  mojoExecution.getPlugin(), 
                                                                                  monitor);
       
       
       
       DefaultAnnotationProcessorConfiguration configuration = new DefaultAnnotationProcessorConfiguration();
       configuration.setOutputDirectory(generatedOutputDirectory);
       configuration.setAnnotationProcessingEnabled(isAnnotationProcessingEnabled);
       configuration.setDependencies(dependencies);
       return configuration;
     }
     
     return null;
  }
  
  @Override
  protected boolean ignoreConfigurator(IMavenProjectFacade mavenProjectFacade, IProgressMonitor monitor) throws CoreException {
    return  !mavenProjectFacade.getMojoExecutions(MavenProcessorAptProjectConfigurator.PROCESSOR_PLUGIN_GROUP_ID, 
        MavenProcessorAptProjectConfigurator.PROCESSOR_PLUGIN_ARTIFACT_ID, 
        monitor, 
        MavenProcessorAptProjectConfigurator.GOAL_PROCESS).isEmpty();
  }

}
