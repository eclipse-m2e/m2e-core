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

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.artifact.Artifact;

import org.codehaus.plexus.component.annotations.Component;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ILocalRepositoryListener;


/**
 * Dispatches local repository events to registered ILocalRepositoryListener's
 */
@Component(role = RepositoryListener.class, hint = EclipseRepositoryListener.ROLE_HINT)
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
