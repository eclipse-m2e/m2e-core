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

import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.MavenImages;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.index.NexusIndex;
import org.eclipse.m2e.core.internal.index.NexusIndexManager;

/**
 * LocalRepositoryNode
 *
 * @author dyocum
 */
public class LocalRepositoryRootNode implements IMavenRepositoryNode{

  public Object[] getChildren() {
    NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
    NexusIndex localIndex = indexManager.getLocalIndex();
    NexusIndex workspaceIndex = indexManager.getWorkspaceIndex();
    return new Object[]{
        new LocalRepositoryNode(localIndex), 
        new WorkspaceRepositoryNode(workspaceIndex)
      };
  }

  public String getName() {
    return Messages.LocalRepositoryRootNode_name;
  }

  public boolean hasChildren() {
    return true;
  }

  public Image getImage() {
    return MavenImages.IMG_INDEXES;
  }

  public boolean isUpdating() {
    return false;
  }
  
}
