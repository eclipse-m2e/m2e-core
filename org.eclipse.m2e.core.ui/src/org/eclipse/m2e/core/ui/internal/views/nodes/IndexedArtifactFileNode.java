/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.ui.internal.views.nodes;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndexManager;
import org.eclipse.m2e.core.ui.internal.MavenImages;


/**
 * IndexedArtifactFileNode
 *
 * @author dyocum
 */
@SuppressWarnings("restriction")
public class IndexedArtifactFileNode extends PlatformObject implements IMavenRepositoryNode, IArtifactNode, IAdaptable {

  private final IndexedArtifactFile artifactFile;

  public IndexedArtifactFileNode(IndexedArtifactFile artifactFile) {
    this.artifactFile = artifactFile;
  }

  public IndexedArtifactFile getIndexedArtifactFile() {
    return this.artifactFile;
  }

  @Override
  public Object[] getChildren() {
    return null;
  }

  @Override
  public String getName() {
    String label = artifactFile.artifact;
    if(artifactFile.classifier != null) {
      label += " : " + artifactFile.classifier; //$NON-NLS-1$
    }
    if(artifactFile.version != null) {
      label += " : " + artifactFile.version; //$NON-NLS-1$
    }
    return label;
  }

  @Override
  public boolean hasChildren() {
    return false;
  }

  @Override
  public Image getImage() {
    if(artifactFile.sourcesExists == IIndex.PRESENT) {
      return MavenImages.IMG_VERSION_SRC;
    }
    return MavenImages.IMG_VERSION;

  }

  @Override
  public String getDocumentKey() {
    return NexusIndexManager.getDocumentKey(artifactFile.getArtifactKey());
  }

  @Override
  public boolean isUpdating() {
    return false;
  }

  public static class AdapterFactory implements IAdapterFactory {

    private static final Class<?>[] ADAPTERS = new Class[] {ArtifactKey.class, IndexedArtifactFile.class};

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
      if(adaptableObject instanceof IndexedArtifactFileNode) {
        IndexedArtifactFileNode node = (IndexedArtifactFileNode) adaptableObject;
        IndexedArtifactFile artifactFile = node.artifactFile;
        if(ArtifactKey.class.equals(adapterType)) {
          return adapterType.cast(new ArtifactKey(artifactFile.group, artifactFile.artifact, artifactFile.version,
              artifactFile.classifier));
        } else if(IndexedArtifactFile.class.equals(adapterType)) {
          return adapterType.cast(artifactFile);
        }
      }
      return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
      return ADAPTERS;
    }

  }
}
