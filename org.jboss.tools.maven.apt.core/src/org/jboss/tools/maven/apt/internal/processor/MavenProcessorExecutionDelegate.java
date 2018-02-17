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

import static org.jboss.tools.maven.apt.internal.utils.ProjectUtils.containsAptProcessors;
import static org.jboss.tools.maven.apt.internal.utils.ProjectUtils.filterToResolvedJars;
import static org.jboss.tools.maven.apt.internal.utils.ProjectUtils.getProjectArtifacts;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;

import org.jboss.tools.maven.apt.internal.AnnotationProcessorConfiguration;
import org.jboss.tools.maven.apt.internal.utils.ProjectUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;


/**
 * MavenProcessorExecutionDelegate
 *
 * @author Fred Bricon
 */
public class MavenProcessorExecutionDelegate extends MavenProcessorJdtAptDelegate {

  @Override
  public void configureProject(IProgressMonitor monitor) throws CoreException {
    //Disable JDT Apt
    IProject eclipseProject = mavenFacade.getProject();

    ProjectUtils.disableApt(eclipseProject);

    // In case the Javaconfigurator was not called yet (eg. maven-processor-plugin being bound to process-sources,
    // that project configurator runs first) We need to add the Java Nature before setting the APT config.
    if(!eclipseProject.hasNature(JavaCore.NATURE_ID)) {
      AbstractProjectConfigurator.addNature(eclipseProject, JavaCore.NATURE_ID, monitor);
    }

    AnnotationProcessorConfiguration configuration = getAnnotationProcessorConfiguration(monitor);

    File generatedSourcesDirectory = configuration.getOutputDirectory();

    // If this project has no valid generatedSourcesDirectory, we have nothing to do
    if(generatedSourcesDirectory == null) {
      return;
    }

    //The plugin dependencies are added first to the classpath
    LinkedHashSet<File> resolvedJarArtifacts = new LinkedHashSet<>(configuration.getDependencies());
    // Get the project's dependencies
    if(configuration.isAddProjectDependencies()) {
      List<Artifact> artifacts = getProjectArtifacts(mavenFacade);
      resolvedJarArtifacts.addAll(filterToResolvedJars(artifacts));
    }

    // Inspect the dependencies to see if any contain APT processors
    boolean isAnnotationProcessingEnabled = configuration.isAnnotationProcessingEnabled()
        && containsAptProcessors(resolvedJarArtifacts);

    if(isAnnotationProcessingEnabled) {
      //Make sure the output folder exists so it can be added to the classpath
      if(!generatedSourcesDirectory.exists()) {
        generatedSourcesDirectory.mkdirs();
      }

      File generatedTestSourcesDirectory = configuration.getTestOutputDirectory();
      if((generatedTestSourcesDirectory != null) && !generatedTestSourcesDirectory.exists()) {
        generatedTestSourcesDirectory.mkdirs();
      }
    }

  }

  @Override
  public AbstractBuildParticipant getMojoExecutionBuildParticipant(MojoExecution execution) {
    return new MavenProcessorBuildParticipant(execution);
  }

}
