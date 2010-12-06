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

package org.eclipse.m2e.core.project.configurator;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecution;


public final class PluginExecutionFilter {

  private final String groupId;

  private final String artifactId;

  private final String versionRange;

  private final Set<String> goals;

  private final VersionRange parsedVersionRange;

  public PluginExecutionFilter(String groupId, String artifactId, String versionRange, String goals) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.versionRange = versionRange;
    this.goals = new LinkedHashSet<String>(Arrays.asList(goals.split(","))); //$NON-NLS-1$;
    try {
      this.parsedVersionRange = VersionRange.createFromVersionSpec(versionRange);
    } catch(InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException("Can't parse version range", e);
    }
  }

  public String getVersionRange() {
    return this.versionRange;
  }

  public String getGroupId() {
    return this.groupId;
  }

  public String getArtifactId() {
    return this.artifactId;
  }

  public Set<String> getGoals() {
    return this.goals;
  }

  /**
   * @return true if mojoExecution matches this key or false otherwise
   */
  public boolean match(MojoExecution mojoExecution) {
    if(!groupId.equals(mojoExecution.getGroupId()) || !artifactId.equals(mojoExecution.getArtifactId())) {
      return false;
    }

    DefaultArtifactVersion version = new DefaultArtifactVersion(mojoExecution.getVersion());

    if(!parsedVersionRange.containsVersion(version)) {
      return false;
    }

    return goals.contains(mojoExecution.getGoal());
  }

}
