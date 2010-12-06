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

package org.eclipse.m2e.core.ui.internal.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import org.eclipse.m2e.core.ui.internal.lifecycle.AbstractLifecyclePropertyPage;


/**
 * Simple lifecycle mapping properties page that displays static text.
 * 
 * @author igor
 */
public class SimpleLifecycleMappingPropertyPage extends AbstractLifecyclePropertyPage {

  private String message;

  public SimpleLifecycleMappingPropertyPage(String message) {
    this.message = message;
  }

  public Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(GridData.FILL));
    Label noInfoLabel = new Label(composite, SWT.NONE);
    noInfoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
    noInfoLabel.setAlignment(SWT.CENTER);
    noInfoLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
    noInfoLabel.setData("name", "noInfoLabel"); //$NON-NLS-1$ //$NON-NLS-2$
    noInfoLabel.setText(getMessage());
    return composite;
  }

  protected String getMessage() {
    return message;
  }

  public void performDefaults() {
  }

  public boolean performOk() {
    return true;
  }

}
