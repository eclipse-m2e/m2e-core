/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.internal.IMavenConstants;


public class ArtifactNotFoundProblemInfo extends MavenProblemInfo {
  private final Artifact artifact;

  public ArtifactNotFoundProblemInfo(Artifact artifact, boolean offline, SourceLocation location) {
    super(location);

    this.artifact = artifact;

    String message = NLS.bind(org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_missing,
        artifact.toString());
    if(offline) {
      message = NLS.bind(org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_offline, message);
    }
    setMessage(message);
  }

  public Artifact getArtifact() {
    return this.artifact;
  }

  /**
   * Adds the missing artifact groupId, artifactId, version and classifier as marker attributes.
   *
   * @since 1.4.0
   */
  @Override
  public void processMarker(IMarker marker) throws CoreException {
    super.processMarker(marker);
    if(artifact != null) {
      marker.setAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, artifact.getGroupId());
      marker.setAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, artifact.getArtifactId());
      marker.setAttribute(IMavenConstants.MARKER_ATTR_VERSION, artifact.getVersion());
      marker.setAttribute(IMavenConstants.MARKER_ATTR_CLASSIFIER, artifact.getClassifier());
    }
  }
}
