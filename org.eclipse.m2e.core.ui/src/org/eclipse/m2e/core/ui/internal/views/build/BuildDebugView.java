/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.views.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.m2e.core.internal.builder.BuildDebugHook;
import org.eclipse.m2e.core.internal.builder.MavenBuilder;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


@SuppressWarnings("restriction")
public class BuildDebugView extends ViewPart implements BuildDebugHook {

  /*package*/static final Comparator<Node> NODE_COMPARATOR = (p1, p2) -> {
    int d = p2.getBuildCount() - p1.getBuildCount();
    if(d != 0) {
      return d;
    }
    return p1.getName().compareTo(p2.getName());
  };

  /*package*/TreeViewer viewer;

  /*package*/final Object projectsLock = new Object() {
  };

  /*package*/final Map<String, ProjectNode> projects = new ConcurrentHashMap<>();

  /*package*/final Job refreshJob = new Job("") {
    @Override
    protected IStatus run(IProgressMonitor monitor) {
      getSite().getShell().getDisplay().asyncExec(() -> viewer.refresh());
      return Status.OK_STATUS;
    }
  };

  /*package*/volatile boolean suspended = true;

  @Override
  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    Tree tree = viewer.getTree();
    tree.setHeaderVisible(true);
    tree.setLinesVisible(true);

    TreeViewerColumn treeViewerColumn = new TreeViewerColumn(viewer, SWT.NONE);
    TreeColumn trclmnName = treeViewerColumn.getColumn();
    trclmnName.setWidth(400);
    trclmnName.setText(Messages.BuildDebugView_columnName);

    TreeViewerColumn treeViewerColumn_1 = new TreeViewerColumn(viewer, SWT.NONE);
    TreeColumn trclmnBuildCount = treeViewerColumn_1.getColumn();
    trclmnBuildCount.setWidth(100);
    trclmnBuildCount.setText(Messages.BuildDebugView_columnBuildNumber);
    viewer.setLabelProvider(new ITableLabelProvider() {

      @Override
      public void removeListener(ILabelProviderListener listener) {
      }

      @Override
      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      @Override
      public void dispose() {
      }

      @Override
      public void addListener(ILabelProviderListener listener) {
      }

      @Override
      public String getColumnText(Object element, int columnIndex) {
        if(element instanceof Node) {
          return getColumnText((Node) element, columnIndex);
        }

        if(columnIndex == 0) {
          return element.toString();
        }

        return null;
      }

      private String getColumnText(Node element, int columnIndex) {
        switch(columnIndex) {
          case 0:
            return element.getName();
          case 1:
            return Integer.toString(element.getBuildCount());
          default:
            // fall through
        }
        return null;
      }

      @Override
      public Image getColumnImage(Object element, int columnIndex) {
        return null;
      }
    });

