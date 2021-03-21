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

package org.eclipse.m2e.scm.internal.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.core.ui.internal.wizards.AbstractMavenProjectWizard;
import org.eclipse.m2e.core.ui.internal.wizards.MavenDependenciesWizardPage;
import org.eclipse.m2e.core.ui.internal.wizards.MavenProjectWizardLocationPage;
import org.eclipse.m2e.scm.MavenProjectPomScanner;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.internal.Messages;


/**
 * A wizard used to import projects for Maven artifacts
 * 
 * @author Eugene Kuleshov
 */
public class MavenMaterializePomWizard extends AbstractMavenProjectWizard implements IImportWizard, INewWizard {

  MavenDependenciesWizardPage selectionPage;

  MavenProjectWizardLocationPage locationPage;

  Button checkOutAllButton;

  Button useDeveloperConnectionButton;

  // TODO replace with ArtifactKey
  private Dependency[] dependencies;

  public MavenMaterializePomWizard() {
    importConfiguration = new ProjectImportConfiguration();
    setNeedsProgressMonitor(true);
    setWindowTitle(Messages.MavenMaterializePomWizard_title);
  }

  public void setDependencies(Dependency[] dependencies) {
    this.dependencies = dependencies;
  }

  public Dependency[] getDependencies() {
    return dependencies;
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    super.init(workbench, selection);

    ArrayList<Dependency> dependencies = new ArrayList<Dependency>();

    for(Object element : selection) {
      ArtifactKey artifactKey = SelectionUtil.getType(element, ArtifactKey.class);
      if(artifactKey != null) {
        Dependency d = new Dependency();
        d.setGroupId(artifactKey.getGroupId());
        d.setArtifactId(artifactKey.getArtifactId());
        d.setVersion(artifactKey.getVersion());
        d.setClassifier(artifactKey.getClassifier());
        dependencies.add(d);
      }
    }

    setDependencies(dependencies.toArray(new Dependency[dependencies.size()]));
  }

  public void addPages() {
    selectionPage = new MavenDependenciesWizardPage(importConfiguration, //
        Messages.MavenMaterializePomWizard_dialog_title, //
        Messages.MavenMaterializePomWizard_dialog_message) {
      protected void createAdvancedSettings(Composite composite, GridData gridData) {
        checkOutAllButton = new Button(composite, SWT.CHECK);
        checkOutAllButton.setText(Messages.MavenMaterializePomWizard_btnCheckout);
        checkOutAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

        useDeveloperConnectionButton = new Button(composite, SWT.CHECK);
        useDeveloperConnectionButton.setText(Messages.MavenMaterializePomWizard_btnDev);
        useDeveloperConnectionButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

        super.createAdvancedSettings(composite, gridData);
      }
    };
    selectionPage.setDependencies(dependencies);

    locationPage = new MavenProjectWizardLocationPage(
        importConfiguration, //
        Messages.MavenMaterializePomWizard_location_title, Messages.MavenMaterializePomWizard_location_message,
        workingSets);
    locationPage.setLocationPath(SelectionUtil.getSelectedLocation(selection));

    addPage(selectionPage);
    addPage(locationPage);
  }

  public boolean canFinish() {
    return super.canFinish();
  }

  public boolean performFinish() {
    if(!canFinish()) {
      return false;
    }

    final Dependency[] dependencies = selectionPage.getDependencies();

    final boolean checkoutAllProjects = checkOutAllButton.getSelection();
    final boolean developer = useDeveloperConnectionButton.getSelection();

    MavenProjectCheckoutJob job = new MavenProjectCheckoutJob(importConfiguration, checkoutAllProjects, workingSets) {
      protected List<MavenProjectScmInfo> getProjects(IProgressMonitor monitor) throws InterruptedException {
        MavenProjectPomScanner<MavenProjectScmInfo> scanner = new MavenProjectPomScanner<MavenProjectScmInfo>(
            developer, dependencies);
        scanner.run(monitor);
        // XXX handle errors/warnings

        return scanner.getProjects();
      }
    };

    if(!locationPage.isInWorkspace()) {
      job.setLocation(locationPage.getLocationPath().toFile());
    }

    job.schedule();

    return true;
  }
}
