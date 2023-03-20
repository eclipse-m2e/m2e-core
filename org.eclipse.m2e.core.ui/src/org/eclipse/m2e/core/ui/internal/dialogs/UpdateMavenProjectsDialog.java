/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.dialogs;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.components.NestedProjectsComposite;


public class UpdateMavenProjectsDialog extends TitleAreaDialog {

  private NestedProjectsComposite nestedProjectsComposite;

  private Button offlineModeBtn;

  private Button forceUpdateBtn;

  private final IProject[] initialSelection;

  private List<IProject> selectedProjects;

  private boolean offlineMode;

  /**
   * Force update of snapshots and releases from remote repositories
   */
  private boolean forceUpdateDependencies;

  /**
   * Update project configuration
   */
  private boolean updateConfiguration;

  /**
   * Perform full/clean build after project update
   */
  private boolean cleanProjects;

  /**
   * Perform refresh from local before doing anything else.
   */
  private boolean refreshFromLocal;

  protected String dialogTitle;

  protected String dialogMessage;

  public UpdateMavenProjectsDialog(Shell parentShell, IProject[] initialSelection) {
    super(parentShell);
    this.initialSelection = initialSelection;
    this.dialogTitle = Messages.UpdateMavenProjectDialog_title;
    this.dialogMessage = Messages.UpdateMavenProjectDialog_dialogMessage;
    offlineMode = MavenPlugin.getMavenConfiguration().isOffline();
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(getDialogTitle());
  }

  /**
   * Create contents of the dialog.
   *
   * @param parent
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    GridLayout gridLayout = (GridLayout) area.getLayout();
    gridLayout.verticalSpacing = 5;
    gridLayout.marginBottom = 5;
    gridLayout.marginRight = 5;
    gridLayout.marginLeft = 5;

    nestedProjectsComposite = new NestedProjectsComposite(area, SWT.NONE, initialSelection, true);
    nestedProjectsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    Composite optionsComposite = new Composite(area, SWT.NONE);
    optionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    GridLayout gl_optionsComposite = new GridLayout(1, false);
    gl_optionsComposite.marginHeight = 0;
    gl_optionsComposite.marginWidth = 0;
    optionsComposite.setLayout(gl_optionsComposite);

    offlineModeBtn = new Button(optionsComposite, SWT.CHECK);
    offlineModeBtn.setText(Messages.UpdateDepenciesDialog_offline);
    offlineModeBtn.setSelection(offlineMode);

    Button btnCheckButton = new Button(optionsComposite, SWT.CHECK);
    btnCheckButton.setEnabled(false);
    btnCheckButton.setSelection(true);
    btnCheckButton.setText(Messages.UpdateMavenProjectDialog_btnCheckButton_text);

    forceUpdateBtn = new Button(optionsComposite, SWT.CHECK);
    GridData gd_forceUpdateBtn = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
    gd_forceUpdateBtn.horizontalIndent = 15;
    forceUpdateBtn.setLayoutData(gd_forceUpdateBtn);
    forceUpdateBtn.setText(Messages.UpdateDepenciesDialog_forceUpdate);

    btnUpdateProjectConfiguration = new Button(optionsComposite, SWT.CHECK);
    btnUpdateProjectConfiguration.setSelection(true);
    btnUpdateProjectConfiguration.setText(Messages.UpdateMavenProjectDialog_btnUpdateProjectConfiguration_text);

    btnRefreshFromLocal = new Button(optionsComposite, SWT.CHECK);
    btnRefreshFromLocal.setSelection(true);
    btnRefreshFromLocal.setText(Messages.UpdateMavenProjectsDialog_btnRefreshFromLocal_text);

    btnCleanProjects = new Button(optionsComposite, SWT.CHECK);
    btnCleanProjects.setSelection(true);
    btnCleanProjects.setText(Messages.UpdateMavenProjectDialog_btnCleanProjects_text);

    setTitle(getDialogTitle());
    setMessage(getDialogMessage());
    Image image = MavenImages.WIZ_UPDATE_PROJECT.createImage();
    setTitleImage(image);
    area.addDisposeListener(e -> image.dispose());
    return area;
  }

  /**
   * Create contents of the button bar.
   *
   * @param parent
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected void okPressed() {
    selectedProjects = nestedProjectsComposite.getSelectedProjects();

    offlineMode = offlineModeBtn.getSelection();
    forceUpdateDependencies = forceUpdateBtn.getSelection();
    updateConfiguration = btnUpdateProjectConfiguration.getSelection();
    cleanProjects = btnCleanProjects.getSelection();
    refreshFromLocal = btnRefreshFromLocal.getSelection();
    super.okPressed();
  }

  public List<IProject> getSelectedProjects() {
    return selectedProjects;
  }

  public boolean isOffline() {
    return offlineMode;
  }

  public boolean isForceUpdateDependencies() {
    return forceUpdateDependencies;
  }

  public boolean isUpdateConfiguration() {
    return updateConfiguration;
  }

  public boolean isCleanProjects() {
    return cleanProjects;
  }

  public boolean isRefreshFromLocal() {
    return refreshFromLocal;
  }

  private Button btnUpdateProjectConfiguration;

  private Button btnCleanProjects;

  private Button btnRefreshFromLocal;

  /**
   * @return Returns the dialogTitle or an empty String if the value is null.
   */
  public String getDialogTitle() {
    if(dialogTitle == null) {
      dialogTitle = ""; //$NON-NLS-1$
    }
    return dialogTitle;
  }

  /**
   * @return Returns the dialogMessage or an empty String if the value is null.
   */
  public String getDialogMessage() {
    if(dialogMessage == null) {
      dialogMessage = ""; //$NON-NLS-1$
    }
    return dialogMessage;
  }

  /**
   * @param dialogTitle The dialogTitle to set.
   */
  public void setDialogTitle(String dialogTitle) {
    this.dialogTitle = dialogTitle;
  }

  /**
   * @param dialogMessage The dialogMessage to set.
   */
  public void setDialogMessage(String dialogMessage) {
    this.dialogMessage = dialogMessage;
  }
}
