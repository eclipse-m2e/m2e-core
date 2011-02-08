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

package org.eclipse.m2e.core.ui.internal.lifecycle;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;

/**
 * ConfiguratorsTableContentProvider
 *
 * @author dyocum
 */
public class ProjectConfiguratorsTableContentProvider implements IStructuredContentProvider {

  public ProjectConfiguratorsTableContentProvider(){
  }
  
  protected String[] getNoConfigMsg(){
    return new String[]{Messages.ProjectConfiguratorsTableContentProvider_no_configs};
  }
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
   */
  public Object[] getElements(Object inputElement) {
    
    if(inputElement == null || !(inputElement instanceof AbstractProjectConfigurator[]) || ((AbstractProjectConfigurator[])inputElement).length == 0){
      return getNoConfigMsg();
    }
    return (AbstractProjectConfigurator[])inputElement;

  
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IContentProvider#dispose()
   */
  public void dispose() {
    // TODO Auto-generated method dispose
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // TODO Auto-generated method inputChanged
    
  }
}