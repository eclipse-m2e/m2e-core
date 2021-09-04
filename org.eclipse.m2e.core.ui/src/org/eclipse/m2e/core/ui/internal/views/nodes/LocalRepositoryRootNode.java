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

import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndex;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndexManager;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * LocalRepositoryNode
 *
 * @author dyocum
 */
public class LocalRepositoryRootNode implements IMavenRepositoryNode {

  @Override
  public Object[] getChildren() {
    NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getIndexManager();
    NexusIndex localIndex = indexManager.getLocalIndex();
    NexusIndex workspaceIndex = indexManager.getWorkspaceIndex();
    return new Object[] {new LocalRepositoryNode(localIndex), new WorkspaceRepositoryNode(workspaceIndex)};
  }

  @Override
  public String getName() {
    return Messages.LocalRepositoryRootNode_name;
  }

  @Override
  public boolean hasChildren() {
    return true;
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
