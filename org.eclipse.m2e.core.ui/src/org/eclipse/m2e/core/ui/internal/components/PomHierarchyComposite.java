/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
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

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.util.ParentGatherer;


public class PomHierarchyComposite extends Composite implements IInputSelectionProvider {
  private static final Logger LOG = LoggerFactory.getLogger(PomHierarchyComposite.class);

  private TreeViewer pomsViewer;

  private List<MavenProject> hierarchy;

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

  public void setEnabled(boolean bool) {
    pomsViewer.getTree().setEnabled(bool);
    super.setEnabled(bool);
  }

  public void computeHeirarchy(final IMavenProjectFacade project, IRunnableContext context) {
    try {
      if(context != null) {
        context.run(false, true, new IRunnableWithProgress() {
          public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
              computeHeirarchy(project, monitor);
            } catch(CoreException e) {
              throw new InvocationTargetException(e);
            }
          }
        });
      } else {
        computeHeirarchy(project, new NullProgressMonitor());
      }
    } catch(Exception e) {
      LOG.error("An error occurred building pom heirarchy", e); //$NON-NLS-1$
    }
  }

  private void computeHeirarchy(IMavenProjectFacade projectFacade, IProgressMonitor monitor) throws CoreException {
    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addAll(new ParentGatherer(projectFacade.getMavenProject(), projectFacade).getParentHierarchy(monitor));
    setHierarchy(hierarchy);
  }

  public void setHierarchy(LinkedList<MavenProject> hierarchy) {
    this.hierarchy = hierarchy;
    pomsViewer.setInput(hierarchy);
    pomsViewer.expandAll();
  }

  public static class DepLabelProvider extends LabelProvider implements IColorProvider {
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText(Object element) {
      MavenProject project = null;
      if(element instanceof MavenProject) {
        project = (MavenProject) element;
      } else if(element instanceof Object[]) {
        project = (MavenProject) ((Object[]) element)[0];
      } else {
        return ""; //$NON-NLS-1$
      }
      StringBuffer buffer = new StringBuffer();
      buffer.append(project.getGroupId() + " : " + project.getArtifactId() + " : " + project.getVersion()); //$NON-NLS-1$ //$NON-NLS-2$
      return buffer.toString();
    }

    public Color getForeground(Object element) {
      if(element instanceof MavenProject) {
        MavenProject project = (MavenProject) element;
        IMavenProjectFacade search = MavenPlugin.getMavenProjectRegistry()
            .getMavenProject(project.getGroupId(), project.getArtifactId(), project.getVersion());
        if(search == null) {
          // This project is not in the workspace
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    public Color getBackground(Object element) {
      return null;
    }

    public Image getImage(Object element) {
      if(element instanceof MavenProject) {
        MavenProject project = (MavenProject) element;
        IMavenProjectFacade search = MavenPlugin.getMavenProjectRegistry()
            .getMavenProject(project.getGroupId(), project.getArtifactId(), project.getVersion());
        if(search == null) {
          // This project is not in the workspace
          return MavenImages.getOverlayImage(MavenImages.PATH_JAR, MavenImages.PATH_LOCK, IDecoration.BOTTOM_LEFT);
        } else {
          return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
        }
      }
      return null;
    }
  }

  public static class PomHeirarchyContentProvider implements ITreeContentProvider {
    private LinkedList<MavenProject> projects;

    public PomHeirarchyContentProvider() {
    }

    @SuppressWarnings("unchecked")
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      if(newInput instanceof LinkedList) {
        this.projects = (LinkedList<MavenProject>) newInput;
      }
    }

    public void dispose() {
    }

    public boolean hasChildren(Object element) {
      Object[] children = getChildren(element);

      return children.length != 0;
    }

    public Object getParent(Object element) {
      if(element instanceof MavenProject) {
        MavenProject project = (MavenProject) element;
        return project.getParent();
      }
      return null;
    }

    /*
     * Return root element (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang
     * .Object)
     */
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement) {

      if(inputElement instanceof LinkedList) {
        LinkedList<MavenProject> projects = (LinkedList<MavenProject>) inputElement;
        if(projects.isEmpty()) {
          return new Object[0];
        }
        return new Object[] {projects.getLast()};
      }
      return new Object[0];
    }

    public Object[] getChildren(Object parentElement) {
      if(parentElement instanceof MavenProject) {
        /*
         * Walk the hierarchy list until we find the parentElement and
         * return the previous element, which is the child.
         */
        MavenProject parent = (MavenProject) parentElement;

        if(projects.size() == 1) {
          // No parent exists, only one element in the tree
          return new Object[0];
        }

        if(projects.getFirst().equals(parent)) {
          // We are the final child
          return new Object[0];
        }

        ListIterator<MavenProject> iter = projects.listIterator();
        while(iter.hasNext()) {
          MavenProject next = iter.next();
          if(next.equals(parent)) {
            iter.previous();
            MavenProject previous = iter.previous();
            return new Object[] {previous};
          }
        }
      }
      return new Object[0];
    }
  }

  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    pomsViewer.addSelectionChangedListener(listener);
  }

  public Object getInput() {
    return pomsViewer.getInput();
  }

  public ISelection getSelection() {
    return pomsViewer.getSelection();
  }

  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    pomsViewer.removeSelectionChangedListener(listener);
  }

  public void setSelection(ISelection selection) {
    pomsViewer.setSelection(selection);
  }

  public List<MavenProject> getHierarchy() {
    return hierarchy;
  }

  public MavenProject fromSelection() {
    ISelection selection = pomsViewer.getSelection();
    if(selection instanceof IStructuredSelection) {
      Object obj = ((IStructuredSelection) selection).getFirstElement();
      if(obj instanceof MavenProject) {
        return (MavenProject) obj;
      }
    }
    return null;
  }
}
