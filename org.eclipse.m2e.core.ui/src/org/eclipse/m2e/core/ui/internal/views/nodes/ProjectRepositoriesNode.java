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

import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndex;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndexManager;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Parent node for all artifact repositories configured in pom.xml files.
 */
public class ProjectRepositoriesNode implements IMavenRepositoryNode {

  private final NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getIndexManager();

  private final IRepositoryRegistry repositoryRegistry = MavenPlugin.getRepositoryRegistry();

  @Override
  public Object[] getChildren() {
    ArrayList<Object> nodes = new ArrayList<>();
    for(IRepository repo : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_PROJECT)) {
      NexusIndex index = indexManager.getIndex(repo);
      RepositoryNode node = new RepositoryNode(index);
      nodes.add(node);
    }
    return nodes.toArray(new Object[nodes.size()]);
  }

  @Override
  public Image getImage() {
    return MavenImages.IMG_INDEXES;
  }

  @Override
  public String getName() {
    return Messages.ProjectRepositoriesNode_name;
  }

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
  public boolean isUpdating() {
    return false;
  }

}
