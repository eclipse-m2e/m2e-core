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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;


/**
 * Wizard page to enter parameters required for artifact installation.
 *
 * @author Guillaume Sauthier
 * @author Mike Haller
 * @author Eugene Kuleshov
 */
public class MavenInstallFileArtifactWizardPage extends WizardPage {
  private static final Logger log = LoggerFactory.getLogger(MavenInstallFileArtifactWizardPage.class);

  Text artifactFileNameText;

  Text pomFileNameText;

  private Combo groupIdCombo;

  private Combo artifactIdCombo;

  private Combo versionCombo;

  private Combo packagingCombo;

  private Combo classifierCombo;

  Button createChecksumButton;

  Button generatePomButton;

  private final IFile file;

  public MavenInstallFileArtifactWizardPage(IFile file) {
    super("mavenInstallFileWizardPage");
    this.file = file;
    this.setTitle(Messages.MavenInstallFileArtifactWizardPage_title);
    this.setDescription(Messages.MavenInstallFileArtifactWizardPage_desc);
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(3, false));
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    ModifyListener modifyingListener = e -> pageChanged();

    Label artifactFileNameLabel = new Label(container, SWT.NONE);
    artifactFileNameLabel.setText(Messages.MavenInstallFileArtifactWizardPage_lblFileName);

    artifactFileNameText = new Text(container, SWT.BORDER);
    artifactFileNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    artifactFileNameText.setData("name", "artifactFileNametext"); //$NON-NLS-1$ //$NON-NLS-2$
    artifactFileNameText.addModifyListener(e -> {
      updateFileName(getArtifactFileName());
      pageChanged();
    });

