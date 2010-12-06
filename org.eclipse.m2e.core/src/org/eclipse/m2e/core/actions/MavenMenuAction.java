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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.actions.DefaultMavenMenuCreator;


/**
 * Maven menu action
 * 
 * @author Eugene Kuleshov
 */
public class MavenMenuAction implements IObjectActionDelegate, IMenuCreator {

  private static final String EXTENSION_MENU_ITEMS = IMavenConstants.PLUGIN_ID + ".m2menu"; //$NON-NLS-1$

  boolean fillMenu;

  IAction delegateAction;

  List<AbstractMavenMenuCreator> creators = null;

  // IObjectActionDelegate

  public void run(IAction action) {
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      // this.selection = (IStructuredSelection) selection;
      this.fillMenu = true;

      if(delegateAction != action) {
        delegateAction = action;
        delegateAction.setMenuCreator(this);
      }

      action.setEnabled(!selection.isEmpty());

      for(AbstractMavenMenuCreator creator : getCreators()) {
        creator.selectionChanged(action, selection);
      }
    }
  }

  // IMenuCreator

  public void dispose() {
  }

  public Menu getMenu(Control parent) {
    return null;
  }

  public Menu getMenu(Menu parent) {
    Menu menu = new Menu(parent);

    /**
     * Add listener to re-populate the menu each time it is shown because MenuManager.update(boolean, boolean) doesn't
     * dispose pull-down ActionContribution items for each popup menu.
     */
    menu.addMenuListener(new MenuAdapter() {
      public void menuShown(MenuEvent e) {
        if(fillMenu) {
          Menu m = (Menu) e.widget;

          for(MenuItem item : m.getItems()) {
            item.dispose();
          }

          IMenuManager mgr = new MenuManager("#maven"); //$NON-NLS-1$
          mgr.add(new GroupMarker(AbstractMavenMenuCreator.NEW));
          mgr.insertAfter(AbstractMavenMenuCreator.NEW, new GroupMarker(AbstractMavenMenuCreator.UPDATE));
          mgr.insertAfter(AbstractMavenMenuCreator.UPDATE, new GroupMarker(AbstractMavenMenuCreator.OPEN));
          mgr.insertAfter(AbstractMavenMenuCreator.OPEN, new GroupMarker(AbstractMavenMenuCreator.NATURE));
          mgr.insertAfter(AbstractMavenMenuCreator.NATURE, new GroupMarker(AbstractMavenMenuCreator.IMPORT));
          
          for(AbstractMavenMenuCreator creator : getCreators()) {
            creator.createMenu(mgr);
          }

          for(IContributionItem item : mgr.getItems()) {
            item.fill(m, -1);
          }
          
          fillMenu = false;
        }
      }
    });

    return menu;
  }

  List<AbstractMavenMenuCreator> getCreators() {
    if(creators == null) {
      creators = new ArrayList<AbstractMavenMenuCreator>();
      creators.add(new DefaultMavenMenuCreator());

      IExtensionRegistry registry = Platform.getExtensionRegistry();
      IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_MENU_ITEMS);
      if(extensionPoint!=null) {
        for(IExtension extension : extensionPoint.getExtensions()) {
          IConfigurationElement[] elements = extension.getConfigurationElements();
          for(IConfigurationElement element : elements) {
            try {
              AbstractMavenMenuCreator creator = (AbstractMavenMenuCreator) element.createExecutableExtension("class"); //$NON-NLS-1$
              creators.add(creator);
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        }
      }
    }
    return creators;
  }

}
