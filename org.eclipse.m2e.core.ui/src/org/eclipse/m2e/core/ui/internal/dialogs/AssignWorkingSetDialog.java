/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.dialogs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.WorkingSets;
import org.eclipse.m2e.core.ui.internal.components.NestedProjectsComposite;


/**
 * @since 1.5
 */
@SuppressWarnings("restriction")
public class AssignWorkingSetDialog extends TitleAreaDialog {

  private final IProject[] initialSelection;

  NestedProjectsComposite selectedProjects;

  Set<IProject> allWorkingSetProjects = new HashSet<>(WorkingSets.getProjects());

  Combo workingSetCombo;

  String workingSetName;

  public AssignWorkingSetDialog(Shell parentShell, IProject[] initialSelection) {
    super(parentShell);
    this.initialSelection = initialSelection;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    setTitle(Messages.AssignWorkingSetDialog_title);

    Composite area = (Composite) super.createDialogArea(parent);

    Composite composite = new Composite(area, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    Composite filtersComposite = new Composite(composite, SWT.NONE);
    filtersComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
    GridLayout gl_filtersComposite = new GridLayout(4, false);
    gl_filtersComposite.verticalSpacing = 0;
    gl_filtersComposite.marginWidth = 0;
    gl_filtersComposite.marginHeight = 0;
    filtersComposite.setLayout(gl_filtersComposite);

    final Button btnFilterAssignedProjects = new Button(filtersComposite, SWT.CHECK);
    btnFilterAssignedProjects.setText(Messages.AssignWorkingSetDialog_btnFilterAssignedProjects_text);
    btnFilterAssignedProjects.setSelection(true);
    btnFilterAssignedProjects
        .addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> selectedProjects.refresh()));

    final Button btnFilterClosedProjects = new Button(filtersComposite, SWT.CHECK);
    btnFilterClosedProjects.setText(Messages.AssignWorkingSetDialog_btnFilterClosedProjects_text);
    btnFilterClosedProjects.setSelection(true);
    btnFilterClosedProjects
        .addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> selectedProjects.refresh()));

    this.selectedProjects = new NestedProjectsComposite(composite, SWT.NONE, initialSelection, false) {
      @Override
      protected boolean isInteresting(IProject project) throws CoreException {
        if(btnFilterClosedProjects.getSelection() && !project.isAccessible()) {
          return false;
        }
        if(project.isAccessible() && !project.hasNature(IMavenConstants.NATURE_ID)) {
          // project.hasNature throws an exception for inaccessible projects
          return false;
        }
        return !btnFilterAssignedProjects.getSelection() || !allWorkingSetProjects.contains(project);
      }
    };
    selectedProjects.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

    Composite workingSetComposite = new Composite(composite, SWT.NONE);
    workingSetComposite.setLayout(new GridLayout(3, false));
    workingSetComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));

    Label lblNewLabel = new Label(workingSetComposite, SWT.NONE);
    lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    lblNewLabel.setText(Messages.AssignWorkingSetDialog_lblWorkingSet);

    workingSetCombo = new Combo(workingSetComposite, SWT.BORDER);
    workingSetCombo.addModifyListener(e -> workingSetName = workingSetCombo.getText());
    GridData gd_workingSetName = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_workingSetName.horizontalIndent = 10;
    workingSetCombo.setLayoutData(gd_workingSetName);
    workingSetCombo.setItems(WorkingSets.getWorkingSets());

    selectedProjects.addSelectionChangeListener(event -> {
      IProject selection = selectedProjects.getSelection();
      if(selection != null && workingSetCombo.getSelectionIndex() < 0) {
        workingSetCombo.setText(selection.getName());
      }
    });

    Button btnAssign = new Button(workingSetComposite, SWT.NONE);
    btnAssign.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnAssign.setText(Messages.AssignWorkingSetDialog_btnAssign_text);
    btnAssign.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      assignWorkingSets();
      selectedProjects.reset();
    }));

    return area;
  }

  public void assignWorkingSets() {
    IProject[] projects = selectedProjects.getSelectedProjects();
    if(projects != null && projects.length > 0 && workingSetName != null && !workingSetName.isEmpty()) {
      WorkingSets.addToWorkingSet(projects, workingSetName);
      allWorkingSetProjects.addAll(Arrays.asList(projects));
    }
  }
}
