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

  @Override
  public void inputChanged(Viewer v, Object oldInput, Object newInput) {
  }

  @Override
  public void dispose() {
  }

  @Override
  public Object[] getElements(Object parent) {
    return getChildren(parent);
  }

  @Override
  public Object getParent(Object child) {
    return null;
  }

  @Override
  public boolean hasChildren(Object parent) {
    if(parent instanceof IMavenRepositoryNode) {
      return ((IMavenRepositoryNode) parent).hasChildren();
    }
    return false;
  }

  public Object[] getRootNodes() {
    if(localNode == null) {
      localNode = new LocalRepositoryRootNode();

    }
    if(globalNode == null) {
      globalNode = new GlobalRepositoriesNode();
    }
    if(projectNode == null) {
      projectNode = new ProjectRepositoriesNode();
    }
    if(customNode == null) {
      customNode = new CustomRepositoriesNode();
    }
    return new Object[] {localNode, globalNode, projectNode, customNode};
  }

  @Override
  public Object[] getChildren(Object parent) {
    if(parent instanceof IViewSite) {
      return getRootNodes();
    } else if(parent instanceof IMavenRepositoryNode) {
      return ((IMavenRepositoryNode) parent).getChildren();
    }
    return new Object[0];
  }
}
