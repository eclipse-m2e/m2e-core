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

package org.eclipse.m2e.core.ui.internal.dialogs;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


public class UpdateMavenProjectsDialog extends TitleAreaDialog implements IMenuListener {

  private static final Logger log = LoggerFactory.getLogger(UpdateMavenProjectsDialog.class);

  private static final String SEPARATOR = System.getProperty("file.separator"); //$NON-NLS-1$

  private CheckboxTreeViewer codebaseViewer;

  private Collection<IProject> projects;

  private Button offlineModeBtn;

  private Button forceUpdateBtn;

  private List<String> projectPaths;

  private final IProject[] initialSelection;

  private IProject[] selectedProjects;

  private boolean offlineMode;

  /**
   * Force update of snapshots and releases from remote repositories
   */
  private boolean forceUpdateDependencies;

  /**
   * Update project configuration
   */
  private boolean updateConfiguration;

  /**
   * Perform full/clean build after project update
   */
  private boolean rebuild;

  protected String dialogTitle;

  protected String dialogMessage;

  public UpdateMavenProjectsDialog(Shell parentShell, IProject[] initialSelection) {
    super(parentShell);
    this.initialSelection = initialSelection;
    this.dialogTitle = Messages.UpdateMavenProjectDialog_title;
    this.dialogMessage = Messages.UpdateMavenProjectDialog_dialogMessage;
    offlineMode = MavenPlugin.getMavenConfiguration().isOffline();
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(getDialogTitle());
  }

