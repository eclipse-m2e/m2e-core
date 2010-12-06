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

package org.eclipse.m2e.core.ui.internal.views.nodes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.MavenImages;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.index.NexusIndex;
import org.eclipse.m2e.core.internal.index.NexusIndexManager;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;

/**
 * AbstractRepositoriesNode
 *
 * @author igor
 */
public abstract class AbstractRepositoriesNode implements IMavenRepositoryNode {

  protected final NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
  protected final IRepositoryRegistry repositoryRegistry = MavenPlugin.getDefault().getRepositoryRegistry();

  public Object[] getChildren() {

    ArrayList<Object> mirrorNodes = new ArrayList<Object>();
    ArrayList<Object> globalRepoNodes = new ArrayList<Object>();

    for (IRepository repo : getRepositories()) {
      NexusIndex index = indexManager.getIndex(repo);
      RepositoryNode node = new RepositoryNode(index);
      if (repo.getMirrorOf() != null) {
        mirrorNodes.add(node); 
      } else {
        globalRepoNodes.add(node);
      }
    }

    ArrayList<Object> nodes = new ArrayList<Object>();
    nodes.addAll(mirrorNodes);
    nodes.addAll(globalRepoNodes);

    return nodes.toArray(new Object[nodes.size()]);
  }

  protected abstract List<IRepository> getRepositories();

  public String toString() {
    return getName();
  }

  public boolean hasChildren() {
    Object[] kids = getChildren();
    return kids != null && kids.length > 0;
  }

  public Image getImage() {
    return MavenImages.IMG_INDEXES;
  }

  public boolean isUpdating() {
    return false;
  }

}
