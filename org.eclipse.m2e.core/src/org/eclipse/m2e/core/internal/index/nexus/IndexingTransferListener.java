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

package org.eclipse.m2e.core.internal.index.nexus;

import java.io.File;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ILocalRepositoryListener;


public class IndexingTransferListener implements ILocalRepositoryListener {

  private final NexusIndexManager indexManager;

  public IndexingTransferListener(NexusIndexManager indexManager) {
    this.indexManager = indexManager;
  }

  @Override
  public void artifactInstalled(File repositoryBasedir, ArtifactKey baseArtifact, ArtifactKey artifact,
      File artifactFile) {
    NexusIndex localIndex = indexManager.getLocalIndex();
    if(artifactFile.getName().endsWith(".jar")) { //$NON-NLS-1$
      localIndex.addArtifact(artifactFile, artifact);
    }
  }

}
