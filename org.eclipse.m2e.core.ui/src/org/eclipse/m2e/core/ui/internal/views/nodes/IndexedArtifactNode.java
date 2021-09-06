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
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * IndexedArtifactNode
 *
 * @author dyocum
 */
@SuppressWarnings("restriction")
public class IndexedArtifactNode implements IMavenRepositoryNode, IArtifactNode {

  private final IndexedArtifact artifact;

  private Object[] kids = null;

  public IndexedArtifactNode(IndexedArtifact artifact) {
    this.artifact = artifact;
  }

  @Override
  public Object[] getChildren() {
    Set<IndexedArtifactFile> files = artifact.getFiles();
    if(files == null) {
      return new Object[0];
    }
    ArrayList<Object> fileList = new ArrayList<>();
    for(IndexedArtifactFile iaf : files) {
      fileList.add(new IndexedArtifactFileNode(iaf));
    }
    kids = fileList.toArray(new IndexedArtifactFileNode[fileList.size()]);
    return kids;
  }

  @Override
  public String getName() {
    // return a.group + ":" + a.artifact;
    String pkg = artifact.getPackaging();
    if(pkg == null) {
      pkg = Messages.IndexedArtifactNode_no_pack;
    }
    return artifact.getArtifactId() + " - " + pkg; //$NON-NLS-1$
  }

  @Override
  public boolean hasChildren() {
    //return kids != null && kids.length > 0;
    return true;
  }

  @Override
  public Image getImage() {
    return MavenImages.IMG_JAR;
  }

  @Override
  public String getDocumentKey() {
    return artifact.getArtifactId();
  }

  @Override
  public boolean isUpdating() {
    return false;
  }

}
