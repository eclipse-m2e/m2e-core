/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscovery;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;


@SuppressWarnings("restriction")
public abstract class AbstractMavenProjectWizard extends Wizard {

  protected IStructuredSelection selection;

  protected ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();

  protected List<IWorkingSet> workingSets = new ArrayList<>();

  private IMavenDiscovery discovery;

  private IMavenDiscoveryUI pageFactory;

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
    this.importConfiguration = new ProjectImportConfiguration();
    this.discovery = M2EUIPluginActivator.getDefault().getMavenDiscovery();
    this.pageFactory = M2EUIPluginActivator.getDefault().getImportWizardPageFactory();
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

  public IMavenDiscovery getDiscovery() {
    return discovery;
  }

  public IMavenDiscoveryUI getPageFactory() {
    return pageFactory;
  }
}
