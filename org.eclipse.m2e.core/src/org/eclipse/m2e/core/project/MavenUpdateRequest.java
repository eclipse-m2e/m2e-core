/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.project;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.internal.IMavenConstants;


/**
 * Maven project update request
 *
 * @author Eugene Kuleshov
 */
public class MavenUpdateRequest {

  /**
   * Put Maven repository system in offline mode. Same effect as -o mvn command line parameter.
   */
  private boolean offline = false;

  /**
   * Forces a check for updated releases and snapshots on remote repositories. Same effect as -U mvn command line
   * parameter.
   */
  private boolean forceDependencyUpdate = false;

  /**
   * Set of {@link IFile}
   */
  private final Set<IFile> pomFiles = new LinkedHashSet<>();

  public MavenUpdateRequest(boolean offline, boolean forceDependencyUpdate) {
    this.offline = offline;
    this.forceDependencyUpdate = forceDependencyUpdate;
  }

  public MavenUpdateRequest(IProject project, boolean offline, boolean updateSnapshots) {
    this(offline, updateSnapshots);
    addPomFile(project);
  }

  public MavenUpdateRequest(IProject[] projects, boolean offline, boolean updateSnapshots) {
    this(offline, updateSnapshots);

    for(IProject project : projects) {
      addPomFile(project);
    }
  }

  public boolean isOffline() {
    return this.offline;
  }

  public boolean isForceDependencyUpdate() {
    return this.forceDependencyUpdate;
  }

  public void addPomFiles(Set<IFile> pomFiles) {
    for(IFile pomFile : pomFiles) {
      addPomFile(pomFile);
    }
  }

  public void addPomFile(IFile pomFile) {
    pomFiles.add(pomFile);
  }

  public void addPomFile(IProject project) {
    pomFiles.add(project.getFile(IMavenConstants.POM_FILE_NAME));
  }

  public void removePomFile(IFile pomFile) {
    pomFiles.remove(pomFile);
  }

  /**
   * Returns Set of {@link IFile}
   */
  public Set<IFile> getPomFiles() {
    return this.pomFiles;
  }

  public boolean isEmpty() {
    return this.pomFiles.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("["); //$NON-NLS-1$
    String sep = ""; //$NON-NLS-1$
    for(IFile pomFile : pomFiles) {
      sb.append(sep);
      sb.append(pomFile.getFullPath());
      sep = ", "; //$NON-NLS-1$
    }
    sb.append("]"); //$NON-NLS-1$

    if(offline) {
      sb.append(" offline"); //$NON-NLS-1$
    }
    if(forceDependencyUpdate) {
      sb.append(" forceDependencyUpdate"); //$NON-NLS-1$
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object object) {
    if(this == object) {
      return true;
    }

    if(!(object instanceof MavenUpdateRequest)) {
      return false;
    }

    MavenUpdateRequest request = (MavenUpdateRequest) object;

    return this.offline == request.offline //
        && this.forceDependencyUpdate == request.forceDependencyUpdate //
        && this.pomFiles.equals(request.pomFiles);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = result * 31 + (this.offline ? 1 : 0);
    result = result * 31 + (this.forceDependencyUpdate ? 1 : 0);
    result = result * 31 + this.pomFiles.hashCode();
    return result;
  }

}
