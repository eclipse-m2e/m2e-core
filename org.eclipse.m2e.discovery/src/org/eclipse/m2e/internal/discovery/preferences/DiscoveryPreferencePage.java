/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.Messages;


public class DiscoveryPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  IWorkbench workbench;

  public DiscoveryPreferencePage() {
    super(Messages.DiscoveryPreferencePage_title);
    noDefaultAndApplyButton();
  }

  public void init(IWorkbench workbench) {
    this.workbench = workbench;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    composite.setLayout(new GridLayout(2, false));

    Label lblCatalogUrl = new Label(composite, SWT.NONE);
    lblCatalogUrl.setText(Messages.DiscoveryPreferencePage_catalogUrl);

    Text catalogUrl = new Text(composite, SWT.BORDER);
    catalogUrl.setEditable(false);
    catalogUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    catalogUrl.setText(MavenDiscovery.PATH);

    Button btnOpenCatalog = new Button(composite, SWT.NONE);
    btnOpenCatalog.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        MavenDiscovery.launchWizard(workbench.getModalDialogShellProvider().getShell());
      }
    });
    btnOpenCatalog.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    btnOpenCatalog.setSize(92, 29);
    btnOpenCatalog.setText(Messages.DiscoveryPreferencePage_openCatalog);
    new Label(composite, SWT.NONE);
    return composite;
  }
}
