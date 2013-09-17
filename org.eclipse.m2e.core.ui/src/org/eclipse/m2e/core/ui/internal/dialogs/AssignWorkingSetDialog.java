/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.components.NestedProjectsComposite;
import org.eclipse.m2e.core.ui.internal.components.WorkingSetGroup;


/**
 * @since 1.5
 */
@SuppressWarnings("restriction")
public class AssignWorkingSetDialog extends TitleAreaDialog {

  private final IProject[] initialSelection;

  NestedProjectsComposite selectedProjects;

  private List<IWorkingSet> workingSets = new ArrayList<IWorkingSet>();

  Set<IProject> allWorkingSetProjects = getAllWorkingSetProjects();

  WorkingSetGroup workingSetGroup;

  public AssignWorkingSetDialog(Shell parentShell, IProject[] initialSelection) {
    super(parentShell);
    this.initialSelection = initialSelection;
  }

  private static Set<IProject> getAllWorkingSetProjects() {
    Set<IProject> projects = new HashSet<IProject>();
    IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
    for(IWorkingSet workingSet : manager.getAllWorkingSets()) {
      try {
        for(IAdaptable element : workingSet.getElements()) {
          IProject project = (IProject) element.getAdapter(IProject.class);
          if(project != null) {
            projects.add(project);
          }
        }
      } catch(IllegalStateException ignored) {
        // ignore bad/misconfigured working sets
      }
    }
    return projects;
  }

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
    btnFilterAssignedProjects.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectedProjects.refresh();
      }
    });

    final Button btnFilterClosedProjects = new Button(filtersComposite, SWT.CHECK);
    btnFilterClosedProjects.setText(Messages.AssignWorkingSetDialog_btnFilterClosedProjects_text);
    btnFilterClosedProjects.setSelection(true);
    btnFilterClosedProjects.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectedProjects.refresh();
      }
    });

    this.selectedProjects = new NestedProjectsComposite(composite, SWT.NONE, initialSelection) {
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

      @Override
      protected void createButtons(Composite selectionActionComposite) {
        super.createButtons(selectionActionComposite);

        Label label = new Label(selectionActionComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        Button btnAssign = new Button(selectionActionComposite, SWT.NONE);
        btnAssign.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnAssign.setText(Messages.AssignWorkingSetDialog_btnAssign_text);
        btnAssign.addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            assignWorkingSets();
            reset();
          }
        });
      }
    };
    selectedProjects.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

    this.workingSetGroup = new WorkingSetGroup(composite, workingSets, getShell());

    return area;
  }

  public IWorkingSet[] getWorkingSets() {
    return workingSets.toArray(new IWorkingSet[workingSets.size()]);
  }

  public IProject[] getSelectedProjects() {
    return selectedProjects.getSelectedProjects();
  }

  public void assignWorkingSets() {
    IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
    IWorkingSet[] workingSets = getWorkingSets();
    for(IProject project : getSelectedProjects()) {
      manager.addToWorkingSets(project, workingSets);
      allWorkingSetProjects.add(project);
    }
  }

}