    viewer.setContentProvider(new ITreeContentProvider() {

      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      @Override
      public void dispose() {
      }

      @Override
      public boolean hasChildren(Object element) {
        if(element instanceof ContainerNode) {
          return !((ContainerNode) element).getResources().isEmpty();
        }
        if(element instanceof CollectionNode<?>) {
          return !((CollectionNode<?>) element).getMembers().isEmpty();
        }
        return false;
      }

      @Override
      public Object getParent(Object element) {
        return null;
      }

      @Override
      public Object[] getElements(Object inputElement) {
        if(inputElement == projects) {
          List<ProjectNode> sorted;
          synchronized(projectsLock) {
            sorted = new ArrayList<>(projects.values());
          }
          Collections.sort(sorted, NODE_COMPARATOR);
          return sorted.toArray();
        }
        return new Object[0];
      }

      @Override
      public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof ProjectNode) {
          ArrayList<Object> result = new ArrayList<>();

          final ProjectNode projectNode = (ProjectNode) parentElement;

          final List<ResourceNode> resources = new ArrayList<>(projectNode.getResources());
          if(!resources.isEmpty()) {
            Collections.sort(resources, NODE_COMPARATOR);
            result.add(new CollectionNode<>(Messages.BuildDebugView_nodeDelta, resources));
          }

          final List<MojoExecutionNode> executions = new ArrayList<>(projectNode.getMojoExecutions());
          if(!executions.isEmpty()) {
            Collections.sort(executions, NODE_COMPARATOR);
            result.add(new CollectionNode<>(Messages.BuildDebugView_nodeExecutions, executions));
          }

          return result.toArray();
        } else if(parentElement instanceof CollectionNode<?>) {
          return ((CollectionNode<?>) parentElement).getMembers().toArray();
        } else if(parentElement instanceof ContainerNode) {
          return ((ContainerNode) parentElement).getResources().toArray();
        }
        return null;
      }
    });

    viewer.setInput(projects);

    IActionBars actionBars = getViewSite().getActionBars();
    IToolBarManager toolBar = actionBars.getToolBarManager();
    Action suspendAction = new Action(Messages.BuildDebugView_actionSuspend, IAction.AS_CHECK_BOX) {
      @Override
      public void run() {
        suspended = isChecked();
      }
    };
    suspendAction.setImageDescriptor(MavenImages.SUSPEND);
    suspendAction.setChecked(suspended);
    Action clearAction = new Action(Messages.BuildDebugView_actionClear, MavenImages.CLEAR) {
      @Override
      public void run() {
        synchronized(projectsLock) {
          projects.clear();
        }
        refreshJob.schedule();
      }
    };
    Action collapseAll = new Action(Messages.BuildDebugView_actionCollapseAll, MavenImages.COLLAPSEALL) {
      @Override
      public void run() {
        viewer.collapseAll();
      }
    };
    toolBar.add(collapseAll);
    toolBar.add(clearAction);
    toolBar.add(suspendAction);
    actionBars.updateActionBars();
  }

  @Override
  public void setFocus() {
  }

  @Override
  public void init(IViewSite site) throws PartInitException {
    super.init(site);
    MavenBuilder.addDebugHook(this);
  }

  @Override
  public void dispose() {
    MavenBuilder.removeDebugHook(this);
    super.dispose();
  }

  @Override
  public void buildStart(IMavenProjectFacade projectFacade, int kind, Map<String, String> args,
      Map<MojoExecutionKey, List<AbstractBuildParticipant>> participants, IResourceDelta delta, IProgressMonitor monitor) {

    if(suspended) {
      return;
    }

    final ProjectNode projectNode = getProjectNode(projectFacade);

    final int buildCount = projectNode.incrementBuildCount();

    try {
      if(delta != null) {
        delta.accept(delta1 -> {
          if(delta1.getAffectedChildren().length == 0) {
            IResource resource = delta1.getResource();
            if(resource instanceof IFile || resource instanceof IFolder) {
              projectNode.addResource(resource.getProjectRelativePath()).setBuildCount(buildCount);
            }
          }
          return true; // keep visiting
        });
      }
      refreshJob.schedule(1000L);
    } catch(CoreException ex) {
      ErrorDialog.openError(getSite().getShell(), Messages.BuildDebugView_errorTitle,
          Messages.BuildDebugView_errorDescription, ex.getStatus());
    }
  }

  private ProjectNode getProjectNode(IMavenProjectFacade projectFacade) {
    synchronized(projectsLock) {
      IProject project = projectFacade.getProject();
      ProjectNode projectNode = projects.get(project.getName());
      if(projectNode == null) {
        projectNode = new ProjectNode(project.getName());
        projects.put(project.getName(), projectNode);
      }
      return projectNode;
    }
  }

  @Override
  public void buildParticipant(IMavenProjectFacade projectFacade, MojoExecutionKey mojoExecutionKey,
      AbstractBuildParticipant participant, Set<File> files, IProgressMonitor monitor) {

    if(suspended || files == null || files.isEmpty()) {
      return;
    }

    final ProjectNode projectNode = getProjectNode(projectFacade);
    final int buildCount = projectNode.getBuildCount();

    // TODO secondary participants
    // ... although they are unlikely to use BuildContext so we don't know what resources they modify
    final MojoExecutionNode executionNode = projectNode.getMojoExecutionNode(mojoExecutionKey);
    executionNode.setBuildCount(buildCount);
    for(File file : files) {
      executionNode.addResource(projectFacade.getProjectRelativePath(file.getAbsolutePath())).setBuildCount(buildCount);
    }
  }
}
