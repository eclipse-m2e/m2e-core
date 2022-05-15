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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.views.nodes.IArtifactNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.IMavenRepositoryNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.RepositoryNode;


/**
 * Maven repository view
 *
 * @author dyocum
 */
public class MavenRepositoryView extends ViewPart {

  /**
   *
   */
  private static final String MENU_OPEN_GRP = "open";

  /**
   *
   */
  private static final String MENU_UPDATE_GRP = "update";

  private static final String MENU_ID = ".repositoryViewMenu"; //$NON-NLS-1$ 

  private IAction collapseAllAction;

  private IAction reloadSettings;

  BaseSelectionListenerAction openPomAction;

  private BaseSelectionListenerAction copyUrlAction;

  //private BaseSelectionListenerAction materializeProjectAction;

  TreeViewer viewer;

  private RepositoryViewContentProvider contentProvider;

  private DrillDownAdapter drillDownAdapter;

  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

  @Override
  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    contentProvider = new RepositoryViewContentProvider();
    viewer.setContentProvider(contentProvider);

    RepositoryViewLabelProvider labelProvider = new RepositoryViewLabelProvider(viewer.getTree().getFont());
    viewer.setLabelProvider(new DecoratingStyledCellLabelProvider(labelProvider,
        PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator(), null));

    viewer.addDoubleClickListener(event -> {

    });
    viewer.setInput(getViewSite());
    drillDownAdapter = new DrillDownAdapter(viewer);

    makeActions();
    hookContextMenu();

    viewer.addDoubleClickListener(event -> openPomAction.run());

    contributeToActionBars();
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu-" + MENU_ID); //$NON-NLS-1$
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(manager -> {
      MavenRepositoryView.this.fillContextMenu(manager);
      manager.update();
    });

    Menu menu = menuMgr.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);
    getSite().registerContextMenu(M2EUIPluginActivator.PLUGIN_ID + MENU_ID, menuMgr, viewer);
  }

  private void contributeToActionBars() {
    IActionBars bars = getViewSite().getActionBars();
    fillLocalPullDown(bars.getMenuManager());
    fillLocalToolBar(bars.getToolBarManager());
  }

  private void fillLocalPullDown(IMenuManager manager) {
    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(reloadSettings);
  }

  protected List<IMavenRepositoryNode> getSelectedRepositoryNodes(List<?> elements) {
    ArrayList<IMavenRepositoryNode> list = new ArrayList<>();
    if(elements != null) {
      for(Object elem : elements) {
        if(elem instanceof IMavenRepositoryNode) {
          list.add((IMavenRepositoryNode) elem);
        }
      }
    }
    return list;
  }

  protected List<IArtifactNode> getArtifactNodes(List<?> elements) {
    if(elements == null || elements.isEmpty()) {
      return null;
    }
    ArrayList<IArtifactNode> list = new ArrayList<>();
    for(Object elem : elements) {
      if(elem instanceof IArtifactNode) {
        IArtifactNode node = (IArtifactNode) elem;
        list.add(node);
      }
    }
    return list;
  }

  void fillContextMenu(IMenuManager manager) {
    manager.add(new Separator(MENU_OPEN_GRP));
    manager.add(new Separator(MENU_UPDATE_GRP));
    manager.add(new Separator("import")); //$NON-NLS-1$
    manager.prependToGroup(MENU_OPEN_GRP, copyUrlAction);
    manager.prependToGroup(MENU_OPEN_GRP, openPomAction);

    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(new Separator());
    drillDownAdapter.addNavigationActions(manager);
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void fillLocalToolBar(IToolBarManager manager) {
    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(reloadSettings);
    manager.add(new Separator());
    drillDownAdapter.addNavigationActions(manager);
  }

  private void makeActions() {
    collapseAllAction = new Action(Messages.MavenRepositoryView_btnCollapse) {
      @Override
      public void run() {
        viewer.collapseAll();
      }
    };
    collapseAllAction.setToolTipText(Messages.MavenRepositoryView_btnCollapse_tooltip);
    collapseAllAction.setImageDescriptor(
        PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));
    reloadSettings = new Action(Messages.MavenRepositoryView_action_reload) {
      @Override
      public void run() {
        String msg = Messages.MavenRepositoryView_reload_msg;
        boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
            Messages.MavenRepositoryView_reload_title, msg);
        if(res) {
          Job job = new WorkspaceJob(Messages.MavenRepositoryView_job_reloading) {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) {
              try {
                MavenPlugin.getMaven().reloadSettings();
              } catch(CoreException ex) {
                return ex.getStatus();
              }
              return Status.OK_STATUS;
            }
          };
          job.schedule();
        }
      }
    };

    reloadSettings.setImageDescriptor(MavenImages.REFRESH);

    openPomAction = new BaseSelectionListenerAction(Messages.MavenRepositoryView_action_open) {
      @Override
      public void run() {
        ISelection selection = viewer.getSelection();
        Object element = ((IStructuredSelection) selection).getFirstElement();
        if(element instanceof IArtifactNode) {
          Artifact f = ((IArtifactNode) element).getArtifact();
          OpenPomAction.openEditor(f.getGroupId(), f.getArtifactId(), f.getVersion(), null);
        }
      }

      @Override
      protected boolean updateSelection(IStructuredSelection selection) {
        return selection.getFirstElement() instanceof IArtifactNode;
      }
    };
    openPomAction.setToolTipText(Messages.MavenRepositoryView_action_open_tooltip);
    openPomAction.setImageDescriptor(MavenImages.POM);

    copyUrlAction = new BaseSelectionListenerAction(Messages.MavenRepositoryView_action_copy) {
      @Override
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        String url = null;
        if(element instanceof RepositoryNode) {
          url = ((RepositoryNode) element).getRepositoryUrl();
        } else if(element instanceof IndexedArtifact) {
          //
        } else if(element instanceof IndexedArtifactFile) {
          //
        }
        if(url != null) {
          Clipboard clipboard = new Clipboard(Display.getCurrent());
          clipboard.setContents(new String[] {url}, new Transfer[] {TextTransfer.getInstance()});
          clipboard.dispose();
        }
      }

      @Override
      protected boolean updateSelection(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        return element instanceof RepositoryNode;
      }
    };
    copyUrlAction.setToolTipText(Messages.MavenRepositoryView_action_copy_tooltip);
    copyUrlAction.setImageDescriptor(
        PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));

