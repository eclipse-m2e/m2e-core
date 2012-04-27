/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.maven.apt.internal.compiler;

import static org.jboss.tools.maven.apt.internal.utils.ProjectUtils.parseProcessorOptions;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.jboss.tools.maven.apt.internal.AbstractAptConfiguratorDelegate;
import org.jboss.tools.maven.apt.internal.AnnotationProcessorConfiguration;
import org.jboss.tools.maven.apt.internal.DefaultAnnotationProcessorConfiguration;
import org.jboss.tools.maven.apt.internal.processor.MavenProcessorJdtAptDelegate;
import org.jboss.tools.maven.apt.internal.utils.PluginDependencyResolver;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

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
   * Ignore this configurator if maven-processor-plugin is also active.
   */
  public boolean isIgnored(IProgressMonitor monitor) {
    try {
      return  !mavenFacade.getMojoExecutions(MavenProcessorJdtAptDelegate.PROCESSOR_PLUGIN_GROUP_ID, 
                                             MavenProcessorJdtAptDelegate.PROCESSOR_PLUGIN_ARTIFACT_ID, 
                                             monitor, 
                                             MavenProcessorJdtAptDelegate.GOAL_PROCESS)
                                             .isEmpty();
    } catch(CoreException ex) {
      //Ohoh!
      ex.printStackTrace();
    }
    return true;
  }

  @Override
  protected AnnotationProcessorConfiguration getAnnotationProcessorConfiguration(IProgressMonitor monitor) throws CoreException {
      
       for(MojoExecution mojoExecution : mavenFacade.getMojoExecutions(COMPILER_PLUGIN_GROUP_ID,
           COMPILER_PLUGIN_ARTIFACT_ID, monitor, GOAL_COMPILE)) {
         File generatedOutputDirectory  = maven.getMojoParameterValue(mavenSession, mojoExecution, OUTPUT_DIRECTORY_PARAMETER, File.class);
         String compilerArgument  = maven.getMojoParameterValue(mavenSession, mojoExecution, "compilerArgument", String.class);
         Map<String, String> options = parseProcessorOptions(compilerArgument);
         
         boolean isAnnotationProcessingEnabled = compilerArgument == null || !compilerArgument.contains("-proc:none");  
         if (isAnnotationProcessingEnabled ) {
           String proc = maven.getMojoParameterValue(mavenSession, mojoExecution, "proc", String.class);
           isAnnotationProcessingEnabled = !"none".equals(proc); 
         }

         PluginDependencyResolver dependencyResolver = new PluginDependencyResolver();
         List<File> dependencies = dependencyResolver.getResolvedPluginDependencies(mavenSession, 
                                                                                    mavenFacade.getMavenProject(), 
                                                                                    mojoExecution.getPlugin(), 
                                                                                    monitor);
         
         DefaultAnnotationProcessorConfiguration configuration = new DefaultAnnotationProcessorConfiguration();
         configuration.setOutputDirectory(generatedOutputDirectory);
         configuration.setAnnotationProcessingEnabled(isAnnotationProcessingEnabled);
         configuration.setDependencies(dependencies);
         configuration.setAnnotationProcessorOptions(options);
         return configuration;
       }
       
       return null;
    }

}
