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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.index.IndexListener;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.internal.index.IndexedArtifactGroup;
import org.eclipse.m2e.core.internal.index.NexusIndex;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;
import org.eclipse.m2e.core.ui.internal.views.nodes.AbstractIndexedRepositoryNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.IArtifactNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.IndexedArtifactFileNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.LocalRepositoryNode;
import org.eclipse.m2e.core.ui.internal.views.nodes.RepositoryNode;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;


/**
 * Maven repository view
 * 
 * @author dyocum
 */
public class MavenRepositoryView extends ViewPart {
  private static final String ENABLE_FULL = Messages.MavenRepositoryView_enable_full;
  private static final String ENABLED_FULL = Messages.MavenRepositoryView_enabled_full;
  private static final String DISABLE_DETAILS = Messages.MavenRepositoryView_disable_details;
  private static final String DISABLED_DETAILS = Messages.MavenRepositoryView_details_disabled;
  private static final String ENABLE_MIN = Messages.MavenRepositoryView_enable_minimum;
  private static final String ENABLED_MIN = Messages.MavenRepositoryView_minimum_enabled;
  
  private IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();

  private IAction collapseAllAction;
  
  private IAction reloadSettings;
  
  BaseSelectionListenerAction openPomAction;

  private BaseSelectionListenerAction updateAction;
  
  private BaseSelectionListenerAction rebuildAction;
  
  private DisableIndexAction disableAction;
  private EnableMinIndexAction enableMinAction;
  private EnableFullIndexAction enableFullAction;

  private BaseSelectionListenerAction copyUrlAction;
  
  //private BaseSelectionListenerAction materializeProjectAction;
  
  TreeViewer viewer;
  private RepositoryViewContentProvider contentProvider;

  private DrillDownAdapter drillDownAdapter;

  private IndexListener indexListener;

