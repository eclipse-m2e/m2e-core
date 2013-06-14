/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal.dialogs;

import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.components.PomHierarchyComposite;
import org.eclipse.m2e.core.ui.internal.dialogs.AbstractMavenDialog;
import org.eclipse.m2e.core.ui.internal.util.ParentHierarchyEntry;
import org.eclipse.m2e.editor.xml.MvnIndexPlugin;
import org.eclipse.m2e.editor.xml.internal.Messages;


@SuppressWarnings("restriction")
public class SelectSPDXLicenseDialog extends AbstractMavenDialog {

  /*package*/IMavenProjectFacade targetProject;

  /*package*/SPDXLicense license;

  /*package*/final IMavenProjectFacade project;

  /*package*/static final IStatus STATUS_NO_LICENSE_SELECTION = new Status(IStatus.ERROR, MvnIndexPlugin.PLUGIN_ID,
      Messages.SelectSPDXLicenseDialog_noLicenseSelected_status);

  /*package*/static final IStatus STATUS_NO_WORKSPACE_POM_SELECTION = new Status(IStatus.ERROR,
      MvnIndexPlugin.PLUGIN_ID, Messages.SelectSPDXLicenseDialog_noWorkspacePomSelected_status);

  public SelectSPDXLicenseDialog(Shell parentShell, IMavenProjectFacade project) {
    super(parentShell, SelectSPDXLicenseDialog.class.getName());
    setStatusLineAboveButtons(true);
    setTitle(Messages.SelectSPDXLicenseDialog_Title);
    this.project = project;
    this.targetProject = project;
    updateStatus(STATUS_NO_LICENSE_SELECTION);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);

    Label lblLicenseNameFilter = new Label(container, SWT.NONE);
    lblLicenseNameFilter.setText(Messages.SelectSPDXLicenseDialog_lblLicenseNameFilter_text);

    final Text licenseFilter = new Text(container, SWT.BORDER | SWT.SEARCH);
    licenseFilter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    Label lblLicenses = new Label(container, SWT.NONE);
    lblLicenses.setText(Messages.SelectSPDXLicenseDialog_lblLicenses_text);

    final TableViewer licensesViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
    Table licensesTable = licensesViewer.getTable();
    licensesTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {
        handleDoubleClick();
      }
    });
    licensesTable.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ISelection selection = licensesViewer.getSelection();
        if(selection instanceof IStructuredSelection && !selection.isEmpty()) {
          license = (SPDXLicense) ((IStructuredSelection) selection).getFirstElement();
        } else {
          license = null;
        }
        updateStatus();
      }
    });
    GridData gd_licensesTable = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    gd_licensesTable.heightHint = 400;
    licensesTable.setLayoutData(gd_licensesTable);
    licensesViewer.setContentProvider(new IStructuredContentProvider() {
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public void dispose() {
      }

      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof Collection<?>) {
          return ((Collection<?>) inputElement).toArray();
        }
        return null;
      }
    });
    licensesViewer.setLabelProvider(new ILabelProvider() {
      public void removeListener(ILabelProviderListener listener) {
      }

      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      public void dispose() {
      }

      public void addListener(ILabelProviderListener listener) {
      }

      public String getText(Object element) {
        if(element instanceof SPDXLicense) {
          return ((SPDXLicense) element).getName();
        }
        return null;
      }

      public Image getImage(Object element) {
        return null;
      }
    });
    licensesViewer.setInput(SPDXLicense.getStandardLicenses());

    licenseFilter.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        String text = licenseFilter.getText();
        ViewerFilter[] filters;
        if(text != null && text.trim().length() > 0) {
          filters = new ViewerFilter[] {new LicenseFilter(text.trim())};
        } else {
          filters = new ViewerFilter[] {};
        }
        licensesViewer.setFilters(filters);
      }
    });

    Label lblPomxml = new Label(container, SWT.NONE);
    lblPomxml.setText(Messages.SelectSPDXLicenseDialog_lblPomxml_text);

    final PomHierarchyComposite parentComposite = new PomHierarchyComposite(container, SWT.NONE);
    parentComposite.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {
        handleDoubleClick();
      }
    });
    parentComposite.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = parentComposite.getSelection();
        if(selection instanceof IStructuredSelection && !selection.isEmpty()) {
          ParentHierarchyEntry mavenProject = (ParentHierarchyEntry) ((IStructuredSelection) selection)
              .getFirstElement();
          targetProject = mavenProject.getFacade();
          updateStatus();
        }
      }
    });
    parentComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    parentComposite.computeHeirarchy(project, null);
    parentComposite.setSelection(new StructuredSelection(parentComposite.getHierarchy().get(0)));

    return container;
  }

  public SPDXLicense getLicense() {
    return license;
  }

  public IMavenProjectFacade getTargetProject() {
    return targetProject;
  }

  protected void computeResult() {
    // TODO Auto-generated method computeResult

  }

  private static class LicenseFilter extends ViewerFilter {

    private final String text;

    public LicenseFilter(String text) {
      this.text = text.toLowerCase();
    }

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(element instanceof SPDXLicense) {
        return ((SPDXLicense) element).getName().toLowerCase().contains(text);
      }
      return false;
    }

  }

  /*package*/void updateStatus() {
    updateStatus(getStatus());
  }

  private IStatus getStatus() {
    IStatus status;
    if(license == null) {
      status = STATUS_NO_LICENSE_SELECTION;
    } else if(targetProject == null) {
      status = STATUS_NO_WORKSPACE_POM_SELECTION;
    } else {
      status = Status.OK_STATUS;
    }
    return status;
  }

  /*package*/void handleDoubleClick() {
    if(getStatus().isOK()) {
      okPressed();
    }
  }

}
