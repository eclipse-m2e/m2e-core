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
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.MavenImages;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.Messages;

/**
 * IndexedArtifactNode
 *
 * @author dyocum
 */
public class IndexedArtifactNode implements IMavenRepositoryNode, IArtifactNode {

  private IndexedArtifact artifact;
  private Object[] kids = null;
  public IndexedArtifactNode(IndexedArtifact artifact){
    this.artifact = artifact;
  }
  /* (non-Javadoc)
   * @see org.eclipse.m2e.ui.internal.views.IMavenRepositoryNode#getChildren()
   */
  public Object[] getChildren() {
    Set<IndexedArtifactFile> files = artifact.getFiles();
    if(files == null){
      return new Object[0];
    }
    ArrayList<Object> fileList = new ArrayList<Object>();
    for(IndexedArtifactFile iaf : files){
      fileList.add(new IndexedArtifactFileNode(iaf));
    }
    kids = fileList.toArray(new IndexedArtifactFileNode[fileList.size()]);
    return kids;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.ui.internal.views.IMavenRepositoryNode#getName()
   */
  public String getName() {
    // return a.group + ":" + a.artifact;
    String pkg = artifact.getPackaging();
    if(pkg == null){
      pkg = Messages.IndexedArtifactNode_no_pack;
    }
    return artifact.getArtifactId() + " - " + pkg; //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.ui.internal.views.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
    //return kids != null && kids.length > 0;
    return true;
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.m2e.ui.internal.views.nodes.IMavenRepositoryNode#getImage()
   */
  public Image getImage() {
    return MavenImages.IMG_JAR;
  }
  /* (non-Javadoc)
   * @see org.eclipse.m2e.ui.internal.views.nodes.IArtifactNode#getDocumentKey()
   */
  public String getDocumentKey() {
    return artifact.getArtifactId();
  }
  /* (non-Javadoc)
   * @see org.eclipse.m2e.ui.internal.views.nodes.IMavenRepositoryNode#isUpdating()
   */
  public boolean isUpdating() {
    // TODO Auto-generated method isUpdating
    return false;
  }

}
