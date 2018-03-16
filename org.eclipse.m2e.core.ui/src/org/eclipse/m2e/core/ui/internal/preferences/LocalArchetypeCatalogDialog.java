/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;

import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Local Archetype catalog dialog
 * 
 * @author Eugene Kuleshov
 */
public class LocalArchetypeCatalogDialog extends TitleAreaDialog {

  private static final String DIALOG_SETTINGS = LocalArchetypeCatalogDialog.class.getName();

  private static final String KEY_LOCATIONS = "catalogLocation"; //$NON-NLS-1$

  private static final int MAX_HISTORY = 15;

  private String title;

  private String message;

  Combo catalogLocationCombo;

  private Text catalogDescriptionText;

  private IDialogSettings dialogSettings;

  private ArchetypeCatalogFactory archetypeCatalogFactory;

  protected LocalArchetypeCatalogDialog(Shell shell, ArchetypeCatalogFactory factory) {
    super(shell);
    this.archetypeCatalogFactory = factory;
    this.title = Messages.LocalArchetypeCatalogDialog_title;
    this.message = Messages.LocalArchetypeCatalogDialog_message;
    setShellStyle(SWT.DIALOG_TRIM);
    setHelpAvailable(false);

    IDialogSettings pluginSettings = M2EUIPluginActivator.getDefault().getDialogSettings();
    dialogSettings = pluginSettings.getSection(DIALOG_SETTINGS);
    if(dialogSettings == null) {
      dialogSettings = new DialogSettings(DIALOG_SETTINGS);
      pluginSettings.addSection(dialogSettings);
    }
  }

  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    setTitle(title);
    setMessage(message);
    return control;
  }

  protected Control createDialogArea(Composite parent) {
    Composite composite1 = (Composite) super.createDialogArea(parent);

    Composite composite = new Composite(composite1, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginTop = 7;
    gridLayout.marginWidth = 12;
    gridLayout.numColumns = 3;
    composite.setLayout(gridLayout);

    Label catalogLocationLabel = new Label(composite, SWT.NONE);
    catalogLocationLabel.setText(Messages.LocalArchetypeCatalogDialog_lblCatalog);

    catalogLocationCombo = new Combo(composite, SWT.NONE);
    GridData gd_catalogLocationCombo = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_catalogLocationCombo.widthHint = 250;
    catalogLocationCombo.setLayoutData(gd_catalogLocationCombo);
    catalogLocationCombo.setItems(getSavedValues(KEY_LOCATIONS));

    Button browseButton = new Button(composite, SWT.NONE);
    browseButton.setText(Messages.LocalArchetypeCatalogDialog_btnBrowse);
    browseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell());
        dialog.setText(Messages.LocalArchetypeCatalogDialog_dialog_title);
        String location = dialog.open();
        if(location != null) {
          catalogLocationCombo.setText(location);
          update();
        }
      }
    });
    setButtonLayoutData(browseButton);

    Label catalogDescriptionLabel = new Label(composite, SWT.NONE);
    catalogDescriptionLabel.setText(Messages.LocalArchetypeCatalogDialog_lblDesc);

    catalogDescriptionText = new Text(composite, SWT.BORDER);
    catalogDescriptionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    if(archetypeCatalogFactory != null) {
      catalogLocationCombo.setText(archetypeCatalogFactory.getId());
      catalogDescriptionText.setText(archetypeCatalogFactory.getDescription());
    }

    ModifyListener modifyListener = new ModifyListener() {
      public void modifyText(final ModifyEvent e) {
        update();
      }
    };
    catalogLocationCombo.addModifyListener(modifyListener);
    catalogDescriptionText.addModifyListener(modifyListener);

    return composite;
  }

  private String[] getSavedValues(String key) {
    String[] array = dialogSettings.getArray(key);
    return array == null ? new String[0] : array;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(title);
  }

  public void create() {
    super.create();
    getButton(IDialogConstants.OK_ID).setEnabled(false);
  }

  protected void okPressed() {
    String description = catalogDescriptionText.getText().trim();
    String location = catalogLocationCombo.getText().trim();

    archetypeCatalogFactory = new LocalCatalogFactory(location, description, true);

    saveValue(KEY_LOCATIONS, location);

    super.okPressed();
  }

  public ArchetypeCatalogFactory getArchetypeCatalogFactory() {
    return archetypeCatalogFactory;
  }

  private void saveValue(String key, String value) {
    List<String> dirs = new ArrayList<String>();
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

    String location = catalogLocationCombo.getText().trim();
    if(location.length() == 0) {
      setErrorMessage(Messages.LocalArchetypeCatalogDialog_error_no_location);
      return false;
    }

    if(!new File(location).exists()) {
      setErrorMessage(Messages.LocalArchetypeCatalogDialog_error_exist);
      return false;
    }

    LocalCatalogFactory factory = new LocalCatalogFactory(location, null, true);
    ArchetypeCatalog archetypeCatalog;
    try {
      archetypeCatalog = factory.getArchetypeCatalog();
    } catch(CoreException ex) {
      setMessage(NLS.bind(Messages.LocalArchetypeCatalogDialog_error, ex.getMessage()), IStatus.ERROR);
      return false;
    }
    List<Archetype> archetypes = archetypeCatalog.getArchetypes();
    if(archetypes == null || archetypes.size() == 0) {
      setMessage(Messages.LocalArchetypeCatalogDialog_error_empty, IStatus.WARNING);
    }

    return true;
  }

}
