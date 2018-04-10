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

package org.eclipse.m2e.core.ui.internal.util;

import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.m2e.core.internal.M2EUtils;


/**
 * M2EUtils
 * 
 * @author dyocum
 */
public class M2EUIUtils {

  public static Font deriveFont(Font f, int style, int height) {
    FontData[] fd = f.getFontData();
    FontData[] newFD = new FontData[fd.length];
    for(int i = 0; i < fd.length; i++ ) {
      newFD[i] = new FontData(fd[i].getName(), height, style);
    }
    return new Font(Display.getCurrent(), newFD);
  }

  public static void showErrorDialog(Shell shell, String title, String msg, Exception e) {
	StringBuilder buff = new StringBuilder(msg);
    Throwable t = M2EUtils.getRootCause(e);
    if(t != null && !nullOrEmpty(t.getMessage())) {
      buff.append(t.getMessage());
    }
    MessageDialog.openError(shell, title, buff.toString());
  }

  public static boolean nullOrEmpty(String s) {
    return s == null || s.length() == 0;
  }

  /**
   * @param shell
   * @param string
   * @param string2
   * @param updateErrors
   */
  public static void showErrorsForProjectsDialog(final Shell shell, final String title, final String message,
      final Map<String, Throwable> errorMap) {
    // TODO Auto-generated method showErrorsForProjectsDialog
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        String[] buttons = {IDialogConstants.OK_LABEL};
        int ok_button = 0;
        M2EErrorDialog errDialog = new M2EErrorDialog(shell, title, Dialog.getImage(Dialog.DLG_IMG_MESSAGE_ERROR),
            message, MessageDialog.ERROR, buttons, ok_button, errorMap);
        errDialog.create();
        errDialog.open();
      }
    });

  }

  public static void addRequiredDecoration(Control control) {
    FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
        FieldDecorationRegistry.DEC_REQUIRED);
    ControlDecoration controlDecoration = new ControlDecoration(control, SWT.LEFT | SWT.CENTER);
    controlDecoration.setDescriptionText(fieldDecoration.getDescription());
    controlDecoration.setImage(fieldDecoration.getImage());
  }
}
