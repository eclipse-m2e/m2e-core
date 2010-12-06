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

package org.eclipse.m2e.core.ui.internal.views;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewSite;

import org.eclipse.m2e.core.ui.internal.views.nodes.CustomRepositoriesNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.GlobalRepositoriesNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.IMavenRepositoryNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.LocalRepositoryRootNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.ProjectRepositoriesNode;

/**
 * RepositoryViewContentProvider
 *
 * @author dyocum
 */
public class RepositoryViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

  private LocalRepositoryRootNode localNode;
  private GlobalRepositoriesNode globalNode;
  private ProjectRepositoriesNode projectNode;
  private CustomRepositoriesNode customNode;
  
  public RepositoryViewContentProvider() {
  }

  public void inputChanged(Viewer v, Object oldInput, Object newInput) {
  }

  public void dispose() {
  }
  
  public Object[] getElements(Object parent) {
    return getChildren(parent);
  }

  public Object getParent(Object child) {
    return null;
  }

  public boolean hasChildren(Object parent) {
    if(parent instanceof IMavenRepositoryNode){
      return ((IMavenRepositoryNode)parent).hasChildren();
    }
    return false;
  }

  public Object[] getRootNodes(){
    if(localNode == null){
      localNode = new LocalRepositoryRootNode();
      
    }
    if(globalNode == null){
      globalNode = new GlobalRepositoriesNode();
    }
    if(projectNode == null) {
      projectNode = new ProjectRepositoriesNode();
    }
    if(customNode == null) {
      customNode = new CustomRepositoriesNode();
    }
    return new Object[]{localNode, globalNode, projectNode, customNode};
  }
  
  public Object[] getChildren(Object parent) {
    if(parent instanceof IViewSite){
      return getRootNodes();
    } else if(parent instanceof IMavenRepositoryNode){
      return ((IMavenRepositoryNode)parent).getChildren();
    }
    return new Object[0];
  }
}