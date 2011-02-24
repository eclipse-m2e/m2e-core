/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.internal.markers;

import java.io.File;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class SourceLocationHelper {
  private static final String SELF = ""; //$NON-NLS-1$

  private static final String PROJECT = "project"; //$NON-NLS-1$

  private static final String PACKAGING = "packaging"; //$NON-NLS-1$

  private static final String PLUGIN = "plugin"; //$NON-NLS-1$

  private static final String PARENT = "parent"; //$NON-NLS-1$

  public static final String CONFIGURATION = "configuration"; //$NON-NLS-1$

  public static final String VERSION = "version"; //$NON-NLS-1$

  public static final String EXECUTION = "execution"; //$NON-NLS-1$

  private static final int COLUMN_START_OFFSET = 2;

  private static final int COLUMN_END_OFFSET = 1;

  public static SourceLocation findPackagingLocation(MavenProject mavenProject) {
    InputLocation inputLocation = mavenProject.getModel().getLocation(PACKAGING);
    if(inputLocation != null) {
      return new SourceLocation(inputLocation.getLineNumber(), inputLocation.getColumnNumber() - PACKAGING.length()
          - COLUMN_START_OFFSET, inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
    }
    inputLocation = mavenProject.getModel().getLocation(SELF);
    return new SourceLocation(inputLocation.getLineNumber(), inputLocation.getColumnNumber() - PROJECT.length()
        - COLUMN_START_OFFSET, inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
  }

  public static SourceLocation findLocation(Plugin plugin, String attribute) {
    InputLocation inputLocation = plugin.getLocation(attribute);
    if(inputLocation != null) {
      return new SourceLocation(inputLocation.getSource().getLocation(), inputLocation.getSource().getModelId(),
          inputLocation.getLineNumber(), inputLocation.getColumnNumber() - attribute.length() - COLUMN_START_OFFSET,
          inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
    }
    inputLocation = plugin.getLocation(SELF);
    return new SourceLocation(inputLocation.getSource().getLocation(), inputLocation.getSource().getModelId(),
        inputLocation.getLineNumber(), inputLocation.getColumnNumber() - PLUGIN.length() - COLUMN_START_OFFSET,
        inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
  }

  public static SourceLocation findLocation(MavenProject mavenProject, MojoExecutionKey mojoExecutionKey) {
    Plugin plugin = mavenProject.getPlugin(mojoExecutionKey.getGroupId() + ":" + mojoExecutionKey.getArtifactId());

    InputLocation inputLocation = plugin.getLocation(SELF);
    if(inputLocation == null) {
      // Plugin is specified in the maven lifecycle definition, not explicit in current pom or parent pom
      inputLocation = mavenProject.getModel().getLocation(PACKAGING);
      if(inputLocation != null) {
        return new SourceLocation(inputLocation.getLineNumber(), inputLocation.getColumnNumber() - PACKAGING.length()
            - COLUMN_START_OFFSET, inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
      }
      inputLocation = mavenProject.getModel().getLocation(SELF);
      return new SourceLocation(inputLocation.getLineNumber(), inputLocation.getColumnNumber() - PROJECT.length()
          - COLUMN_START_OFFSET, inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
    }

    String elementName;
    InputLocation executionInputLocation = findExecutionLocation(plugin, mojoExecutionKey.getExecutionId());
    if(executionInputLocation != null) {
      inputLocation = executionInputLocation;
      elementName = EXECUTION;
    } else {
      elementName = PLUGIN;
    }

    File pomFile = mavenProject.getFile();
    if(pomFile.getAbsolutePath().equals(inputLocation.getSource().getLocation())) {
      // Plugin/execution is specified in current pom
      return new SourceLocation(inputLocation.getLineNumber(), inputLocation.getColumnNumber() - elementName.length()
          - COLUMN_START_OFFSET, inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
    }

    // Plugin/execution is specified in some parent pom
    SourceLocation causeLocation = new SourceLocation(inputLocation.getSource().getLocation(), inputLocation
        .getSource().getModelId(), inputLocation.getLineNumber(), inputLocation.getColumnNumber()
        - elementName.length() - COLUMN_START_OFFSET, inputLocation.getColumnNumber() - COLUMN_END_OFFSET);
    inputLocation = mavenProject.getModel().getParent().getLocation(SELF);
    return new SourceLocation(inputLocation.getLineNumber(), inputLocation.getColumnNumber() - PARENT.length()
        - COLUMN_START_OFFSET, inputLocation.getColumnNumber() - COLUMN_END_OFFSET, causeLocation);
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
}