  /**
   * Create contents of the dialog.
   * 
   * @param parent
   */
  @Override
  @SuppressWarnings("rawtypes")
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);

    GridLayout layout = new GridLayout(2, false);
    layout.marginLeft = 12;
    container.setLayout(layout);
    container.setLayoutData(new GridData(GridData.FILL_BOTH));

    Label lblAvailable = new Label(container, SWT.NONE);
    lblAvailable.setText(Messages.UpdateDepenciesDialog_availableCodebasesLabel);
    lblAvailable.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

    codebaseViewer = new CheckboxTreeViewer(container, SWT.BORDER);
    codebaseViewer.setContentProvider(new ITreeContentProvider() {

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public Object[] getElements(Object element) {
        if(element instanceof Collection) {
          return ((Collection) element).toArray();
        }
        return null;
      }

      public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof IProject) {
          String elePath = new File(((IProject) parentElement).getLocationURI()).toString() + SEPARATOR;
          String prevPath = null;
          List<IProject> children = new ArrayList<IProject>();
          for(String path : projectPaths) {
            if(path.length() != elePath.length() && path.startsWith(elePath)) {
              if(prevPath == null || !path.startsWith(prevPath)) {
                prevPath = path;
                children.add(getProject(path));
              }
            }
          }
          return children.toArray();
        } else if(parentElement instanceof Collection) {
          return ((Collection) parentElement).toArray();
        }
        return null;
      }

      public Object getParent(Object element) {
        String elePath = new File(((IProject) element).getLocationURI()).toString() + SEPARATOR;
        String prevPath = null;
        for(String path : projectPaths) {
          if(elePath.length() != path.length() && elePath.startsWith(path)
              && (prevPath == null || prevPath.length() < path.length())) {
            prevPath = path;
          }
        }
        return prevPath == null ? projects : getProject(prevPath);
      }

      public boolean hasChildren(Object element) {
        if(element instanceof IProject) {
          String elePath = new File(((IProject) element).getLocationURI()).toString() + SEPARATOR;
          for(String path : projectPaths) {
            if(elePath.length() != path.length() && path.startsWith(elePath)) {
              return true;
            }
          }
        } else if(element instanceof Collection) {
          return !((Collection) element).isEmpty();
        }
        return false;
      }
    });
    codebaseViewer.setLabelProvider(new LabelProvider() {
      public Image getImage(Object element) {
        return MavenImages.createOverlayImage(MavenImages.MVN_PROJECT, PlatformUI.getWorkbench().getSharedImages()
            .getImage(IDE.SharedImages.IMG_OBJ_PROJECT), MavenImages.MAVEN_OVERLAY, IDecoration.TOP_LEFT);
      }

      public String getText(Object element) {
        return element instanceof IProject ? ((IProject) element).getName() : ""; //$NON-NLS-1$
      }
    });
    projects = getMavenCodebases();
    codebaseViewer.setInput(projects);
    codebaseViewer.expandAll();
    for(IProject project : initialSelection) {
      codebaseViewer.setSubtreeChecked(project, true);
    }

    // Reveal the first element
    if(initialSelection.length > 0) {
      codebaseViewer.reveal(initialSelection[0]);
    }

    Tree tree = codebaseViewer.getTree();
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    gd.heightHint = 300;
    gd.widthHint = 300;
    tree.setLayoutData(gd);

    Composite selectionActionComposite = new Composite(container, SWT.NONE);
    selectionActionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
    selectionActionComposite.setLayout(new GridLayout(1, false));

    Button selectAllBtn = new Button(selectionActionComposite, SWT.NONE);
    selectAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    selectAllBtn.setText(Messages.UpdateDepenciesDialog_selectAll);
    selectAllBtn.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        for(IProject project : projects) {
          codebaseViewer.setSubtreeChecked(project, true);
        }
      }

      public void widgetDefaultSelected(SelectionEvent e) {

      }
    });

    Button deselectAllBtn = new Button(selectionActionComposite, SWT.NONE);
    deselectAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
    deselectAllBtn.setText(Messages.UpdateDepenciesDialog_deselectAll);
    deselectAllBtn.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        for(IProject project : projects) {
          codebaseViewer.setSubtreeChecked(project, false);
        }
      }

      public void widgetDefaultSelected(SelectionEvent e) {

      }
    });

    Button expandAllBtn = new Button(selectionActionComposite, SWT.NONE);
    expandAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1));
    expandAllBtn.setText(Messages.UpdateDepenciesDialog_expandAll);
    expandAllBtn.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        codebaseViewer.expandAll();
      }

      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });

    Button collapseAllBtn = new Button(selectionActionComposite, SWT.NONE);
    collapseAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1));
    collapseAllBtn.setText(Messages.UpdateDepenciesDialog_collapseAll);
    collapseAllBtn.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        codebaseViewer.collapseAll();
      }

      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });

    Composite optionsComposite = new Composite(container, SWT.NONE);
    optionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
    optionsComposite.setLayout(new GridLayout(2, false));

    offlineModeBtn = new Button(optionsComposite, SWT.CHECK);
    offlineModeBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    offlineModeBtn.setText(Messages.UpdateDepenciesDialog_offline);
    offlineModeBtn.setSelection(offlineMode);

    Button btnCheckButton = new Button(optionsComposite, SWT.CHECK);
    btnCheckButton.setEnabled(false);
    btnCheckButton.setSelection(true);
    btnCheckButton.setText(Messages.UpdateMavenProjectDialog_btnCheckButton_text);
    new Label(optionsComposite, SWT.NONE);

    forceUpdateBtn = new Button(optionsComposite, SWT.CHECK);
    GridData gd_forceUpdateBtn = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
    gd_forceUpdateBtn.horizontalIndent = 15;
    forceUpdateBtn.setLayoutData(gd_forceUpdateBtn);
    forceUpdateBtn.setText(Messages.UpdateDepenciesDialog_forceUpdate);
    forceUpdateBtn.setSelection(true);
    new Label(optionsComposite, SWT.NONE);

    btnUpdateProjectConfiguration = new Button(optionsComposite, SWT.CHECK);
    btnUpdateProjectConfiguration.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    btnUpdateProjectConfiguration.setSelection(true);
    btnUpdateProjectConfiguration.setText(Messages.UpdateMavenProjectDialog_btnUpdateProjectConfiguration_text);

    btnCleanFullBuild = new Button(optionsComposite, SWT.CHECK);
    btnCleanFullBuild.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    btnCleanFullBuild.setSelection(true);
    btnCleanFullBuild.setText(Messages.UpdateMavenProjectDialog_btnCleanFullBuild_text);

    setTitle(getDialogTitle());
    setMessage(getDialogMessage());
    createMenu();
    return area;
  }

  /**
   * Create contents of the button bar.
   * 
   * @param parent
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  protected void okPressed() {
    Object[] obj = codebaseViewer.getCheckedElements();
    IProject[] projects = new IProject[obj.length];
    for(int i = 0; i < obj.length; i++ ) {
      projects[i] = (IProject) obj[i];
    }
    selectedProjects = projects;

    offlineMode = offlineModeBtn.getSelection();
    forceUpdateDependencies = forceUpdateBtn.getSelection();
    updateConfiguration = btnUpdateProjectConfiguration.getSelection();
    rebuild = btnCleanFullBuild.getSelection();
    super.okPressed();
  }

  private Collection<IProject> getMavenCodebases() {
    projectPaths = new LinkedList<String>();

    for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      try {
        if(project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)) {
          URI locationURI = project.getLocationURI();
          if(locationURI != null) {
            projectPaths.add(new File(locationURI).toString() + SEPARATOR);
          }
        }
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    Collections.sort(projectPaths);

    if(projectPaths.isEmpty()) {
      return Collections.<IProject> emptyList();
    }
    projects = new ArrayList<IProject>();
    String previous = projectPaths.get(0);
    addProject(projects, previous);
    for(String path : projectPaths) {
      if(!path.startsWith(previous)) {
        previous = path;
        projects.add(getProject(path));
      }
    }
    return projects;
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

  public IProject[] getSelectedProjects() {
    return selectedProjects;
  }

  public boolean isOffline() {
    return offlineMode;
  }

  public boolean isForceUpdateDependencies() {
    return forceUpdateDependencies;
  }

  public boolean isUpdateConfiguration() {
    return updateConfiguration;
  }

  public boolean isRebuild() {
    return rebuild;
  }

  private IProject getProject(String path) {
    IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot()
        .findContainersForLocationURI(new File(path).toURI());
    for(IContainer container : containers) {
      if(container instanceof IProject) {
        return (IProject) container;
      }
    }
    return null;
  }

  private void createMenu() {
    MenuManager menuMgr = new MenuManager();
    Menu contextMenu = menuMgr.createContextMenu(codebaseViewer.getControl());
    menuMgr.addMenuListener(this);
    codebaseViewer.getControl().setMenu(contextMenu);
    menuMgr.setRemoveAllWhenShown(true);
  }

  private IProject getSelection() {
    ISelection selection = codebaseViewer.getSelection();
    if(selection instanceof IStructuredSelection) {
      return (IProject) ((IStructuredSelection) selection).getFirstElement();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
   */
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
    public void run() {
      codebaseViewer.setSubtreeChecked(getSelection(), true);
    }
  };

  private final Action deselectTree = new Action(Messages.UpdateDepenciesDialog_deselectTree) {
    public void run() {
      codebaseViewer.setSubtreeChecked(getSelection(), false);
    }
  };

  private Button btnUpdateProjectConfiguration;

  private Button btnCleanFullBuild;

  /**
   * @return Returns the dialogTitle or an empty String if the value is null.
   */
  public String getDialogTitle() {
    if(dialogTitle == null) {
      dialogTitle = ""; //$NON-NLS-1$
    }
    return dialogTitle;
  }

  /**
   * @return Returns the dialogMessage or an empty String if the value is null.
   */
  public String getDialogMessage() {
    if(dialogMessage == null) {
      dialogMessage = ""; //$NON-NLS-1$
    }
    return dialogMessage;
  }

  /**
   * @param dialogTitle The dialogTitle to set.
   */
  public void setDialogTitle(String dialogTitle) {
    this.dialogTitle = dialogTitle;
  }

  /**
   * @param dialogMessage The dialogMessage to set.
   */
  public void setDialogMessage(String dialogMessage) {
    this.dialogMessage = dialogMessage;
  }
}
