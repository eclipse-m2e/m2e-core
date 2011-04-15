/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.ui.internal.dialogs;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.MavenImages;

/**
 * UpdateDep
 *
 * @author matthew
 */
public class UpdateDepenciesDialog extends TitleAreaDialog {
  private static final Logger log = LoggerFactory.getLogger(UpdateDepenciesDialog.class);

  private CheckboxTableViewer codebaseViewer;

  private Collection<IProject> projects;

  private Button checkSnapshots;

  private Button updateRemote;

  private IProject[] selectedProjects;

  private boolean isCheckSnapshots = false;

  private boolean isUpdateRemote = false;

  /**
   * Create the dialog.
   * @param parentShell
   */
  public UpdateDepenciesDialog(Shell parentShell) {
    super(parentShell);
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText("Update Maven Dependencies");
  }

  /**
   * Create contents of the dialog.
   * @param parent
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);
    container.setLayout(new GridLayout(2, false));
    container.setLayoutData(new GridData(GridData.FILL_BOTH));

    Label lblAvailable = new Label(container, SWT.NONE);
    lblAvailable.setText("Available Maven Codebases");
    lblAvailable.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

    codebaseViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
    codebaseViewer.setContentProvider(new IStructuredContentProvider() {

      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof Collection) {
          return ((Collection) inputElement).toArray();
        }
        return null;
      }

      public void dispose() {
        // TODO Auto-generated method dispose

      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // TODO Auto-generated method inputChanged

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
    projects = getMavenCodebases(new NullProgressMonitor());
    codebaseViewer.setInput(projects);

    Table table = codebaseViewer.getTable();
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
    gd.heightHint = 300;
    gd.widthHint = 300;
    table.setLayoutData(gd);

    Button selectAllBtn = new Button(container, SWT.NONE);
    selectAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    selectAllBtn.setText("Select All");
    selectAllBtn.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        codebaseViewer.setAllChecked(true);
      }

      public void widgetDefaultSelected(SelectionEvent e) {

      }
    });

    Button deselectAllBtn = new Button(container, SWT.NONE);
    deselectAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
    deselectAllBtn.setText("Deselect All");
    deselectAllBtn.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        codebaseViewer.setAllChecked(false);
      }

      public void widgetDefaultSelected(SelectionEvent e) {

      }
    });

    checkSnapshots = new Button(container, SWT.CHECK);
    checkSnapshots.setText("Forces a check for updated releases and snapshots on remote");
    checkSnapshots.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

    updateRemote = new Button(container, SWT.CHECK);
    updateRemote.setText("Do not automatically update dependencies from remote");
    updateRemote.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

    setTitle("Update Maven Dependencies");
    setMessage("Select Maven codebases to update dependencies");

    return area;
  }

  /**
   * Create contents of the button bar.
   * @param parent
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  protected void okPressed() {
    ISelection selection = codebaseViewer.getSelection();
    if(selection instanceof IStructuredSelection) {
      Object[] obj = ((IStructuredSelection) selection).toArray();
      IProject[] projects = new IProject[obj.length];
      for(int i = 0; i < obj.length; i++ ) {
        projects[i] = (IProject) obj[i];
      }
      selectedProjects = projects;
    }
    super.okPressed();
  }

  /**
   * Return the initial size of the dialog.
   */
//  @Override
//  protected Point getInitialSize() {
//    return new Point(450, 450);
//  }

  public Collection<IProject> getMavenCodebases(IProgressMonitor monitor) {
    Set<IProject> projects = new HashSet<IProject>();
    SubMonitor mon = SubMonitor.convert(monitor, MavenPlugin.getMavenProjectRegistry().getProjects().length);
    try {
      for(IMavenProjectFacade facade : MavenPlugin.getMavenProjectRegistry().getProjects()) {
        try {
          MavenProject root = facade.getMavenProject(mon.newChild(1));
          while(root.getParent() != null && root.getParent().getFile() != null) {
            root = root.getParent();
          }
          if(root.getFile() != null) {
            IFile pomFile = M2EUtils.getPomFile(root);
            if(pomFile != null) {
              projects.add(pomFile.getProject());
            }
          }
        } catch(CoreException ex) {
          log.error(ex.getMessage(), ex);
        }
      }
    } finally {
      mon.done();
    }
    return projects;
  }

  public IProject[] getSelectedProjects() {
    return selectedProjects;
  }

  public boolean isCheckSnapshots() {
    return isCheckSnapshots;
  }

  public boolean isUpdateRemote() {
    return isUpdateRemote;
  }
}
