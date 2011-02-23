/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecycle.discovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;


public class ProjectLifecycleMappingConfiguration {

  private final String relpath;

  private final List<MojoExecutionMappingConfiguration> mojoExecutionConfigurations = new ArrayList<MojoExecutionMappingConfiguration>();

  private final MavenProject mavenProject;

  private final PackagingTypeMappingConfiguration configuration;

  private final List<MojoExecution> mojoExecutions;

  public ProjectLifecycleMappingConfiguration(String relpath, MavenProject project, List<MojoExecution> mojoExecutions,
      PackagingTypeMappingConfiguration configuration) {
    this.relpath = relpath;
    this.mavenProject = project;
    this.mojoExecutions = mojoExecutions;
    this.configuration = configuration;
  }

  /**
   * Project pom.xml path relative to request operation root.
   */
  public String getRelpath() {
    return relpath;
  }

  public PackagingTypeMappingConfiguration getPackagingTypeMappingConfiguration() {
    return configuration;
  }

  public List<MojoExecutionMappingConfiguration> getMojoExecutionConfigurations() {
    return mojoExecutionConfigurations;
  }

  public void addMojoExecution(MojoExecutionMappingConfiguration configuration) {
    mojoExecutionConfigurations.add(configuration);
  }

  public String getMavenText() {
    return relpath + " artifactId=" + mavenProject.getArtifactId() + " packaging=" + mavenProject.getPackaging();
  }

  public MavenProject getMavenProject() {
    return mavenProject;
  }

  public List<MojoExecution> getMojoExecutions() {
    return mojoExecutions;
  }
}
