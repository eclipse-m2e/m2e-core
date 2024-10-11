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

package org.eclipse.m2e.core.ui.internal.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


public class MavenPropertyDialog extends Dialog {

  private static final String DIALOG_SETTINGS = MavenPropertyDialog.class.getName();

  private final String title;

  private final String initialName;

  private final String initialValue;

  private final VerifyListener verifyListener;

  protected Text nameText;

  protected Text valueText;

  private String name;

  private String value;

  public MavenPropertyDialog(Shell shell, String title, String initialName, String initialValue,
      VerifyListener verifyListener) {
    super(shell);
    this.title = title;
    this.initialName = initialName;
    this.initialValue = initialValue;
    this.verifyListener = verifyListener;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginTop = 7;
    gridLayout.marginWidth = 12;
    comp.setLayout(gridLayout);

    Label nameLabel = new Label(comp, SWT.NONE);
    nameLabel.setText(Messages.launchPropertyDialogName);
    nameLabel.setFont(comp.getFont());

    nameText = new Text(comp, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 300;
    nameText.setLayoutData(gd);
    nameText.setFont(comp.getFont());
    nameText.setText(initialName == null ? "" : initialName); //$NON-NLS-1$
    nameText.addModifyListener(e -> updateButtons());

    Label valueLabel = new Label(comp, SWT.NONE);
    valueLabel.setText(Messages.launchPropertyDialogValue);
    valueLabel.setFont(comp.getFont());

    valueText = new Text(comp, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.widthHint = 300;
    valueText.setLayoutData(gd);
    valueText.setFont(comp.getFont());
    valueText.setText(initialValue == null ? "" : initialValue); //$NON-NLS-1$
    valueText.addModifyListener(e -> updateButtons());

//    if(variables) {
//      Button variablesButton = new Button(comp, SWT.PUSH);
//      variablesButton.setText(Messages.getString("launch.propertyDialog.browseVariables")); //$NON-NLS-1$;
//      gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
//      gd.horizontalSpan = 2;
//      int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
//      gd.widthHint = Math.max(widthHint, variablesButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
//      variablesButton.setLayoutData(gd);
//      variablesButton.setFont(comp.getFont());
//
//      variablesButton.addSelectionListener(new SelectionAdapter() {
//        public void widgetSelected(SelectionEvent se) {
//          StringVariableSelectionDialog variablesDialog = new StringVariableSelectionDialog(getShell());
//          if(variablesDialog.open() == IDialogConstants.OK_ID) {
//            String variable = variablesDialog.getVariableExpression();
//            if(variable != null) {
//              valueText.insert(variable.trim());
//            }
//          }
//        }
//      });
//    }

    return comp;
  }

  public String getName() {
    return this.name;
  }

  public String getValue() {
    return this.value;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
   */
  @Override
  protected void buttonPressed(int buttonId) {
    if(buttonId == IDialogConstants.OK_ID) {
      name = nameText.getText();
      value = valueText.getText();
    } else {
      name = null;
      value = null;
    }
    super.buttonPressed(buttonId);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    if(title != null) {
      shell.setText(title);
    }
//    if (fInitialValues[0].length() == 0) {
//      PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAntUIHelpContextIds.ADD_PROPERTY_DIALOG);
//    } else {
//      PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IAntUIHelpContextIds.EDIT_PROPERTY_DIALOG);
//    }
  }

  /**
   * Enable the OK button if valid input
   */
  protected void updateButtons() {
    String name = nameText.getText().trim();
    String value = valueText.getText().trim();
    // verify name
    Event e = new Event();
    e.widget = nameText;
    VerifyEvent ev = new VerifyEvent(e);
    ev.doit = true;
    if(verifyListener != null) {
      ev.text = name;
      verifyListener.verifyText(ev);
    }
    getButton(IDialogConstants.OK_ID).setEnabled((name.length() > 0) && (value.length() > 0) && ev.doit);
  }

  /**
   * Enable the buttons on creation.
   *
   * @see org.eclipse.jface.window.Window#create()
   */
  @Override
  public void create() {
    super.create();
    updateButtons();
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    IDialogSettings pluginSettings = M2EUIPluginActivator.getDefault().getDialogSettings();
    IDialogSettings dialogSettings = pluginSettings.getSection(DIALOG_SETTINGS);
    if(dialogSettings == null) {
      dialogSettings = new DialogSettings(DIALOG_SETTINGS);
      pluginSettings.addSection(dialogSettings);
    }
    return dialogSettings;
  }

}
