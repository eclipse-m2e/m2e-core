/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.preferences;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;

import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Remote Archetype catalog dialog
 *
 * @author Eugene Kuleshov
 */
public class RemoteArchetypeCatalogDialog extends TitleAreaDialog {

  /**
   *
   */
  private static final int VERIFY_ID = IDialogConstants.CLIENT_ID + 1;

  private static final String DIALOG_SETTINGS = RemoteArchetypeCatalogDialog.class.getName();

  private static final String KEY_LOCATIONS = "catalogUrl"; //$NON-NLS-1$

  private static final int MAX_HISTORY = 15;

  private final String title;

  private final String message;

  Combo catalogUrlCombo;

  private Text catalogDescriptionText;

  private IDialogSettings dialogSettings;

  private ArchetypeCatalogFactory archetypeCatalogFactory;

  Button verifyButton;

  protected RemoteArchetypeCatalogDialog(Shell shell, ArchetypeCatalogFactory factory) {
    super(shell);
    this.archetypeCatalogFactory = factory;
    this.title = Messages.RemoteArchetypeCatalogDialog_title;
    this.message = Messages.RemoteArchetypeCatalogDialog_message;
    setShellStyle(SWT.DIALOG_TRIM);
    setHelpAvailable(false);

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
    update();
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

    Label catalogLocationLabel = new Label(composite, SWT.NONE);
    catalogLocationLabel.setText(Messages.RemoteArchetypeCatalogDialog_lblCatalog);

    catalogUrlCombo = new Combo(composite, SWT.NONE);
    GridData gd_catalogLocationCombo = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_catalogLocationCombo.widthHint = 250;
    catalogUrlCombo.setLayoutData(gd_catalogLocationCombo);
    catalogUrlCombo.setItems(getSavedValues(KEY_LOCATIONS));

    Label catalogDescriptionLabel = new Label(composite, SWT.NONE);
    catalogDescriptionLabel.setText(Messages.RemoteArchetypeCatalogDialog_lblDesc);

    catalogDescriptionText = new Text(composite, SWT.BORDER);
    catalogDescriptionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    if(archetypeCatalogFactory != null) {
      catalogUrlCombo.setText(archetypeCatalogFactory.getId());
      catalogDescriptionText.setText(archetypeCatalogFactory.getDescription());
    }

    ModifyListener modifyListener = e -> update();
    catalogUrlCombo.addModifyListener(modifyListener);
    catalogDescriptionText.addModifyListener(modifyListener);

    return composite;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.TrayDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createButtonBar(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    composite.setFont(parent.getFont());

    // create help control if needed
    if(isHelpAvailable()) {
      createHelpControl(composite);
    }

    verifyButton = createButton(composite, VERIFY_ID, Messages.RemoteArchetypeCatalogDialog_btnVerify, false);
    verifyButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      verifyButton.setEnabled(false);
      String url = catalogUrlCombo.getText();
      final RemoteCatalogFactory factory = new RemoteCatalogFactory(url, null, true);

      new Job(Messages.RemoteArchetypeCatalogDialog_job_download) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          IStatus status = Status.OK_STATUS;
          ArchetypeCatalog catalog = null;
          try {
            catalog = factory.getArchetypeCatalog();
          } finally {
            final IStatus s = status;
            final List<Archetype> archetypes = ((catalog == null) ? Collections.emptyList() : catalog.getArchetypes());
            Shell shell = getShell();
            if(shell == null) {
              return status;
            }
            shell.getDisplay().asyncExec(() -> {
              if(verifyButton.isDisposed()) {
                return;
              }
              verifyButton.setEnabled(true);
              if(!s.isOK()) {
                setErrorMessage(NLS.bind(Messages.RemoteArchetypeCatalogDialog_error_read, s.getMessage()));
                getButton(IDialogConstants.OK_ID).setEnabled(false);
              } else if(archetypes.size() == 0) {
                setMessage(Messages.RemoteArchetypeCatalogDialog_error_empty, IStatus.WARNING);
              } else {
                setMessage(NLS.bind(Messages.RemoteArchetypeCatalogDialog_message_found, archetypes.size()),
                    IStatus.INFO);
              }
            });
          }
          return Status.OK_STATUS;
        }
      }.schedule();
    }));

    Label filler = new Label(composite, SWT.NONE);
    filler.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
    layout.numColumns++ ;

    super.createButtonsForButtonBar(composite); // cancel button

    return composite;
  }

  @Override
  protected Button getButton(int id) {
    return super.getButton(id);
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
    String description = catalogDescriptionText.getText().trim();
    String location = catalogUrlCombo.getText().trim();

    archetypeCatalogFactory = new RemoteCatalogFactory(location, description, true);

    saveValue(KEY_LOCATIONS, location);

    super.okPressed();
  }

  public ArchetypeCatalogFactory getArchetypeCatalogFactory() {
    return archetypeCatalogFactory;
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
    getButton(IDialogConstants.OK_ID).setEnabled(isValid);
    getButton(VERIFY_ID).setEnabled(isValid);
  }

  private boolean isValid() {
    setErrorMessage(null);
    setMessage(null, IStatus.WARNING);

    String url = catalogUrlCombo.getText().trim();
    boolean isValid = false;
    if(url.isEmpty()) {
      setErrorMessage(Messages.RemoteArchetypeCatalogDialog_error_required);
    } else if(!isWellFormedUrl(url)) {
      setErrorMessage(NLS.bind(Messages.RemoteArchetypeCatalogDialog_error_invalidUrl, url));
    } else {
      isValid = true;
    }
    verifyButton.setEnabled(isValid);
    return isValid;
  }

  public boolean isWellFormedUrl(String url) {
    try {
      new URL(url).toURI();
      return true;
    } catch(Exception O_o) {
      //Not good enough
    }
    return false;
  }

}
