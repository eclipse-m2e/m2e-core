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
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDisovery;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;


@SuppressWarnings("restriction")
public abstract class AbstractMavenProjectWizard extends Wizard {

  protected IStructuredSelection selection;

  protected ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();

  protected List<IWorkingSet> workingSets = new ArrayList<IWorkingSet>();

  private IMavenDisovery discovery;

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
    this.importConfiguration = new ProjectImportConfiguration();
    this.discovery = M2EUIPluginActivator.getDefault().getMavenDiscovery();
    IWorkingSet workingSet = SelectionUtil.getSelectedWorkingSet(selection);
    if(workingSet != null) {
      this.workingSets.add(workingSet);
    }
  }

  @Override
  public void dispose() {
    M2EUIPluginActivator.getDefault().ungetMavenDiscovery(discovery);
    super.dispose();
  }

  public ProjectImportConfiguration getProjectImportConfiguration() {
    return importConfiguration;
  }

  public IMavenDisovery getDiscovery() {
    return discovery;
  }
}
