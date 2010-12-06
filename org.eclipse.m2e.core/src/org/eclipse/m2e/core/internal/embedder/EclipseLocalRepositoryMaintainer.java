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

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;

import org.codehaus.plexus.component.annotations.Component;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.LocalRepositoryEvent;
import org.sonatype.aether.impl.LocalRepositoryMaintainer;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ILocalRepositoryListener;

/**
 * EclipseLocalRepositoryMaintainer
 *
 * @author igor
 */
@Component(role = LocalRepositoryMaintainer.class)
public class EclipseLocalRepositoryMaintainer implements LocalRepositoryMaintainer {

  public static final String ROLE_HINT = EclipseLocalRepositoryMaintainer.class.getName();

  public void artifactDownloaded(LocalRepositoryEvent event) {
    notifyListeners(event);
  }

  public void artifactInstalled(LocalRepositoryEvent event) {
    notifyListeners(event);
  }

  private void notifyListeners(LocalRepositoryEvent event) {
    MavenImpl maven = (MavenImpl) MavenPlugin.getDefault().getMaven();

    File basedir = event.getRepository().getBasedir();
    Artifact artifact = event.getArtifact();
    ArtifactKey key = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
        artifact.getClassifier());
    for(ILocalRepositoryListener listener : maven.getLocalRepositoryListeners()) {
      listener.artifactInstalled(basedir, key, event.getFile());
    }
  }

}
