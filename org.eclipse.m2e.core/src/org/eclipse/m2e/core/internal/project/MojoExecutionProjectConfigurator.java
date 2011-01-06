/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionFilter;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * MojoExecutionProjectConfigurator
 * 
 * @author igor
 */
public class MojoExecutionProjectConfigurator extends AbstractProjectConfigurator {
  private String groupId;

  private String artifactId;

  private final boolean runOnIncremental;

  public MojoExecutionProjectConfigurator(PluginExecutionFilter pluginExecutionFilter, boolean runOnIncremental) {
    this.runOnIncremental = runOnIncremental;
    this.groupId = pluginExecutionFilter.getGroupId();
    this.artifactId = pluginExecutionFilter.getArtifactId();

    addPluginExecutionFilter(pluginExecutionFilter);
  }

  protected MojoExecutionProjectConfigurator(String groupId, String artifactId, String versionRange, String goals,
      boolean runOnIncremental) {
    this.runOnIncremental = runOnIncremental;
    this.groupId = groupId;
    this.artifactId = artifactId;

    addPluginExecutionFilter(groupId, artifactId, versionRange, goals);
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    // do nothing
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(MojoExecution execution) {
    return new MojoExecutionBuildParticipant(execution, runOnIncremental);
  }

  public static MojoExecutionProjectConfigurator fromString(String str, boolean runOnIncremental) {
    if(str == null || str.trim().length() <= 0) {
      return null;
    }

    int p, c;

    p = 0;
    c = nextColonIndex(str, p);
    String groupId = substring(str, p, c);

    p = c + 1;
    c = nextColonIndex(str, p);
    String artifactId = substring(str, p, c);

    p = c + 1;
    c = nextColonIndex(str, p);
    String versionRange = substring(str, p, c);

    p = c + 1;
    String goals = substring(str, p, str.length());

    return new MojoExecutionProjectConfigurator(groupId, artifactId, versionRange, goals, runOnIncremental);
  }

  private static String substring(String str, int start, int end) {
    String substring = str.substring(start, end);
    return "".equals(substring) ? null : substring; //$NON-NLS-1$
  }

  private static int nextColonIndex(String str, int pos) {
    int idx = str.indexOf(':', pos);
    if(idx < 0) {
      throw new IllegalArgumentException("Invalid mojo execution template: " + str);
    }
    return idx;
  }

  @Override
  public String getName() {
    return "execute";
  }

  @Override
  public boolean equals(Object obj) {
    if(!super.equals(obj)) {
      return false;
    }

    MojoExecutionProjectConfigurator otherConfigurator = (MojoExecutionProjectConfigurator) obj;
    return (groupId.equals(otherConfigurator.groupId) && artifactId.equals(otherConfigurator.artifactId) && runOnIncremental == otherConfigurator.runOnIncremental);
  }

  @Override
  public void addPluginExecutionFilter(PluginExecutionFilter filter) {
    if(!groupId.equals(filter.getGroupId()) || !artifactId.equals(filter.getArtifactId())) {
      throw new RuntimeException("Invalid filter for " + toString() + ": " + filter.toString());
    }
    super.addPluginExecutionFilter(filter);
  }

  @Override
  public String toString() {
    return "MojoExecutionProjectConfigurator(GroupId=" + groupId + ",ArtifactId=" + artifactId + ",runOnIncremental="
        + runOnIncremental + ")";
  }
}
