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

package org.eclipse.m2e.refactoring.exclude;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.components.PomHierarchyComposite;
import org.eclipse.m2e.core.ui.internal.util.ParentHierarchyEntry;
import org.eclipse.m2e.refactoring.Messages;


@SuppressWarnings("restriction")
public class ExcludeWizardPage extends UserInputWizardPage implements SelectionListener, ISelectionChangedListener {

  private PomHierarchyComposite pomHierarchy;

  private Button currentPom;

  private Button hierarchy;

  private IMavenProjectFacade facade;

  private CLabel status;

  protected ExcludeWizardPage(IMavenProjectFacade facade) {
    super(Messages.ExcludeWizardPage_title);
    this.facade = facade;
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    setControl(composite);
    composite.setLayout(new GridLayout(1, false));

    Label label = new Label(composite, SWT.NONE);
    label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
    label.setText(Messages.ExcludeWizardPage_location);

    currentPom = new Button(composite, SWT.RADIO);
    GridData gd_currentPom = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
    gd_currentPom.horizontalIndent = 15;
    currentPom.setLayoutData(gd_currentPom);
    currentPom.setText(facade.getArtifactKey().toString());
    currentPom.setSelection(true);
    currentPom.addSelectionListener(this);

    hierarchy = new Button(composite, SWT.RADIO);
    GridData gd_hierarchy = new GridData(SWT.LEFT, SWT.TOP, false, false);
    gd_hierarchy.horizontalIndent = 15;
    hierarchy.setLayoutData(gd_hierarchy);
    hierarchy.setText(Messages.ExcludeWizardPage_selectFromHierarchy);
    hierarchy.addSelectionListener(this);

    pomHierarchy = new PomHierarchyComposite(composite, SWT.BORDER);
    GridData gd_pomHierarchy = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd_pomHierarchy.horizontalIndent = 35;
    pomHierarchy.setLayoutData(gd_pomHierarchy);
    pomHierarchy.setEnabled(false);
    pomHierarchy.addSelectionChangedListener(this);

    status = new CLabel(composite, SWT.WRAP);
    status.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

    pomHierarchy.computeHeirarchy(facade, null);
    getRefactoring().setHierarchy(pomHierarchy.getHierarchy());
  }

  @Override
  protected ExcludeArtifactRefactoring getRefactoring() {
    return (ExcludeArtifactRefactoring) super.getRefactoring();
  }

  @Override
  public void widgetSelected(SelectionEvent e) {
    if(e.getSource() == currentPom) {
      pomHierarchy.setEnabled(false);
    } else if(e.getSource() == hierarchy) {
      pomHierarchy.setEnabled(true);
    }
    updateState();
  }

  @Override
  public void widgetDefaultSelected(SelectionEvent e) {

  }

  private void setStatus(String msg) {
    if(msg == null) {
      status.setImage(null);
      status.setText(""); //$NON-NLS-1$
    } else {
      status.setText(msg);
      status.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
    }
  }

  private void updateState() {
    ParentHierarchyEntry project;
    if(hierarchy.getSelection()) {
      project = pomHierarchy.fromSelection();
    } else {
      project = pomHierarchy.getProject();
    }
    updateStatusBar(project);
    getRefactoring().setExclusionPoint(project);
  }

  private void updateStatusBar(ParentHierarchyEntry project) {
    if(project == null) {
      setStatus(Messages.ExcludeWizardPage_errorSelectPom);
      setPageComplete(false);
    } else if(project.getResource() == null) {
      setStatus(Messages.ExcludeWizardPage_errorNonWorkspacePom);
      setPageComplete(false);
    } else if((project = isAboveDependencyManagement(project)) != null) {
      setStatus(NLS.bind(Messages.ExcludeWizardPage_dependenciesManagedIn, toString(project)));
      setPageComplete(false);
    } else {
      setStatus(null);
      setPageComplete(true);
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    updateState();
  }

  private ParentHierarchyEntry isAboveDependencyManagement(ParentHierarchyEntry project) {
    for(ParentHierarchyEntry cProject : pomHierarchy.getHierarchy()) {
      if(project == cProject) {
        return null;
      }
      DependencyManagement dependencyManagement = cProject.getProject().getOriginalModel().getDependencyManagement();
      if(dependencyManagement != null && !dependencyManagement.getDependencies().isEmpty()) {
        return cProject;
      }
    }
    return null;
  }

  private static String toString(ParentHierarchyEntry project) {
    Model model = project.getProject().getModel();
    return NLS.bind("{0}:{1}:{2}", new String[] {model.getGroupId(), model.getArtifactId(), model.getVersion()}); //$NON-NLS-1$
  }
}
