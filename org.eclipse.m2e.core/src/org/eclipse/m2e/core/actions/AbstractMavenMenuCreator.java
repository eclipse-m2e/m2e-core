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

package org.eclipse.m2e.core.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

import org.eclipse.m2e.core.MavenPlugin;


/**
 * Abstract Maven menu creator can be used to contribute custom entries to the Maven popup menu.
 * <p>
 * Custom items can be added to one of the standard groups {@link #NEW}, {@link #OPEN}, {@link #UPDATE}, {@link #NATURE}
 * or {@link #IMPORT}.
 * 
 * @see org.eclipse.m2e.m2menu extension point
 * 
 * @author Eugene Kuleshov
 */
public abstract class AbstractMavenMenuCreator {
  public static final String NEW = "new"; //$NON-NLS-1$
  public static final String OPEN = "open"; //$NON-NLS-1$
  public static final String UPDATE = "update"; //$NON-NLS-1$
  public static final String NATURE = "nature"; //$NON-NLS-1$
  public static final String IMPORT = "import"; //$NON-NLS-1$
  
  protected IStructuredSelection selection;

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }

  /**
   * Creates menu items in given menu manager.
   */
  public abstract void createMenu(IMenuManager mgr);

  /**
   * A helper method to create IAction instance from given IActionDelegate. 
   */
  protected IAction getAction(IActionDelegate delegate, String id, String text) {
    return getAction(delegate, id, text, (ImageDescriptor) null);
  }
  
  /**
   * A helper method to create IAction instance from given IActionDelegate. 
   */
  protected IAction getAction(IActionDelegate delegate, String id, String text, String image) {
    return getAction(delegate, id, text, MavenPlugin.getImageDescriptor(image));
  }

  protected IAction getAction(IActionDelegate delegate, String id, String text, ImageDescriptor image) {
    ActionProxy action = new ActionProxy(id, text, delegate);
    if(image!=null) {
      action.setImageDescriptor(image);
    }
    return action;
  }

  class ActionProxy extends Action {
    private IActionDelegate action;
    
    public ActionProxy(String id, String text, IActionDelegate action) {
      super(text);
      this.action = action;
      setId(id);
    }
    
    public ActionProxy(String id, String text, IActionDelegate action, int style) {
      super(text, style);
      this.action = action;
      setId(id);
    }
    
    public void run() {
      action.selectionChanged(this, selection);
      action.run(this);
    }
  }
  
}

