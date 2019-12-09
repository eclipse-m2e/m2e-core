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

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;

import javax.inject.Singleton;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.artifact.Artifact;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ILocalRepositoryListener;


/**
 * Dispatches local repository events to registered ILocalRepositoryListener's
 */
@Singleton
public class EclipseRepositoryListener extends AbstractRepositoryListener implements RepositoryListener {

  public static final String ROLE_HINT = "EclipseRepositoryListener";

  public void artifactInstalled(RepositoryEvent event) {
    notifyListeners(event);
  }

  public void artifactDownloaded(RepositoryEvent event) {
    notifyListeners(event);
  }

  private void notifyListeners(RepositoryEvent event) {
    File file = event.getFile();
    if(file != null) {
      MavenImpl maven = (MavenImpl) MavenPlugin.getMaven();
      Artifact artifact = event.getArtifact();
      ArtifactKey key = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
          nes(artifact.getClassifier()));
      ArtifactKey baseKey = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(),
          nes(artifact.getClassifier()));
      File basedir = event.getSession().getLocalRepository().getBasedir();
      for(ILocalRepositoryListener listener : maven.getLocalRepositoryListeners()) {
        listener.artifactInstalled(basedir, baseKey, key, file);
      }
    }
  }

  private static String nes(String str) {
    if(str == null || str.trim().length() == 0) {
      return null;
    }
    return str.trim();
  }
}
