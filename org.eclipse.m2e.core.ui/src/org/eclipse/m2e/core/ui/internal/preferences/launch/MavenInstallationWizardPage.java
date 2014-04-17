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
import java.util.List;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.MavenExternalRuntime;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.components.MavenProjectLabelProvider;


@SuppressWarnings("restriction")
public class MavenInstallationWizardPage extends WizardPage {

  private List<ClassRealmNode> realms;

  private Text location;

  private Button btnAddProject;

  private Button btnRemove;

  private Button btnUp;

  private Button btnDown;

  private TreeViewer treeViewerLibrariries;

  private Text name;

  private AbstractMavenRuntime original;

  class TreeContentProvider implements ITreeContentProvider {

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object inputElement) {
      return ((Collection<?>) inputElement).toArray();
    }

    public Object[] getChildren(Object parentElement) {
      if(parentElement instanceof ClassRealmNode) {
        return ((ClassRealmNode) parentElement).getClasspath().toArray();
      }
      return null;
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      return element instanceof ClassRealmNode;
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
      if(element instanceof ClassRealmNode) {
        return ((ClassRealmNode) element).getName();
      } else if(element instanceof ClasspathEntryNode) {
        return ((ClasspathEntryNode) element).getName();
      }
      return null;
    }
  }

  public MavenInstallationWizardPage(AbstractMavenRuntime original) {
    super(Messages.ExternalInstallPage_pageName);
    this.original = original;
    setDescription(Messages.ExternalInstallPage_description);

    List<ClassRealmNode> realms = new ArrayList<ClassRealmNode>();
//    for(Map.Entry<String, List<String>> realm : installation.getRealms().entrySet()) {
//      ClassRealmNode realmNode = new ClassRealmNode(realm.getKey());
//      realmNode.setClasspath(toClasspathEntries(realmNode, realm.getValue()));
//      realms.add(realmNode);
//    }
    this.realms = realms;

  }

  public List<ClasspathEntryNode> toClasspathEntries(ClassRealmNode realm, List<String> classpath) {
    List<ClasspathEntryNode> result = new ArrayList<ClasspathEntryNode>();
    for(String entry : classpath) {
      result.add(new ArchiveEntryNode(realm, entry));
    }
    return result;
  }

  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);

    setControl(container);
    container.setLayout(new GridLayout(3, false));

    Label lblInstallationLocation = new Label(container, SWT.NONE);
    lblInstallationLocation.setText(Messages.ExternalInstallPage_lblInstallationLocation_text);

    location = new Text(container, SWT.BORDER);
    location.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateStatus();
      }
    });
    location.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    Button btnDirectory = new Button(container, SWT.NONE);
    btnDirectory.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectLocationAction();
      }
    });
    btnDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnDirectory.setText(Messages.ExternalInstallPage_btnDirectory_text);

    Label lblInstallationName = new Label(container, SWT.NONE);
    lblInstallationName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
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
    treeViewerLibrariries.setInput(realms);
    Tree treeLibraries = treeViewerLibrariries.getTree();
    treeLibraries.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 5));

    btnAddProject = new Button(container, SWT.NONE);
    btnAddProject.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        addProjectAction();
      }
    });
    btnAddProject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnAddProject.setText(Messages.ExternalInstallPage_btnAddProject_text);

    btnRemove = new Button(container, SWT.NONE);
    btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnRemove.setText(Messages.ExternalInstallPage_btnRemove_text);

    btnUp = new Button(container, SWT.NONE);
    btnUp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnUp.setText(Messages.ExternalInstallPage_btnUp_text);

    btnDown = new Button(container, SWT.NONE);
    btnDown.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnDown.setText(Messages.ExternalInstallPage_btnDown_text);

    Button btnRestoreDefault = new Button(container, SWT.NONE);
    btnRestoreDefault.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1));
    btnRestoreDefault.setText(Messages.ExternalInstallPage_btnRestoreDefault_text);

    if(original != null) {
      location.setText(original.getLocation());
      if(!original.isLegacy()) {
        name.setText(original.getName());
      }
    }

    updateButtonsState();
    updateStatus();
  }

  protected void updateButtonsState() {
    Object selection = getSelectedElement();

    // can move/remove classpath entries only
    boolean editEnabled = selection instanceof ClasspathEntryNode;
    btnUp.setEnabled(editEnabled);
    btnDown.setEnabled(editEnabled);
    btnRemove.setEnabled(editEnabled);

    // add project requires insertion point
    btnAddProject.setEnabled(selection != null);
  }

  private Object getSelectedElement() {
    return ((IStructuredSelection) treeViewerLibrariries.getSelection()).getFirstElement();
  }

  protected void addProjectAction() {
    List<Object> projects = new ArrayList<Object>();
    for(IMavenProjectFacade facade : MavenPlugin.getMavenProjectRegistry().getProjects()) {
      projects.add(facade.getProject());
    }
    ListSelectionDialog dialog = new ListSelectionDialog(getShell(), projects, new ArrayContentProvider(),
        new MavenProjectLabelProvider(), "Select projects to add:");
    dialog.setTitle("Project selection");
    dialog.setHelpAvailable(false);
    if(dialog.open() == Window.OK) {
      Object insertionPoint = getSelectedElement();
      if(insertionPoint instanceof ClassRealmNode) {
        ClassRealmNode realm = (ClassRealmNode) insertionPoint;
        for(Object object : dialog.getResult()) {
          realm.getClasspath().add(0, new ProjectEntryNode(realm, (IProject) object));
        }
      } else if(insertionPoint instanceof ClasspathEntryNode) {
        ClasspathEntryNode entry = (ClasspathEntryNode) insertionPoint;
        ClassRealmNode realm = entry.getRealm();
        int idx = realm.getIndex(entry);
        for(Object object : dialog.getResult()) {
          realm.getClasspath().add(idx, new ProjectEntryNode(realm, (IProject) object));
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

    if(location.getText().trim().isEmpty()) {
      setMessage("Enter the home directory of the Maven Installation");
      return;
    }

    if(!isValidMavenInstall(location.getText())) {
      setErrorMessage("Target is not a Maven Home");
      return;
    }

    if(name.getText().trim().isEmpty()) {
      setMessage("Enter a name for the Maven Installation");
      return;
    }

    // TODO name is unique

    setMessage(null);
    setPageComplete(true);
  }

  public AbstractMavenRuntime getResult() {
    return new MavenExternalRuntime(name.getText(), location.getText());
  }
}
