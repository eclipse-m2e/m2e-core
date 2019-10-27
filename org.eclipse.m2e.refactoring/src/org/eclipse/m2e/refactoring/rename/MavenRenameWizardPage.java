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

package org.eclipse.m2e.refactoring.rename;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.m2e.refactoring.Messages;


/**
 * @author Anton Kraev
 */
public class MavenRenameWizardPage extends UserInputWizardPage {
  private Text groupIdText;

  private Text artifactIdText;

  private Text versionText;

  private Button renameCheckbox;

  private String groupId;

  private String artifactId;

  private String version;

  private String newGroupId = ""; //$NON-NLS-1$

  private String newArtifactId = ""; //$NON-NLS-1$

  private String newVersion = ""; //$NON-NLS-1$

  private boolean renamed;

  protected MavenRenameWizardPage() {
    super("MavenRenameWizardPage"); //$NON-NLS-1$
    setDescription(Messages.MavenRenameWizardPage_desc);
    setTitle(Messages.MavenRenameWizardPage_title);
  }

  public void initialize(String groupId, String artifactID, String version) {
    this.groupId = newGroupId = nvl(groupId);
    this.artifactId = newArtifactId = nvl(artifactID);
    this.version = newVersion = nvl(version);
  }

  public String getNewGroupId() {
    return newGroupId;
  }

  public String getNewArtifactId() {
    return newArtifactId;
  }

  public String getNewVersion() {
    return newVersion;
  }

  @Override
  public boolean isPageComplete() {
    boolean renamedArtifact = !newArtifactId.equals(artifactId);
    renameCheckbox.setEnabled(renamedArtifact);
    if(!renamedArtifact) {
      renameCheckbox.setSelection(false);
      renamed = false;
    }
    return !newGroupId.equals(groupId) //
        || renamedArtifact //
        || !newVersion.equals(version) //
        || !isCurrentPage();
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 10;
    gridLayout.marginHeight = 10;
    composite.setLayout(gridLayout);
    initializeDialogUnits(composite);
    Dialog.applyDialogFont(composite);
    setControl(composite);

    Label groupIdLabel = new Label(composite, SWT.NONE);
    groupIdLabel.setLayoutData(new GridData());
    groupIdLabel.setText(Messages.MavenRenameWizardPage_lblGroupId);

    groupIdText = new Text(composite, SWT.BORDER);
    groupIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    groupIdText.setData("name", "groupId"); //$NON-NLS-1$ //$NON-NLS-2$

    Label artifactIdLabel = new Label(composite, SWT.NONE);
    artifactIdLabel.setLayoutData(new GridData());
    artifactIdLabel.setText(Messages.MavenRenameWizardPage_lblArtifactId);

    artifactIdText = new Text(composite, SWT.BORDER);
    artifactIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    artifactIdText.setData("name", "artifactId"); //$NON-NLS-1$ //$NON-NLS-2$

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setLayoutData(new GridData());
    versionLabel.setText(Messages.MavenRenameWizardPage_lblVersion);

    versionText = new Text(composite, SWT.BORDER);
    versionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    versionText.setData("name", "version"); //$NON-NLS-1$ //$NON-NLS-2$

    new Label(composite, SWT.NONE);

    renameCheckbox = new Button(composite, SWT.CHECK);
    renameCheckbox.setText(Messages.MavenRenameWizardPage_cbRenameWorkspace);
    renameCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    renameCheckbox.setData("name", "rename"); //$NON-NLS-1$ //$NON-NLS-2$
    renameCheckbox.setEnabled(false);
    renameCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        renamed = renameCheckbox.getSelection();
        getWizard().getContainer().updateButtons();
      }
    });

    ModifyListener listener = e -> {
      newGroupId = groupIdText.getText();
      newArtifactId = artifactIdText.getText();
      newVersion = versionText.getText();
      getWizard().getContainer().updateButtons();
    };

    groupIdText.setText(groupId);
    artifactIdText.setText(artifactId);
    versionText.setText(version);

    groupIdText.addModifyListener(listener);
    artifactIdText.addModifyListener(listener);
    versionText.addModifyListener(listener);
  }

  private String nvl(String str) {
    return str == null ? "" : str; //$NON-NLS-1$
  }

  public boolean getRenameEclipseProject() {
    return renamed;
  }

}
