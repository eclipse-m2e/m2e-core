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

import org.apache.maven.project.MavenProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.composites.PomHierarchyComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


public class ExcludeWizardPage extends UserInputWizardPage implements SelectionListener, ISelectionChangedListener {

  private PomHierarchyComposite pomHierarchy;

  private Button currentPom;

  private Button hierarchy;

  private IMavenProjectFacade facade;

  private CLabel label;

  protected ExcludeWizardPage(IMavenProjectFacade facade) {
    super("Place to exclude");
    this.facade = facade;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    setControl(composite);
    composite.setLayout(new GridLayout(1, false));

    currentPom = new Button(composite, SWT.RADIO);
    currentPom.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    currentPom.setText("Selected pom");
    currentPom.setSelection(true);
    currentPom.addSelectionListener(this);

    hierarchy = new Button(composite, SWT.RADIO);
    hierarchy.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    hierarchy.setText("Choose from project hierarchy");
    hierarchy.addSelectionListener(this);

    pomHierarchy = new PomHierarchyComposite(composite, SWT.BORDER);
    GridData gd_pomHierarchy = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd_pomHierarchy.horizontalIndent = 15;
    pomHierarchy.setLayoutData(gd_pomHierarchy);
    pomHierarchy.setEnabled(false);
    pomHierarchy.addSelectionChangedListener(this);

    label = new CLabel(composite, SWT.NONE);
    label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    Display.getCurrent().asyncExec(new Runnable() {
      public void run() {
        pomHierarchy.computeHeirarchy(facade, getContainer());
        ((ExcludeArtifactRefactoring) getRefactoring()).setHierarchy(pomHierarchy.getHierarchy());
      }
    });
  }

  /* (non-Javadoc)
   * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
   */
  public void widgetSelected(SelectionEvent e) {
    if(e.getSource() == currentPom) {
      pomHierarchy.setEnabled(false);
    } else if(e.getSource() == hierarchy) {
      pomHierarchy.setEnabled(true);
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
   */
  public void widgetDefaultSelected(SelectionEvent e) {

  }

  private void setStatus(String msg) {
    if(msg == null) {
      label.setImage(null);
      label.setText("");
    } else {
      label.setText(msg);
      label.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
    }
  }

  private void updateState() {
    ExcludeArtifactRefactoring refactoring = (ExcludeArtifactRefactoring) getRefactoring();
    if(pomHierarchy.isEnabled()) {
      MavenProject project = fromSelection(pomHierarchy.getSelection());
      setPageComplete(project != null && project.getFile() != null);
      if(project != null && project.getFile() == null) {
        setStatus("Changes must occur within the workspace");
      } else {
        setStatus(null);
        refactoring.setExclusionPoint(fromSelection(pomHierarchy.getSelection()));
      }
    } else {
      setStatus(null);
      refactoring.setExclusionPoint(facade.getMavenProject());
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
   */
  public void selectionChanged(SelectionChangedEvent event) {
    updateState();
  }

  private MavenProject fromSelection(ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      Object obj = ((IStructuredSelection) selection).getFirstElement();
      if(obj instanceof MavenProject) {
        return (MavenProject) obj;
      }
    }
    return null;
  }
}
