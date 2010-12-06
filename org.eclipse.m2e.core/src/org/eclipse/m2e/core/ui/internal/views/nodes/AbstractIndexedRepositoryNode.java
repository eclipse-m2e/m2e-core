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

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.MavenImages;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.index.IndexedArtifactGroup;
import org.eclipse.m2e.core.internal.index.NexusIndex;


/**
 * AbstractIndexedRepository
 * 
 * @author igor
 */
public abstract class AbstractIndexedRepositoryNode implements IMavenRepositoryNode {

  protected static final Object[] NO_CHILDREN = new Object[0];

  protected final NexusIndex index;
  
  protected AbstractIndexedRepositoryNode(NexusIndex index) {
    this.index = index;
  }

  public Object[] getChildren() {

    if(index == null) {
      return NO_CHILDREN;
    }

    try {
      IndexedArtifactGroup[] rootGroups = index.getRootIndexedArtifactGroups();
      if(rootGroups == null) {
        return NO_CHILDREN;
      }
      IndexedArtifactGroupNode[] children = new IndexedArtifactGroupNode[rootGroups.length];
      Arrays.sort(rootGroups);
      for(int i = 0; i < rootGroups.length; i++ ) {
        children[i] = new IndexedArtifactGroupNode(rootGroups[i]);
      }
      return children;
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      return NO_CHILDREN;
    }
  }

  public Image getImage() {
    return MavenImages.IMG_INDEX; 
  }

  public boolean hasChildren() {
    return index != null;
  }

  public boolean isUpdating() {
    return index != null && index.isUpdating();
  }

  public NexusIndex getIndex() {
    return index;
  }

  public String getRepositoryUrl() {
    return index.getRepositoryUrl();
  }

  public boolean isEnabledIndex() {
    return index != null && index.isEnabled();
  }
}
