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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Custom Archetype dialog
 *
 * @author Eugene Kuleshov
 */
public class CustomArchetypeDialog extends TitleAreaDialog {

  private static final String DIALOG_SETTINGS = CustomArchetypeDialog.class.getName();

  private static final String KEY_ARCHETYPE_GROUP_ID = "archetypeGroupId"; //$NON-NLS-1$

  private static final String KEY_ARCHETYPE_ARTIFACT_ID = "archetypeArtifactId"; //$NON-NLS-1$

  private static final String KEY_ARCHETYPE_VERSION = "archetypeVersion"; //$NON-NLS-1$

  private static final String KEY_REPOSITORY_URL = "repositoryUrl"; //$NON-NLS-1$

  private static final int MAX_HISTORY = 15;

  private final String title;

  private final String message;

  private Combo archetypeGroupIdCombo;

  private Combo archetypeArtifactIdCombo;

  private Combo archetypeVersionCombo;

  private Combo repositoryCombo;

  private IDialogSettings dialogSettings;

  private String archetypeArtifactId;

  private String archetypeGroupId;

  private String archetypeVersion;

  private String repositoryUrl;

  protected CustomArchetypeDialog(Shell shell, String title) {
    super(shell);
    this.title = title;
    this.message = Messages.CustomArchetypeDialog_message;
    setShellStyle(SWT.DIALOG_TRIM);

    IDialogSettings pluginSettings = M2EUIPluginActivator.getDefault().getDialogSettings();
    dialogSettings = pluginSettings.getSection(DIALOG_SETTINGS);
    if(dialogSettings == null) {
      dialogSettings = new DialogSettings(DIALOG_SETTINGS);
      pluginSettings.addSection(dialogSettings);
    }
  }

  @Override
  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    setTitle(title);
    setMessage(message);
    return control;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite1 = (Composite) super.createDialogArea(parent);

    Composite composite = new Composite(composite1, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginTop = 7;
    gridLayout.marginWidth = 12;
    gridLayout.numColumns = 2;
    composite.setLayout(gridLayout);

    Label archetypeGroupIdLabel = new Label(composite, SWT.NONE);
    archetypeGroupIdLabel.setText(Messages.CustomArchetypeDialog_lblArchetypegroupId);

    archetypeGroupIdCombo = new Combo(composite, SWT.NONE);
    GridData archetypeGroupIdComboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    archetypeGroupIdComboData.widthHint = 350;
    archetypeGroupIdCombo.setLayoutData(archetypeGroupIdComboData);
    archetypeGroupIdCombo.setItems(getSavedValues(KEY_ARCHETYPE_GROUP_ID));
    archetypeGroupIdCombo.setData("name", "archetypeGroupId"); //$NON-NLS-1$ //$NON-NLS-2$

    Label archetypeArtifactIdLabel = new Label(composite, SWT.NONE);
    archetypeArtifactIdLabel.setText(Messages.CustomArchetypeDialog_lblArchetypeartifactid);

    archetypeArtifactIdCombo = new Combo(composite, SWT.NONE);
    archetypeArtifactIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    archetypeArtifactIdCombo.setItems(getSavedValues(KEY_ARCHETYPE_ARTIFACT_ID));
    archetypeArtifactIdCombo.setData("name", "archetypeArtifactId"); //$NON-NLS-1$ //$NON-NLS-2$

    Label archetypeVersionLabel = new Label(composite, SWT.NONE);
    archetypeVersionLabel.setText(Messages.CustomArchetypeDialog_lblArchetypeversion);

    archetypeVersionCombo = new Combo(composite, SWT.NONE);
    archetypeVersionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    archetypeVersionCombo.setItems(getSavedValues(KEY_ARCHETYPE_VERSION));
    archetypeVersionCombo.setData("name", "archetypeVersion"); //$NON-NLS-1$ //$NON-NLS-2$

    Label repositoryLabel = new Label(composite, SWT.NONE);
    repositoryLabel.setText(Messages.CustomArchetypeDialog_lblRepo);

    repositoryCombo = new Combo(composite, SWT.NONE);
    repositoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    repositoryCombo.setItems(getSavedValues(KEY_REPOSITORY_URL));
    repositoryCombo.setData("name", "repository"); //$NON-NLS-1$ //$NON-NLS-2$

    ModifyListener modifyListener = e -> update();

    archetypeGroupIdCombo.addModifyListener(modifyListener);
    archetypeArtifactIdCombo.addModifyListener(modifyListener);
    archetypeVersionCombo.addModifyListener(modifyListener);
    repositoryCombo.addModifyListener(modifyListener);

//    fullIndexButton = new Button(composite, SWT.CHECK);
//    fullIndexButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
//    fullIndexButton.setText("&Full Index");
//    fullIndexButton.setSelection(true);

    return composite;
  }

  private String[] getSavedValues(String key) {
    String[] array = dialogSettings.getArray(key);
    return array == null ? new String[0] : array;
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(title);
  }

  @Override
  public void create() {
    super.create();
    getButton(IDialogConstants.OK_ID).setEnabled(false);
  }

  @Override
  protected void okPressed() {
    archetypeArtifactId = archetypeArtifactIdCombo.getText().trim();
    archetypeGroupId = archetypeGroupIdCombo.getText().trim();
    archetypeVersion = archetypeVersionCombo.getText().trim();
    repositoryUrl = repositoryCombo.getText().trim();

    saveValue(KEY_ARCHETYPE_GROUP_ID, archetypeGroupId);
    saveValue(KEY_ARCHETYPE_ARTIFACT_ID, archetypeArtifactId);
    saveValue(KEY_ARCHETYPE_VERSION, archetypeVersion);
    saveValue(KEY_REPOSITORY_URL, repositoryUrl);

    super.okPressed();
  }

  public String getArchetypeGroupId() {
    return archetypeGroupId;
  }

  public String getArchetypeArtifactId() {
    return archetypeArtifactId;
  }

  public String getArchetypeVersion() {
    return archetypeVersion;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  private void saveValue(String key, String value) {
    List<String> dirs = new ArrayList<>();
    dirs.addAll(Arrays.asList(getSavedValues(key)));

    dirs.remove(value);
    dirs.add(0, value);

    if(dirs.size() > MAX_HISTORY) {
      dirs = dirs.subList(0, MAX_HISTORY);
    }

    dialogSettings.put(key, dirs.toArray(new String[dirs.size()]));
  }

  void update() {
    boolean isValid = isValid();
    // verifyButton.setEnabled(isValid);
    getButton(IDialogConstants.OK_ID).setEnabled(isValid);
  }

  private boolean isValid() {
    setErrorMessage(null);
    setMessage(null, IStatus.WARNING);

    if(archetypeGroupIdCombo.getText().trim().length() == 0) {
      setErrorMessage(Messages.CustomArchetypeDialog_error_grid);
      return false;
    }

    if(archetypeArtifactIdCombo.getText().trim().length() == 0) {
      setErrorMessage(Messages.CustomArchetypeDialog_error_artid);
      return false;
    }

    if(archetypeVersionCombo.getText().trim().length() == 0) {
      setErrorMessage(Messages.CustomArchetypeDialog_error_version);
      return false;
    }

    // TODO check if archetype available locally

    return true;
  }

}
