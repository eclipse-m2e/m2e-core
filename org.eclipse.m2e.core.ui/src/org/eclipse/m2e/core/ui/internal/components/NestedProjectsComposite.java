/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


@SuppressWarnings({"restriction", "synthetic-access"})
public class NestedProjectsComposite extends Composite implements IMenuListener {

  private static final Logger log = LoggerFactory.getLogger(NestedProjectsComposite.class);

  private static final String SEPARATOR = System.getProperty("file.separator"); //$NON-NLS-1$

  CheckboxTreeViewer codebaseViewer;

  Map<String, IProject> projectPaths;

  Collection<IProject> projects;

  IProject[] selectedProjects;

  private Link includeOutDateProjectslink;

  private Composite warningArea;

  private Button addOutOfDateBtn;

  private boolean showOutOfDateUI;

  public NestedProjectsComposite(Composite parent, int style, IProject[] initialSelection, boolean showOutOfDateWarning) {
    super(parent, style);
    this.showOutOfDateUI = showOutOfDateWarning;

    setLayout(new GridLayout(2, false));

    Label lblAvailable = new Label(this, SWT.NONE);
    lblAvailable.setText(Messages.UpdateDepenciesDialog_availableCodebasesLabel);
    lblAvailable.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

    codebaseViewer = new CheckboxTreeViewer(this, SWT.BORDER);
    codebaseViewer.setContentProvider(new ITreeContentProvider() {

        @Override
        public void dispose() {
      }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

        @Override
        public Object[] getElements(Object element) {
        if(element instanceof Collection) {
          return ((Collection<?>) element).toArray();
        }
        return null;
      }

        @Override
        public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof IProject) {
          String elePath = getElePath(parentElement);
          String prevPath = null;
          List<IProject> children = new ArrayList<>();
          for(String path : projectPaths.keySet()) {
            if(path.length() != elePath.length() && path.startsWith(elePath)) {
              if(prevPath == null || !path.startsWith(prevPath)) {
                prevPath = path;
                children.add(getProject(path));
              }
            }
          }
          return children.toArray();
        } else if(parentElement instanceof Collection) {
          return ((Collection<?>) parentElement).toArray();
        }
        return null;
      }

        @Override
        public Object getParent(Object element) {
        String elePath = getElePath(element);
        String prevPath = null;
        for(String path : projectPaths.keySet()) {
          if(elePath.length() != path.length() && elePath.startsWith(path)
              && (prevPath == null || prevPath.length() < path.length())) {
            prevPath = path;
          }
        }
        return prevPath == null ? projects : getProject(prevPath);
      }

        @Override
        public boolean hasChildren(Object element) {
        if(element instanceof IProject) {
          String elePath = getElePath(element);
          for(String path : projectPaths.keySet()) {
            if(elePath.length() != path.length() && path.startsWith(elePath)) {
              return true;
            }
          }
        } else if(element instanceof Collection) {
          return !((Collection<?>) element).isEmpty();
        }
        return false;
      }
    });
    codebaseViewer.setLabelProvider(new MavenProjectLabelProvider() {
        @Override
        public Image getImage(Object element) {
        Image img = super.getImage(element);
        if(showOutOfDateUI && requiresUpdate((IProject) element)) {
          img = MavenImages.createOverlayImage(MavenImages.OOD_MVN_PROJECT, img, MavenImages.OUT_OF_DATE_OVERLAY,
              IDecoration.BOTTOM_RIGHT);
        }
        return img;
      }
    });
    codebaseViewer.setComparator(new ViewerComparator());

    projects = getMavenCodebases();

    // prevent flicker
    codebaseViewer.getTree().setRedraw(false);
    try {
      codebaseViewer.setInput(projects);
      codebaseViewer.expandAll();
      if(initialSelection != null) { // windowbuilder compat
        for(IProject project : initialSelection) {
          setSubtreeChecked(project, true);
        }

        // Reveal the first element
        if(initialSelection.length > 0) {
          codebaseViewer.reveal(initialSelection[0]);
        }
      }
    } finally {
      codebaseViewer.getTree().setRedraw(true);
    }

    Tree tree = codebaseViewer.getTree();
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    gd.heightHint = 300;
    gd.widthHint = 300;
    tree.setLayoutData(gd);

    GridLayout layout = new GridLayout(2, false);
    layout.marginLeft = 10;

    Composite selectionActionComposite = new Composite(this, SWT.NONE);
    selectionActionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
    GridLayout gl_selectionActionComposite = new GridLayout(1, false);
    gl_selectionActionComposite.marginWidth = 0;
    gl_selectionActionComposite.marginHeight = 0;
    selectionActionComposite.setLayout(gl_selectionActionComposite);

    createButtons(selectionActionComposite);

    createOutOfDateProjectsWarning(parent);

    createMenu();

    codebaseViewer.addSelectionChangedListener(event -> updateSelectedProjects());

    updateSelectedProjects();
  }

  private void createOutOfDateProjectsWarning(Composite composite) {
    if(!showOutOfDateUI) {
      return;
    }
    warningArea = new Composite(composite, SWT.NONE);
    warningArea.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
    warningArea.setLayout(new RowLayout(SWT.HORIZONTAL));
    Label warningImg = new Label(warningArea, SWT.NONE);
    warningImg.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));

    includeOutDateProjectslink = new Link(warningArea, SWT.NONE);
    includeOutDateProjectslink.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        includeOutOfDateProjects();
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        includeOutOfDateProjects();
      }
    });
  }

  private void updateIncludeOutDateProjectsLink(int outOfDateProjectsCount) {
    boolean visibility = true;
    String text = ""; //$NON-NLS-1$
    String btnTooltip;
    if(outOfDateProjectsCount == 0) {
      visibility = false;
      btnTooltip = Messages.NestedProjectsComposite_OutOfDateProjectBtn_Generic_Tooltip;
    } else if(outOfDateProjectsCount > 1) {
      text = NLS.bind(Messages.NestedProjectsComposite_Multiple_OOD_Projects_Link, outOfDateProjectsCount);
      btnTooltip = NLS.bind(Messages.NestedProjectsComposite_OutOfDateProjectBtn_AddProjects_Tooltip,
          outOfDateProjectsCount);
    } else {
      text = Messages.NestedProjectsComposite_Single_OOD_Project_Link;
      btnTooltip = Messages.NestedProjectsComposite_OutOfDateProjectBtn_AddOneProject_Tooltip;
    }

    if(includeOutDateProjectslink != null && addOutOfDateBtn != null && warningArea != null) {
      includeOutDateProjectslink.setText(text);
      addOutOfDateBtn.setToolTipText(btnTooltip);
      warningArea.setVisible(visibility);
      warningArea.getParent().layout(new Control[] {warningArea});
    }
  }

  private int computeOutOfDateProjectsCount() {
    int outOfDateProjectsCount = 0;
    for(IProject p : projectPaths.values()) {
      if(requiresUpdate(p) && !codebaseViewer.getChecked(p)) {
        outOfDateProjectsCount++ ;
      }
    }
    return outOfDateProjectsCount;
  }

  private void includeOutOfDateProjects() {
    for(IProject project : projectPaths.values()) {
      if(requiresUpdate(project)) {
        codebaseViewer.setChecked(project, true);
      }
    }
    updateSelectedProjects();
  }

  private void updateSelectedProjects() {
    selectedProjects = internalGetSelectedProjects();
    updateIncludeOutDateProjectsLink(computeOutOfDateProjectsCount());
  }

  private void setSubtreeChecked(Object obj, boolean checked) {
    // CheckBoxTreeViewer#setSubtreeChecked is severely inefficient
    codebaseViewer.setChecked(obj, checked);
    Object[] children = ((ITreeContentProvider) codebaseViewer.getContentProvider()).getChildren(obj);
    if(children != null) {
      for(Object child : children) {
        setSubtreeChecked(child, checked);
      }
    }
  }

  protected void createButtons(Composite selectionActionComposite) {
    Button selectAllBtn = new Button(selectionActionComposite, SWT.NONE);
    selectAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    selectAllBtn.setText(Messages.UpdateDepenciesDialog_selectAll);
    selectAllBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      for(IProject project : projects) {
        setSubtreeChecked(project, true);
      }
      updateSelectedProjects();
    }));

    if(showOutOfDateUI) {
      addOutOfDateBtn = new Button(selectionActionComposite, SWT.NONE);
      addOutOfDateBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
      addOutOfDateBtn.setText(Messages.NestedProjectsComposite_Add_OutOfDate);
      addOutOfDateBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> includeOutOfDateProjects()));
    }

    Button deselectAllBtn = new Button(selectionActionComposite, SWT.NONE);
    deselectAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
    deselectAllBtn.setText(Messages.UpdateDepenciesDialog_deselectAll);
    deselectAllBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      codebaseViewer.setCheckedElements(new Object[0]);
      updateSelectedProjects();
    }));

    Button expandAllBtn = new Button(selectionActionComposite, SWT.NONE);
    expandAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1));
    expandAllBtn.setText(Messages.UpdateDepenciesDialog_expandAll);
    expandAllBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> codebaseViewer.expandAll()));

    Button collapseAllBtn = new Button(selectionActionComposite, SWT.NONE);
    collapseAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1));
    collapseAllBtn.setText(Messages.UpdateDepenciesDialog_collapseAll);
    collapseAllBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> codebaseViewer.collapseAll()));
  }

  String getElePath(Object element) {
    if(element instanceof IProject) {
      IProject project = (IProject) element;
      URI locationURI = project.getLocationURI();

      try {
        IFileStore store = EFS.getStore(locationURI);
        File file = store.toLocalFile(0, null);
        if(file == null) {
          file = store.toLocalFile(EFS.CACHE, null);
        }
        return file.toString() + SEPARATOR;
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    return null;
  }

  IProject getProject(String path) {
    return projectPaths.get(path);
  }

  private Collection<IProject> getMavenCodebases() {
    projectPaths = new TreeMap<>();

    for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      try {
        if(isInteresting(project)) {
          if(project.getLocationURI() != null) {
            String path = getElePath(project);
            if(path != null) {
              projectPaths.put(path, project);
            }
          }
        }
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }

    if(projectPaths.isEmpty()) {
      return Collections.<IProject> emptyList();
    }
    List<IProject> projects = new ArrayList<>();
    String previous = projectPaths.keySet().iterator().next();
    addProject(projects, previous);
    for(String path : projectPaths.keySet()) {
      if(!path.startsWith(previous)) {
        previous = path;
        IProject project = getProject(path);
        if(project != null) {
          projects.add(project);
        }
      }
    }
    return projects;
  }

  protected boolean isInteresting(IProject project) throws CoreException {
    return project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID);
  }

  private static void addProject(Collection<IProject> projects, String location) {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for(IContainer container : root.findContainersForLocationURI(new File(location).toURI())) {
      if(container instanceof IProject) {
        projects.add((IProject) container);
        break;
      }
    }
  }

  private void createMenu() {
    MenuManager menuMgr = new MenuManager();
    Menu contextMenu = menuMgr.createContextMenu(codebaseViewer.getControl());
    menuMgr.addMenuListener(this);
    codebaseViewer.getControl().setMenu(contextMenu);
    menuMgr.setRemoveAllWhenShown(true);
  }

  @Override
  public void menuAboutToShow(IMenuManager manager) {
    if(codebaseViewer.getSelection().isEmpty()) {
      return;
    }

    if(codebaseViewer.getSelection() instanceof IStructuredSelection) {
      manager.add(selectTree);
      manager.add(deselectTree);
    }
  }

  private final Action selectTree = new Action(Messages.UpdateDepenciesDialog_selectTree) {
      @Override
      public void run() {
      setSubtreeChecked(getSelection(), true);
      updateSelectedProjects();
    }
  };

  private final Action deselectTree = new Action(Messages.UpdateDepenciesDialog_deselectTree) {
      @Override
      public void run() {
      setSubtreeChecked(getSelection(), false);
      updateSelectedProjects();
    }
  };

  public IProject getSelection() {
    ISelection selection = codebaseViewer.getSelection();
    if(selection instanceof IStructuredSelection) {
      return (IProject) ((IStructuredSelection) selection).getFirstElement();
    }
    return null;
  }

  public IProject[] getSelectedProjects() {
    return selectedProjects;
  }

  IProject[] internalGetSelectedProjects() {
    Object[] obj = codebaseViewer.getCheckedElements();
    IProject[] projects = new IProject[obj.length];
    for(int i = 0; i < obj.length; i++ ) {
      projects[i] = (IProject) obj[i];
    }
    return projects;
  }

  public void refresh() {
    projects = getMavenCodebases();
    codebaseViewer.setInput(projects);
    codebaseViewer.expandAll();
  }

  public void reset() {
    projects = getMavenCodebases();
    codebaseViewer.setInput(projects);
    codebaseViewer.expandAll();
    codebaseViewer.setCheckedElements(new Object[0]);
    updateSelectedProjects();
  }

  public void addSelectionChangeListener(ISelectionChangedListener listener) {
    codebaseViewer.addSelectionChangedListener(listener);
  }

  //XXX probably move to a utility class
  private boolean requiresUpdate(IProject project) {
    try {
      IMarker[] markers = project.findMarkers(IMavenConstants.MARKER_CONFIGURATION_ID, true, IResource.DEPTH_ZERO);
      for(IMarker marker : markers) {
        String message = (String) marker.getAttribute(IMarker.MESSAGE);
        //XXX need a better way to identify these than rely on the marker message
        if(org.eclipse.m2e.core.internal.Messages.ProjectConfigurationUpdateRequired.equals(message)) {
          return true;
        }
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
    return false;
  }
}
