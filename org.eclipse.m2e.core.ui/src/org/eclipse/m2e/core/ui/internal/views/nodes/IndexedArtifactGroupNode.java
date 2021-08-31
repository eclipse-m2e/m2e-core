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

package org.eclipse.m2e.core.ui.internal.views.nodes;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.nexus.IndexedArtifactGroup;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndexManager;


/**
 * IndexedArtifactGroupNode
 *
 * @author dyocum
 */
public class IndexedArtifactGroupNode implements IMavenRepositoryNode, IArtifactNode {

  private final IndexedArtifactGroup indexedArtifactGroup;

  private Object[] kids = null;

  public IndexedArtifactGroupNode(IndexedArtifactGroup group) {
    this.indexedArtifactGroup = group;
  }

  @Override
  public Object[] getChildren() {
    NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getIndexManager();

    IndexedArtifactGroup resolvedGroup = indexManager.resolveGroup(indexedArtifactGroup);
    //IndexedArtifactGroup resolvedGroup = indexedArtifactGroup;
    ArrayList<Object> results = new ArrayList<>();
    Collection<IndexedArtifactGroup> groups = resolvedGroup.getNodes().values();
    for(IndexedArtifactGroup group : groups) {
      IndexedArtifactGroupNode node = new IndexedArtifactGroupNode(group);
      results.add(node);
    }

    Collection<IndexedArtifact> artifacts = resolvedGroup.getFiles().values(); // IndexedArtifact
    for(IndexedArtifact artifact : artifacts) {
      IndexedArtifactNode artifactNode = new IndexedArtifactNode(artifact);
      results.add(artifactNode);
    }
    kids = results.toArray(new Object[results.size()]);
    return kids;
  }

  @Override
  public String getName() {
    String prefix = indexedArtifactGroup.getPrefix();
    int n = prefix.lastIndexOf('.');
    return n < 0 ? prefix : prefix.substring(n + 1);
  }

  @Override
  public boolean hasChildren() {
//    if(kids == null){
//      kids = getChildren();
//    }
//    return kids != null && kids.length > 0;
    return true;
  }

  @Override
  public Image getImage() {
    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
  }

  @Override
  public String getDocumentKey() {
    return indexedArtifactGroup.getPrefix();
  }

  @Override
  public boolean isUpdating() {
    // TODO Auto-generated method isUpdating
    return false;
  }

}
