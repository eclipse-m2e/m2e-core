/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.ClasspathEntry;
import org.eclipse.m2e.core.internal.launch.MavenExternalRuntime;
import org.eclipse.m2e.core.internal.launch.MavenWorkspaceRuntime;
import org.eclipse.m2e.core.internal.launch.ProjectClasspathEntry;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.components.MavenProjectLabelProvider;


@SuppressWarnings("restriction")
public class MavenInstallationWizardPage extends WizardPage {

  private List<ClasspathEntry> extensions;

  private Text location;

  private Button btnAddProject;

  private Button btnRemove;

  private Button btnUp;

  private Button btnDown;

  private TreeViewer treeViewerLibrariries;

  private Text name;

  private AbstractMavenRuntime original;

  private Button btnExternal;

  private Button btnWorkspace;

  private Button btnDirectory;

  private Set<String> usedNames;

  class TreeContentProvider implements ITreeContentProvider {

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object inputElement) {
      return ((Collection<?>) inputElement).toArray();
    }

    public Object[] getChildren(Object parentElement) {
      return null;
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      return false;
    }

  }

  class TreeLabelProvider implements ILabelProvider {

    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }

    public Image getImage(Object element) {
      return null;
    }

    public String getText(Object element) {
      if(element instanceof ProjectClasspathEntry) {
        return ((ProjectClasspathEntry) element).getProject();
      }
      return element.toString();
    }
  }

  public MavenInstallationWizardPage(AbstractMavenRuntime original, Set<String> usedNames) {
    super(Messages.ExternalInstallPage_pageName);
    this.original = original;
    this.usedNames = usedNames;
    setDescription(Messages.ExternalInstallPage_description);

    this.extensions = original != null && original.getExtensions() != null ? original.getExtensions()
        : new ArrayList<ClasspathEntry>();
  }

  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);

    setControl(container);
    container.setLayout(new GridLayout(3, false));

    Label lblInstallationType = new Label(container, SWT.NONE);
    lblInstallationType.setText(Messages.MavenInstallationWizardPage_lblInstallationType_text);

    Composite composite = new Composite(container, SWT.NONE);
    RowLayout rl_composite = new RowLayout(SWT.HORIZONTAL);
    rl_composite.fill = true;
    composite.setLayout(rl_composite);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

    btnExternal = new Button(composite, SWT.RADIO);
    btnExternal.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        changeRuntimeTypeAction();
      }
    });
    btnExternal.setText(Messages.MavenInstallationWizardPage_btnExternal_text_1);

    btnWorkspace = new Button(composite, SWT.RADIO);
    btnWorkspace.setText(Messages.MavenInstallationWizardPage_btnWorkspace_text);

    Label lblInstallationLocation = new Label(container, SWT.NONE);
    lblInstallationLocation.setText(Messages.ExternalInstallPage_lblInstallationLocation_text);

    location = new Text(container, SWT.BORDER);
    location.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateStatus();
      }
    });
    location.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    btnDirectory = new Button(container, SWT.NONE);
    btnDirectory.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectLocationAction();
      }
    });
    btnDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnDirectory.setText(Messages.ExternalInstallPage_btnDirectory_text);

    Label lblInstallationName = new Label(container, SWT.NONE);
    lblInstallationName.setText(Messages.ExternalInstallPage_lblInstallationName_text);

    name = new Text(container, SWT.BORDER);
    name.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateStatus();
      }
    });
    name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    Label lblInstallationLibraries = new Label(container, SWT.NONE);
    lblInstallationLibraries.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    lblInstallationLibraries.setText(Messages.ExternalInstallPage_lblInstallationLibraries_text);

    treeViewerLibrariries = new TreeViewer(container, SWT.BORDER);
    treeViewerLibrariries.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateButtonsState();
      }
    });
    treeViewerLibrariries.setContentProvider(new TreeContentProvider());
    treeViewerLibrariries.setLabelProvider(new TreeLabelProvider());
    treeViewerLibrariries.setInput(extensions);
    Tree treeLibraries = treeViewerLibrariries.getTree();
    treeLibraries.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 5));

    btnAddProject = new Button(container, SWT.NONE);
    btnAddProject.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        addProjectExtensionAction();
      }
    });
    btnAddProject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnAddProject.setText(Messages.ExternalInstallPage_btnAddProject_text);

    btnRemove = new Button(container, SWT.NONE);
    btnRemove.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        removeExtensionAction();
      }
    });
    btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnRemove.setText(Messages.ExternalInstallPage_btnRemove_text);

    btnUp = new Button(container, SWT.NONE);
    btnUp.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        moveExtensionAction(-1);
      }
    });
    btnUp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnUp.setText(Messages.ExternalInstallPage_btnUp_text);

    btnDown = new Button(container, SWT.NONE);
    btnDown.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        moveExtensionAction(1);
      }
    });
    btnDown.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnDown.setText(Messages.ExternalInstallPage_btnDown_text);

    Button btnRestoreDefault = new Button(container, SWT.NONE);
    btnRestoreDefault.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        resetExtensionsAction();
      }
    });
    btnRestoreDefault.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1));
    btnRestoreDefault.setText(Messages.ExternalInstallPage_btnRestoreDefault_text);

    if(original instanceof MavenExternalRuntime) {
      btnExternal.setSelection(true);
      location.setText(original.getLocation());
      if(!original.isLegacy()) {
        name.setText(original.getName());
      }
    }
    if(original instanceof MavenWorkspaceRuntime) {
      btnWorkspace.setSelection(true);
      name.setText(original.getName());
    } else {
      btnWorkspace.setEnabled(new MavenWorkspaceRuntime("test").isAvailable()); //$NON-NLS-1$
    }
    if(original == null) {
      btnExternal.setSelection(true);
    }

    updateButtonsState();
    updateStatus();
  }

  protected void changeRuntimeTypeAction() {
    location.setEnabled(btnExternal.getSelection());
    btnDirectory.setEnabled(btnExternal.getSelection());
    updateStatus();
  }

  protected void moveExtensionAction(int offset) {
    int from = extensions.indexOf(getSelectedElement());
    int to = Math.min(extensions.size() - 1, Math.max(0, from + offset));
    Collections.swap(extensions, from, to);
    treeViewerLibrariries.refresh();
  }

  protected void resetExtensionsAction() {
    extensions.clear();
    treeViewerLibrariries.refresh();
  }

  protected void removeExtensionAction() {
    Object selection = getSelectedElement();
    extensions.remove(selection);
    treeViewerLibrariries.refresh();
  }

  protected void updateButtonsState() {
    Object selection = getSelectedElement();

    // can move/remove classpath entries only
    boolean editEnabled = selection != null;
    btnUp.setEnabled(editEnabled);
    btnDown.setEnabled(editEnabled);
    btnRemove.setEnabled(editEnabled);
  }

  private Object getSelectedElement() {
    return ((IStructuredSelection) treeViewerLibrariries.getSelection()).getFirstElement();
  }

  protected void addProjectExtensionAction() {
    List<Object> projects = new ArrayList<Object>();
    for(IMavenProjectFacade facade : MavenPlugin.getMavenProjectRegistry().getProjects()) {
      projects.add(facade.getProject());
    }
    ListSelectionDialog dialog = new ListSelectionDialog(getShell(), projects, new ArrayContentProvider(),
        new MavenProjectLabelProvider(), Messages.MavenInstallationWizardPage_selectProjectMessage);
    dialog.setTitle(Messages.MavenInstallationWizardPage_selectProjectTitle);
    dialog.setHelpAvailable(false);
    if(dialog.open() == Window.OK) {
      Object insertionPoint = getSelectedElement();
      if(insertionPoint == null || insertionPoint instanceof ClasspathEntry) {
        int idx = Math.max(0, extensions.indexOf(insertionPoint));
        for(Object object : dialog.getResult()) {
          extensions.add(idx, new ProjectClasspathEntry(((IProject) object).getName()));
        }
      } else {
        throw new IllegalStateException();
      }
      treeViewerLibrariries.refresh();
    }
  }

  protected void selectLocationAction() {
    DirectoryDialog dlg = new DirectoryDialog(getShell());
    dlg.setText(Messages.MavenInstallationsPreferencePage_dialog_install_title);
    dlg.setMessage(Messages.MavenInstallationsPreferencePage_dialog_install_message);
    String dir = dlg.open();
    if(dir == null) {
      return;
    }
    location.setText(dir);
    if(name.getText().trim().isEmpty()) {
      name.setText(new File(dir).getName());
    }
  }

  private boolean isValidMavenInstall(String dir) {
    if(dir == null || dir.length() == 0) {
      return false;
    }
    File selectedDir = new File(dir);
    if(!selectedDir.isDirectory()) {
      return false;
    }
    File binDir = new File(dir, "bin"); //$NON-NLS-1$
    File confDir = new File(dir, "conf"); //$NON-NLS-1$
    File libDir = new File(dir, "lib"); //$NON-NLS-1$
    if(!binDir.exists() || !confDir.exists() || !libDir.exists()) {
      return false;
    }
    return true;
  }

  protected void updateStatus() {
    setPageComplete(false);

    if(btnExternal.getSelection()) {
      if(location.getText().trim().isEmpty()) {
        setMessage(Messages.MavenInstallationWizardPage_messageSelectHomeDirectory);
        return;
      }

      if(!isValidMavenInstall(location.getText())) {
        setErrorMessage(Messages.MavenInstallationWizardPage_messageHomeDirectoryIsNotMavenInstll);
        return;
      }
    }

    if(name.getText().trim().isEmpty()) {
      setMessage(Messages.MavenInstallationWizardPage_messageSelectInstallatonName);
      return;
    }

    if(usedNames.contains(name.getText().trim())) {
      setErrorMessage(Messages.MavenInstallationWizardPage_messageDuplicateInstallationName);
      return;
    }

    setMessage(null);
    setPageComplete(true);
  }

  public AbstractMavenRuntime getResult() {
    AbstractMavenRuntime runtime;
    if(btnExternal.getSelection()) {
      runtime = new MavenExternalRuntime(name.getText(), location.getText());
    } else {
      runtime = new MavenWorkspaceRuntime(name.getText());
    }
    runtime.setExtensions(extensions);
    return runtime;
  }
}
