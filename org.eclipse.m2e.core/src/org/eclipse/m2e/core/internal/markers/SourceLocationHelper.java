/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.markers;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class SourceLocationHelper {
  private SourceLocationHelper() { // static use only
  }

  private static final String SELF = ""; //$NON-NLS-1$

  private static final String PROJECT = "project"; //$NON-NLS-1$

  private static final String PACKAGING = "packaging"; //$NON-NLS-1$

  private static final String PLUGIN = "plugin"; //$NON-NLS-1$

  private static final String PARENT = "parent"; //$NON-NLS-1$

  public static final String CONFIGURATION = "configuration"; //$NON-NLS-1$

  public static final String VERSION = "version"; //$NON-NLS-1$

  public static final String EXECUTION = "execution"; //$NON-NLS-1$

  public static final String DEPENDENCY = "dependency"; //$NON-NLS-1$

  private static final int COLUMN_START_OFFSET = 2;

  private static final int COLUMN_END_OFFSET = 1;

  public static SourceLocation findPackagingLocation(MavenProject mavenProject) {
    InputLocation inputLocation = mavenProject.getModel().getLocation(PACKAGING);
    if(inputLocation != null) {
      return getSourceLocation(inputLocation, PACKAGING);
    }
    inputLocation = mavenProject.getModel().getLocation(SELF);
    return getSourceLocation(inputLocation, PROJECT);
  }

  public static SourceLocation findLocation(Plugin plugin, String attribute) {
    InputLocation inputLocation = plugin.getLocation(attribute);
    if(inputLocation != null) {
      return getSourceLocation(inputLocation, attribute);
    }
    inputLocation = plugin.getLocation(SELF);
    return getSourceLocation(inputLocation, PLUGIN);
  }

  public static SourceLocation findLocation(IResource pomResource, ModelProblem modelProblem) {
    int lineNumber = Math.max(1, modelProblem.getLineNumber());
    if(pomResource == null) {
      return new SourceLocation(lineNumber, 1, 1);
    }

    int columnNumber = Math.max(1, modelProblem.getColumnNumber());
    IPath pomLocation = pomResource.getLocation();
    if(pomLocation != null && pomLocation.toOSString().equals(modelProblem.getSource())) {
      return new SourceLocation(lineNumber, 1, columnNumber - COLUMN_END_OFFSET);
    }
    SourceLocation causeLocation = new SourceLocation(modelProblem.getSource(), modelProblem.getModelId(), lineNumber,
        1, columnNumber - COLUMN_END_OFFSET);
    return new SourceLocation(1, 1, 1, causeLocation);
  }

  public static SourceLocation findLocation(MavenProject mavenProject, MojoExecutionKey mojoExecutionKey) {
    Plugin plugin = mavenProject.getPlugin(mojoExecutionKey.groupId() + ":" + mojoExecutionKey.artifactId());

    InputLocation inputLocation = plugin != null ? plugin.getLocation(SELF) : null;
    if(inputLocation == null || inputLocation.getLineNumber() < 0) {
      // Plugin is specified in the maven lifecycle definition, not explicit in current pom or parent pom
      inputLocation = mavenProject.getModel().getLocation(PACKAGING);
      if(inputLocation != null) {
        return getSourceLocation(inputLocation, PACKAGING);
      }
      inputLocation = mavenProject.getModel().getLocation(SELF);
      return getSourceLocation(inputLocation, PROJECT);
    }

    String elementName;
    InputLocation executionInputLocation = findExecutionLocation(plugin, mojoExecutionKey.executionId());
    if(executionInputLocation != null && executionInputLocation.getLineNumber() >= 0) {
      inputLocation = executionInputLocation;
      elementName = EXECUTION;
    } else {
      elementName = PLUGIN;
    }

    File pomFile = mavenProject.getFile();
    if(pomFile.getAbsolutePath().equals(inputLocation.getSource().getLocation())) {
      // Plugin/execution is specified in current pom
      return getSourceLocation(inputLocation, elementName);
    }

    // Plugin/execution is specified in some parent pom
    SourceLocation causeLocation = getSourceLocation(inputLocation, elementName);
    inputLocation = mavenProject.getModel().getParent().getLocation(SELF);
    if(inputLocation == null) {
      // parent location cannot be determined for participant-added parents
      return new SourceLocation(1, 1, 1, causeLocation);
    }
    return getSourceLocation(inputLocation, PARENT, causeLocation);
  }

  private static InputLocation findExecutionLocation(Plugin plugin, String executionId) {
    if(executionId == null) {
      return null;
    }

    PluginExecution pluginExecution = plugin.getExecutionsAsMap().get(executionId);
    if(pluginExecution == null) {
      return null;
    }

    return pluginExecution.getLocation(SELF);
  }

  private static org.apache.maven.model.Dependency getMavenDependency(MavenProject mavenProject,
      Dependency dependency) {
    org.apache.maven.model.Dependency found = findDependency(mavenProject.getDependencies(), dependency);
    if(found == null) {
      DependencyManagement depMgmt = mavenProject.getModel().getDependencyManagement();
      if(depMgmt != null) {
        found = findDependency(depMgmt.getDependencies(), dependency);

        if(found != null) {

          // missing transitive managed dependency
          String projectId = mavenProject.getModel().getLocation(SELF).getSource().getModelId();
          String depId = found.getLocation(SELF).getSource().getModelId();

          if(!projectId.equals(depId)) {
            // let's see if it comes from a directly imported pom
            DependencyManagement origMgmt = mavenProject.getOriginalModel().getDependencyManagement();
            org.apache.maven.model.Dependency importDep = findDependencyImport(origMgmt, depId);
            // use it to show marker on
            found = importDep;
          }
        }
      }
    }
    return found;
  }

  private static org.apache.maven.model.Dependency findDependencyImport(DependencyManagement origMgmt, String depId) {
    if(origMgmt != null) {
      for(org.apache.maven.model.Dependency dependency : origMgmt.getDependencies()) {

        if("import".equals(dependency.getScope()) && "pom".equals(dependency.getType())) {
          String importId = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();
          if(depId.equals(importId)) {
            return dependency;
          }
        }
      }
    }
    return null;
  }

  private static org.apache.maven.model.Dependency findDependency(List<org.apache.maven.model.Dependency> dependencies,
      Dependency dependency) {
    for(org.apache.maven.model.Dependency mavenDependency : dependencies) {
      Artifact dependencyArtifact = dependency.getArtifact();
      if(mavenDependency.getArtifactId().equals(dependencyArtifact.getArtifactId())
          && mavenDependency.getGroupId().equals(dependencyArtifact.getGroupId())
          && eq(mavenDependency.getVersion(), dependencyArtifact.getVersion())
          && eq(mavenDependency.getClassifier(), dependencyArtifact.getClassifier())) {
        return mavenDependency;
      }
    }
    return null;
  }

  private static boolean eq(String s1, String s2) {
    if(s1 != null && s1.isBlank()) {
      s1 = null;
    }
    if(s2 != null && s2.isBlank()) {
      s2 = null;
    }
    return Objects.equals(s1, s2);
  }

  public static SourceLocation findLocation(MavenProject mavenProject, Dependency dependency) {
    org.apache.maven.model.Dependency mavenDependency = getMavenDependency(mavenProject, dependency);
    return findLocation(mavenProject, mavenDependency);
  }

  public static SourceLocation findLocation(MavenProject mavenProject, org.apache.maven.model.Dependency dependency) {
    InputLocation inputLocation = null;
    if(dependency != null) {
      inputLocation = dependency.getLocation(SELF);
    }
    if(inputLocation == null) {
      // Should never happen
      inputLocation = mavenProject.getModel().getLocation(SELF);
      return new SourceLocation(inputLocation.getLineNumber(), 1, inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
    }

    File pomFile = mavenProject.getFile();
    if(pomFile.getAbsolutePath().equals(inputLocation.getSource().getLocation())) {
      // Dependency is specified in current pom
      return getSourceLocation(inputLocation, DEPENDENCY);
    }

    // Plugin/execution is specified in some parent pom
    SourceLocation causeLocation = getSourceLocation(inputLocation, DEPENDENCY);
    inputLocation = mavenProject.getModel().getParent().getLocation(SELF);
    return getSourceLocation(inputLocation, PARENT, causeLocation);
  }

  private static SourceLocation getSourceLocation(InputLocation inputLocation, String elementName) {
    InputSource source = inputLocation.getSource();
    return new SourceLocation( //
        source != null ? source.getLocation() : null, source != null ? source.getModelId() : null,
        inputLocation.getLineNumber(), //  
        inputLocation.getColumnNumber() - elementName.length() - COLUMN_START_OFFSET,
        inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
  }

  private static SourceLocation getSourceLocation(InputLocation inputLocation, String elementName,
      SourceLocation causeLocation) {
    return new SourceLocation(inputLocation.getLineNumber(),
        inputLocation.getColumnNumber() - elementName.length() - COLUMN_START_OFFSET,
        inputLocation.getColumnNumber() - COLUMN_END_OFFSET, causeLocation);
  }
}