  public void setFocus() {
    viewer.getControl().setFocus();
  }
  
  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    contentProvider = new RepositoryViewContentProvider();
    viewer.setContentProvider(contentProvider);
    viewer.setLabelProvider(new RepositoryViewLabelProvider(viewer.getTree().getFont()));
    
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        
      }
    });
    viewer.setInput(getViewSite());
    drillDownAdapter = new DrillDownAdapter(viewer);

    makeActions();
    hookContextMenu();

    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        openPomAction.run();
      }
    });

    contributeToActionBars();
    this.indexListener = new IndexListener() {

      public void indexAdded(IRepository repository) {
        refreshView();
      }

      public void indexChanged(IRepository repository) {
        refreshView();
      }

      public void indexRemoved(IRepository repository) {
        refreshView();
      }

      public void indexUpdating(IRepository repository){
        Display.getDefault().asyncExec(new Runnable(){
          public void run(){
           viewer.refresh(true); 
          }
        });
      }
    };
    
    indexManager.addIndexListener(this.indexListener);
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        MavenRepositoryView.this.fillContextMenu(manager);
      }
    });

    Menu menu = menuMgr.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);
    getSite().registerContextMenu(menuMgr, viewer);
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

  protected List<AbstractIndexedRepositoryNode> getSelectedRepositoryNodes(List elements){
    ArrayList<AbstractIndexedRepositoryNode> list = new ArrayList<AbstractIndexedRepositoryNode>();
    if (elements != null) {
      for(int i=0;i<elements.size();i++){
        Object elem = elements.get(i);
        if(elem instanceof AbstractIndexedRepositoryNode) {
          list.add((AbstractIndexedRepositoryNode)elem);
        }
      }
    }
    return list;
  }
  protected List<IArtifactNode> getArtifactNodes(List elements){
    if(elements == null || elements.size() == 0){
      return null;
    }
    ArrayList<IArtifactNode> list = new ArrayList<IArtifactNode>();
    for(int i=0;i<elements.size();i++){
      Object elem = elements.get(i);
      if(elem instanceof IArtifactNode){
        IArtifactNode node = (IArtifactNode)elem;
        list.add(node);
      }
    }
    return list;
  }
  void fillContextMenu(IMenuManager manager) {
    manager.add(openPomAction);
    manager.add(copyUrlAction);
//    manager.add(materializeProjectAction);
    manager.add(new Separator());
    manager.add(updateAction);
    manager.add(rebuildAction);
    manager.add(new Separator());
    manager.add(disableAction);
    manager.add(enableMinAction);
    manager.add(enableFullAction);
//    manager.add(deleteFromLocalAction);
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
      public void run() {
        viewer.collapseAll();
      }
    };
    collapseAllAction.setToolTipText(Messages.MavenRepositoryView_btnCollapse_tooltip);
    collapseAllAction.setImageDescriptor(MavenImages.COLLAPSE_ALL);
    reloadSettings = new Action(Messages.MavenRepositoryView_action_reload){
      public void run(){
        String msg = Messages.MavenRepositoryView_reload_msg;
        boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
            Messages.MavenRepositoryView_reload_title, msg);
        if(res){
          Job job = new WorkspaceJob(Messages.MavenRepositoryView_job_reloading) {
            public IStatus runInWorkspace(IProgressMonitor monitor) {
              try {
                MavenPlugin.getDefault().getMaven().reloadSettings();
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
//    deleteFromLocalAction = new BaseSelectionListenerAction("Delete from Repository") {
//      public void run() {
//        List<IArtifactNode> nodes = getArtifactNodes(getStructuredSelection().toList());
//        if(nodes != null){
//          for(IArtifactNode node : nodes){
//            String key = node.getDocumentKey();
//            System.out.println("key: "+key);
//            ((NexusIndexManager)MavenPlugin.getDefault().getIndexManager()).removeDocument("local", null, key);
//          }
//        }
//      }
//
//      protected boolean updateSelection(IStructuredSelection selection) {
//        List<IArtifactNode> nodes = getArtifactNodes(getStructuredSelection().toList());
//        return (nodes != null && nodes.size() > 0);
//      }
//    };
//    deleteFromLocalAction.setToolTipText("Delete the selected GAV from the local repository");
    //updateAction.setImageDescriptor(MavenImages.UPD_INDEX);

    
    updateAction = new BaseSelectionListenerAction(Messages.MavenRepositoryView_action_update) {
      public void run() {
        List<AbstractIndexedRepositoryNode> nodes = getSelectedRepositoryNodes(getStructuredSelection().toList());
        for(AbstractIndexedRepositoryNode node : nodes) {
          if (node instanceof RepositoryNode) {
            ((RepositoryNode) node).getIndex().scheduleIndexUpdate(false);
          }
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        int indexCount = 0;
        for (AbstractIndexedRepositoryNode node : getSelectedRepositoryNodes(selection.toList())) {
          if (node instanceof RepositoryNode && node.isEnabledIndex()) {
            indexCount ++;
          }
        }
        if(indexCount > 1){
          setText(Messages.MavenRepositoryView_update_more);
        } else {
          setText(Messages.MavenRepositoryView_update_one);
        }
        return indexCount > 0;
      }
    };
    updateAction.setToolTipText(Messages.MavenRepositoryView_btnUpdate_tooltip);
    updateAction.setImageDescriptor(MavenImages.UPD_INDEX);

    rebuildAction = new BaseSelectionListenerAction(Messages.MavenRepositoryView_action_rebuild) {
      public void run() {
        List<AbstractIndexedRepositoryNode> nodes = getSelectedRepositoryNodes(getStructuredSelection().toList());
        if(nodes.size() > 0){
          if(nodes.size() == 1){
            NexusIndex index = nodes.get(0).getIndex();
            if (index != null) {
              String repositoryUrl = index.getRepositoryUrl();
              String msg = NLS.bind(Messages.MavenRepositoryView_rebuild_msg, repositoryUrl);
              boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
                  Messages.MavenRepositoryView_rebuild_title, msg);
              if(res) {
                index.scheduleIndexUpdate(true);
              }
            }
          } else {
            String msg = Messages.MavenRepositoryView_rebuild_msg2;
            boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
                Messages.MavenRepositoryView_rebuild_title2, msg);
            if(res) {
              for(AbstractIndexedRepositoryNode node : nodes){
                NexusIndex index = node.getIndex();
                if (index != null) {
                  index.scheduleIndexUpdate(true);
                }
              }
            }            
          }
        }
      }
      
      protected boolean updateSelection(IStructuredSelection selection) {
        int indexCount = 0;
        for (AbstractIndexedRepositoryNode node : getSelectedRepositoryNodes(selection.toList())) {
          if ((node instanceof LocalRepositoryNode) || node.isEnabledIndex()) {
            indexCount ++;
          }
        }
        if(indexCount > 1){
          setText(Messages.MavenRepositoryView_rebuild_many);
        } else {
          setText(Messages.MavenRepositoryView_rebuild_one);
        }
        return indexCount > 0;
      }
    };
    
    rebuildAction.setToolTipText(Messages.MavenRepositoryView_action_rebuild_tooltip);
    rebuildAction.setImageDescriptor(MavenImages.REBUILD_INDEX);

    disableAction = new DisableIndexAction();

    disableAction.setToolTipText(Messages.MavenRepositoryView_action_disable_tooltip);
    disableAction.setImageDescriptor(MavenImages.REBUILD_INDEX);

    enableMinAction = new EnableMinIndexAction();
    enableMinAction.setToolTipText(Messages.MavenRepositoryView_action_enable_tooltip);
    enableMinAction.setImageDescriptor(MavenImages.REBUILD_INDEX);

    enableFullAction = new EnableFullIndexAction();
    enableFullAction.setToolTipText(Messages.MavenRepositoryView_action_enableFull_tooltip);
    enableFullAction.setImageDescriptor(MavenImages.REBUILD_INDEX);

    openPomAction = new BaseSelectionListenerAction(Messages.MavenRepositoryView_action_open) {
      public void run() {
        ISelection selection = viewer.getSelection();
        Object element = ((IStructuredSelection) selection).getFirstElement();
        if(element instanceof IndexedArtifactFileNode) {
          IndexedArtifactFile f = ((IndexedArtifactFileNode) element).getIndexedArtifactFile();
          OpenPomAction.openEditor(f.group, f.artifact, f.version, null);
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        return selection.getFirstElement() instanceof IndexedArtifactFile;
      }
    };
    openPomAction.setToolTipText(Messages.MavenRepositoryView_action_open_tooltip);
    openPomAction.setImageDescriptor(MavenImages.POM);

    copyUrlAction = new BaseSelectionListenerAction(Messages.MavenRepositoryView_action_copy) {
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        String url = null;
        if(element instanceof RepositoryNode) {
          url = ((RepositoryNode) element).getRepositoryUrl();
        } else if(element instanceof IndexedArtifactGroup) {
          IndexedArtifactGroup group = (IndexedArtifactGroup) element;
          String repositoryUrl = group.getRepository().getUrl();
          if(!repositoryUrl.endsWith("/")) { //$NON-NLS-1$
            repositoryUrl += "/"; //$NON-NLS-1$
          }
          url = repositoryUrl + group.getPrefix().replace('.', '/');
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

      protected boolean updateSelection(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        return element instanceof RepositoryNode;
      }
    };
    copyUrlAction.setToolTipText(Messages.MavenRepositoryView_action_copy_tooltip);
    copyUrlAction.setImageDescriptor(MavenImages.COPY);
    
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
    viewer.addSelectionChangedListener(updateAction);
    viewer.addSelectionChangedListener(disableAction);
    viewer.addSelectionChangedListener(enableMinAction);
    viewer.addSelectionChangedListener(enableFullAction);
    viewer.addSelectionChangedListener(rebuildAction);
    viewer.addSelectionChangedListener(copyUrlAction);
//    viewer.addSelectionChangedListener(materializeProjectAction);
  }

  protected void setIndexDetails(AbstractIndexedRepositoryNode node, String details) {
    if (node != null && node.getIndex() != null) {
      try {
        node.getIndex().setIndexDetails(details);
      } catch(CoreException ex) {
        M2EUIUtils.showErrorDialog(this.getViewSite().getShell(), Messages.MavenRepositoryView_error_title, Messages.MavenRepositoryView_error_message, ex);
      }
    }
  }

  protected AbstractIndexedRepositoryNode getSelectedRepositoryNode(IStructuredSelection selection) {
    List elements = selection.toList();
    if (elements.size() != 1) {
      return null;
    }
    Object element = elements.get(0);
    return element instanceof AbstractIndexedRepositoryNode? (AbstractIndexedRepositoryNode) element: null;
  }

  public void dispose() {
//    viewer.removeSelectionChangedListener(materializeProjectAction);
    viewer.removeSelectionChangedListener(copyUrlAction);
    viewer.removeSelectionChangedListener(rebuildAction);
    viewer.removeSelectionChangedListener(disableAction);
    viewer.removeSelectionChangedListener(enableMinAction);
    viewer.removeSelectionChangedListener(enableFullAction);
    viewer.removeSelectionChangedListener(updateAction);
    viewer.removeSelectionChangedListener(openPomAction);
    indexManager.removeIndexListener(this.indexListener);
    super.dispose();
  }

  void refreshView() {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        Object[] expandedElems = viewer.getExpandedElements();
        if (!viewer.getControl().isDisposed()) {
          viewer.setInput(getViewSite());
          if(expandedElems != null && expandedElems.length > 0){
            viewer.setExpandedElements(expandedElems);
          }
        }
      }
    });
  };

  /**
   * Base Selection Listener does not allow the style (radio button/check) to be set.
   * This base class listens to selections and sets the appropriate index value
   * depending on its value
   * AbstractIndexAction
   *
   * @author dyocum
   */
  abstract class AbstractIndexAction extends Action implements ISelectionChangedListener{

    protected abstract String getDetailsValue();
    protected abstract String getActionText();
    
    public AbstractIndexAction(String text, int style){
      super(text, style);
    }
    
    public void run() {
      IStructuredSelection sel = (IStructuredSelection)viewer.getSelection();
      setIndexDetails(getSelectedRepositoryNode(sel), getDetailsValue());
    }
    
    /* 
     */
    public void selectionChanged(SelectionChangedEvent event) {
      IStructuredSelection sel = (IStructuredSelection)event.getSelection();
      updateSelection(sel);
    }
    
    protected void updateSelection(IStructuredSelection selection) {    
      AbstractIndexedRepositoryNode node = getSelectedRepositoryNode(selection);
      updateIndexDetails(node);
      setText(getActionText());
      boolean enabled = (node != null && node instanceof RepositoryNode);
      this.setEnabled(enabled);
    }
    
    protected void updateIndexDetails(AbstractIndexedRepositoryNode node){
      if(node == null || node.getIndex() == null){
        return;
      }
      NexusIndex index = node.getIndex();
      setChecked(getDetailsValue().equals(index.getIndexDetails()));
    }
    
  }
  
  class DisableIndexAction extends AbstractIndexAction {
    public DisableIndexAction(){
      super(DISABLE_DETAILS, IAction.AS_CHECK_BOX);
    }
    
    protected String getDetailsValue(){
      return NexusIndex.DETAILS_DISABLED;
    }
    protected String getActionText(){
      return isChecked() ? DISABLED_DETAILS : DISABLE_DETAILS;
    }
  }
  
  class EnableMinIndexAction extends AbstractIndexAction {
    public EnableMinIndexAction(){
      super(ENABLE_MIN, IAction.AS_CHECK_BOX);
    }
    
    protected String getDetailsValue(){
      return NexusIndex.DETAILS_MIN;
    }
    protected String getActionText(){
      return isChecked() ? ENABLED_MIN : ENABLE_MIN;
    }
  }

  class EnableFullIndexAction extends AbstractIndexAction {
    public EnableFullIndexAction(){
      super(ENABLE_FULL, IAction.AS_CHECK_BOX);
    }
    
    protected String getDetailsValue(){
      return NexusIndex.DETAILS_FULL;
    }
    protected String getActionText(){
      return isChecked() ? ENABLED_FULL : ENABLE_FULL;
    }
  }
}