    final Button artifactFileNameButton = new Button(container, SWT.NONE);
    artifactFileNameButton.setLayoutData(new GridData());
    artifactFileNameButton.setData("name", "externalPomFileButton"); //$NON-NLS-1$ //$NON-NLS-2$
    artifactFileNameButton.setText(Messages.MavenInstallFileArtifactWizardPage_btnFilename);
    artifactFileNameButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      FileDialog fileDialog = new FileDialog(artifactFileNameButton.getShell());
      fileDialog.setText(Messages.MavenInstallFileArtifactWizardPage_file_title);
      fileDialog.setFileName(artifactFileNameText.getText());
      String name = fileDialog.open();
      if(name != null) {
        updateFileName(name);
      }
    }));

    Label pomFileNameLabel = new Label(container, SWT.NONE);
    pomFileNameLabel.setText(Messages.MavenInstallFileArtifactWizardPage_lblPom);

    pomFileNameText = new Text(container, SWT.BORDER);
    pomFileNameText.setData("name", "pomFileNameText"); //$NON-NLS-1$ //$NON-NLS-2$
    pomFileNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    pomFileNameText.addModifyListener(e -> {
      generatePomButton.setSelection(getPomFileName().length() == 0);
      pageChanged();
    });

    final Button pomFileNameButton = new Button(container, SWT.NONE);
    pomFileNameButton.setLayoutData(new GridData());
    pomFileNameButton.setData("name", "externalPomFileButton"); //$NON-NLS-1$ //$NON-NLS-2$
    pomFileNameButton.setText(Messages.MavenInstallFileArtifactWizardPage_btnPom);
    pomFileNameButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      FileDialog fileDialog = new FileDialog(pomFileNameButton.getShell());
      fileDialog.setText(Messages.MavenInstallFileArtifactWizardPage_file_title);
      fileDialog.setFileName(pomFileNameText.getText());
      String res = fileDialog.open();
      if(res != null) {
        updatePOMFileName(res);
      }
    }));

    new Label(container, SWT.NONE);

    generatePomButton = new Button(container, SWT.CHECK);
    generatePomButton.setData("name", "generatePomButton"); //$NON-NLS-1$ //$NON-NLS-2$
    generatePomButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    generatePomButton.setText(Messages.MavenInstallFileArtifactWizardPage_btnGenerate);
    generatePomButton.setSelection(true);
    new Label(container, SWT.NONE);

    createChecksumButton = new Button(container, SWT.CHECK);
    createChecksumButton.setData("name", "createChecksumButton"); //$NON-NLS-1$ //$NON-NLS-2$
    createChecksumButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    createChecksumButton.setText(Messages.MavenInstallFileArtifactWizardPage_btnChecksum);
    createChecksumButton.setSelection(true);

    Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
    GridData separatorData = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
    separatorData.verticalIndent = 5;
    separator.setLayoutData(separatorData);

    Label groupIdlabel = new Label(container, SWT.NONE);
    groupIdlabel.setText(Messages.MavenInstallFileArtifactWizardPage_lblgroupid);

    groupIdCombo = new Combo(container, SWT.NONE);
    groupIdCombo.setData("name", "groupIdCombo"); //$NON-NLS-1$ //$NON-NLS-2$
    groupIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    groupIdCombo.addModifyListener(modifyingListener);
    new Label(container, SWT.NONE);

    Label artifactIdLabel = new Label(container, SWT.NONE);
    artifactIdLabel.setText(Messages.MavenInstallFileArtifactWizardPage_lblArtifact);

    artifactIdCombo = new Combo(container, SWT.NONE);
    artifactIdCombo.setData("name", "artifactIdCombo"); //$NON-NLS-1$ //$NON-NLS-2$
    artifactIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    artifactIdCombo.addModifyListener(modifyingListener);
    new Label(container, SWT.NONE);

    Label versionLabel = new Label(container, SWT.NONE);
    versionLabel.setText(Messages.MavenInstallFileArtifactWizardPage_lblVersion);

    versionCombo = new Combo(container, SWT.NONE);
    versionCombo.setData("name", "versionCombo"); //$NON-NLS-1$ //$NON-NLS-2$
    versionCombo.setText(MavenArtifactComponent.DEFAULT_VERSION);
    GridData versionComboData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    versionComboData.widthHint = 150;
    versionCombo.setLayoutData(versionComboData);
    versionCombo.addModifyListener(modifyingListener);

    Label packagingLabel = new Label(container, SWT.NONE);
    packagingLabel.setText(Messages.MavenInstallFileArtifactWizardPage_lblPackaging);

    packagingCombo = new Combo(container, SWT.NONE);
    packagingCombo.setData("name", "packagingCombo"); //$NON-NLS-1$ //$NON-NLS-2$
    packagingCombo.setText(MavenArtifactComponent.DEFAULT_PACKAGING);
    packagingCombo.setItems(MavenArtifactComponent.PACKAGING_OPTIONS);
    GridData packagingComboData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    packagingComboData.widthHint = 150;
    packagingCombo.setLayoutData(packagingComboData);
    packagingCombo.addModifyListener(modifyingListener);

    Label classifierLabel = new Label(container, SWT.NONE);
    classifierLabel.setText(Messages.MavenInstallFileArtifactWizardPage_lblClassifier);

    classifierCombo = new Combo(container, SWT.NONE);
    classifierCombo.setData("name", "classifierText"); //$NON-NLS-1$ //$NON-NLS-2$
    classifierCombo.setItems("sources", "javadoc"); //$NON-NLS-1$ //$NON-NLS-2$
    GridData classifierTextData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    classifierTextData.widthHint = 150;
    classifierCombo.setLayoutData(classifierTextData);
    classifierCombo.addModifyListener(e -> generatePomButton.setSelection(getClassifier().length() == 0));

    if(file != null) {
      updateFileName(file.getLocation().toOSString());
    }

    setControl(container);
  }

  void updateFileName(String fileName) {
    if(!getArtifactFileName().equals(fileName)) {
      artifactFileNameText.setText(fileName);
    }

    File file = new File(fileName);
    if(!file.exists() || !file.isFile()) {
      return;
    }

    if(fileName.endsWith(".jar") || fileName.endsWith(".war") || fileName.endsWith(".ear")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      packagingCombo.setText(fileName.substring(fileName.length() - 3));
    }

    int n = fileName.lastIndexOf('.');
    if(n > -1) {
      String pomFileName = fileName.substring(0, n) + ".pom"; //$NON-NLS-1$
      if(new File(pomFileName).exists()) {
        pomFileNameText.setText(pomFileName);
      }
    } else {
      pomFileNameText.setText(""); //$NON-NLS-1$
    }

    try {
      IndexedArtifactFile iaf = MavenPlugin.getIndexManager().getAllIndexes().identify(file);
      if(iaf != null) {
        groupIdCombo.setText(iaf.group);
        artifactIdCombo.setText(iaf.artifact);
        versionCombo.setText(iaf.version);
        if(iaf.classifier != null) {
          classifierCombo.setText(iaf.classifier);
        }

        String name = iaf.group + ":" + iaf.artifact + "-" + iaf.version // //$NON-NLS-1$ //$NON-NLS-2$
            + (iaf.classifier == null ? "" : iaf.classifier); //$NON-NLS-1$
        setMessage(NLS.bind(Messages.MavenInstallFileArtifactWizardPage_message, name), WARNING);
        return;
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }

    if(n > -1) {
      String pomFileName = fileName.substring(0, n) + ".pom"; //$NON-NLS-1$
      if(new File(pomFileName).exists()) {
        pomFileNameText.setText(pomFileName);
        readPOMFile(pomFileName);
      }
    }

    ArtifactKey artifactKey = SelectionUtil.getType(file, ArtifactKey.class);
    if(artifactKey != null) {
      groupIdCombo.setText(artifactKey.getGroupId());
      artifactIdCombo.setText(artifactKey.getArtifactId());
      versionCombo.setText(artifactKey.getVersion());
      if(artifactKey.getClassifier() != null) {
        classifierCombo.setText(artifactKey.getClassifier());
      }
    }
  }

  private void updatePOMFileName(String fileName) {
    if(!getPomFileName().equals(fileName))
      pomFileNameText.setText(fileName);

    File file = new File(fileName);
    if(!file.exists() || !file.isFile() || !fileName.endsWith(".pom")) { //$NON-NLS-1$
      return;
    }

    readPOMFile(fileName);
  }

  private void readPOMFile(String fileName) {
    try {
      IMaven maven = MavenPlugin.getMaven();
      MavenProject mavenProject = maven.readProject(new File(fileName), null);

      groupIdCombo.setText(mavenProject.getGroupId());
      artifactIdCombo.setText(mavenProject.getArtifactId());
      versionCombo.setText(mavenProject.getVersion());
      packagingCombo.setText(mavenProject.getPackaging());
      return;

    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  void pageChanged() {
    String artifactFileName = getArtifactFileName();
    if(artifactFileName.length() == 0) {
      updateStatus(Messages.MavenInstallFileArtifactWizardPage_error_no_name);
      return;
    }

    File file = new File(artifactFileName);
    if(!file.exists() || !file.isFile()) {
      updateStatus(Messages.MavenInstallFileArtifactWizardPage_error_missing);
      return;
    }

    String pomFileName = getPomFileName();
    if(pomFileName.length() > 0) {
      if(!new File(pomFileName).exists()) {
        updateStatus(Messages.MavenInstallFileArtifactWizardPage_error_missingpom);
        return;
      }
    }

    if(getGroupId().length() == 0) {
      updateStatus(Messages.MavenInstallFileArtifactWizardPage_error_groupid);
      return;
    }

    if(getArtifactId().length() == 0) {
      updateStatus(Messages.MavenInstallFileArtifactWizardPage_error_artifactid);
      return;
    }

    if(getVersion().length() == 0) {
      updateStatus(Messages.MavenInstallFileArtifactWizardPage_error_version);
      return;
    }

    if(getPackaging().length() == 0) {
      updateStatus(Messages.MavenInstallFileArtifactWizardPage_error_packaging);
      return;
    }

    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getArtifactFileName() {
    return artifactFileNameText.getText().trim();
  }

  public String getPomFileName() {
    return pomFileNameText.getText().trim();
  }

  public String getGroupId() {
    return groupIdCombo.getText().trim();
  }

  public String getArtifactId() {
    return artifactIdCombo.getText().trim();
  }

  public String getVersion() {
    return versionCombo.getText().trim();
  }

  public String getPackaging() {
    return packagingCombo.getText().trim();
  }

  public String getClassifier() {
    return this.classifierCombo.getText().trim();
  }

  public boolean isGeneratePom() {
    return generatePomButton.getSelection();
  }

  public boolean isCreateChecksum() {
    return createChecksumButton.getSelection();
  }

}
