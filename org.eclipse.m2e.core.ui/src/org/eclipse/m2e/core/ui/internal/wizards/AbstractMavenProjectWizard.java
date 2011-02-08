/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/


package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;


public abstract class AbstractMavenProjectWizard extends Wizard {

  protected IStructuredSelection selection;

  protected ProjectImportConfiguration importConfiguration;

  protected List<IWorkingSet> workingSets = new ArrayList<IWorkingSet>();

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
    this.importConfiguration = new ProjectImportConfiguration();
    this.workingSets.add(SelectionUtil.getSelectedWorkingSet(selection));
  }

}
