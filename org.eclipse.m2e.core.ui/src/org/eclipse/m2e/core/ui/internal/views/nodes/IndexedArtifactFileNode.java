/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  private IndexedArtifactFile artifactFile;

  public IndexedArtifactFileNode(IndexedArtifactFile artifactFile) {
    this.artifactFile = artifactFile;
  }

  public IndexedArtifactFile getIndexedArtifactFile() {
    return this.artifactFile;
  }

  public Object[] getChildren() {
    return null;
  }

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

  public boolean hasChildren() {
    return false;
  }

  public Image getImage() {
    if(artifactFile.sourcesExists == IIndex.PRESENT) {
      return MavenImages.IMG_VERSION_SRC;
    }
    return MavenImages.IMG_VERSION;

  }

  public String getDocumentKey() {
    return NexusIndexManager.getDocumentKey(artifactFile.getArtifactKey());
  }

  public boolean isUpdating() {
    return false;
  }

  @SuppressWarnings("rawtypes")
  public static class AdapterFactory implements IAdapterFactory {

    private static final Class[] ADAPTERS = new Class[] {ArtifactKey.class, IndexedArtifactFile.class};

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

    public Class<?>[] getAdapterList() {
      return ADAPTERS;
    }

  }
}
