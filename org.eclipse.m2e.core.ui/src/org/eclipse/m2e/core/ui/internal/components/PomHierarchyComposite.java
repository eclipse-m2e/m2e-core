/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.components;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.util.ParentGatherer;
import org.eclipse.m2e.core.ui.internal.util.ParentHierarchyEntry;


public class PomHierarchyComposite extends Composite implements IInputSelectionProvider {
  private static final Logger LOG = LoggerFactory.getLogger(PomHierarchyComposite.class);

  private TreeViewer pomsViewer;

  private List<ParentHierarchyEntry> hierarchy;

  public PomHierarchyComposite(Composite parent, int style) {
    super(parent, style);
    build();
  }

  private void build() {
    setLayout(new FillLayout(SWT.HORIZONTAL));
    pomsViewer = new TreeViewer(this, SWT.NULL);
    pomsViewer.setLabelProvider(new DepLabelProvider());
    pomsViewer.setContentProvider(new PomHeirarchyContentProvider());
  }

  @Override
  public void setEnabled(boolean bool) {
    pomsViewer.getTree().setEnabled(bool);
    super.setEnabled(bool);
  }

  public void computeHeirarchy(final IMavenProjectFacade project, IRunnableContext context) {
    try {
      if(context == null) {
        context = PlatformUI.getWorkbench().getProgressService();
      }
      context.run(false, true, monitor -> {
        try {
          computeHeirarchy(project, monitor);
        } catch(CoreException e) {
          throw new InvocationTargetException(e);
        }
      });
    } catch(Exception e) {
      LOG.error("An error occurred building pom heirarchy", e); //$NON-NLS-1$
    }
  }

  void computeHeirarchy(IMavenProjectFacade projectFacade, IProgressMonitor monitor) throws CoreException {
    LinkedList<ParentHierarchyEntry> hierarchy = new LinkedList<>();
    hierarchy.addAll(new ParentGatherer(projectFacade).getParentHierarchy(monitor));
    setHierarchy(hierarchy);
  }

  public void setHierarchy(List<ParentHierarchyEntry> hierarchy) {
    this.hierarchy = hierarchy;
    pomsViewer.setInput(hierarchy);
    pomsViewer.expandAll();
  }

  public static class DepLabelProvider extends LabelProvider implements IColorProvider {
    @Override
    public String getText(Object element) {
      ParentHierarchyEntry project = null;
      if(element instanceof ParentHierarchyEntry) {
        project = (ParentHierarchyEntry) element;
      } else if(element instanceof Object[]) {
        project = (ParentHierarchyEntry) ((Object[]) element)[0];
      } else {
        return ""; //$NON-NLS-1$
      }
      StringBuilder buffer = new StringBuilder();
      Model model = project.getProject().getModel();
      buffer.append(model.getGroupId()).append(" : ") //$NON-NLS-1$
          .append(model.getArtifactId()).append(" : ") //$NON-NLS-1$
          .append(model.getVersion());
      return buffer.toString();
    }

    @Override
    public Color getForeground(Object element) {
      if(element instanceof ParentHierarchyEntry) {
        ParentHierarchyEntry project = (ParentHierarchyEntry) element;
        if(project.getFacade() == null) {
          // This project is not in the workspace
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    @Override
    public Color getBackground(Object element) {
      return null;
    }

    @Override
    public Image getImage(Object element) {
      if(element instanceof ParentHierarchyEntry) {
        ParentHierarchyEntry project = (ParentHierarchyEntry) element;
        if(project.getFacade() == null) {
          // This project is not in the workspace
          return MavenImages.getOverlayImage(MavenImages.PATH_JAR, MavenImages.PATH_LOCK, IDecoration.BOTTOM_LEFT);
        }
        return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
      }
      return null;
    }
  }

  public static class PomHeirarchyContentProvider implements ITreeContentProvider {
    private List<ParentHierarchyEntry> projects;

    public PomHeirarchyContentProvider() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      if(newInput instanceof List) {
        this.projects = (List<ParentHierarchyEntry>) newInput;
      }
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean hasChildren(Object element) {
      Object[] children = getChildren(element);

      return children.length != 0;
    }

    @Override
    public Object getParent(Object element) {
      if(element instanceof ParentHierarchyEntry) {
        for(int i = 1; i < projects.size(); i++ ) {
          if(projects.get(i) == element) {
            return projects.get(i - 1);
          }
        }
      }
      return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement) {
      if(inputElement instanceof List) {
        List<ParentHierarchyEntry> projects = (List<ParentHierarchyEntry>) inputElement;
        if(projects.isEmpty()) {
          return new Object[0];
        }
        return new Object[] {projects.get(projects.size() - 1)};
      }
      return new Object[0];
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if(parentElement instanceof ParentHierarchyEntry) {
        /*
         * Walk the hierarchy list until we find the parentElement and
         * return the previous element, which is the child.
         */
        ParentHierarchyEntry parent = (ParentHierarchyEntry) parentElement;

        if(projects.size() == 1) {
          // No parent exists, only one element in the tree
          return new Object[0];
        }

        if(projects.get(0).equals(parent)) {
          // We are the final child
          return new Object[0];
        }

        ListIterator<ParentHierarchyEntry> iter = projects.listIterator();
        while(iter.hasNext()) {
          ParentHierarchyEntry next = iter.next();
          if(next.equals(parent)) {
            iter.previous();
            ParentHierarchyEntry previous = iter.previous();
            return new Object[] {previous};
          }
        }
      }
      return new Object[0];
    }
  }

  @Override
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    pomsViewer.addSelectionChangedListener(listener);
  }

  @Override
  public Object getInput() {
    return pomsViewer.getInput();
  }

  @Override
  public ISelection getSelection() {
    return pomsViewer.getSelection();
  }

  @Override
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    pomsViewer.removeSelectionChangedListener(listener);
  }

  @Override
  public void setSelection(ISelection selection) {
    pomsViewer.setSelection(selection);
  }

  public List<ParentHierarchyEntry> getHierarchy() {
    return hierarchy;
  }

  public ParentHierarchyEntry fromSelection() {
    ISelection selection = pomsViewer.getSelection();
    if(selection instanceof IStructuredSelection) {
      Object obj = ((IStructuredSelection) selection).getFirstElement();
      if(obj instanceof ParentHierarchyEntry) {
        return (ParentHierarchyEntry) obj;
      }
    }
    return null;
  }

  public ParentHierarchyEntry getProject() {
    return hierarchy.get(0);
  }
}
