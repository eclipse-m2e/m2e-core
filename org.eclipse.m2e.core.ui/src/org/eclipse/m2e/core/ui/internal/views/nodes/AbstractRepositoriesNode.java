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
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndex;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndexManager;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;
import org.eclipse.m2e.core.ui.internal.MavenImages;


/**
 * AbstractRepositoriesNode
 *
 * @author igor
 */
public abstract class AbstractRepositoriesNode implements IMavenRepositoryNode {

  protected final NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getIndexManager();

  protected final IRepositoryRegistry repositoryRegistry = MavenPlugin.getRepositoryRegistry();

  @Override
  public Object[] getChildren() {

    ArrayList<Object> mirrorNodes = new ArrayList<>();
    ArrayList<Object> globalRepoNodes = new ArrayList<>();

    for(IRepository repo : getRepositories()) {
      NexusIndex index = indexManager.getIndex(repo);
      RepositoryNode node = new RepositoryNode(index);
      if(repo.getMirrorOf() != null) {
        mirrorNodes.add(node);
      } else {
        globalRepoNodes.add(node);
      }
    }

    ArrayList<Object> nodes = new ArrayList<>();
    nodes.addAll(mirrorNodes);
    nodes.addAll(globalRepoNodes);

    return nodes.toArray(new Object[nodes.size()]);
  }

  protected abstract List<IRepository> getRepositories();

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public boolean hasChildren() {
    Object[] kids = getChildren();
    return kids != null && kids.length > 0;
  }

  @Override
  public Image getImage() {
    return MavenImages.IMG_INDEXES;
  }

  @Override
  public boolean isUpdating() {
    return false;
  }

}
