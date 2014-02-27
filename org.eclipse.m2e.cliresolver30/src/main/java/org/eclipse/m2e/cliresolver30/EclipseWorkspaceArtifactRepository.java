/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.cliresolver30;

import java.util.Properties;

import org.codehaus.plexus.component.annotations.Component;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.LocalArtifactRepository;

import org.eclipse.m2e.cli.WorkspaceState;


/**
 * Enables workspace resolution in Maven 3.0-beta-2 and below.
 */
@Component(role = LocalArtifactRepository.class, hint = LocalArtifactRepository.IDE_WORKSPACE)
public final class EclipseWorkspaceArtifactRepository extends LocalArtifactRepository {

  protected boolean resolveAsEclipseProject(Artifact artifact) {
    Properties state = WorkspaceState.getState();

    if(state == null) {
      return false;
    }

    if(artifact == null) {
      // according to the DefaultArtifactResolver source code, it looks
      // like artifact can be null
      return false;
    }

    return WorkspaceState.resolveArtifact(artifact);
  }

  public Artifact find(Artifact artifact) {
    resolveAsEclipseProject(artifact);
    return artifact;
  }

  public boolean hasLocalMetadata() {
    return false; // XXX
  }

  @Override
  public int hashCode() {
    return 0; // no state
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof EclipseWorkspaceArtifactRepository;
  }
}
