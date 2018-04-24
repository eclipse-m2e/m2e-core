/*******************************************************************************
 * Copyright (c) 2018 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.Map;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.progress.DeferredTreeContentManager;

import org.eclipse.m2e.core.search.ISearchProvider;
import org.eclipse.m2e.core.search.ISearchResultGA;


/**
 * DeferredSelectionComponentProvider
 *
 * @author Matthew Piggott
 */
public class DeferredSearchResultTreeContentProvider implements ITreeContentProvider {

  private DeferredTreeContentManager manager;

  private ISearchProvider provider;

  public void setSearchProvider(ISearchProvider provider) {
    this.provider = provider;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
   */
  @Override
  public Object[] getElements(Object inputElement) {
    return ((Map<?, ?>) inputElement).values().toArray();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
   */
  @Override
  public Object[] getChildren(Object parentElement) {
    if(manager != null && provider != null && parentElement instanceof ISearchResultGA) {
      Object[] children = manager.getChildren(parentElement);
      if(children != null) {
        return children;
      }
    }
    return new Object[0];
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
   */
  @Override
  public Object getParent(Object element) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
   */
  @Override
  public boolean hasChildren(Object element) {
    return (manager != null && provider != null && element instanceof ISearchResultGA);
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if(viewer instanceof AbstractTreeViewer) {
      manager = new DeferredTreeContentManager((AbstractTreeViewer) viewer);
    }
  }
}
