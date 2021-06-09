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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


/**
 * MavenMessageDialog
 *
 * @author dyocum
 */
public class MavenMessageDialog extends MessageDialog {

  private StyledText messageArea;

  /**
   * @param parentShell
   * @param dialogTitle
   * @param dialogTitleImage
   * @param dialogMessage
   * @param dialogImageType
   * @param dialogButtonLabels
   * @param defaultIndex
   */
  public MavenMessageDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
      int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
    super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createCustomArea(Composite parent) {
    // TODO Auto-generated method createCustomArea
    this.messageArea = new StyledText(parent, SWT.WRAP | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    this.messageArea.setLayout(new GridLayout());
    GridData gd = new GridData(SWT.LEFT, SWT.TOP, true, true);
    //size hints
    gd.widthHint = 600;
    gd.heightHint = 300;
    messageArea.setLayoutData(gd);
    return messageArea;
  }

  /**
   * @param parent Parent shell
   * @param title Title of the dialog
   * @param label The label shown above the msg.
   * @param message The actual message to show in the text area.
   */
  public static void openInfo(Shell parent, String title, String label, String message) {
    MavenMessageDialog dialog = new MavenMessageDialog(parent, title, Display.getDefault().getSystemImage(
        SWT.ICON_INFORMATION), // accept
        label, INFORMATION, new String[] {IDialogConstants.OK_LABEL}, 0); // ok
    dialog.create();
    dialog.getMessageArea().setText(message);
    dialog.getDialogArea().pack(true);
    dialog.open();
    return;
  }

  /**
   * @param parent
   * @param title
   * @param label
   * @param message
   * @param severity constants from MessageDialog
   */
  public static void openWithSeverity(Shell parent, String title, String label, String message, int severity) {
    Image icon = severity == IMessageProvider.ERROR ? Display.getDefault().getSystemImage(SWT.ICON_ERROR) : Display
        .getDefault().getSystemImage(SWT.ICON_INFORMATION);
    MavenMessageDialog dialog = new MavenMessageDialog(parent, title, icon, // accept
        label, severity, new String[] {IDialogConstants.OK_LABEL}, 0); // ok
    dialog.create();
    dialog.getMessageArea().setText(message);
    dialog.getDialogArea().pack(true);
    dialog.open();
    return;
  }

  /**
   * @return Returns the messageArea.
   */
  private StyledText getMessageArea() {
    return messageArea;
  }
}
