/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.cliresolver31;

import java.io.File;
import java.util.List;

import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;

import org.codehaus.plexus.component.annotations.Component;

import org.eclipse.m2e.cli.WorkspaceState;


/**
 * Enables workspace resolution in Maven 3.1.0 and newer.
 */
@Component(role = WorkspaceReader.class, hint = "ide")
public final class EclipseWorkspaceReader implements WorkspaceReader {

  private WorkspaceRepository workspaceRepository;

  public EclipseWorkspaceReader() {
    this.workspaceRepository = new WorkspaceRepository("ide", getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode(); // no state
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof EclipseWorkspaceReader;
  }

  public WorkspaceRepository getRepository() {
    return workspaceRepository;
  }

  public File findArtifact(org.eclipse.aether.artifact.Artifact artifact) {
    return WorkspaceState.findArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(),
        artifact.getClassifier(), artifact.getBaseVersion());
  }

  public List<String> findVersions(org.eclipse.aether.artifact.Artifact artifact) {
    return WorkspaceState.findVersions(artifact.getGroupId(), artifact.getArtifactId());
  }

}
