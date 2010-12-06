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

package org.eclipse.m2e.core.ui.internal.search;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.ui.IMemento;

/**
 * Maven search result page
 *
 * @author Eugene Kuleshov
 */
public class MavenSearchResultPage extends AbstractTextSearchViewPage {

  public MavenSearchResultPage() {
    super(FLAG_LAYOUT_TREE | FLAG_LAYOUT_FLAT);
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTableViewer(org.eclipse.jface.viewers.TableViewer)
   */
  protected void configureTableViewer(TableViewer viewer) {
    // TODO Auto-generated method configureTableViewer
    
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#configureTreeViewer(org.eclipse.jface.viewers.TreeViewer)
   */
  protected void configureTreeViewer(TreeViewer viewer) {
    // TODO Auto-generated method configureTreeViewer
    
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#clear()
   */
  protected void clear() {
    // TODO Auto-generated method clear
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#elementsChanged(java.lang.Object[])
   */
  protected void elementsChanged(Object[] objects) {
    // TODO Auto-generated method elementsChanged
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#restoreState(org.eclipse.ui.IMemento)
   */
  public void restoreState(IMemento memento) {
    super.restoreState(memento);

    // TODO Auto-generated method restoreState
    
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#saveState(org.eclipse.ui.IMemento)
   */
  public void saveState(IMemento memento) {
    super.saveState(memento);
    // TODO Auto-generated method saveState
  }
  
}
