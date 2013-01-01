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

import org.eclipse.osgi.util.NLS;

import org.sonatype.aether.artifact.Artifact;


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
}