//    materializeProjectAction = new BaseSelectionListenerAction(Messages.MavenRepositoryView_action_materialize) {
//      public void run() {
//        Object element = getStructuredSelection().getFirstElement();
//        if(element instanceof IndexedArtifactFileNode){
//          MaterializeAction action = new MaterializeAction();
//          StructuredSelection sel = new StructuredSelection(new Object[]{((IndexedArtifactFileNode) element).getIndexedArtifactFile()});
//          action.selectionChanged(this, sel);
//          action.run(this);
//        }
//      }
//
//      protected boolean updateSelection(IStructuredSelection selection) {
//        return selection.getFirstElement() instanceof IndexedArtifactFileNode;
//      }
//    };
//    materializeProjectAction.setImageDescriptor(MavenImages.IMPORT_PROJECT);

    viewer.addSelectionChangedListener(openPomAction);
    viewer.addSelectionChangedListener(copyUrlAction);
//    viewer.addSelectionChangedListener(materializeProjectAction);
  }

  protected RepositoryNode getSelectedRepositoryNode(IStructuredSelection selection) {
    List<?> elements = selection.toList();
    if(elements.size() != 1) {
      return null;
    }
    Object element = elements.get(0);
    return element instanceof RepositoryNode ? (RepositoryNode) element : null;
  }

  @Override
  public void dispose() {
//    viewer.removeSelectionChangedListener(materializeProjectAction);
    viewer.removeSelectionChangedListener(copyUrlAction);
    viewer.removeSelectionChangedListener(openPomAction);
    super.dispose();
  }

  void refreshView() {
    Display.getDefault().asyncExec(() -> {
      Object[] expandedElems = viewer.getExpandedElements();
      if(!viewer.getControl().isDisposed()) {
        viewer.setInput(getViewSite());
        if(expandedElems != null && expandedElems.length > 0) {
          viewer.setExpandedElements(expandedElems);
        }
      }
    });
  }

}
