/*******************************************************************************
 * Copyright (c) 2018 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.ui.internal.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Composite which can be used to show an IStatus message & image
 *
 * @author Matthew Piggott
 */
public class WarningComposite extends Composite {

  private Text warningLabel;

  private Label warningImg;

  public WarningComposite(Composite parent, int style) {
    super(parent, style);

    this.setVisible(false);

    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).span(2, 1).hint(100, SWT.DEFAULT)
        .applyTo(this);
    this.setLayout(new GridLayout(2, false));

    warningImg = new Label(this, SWT.NONE);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(warningImg);

    warningLabel = new Text(this, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
    warningLabel.setBackground(parent.getBackground());
    GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.FILL).grab(true, false).applyTo(warningLabel);
  }

  public void setStatus(IStatus status) {
    warningLabel.setText(status.getMessage() != null ? status.getMessage() : "");
    setImage(status.getSeverity());
    this.setVisible(IStatus.OK != status.getSeverity());
    this.pack(true);
  }

  private void setImage(int severity) {
    if(IStatus.ERROR == severity) {
      warningImg.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR));
    } else if(IStatus.WARNING == severity) {
      warningImg.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
    } else if(IStatus.INFO == severity) {
      warningImg.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO));
    } else {
      warningImg.setImage(null);
    }
  }
}
