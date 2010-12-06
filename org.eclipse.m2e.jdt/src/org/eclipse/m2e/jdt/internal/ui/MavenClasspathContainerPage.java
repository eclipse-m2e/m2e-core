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

package org.eclipse.m2e.jdt.internal.ui;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.jdt.internal.Messages;


/**
 * MavenClasspathContainerPage
 * 
 * @author Eugene Kuleshov
 */
public class MavenClasspathContainerPage extends WizardPage implements IClasspathContainerPage,
    IClasspathContainerPageExtension {

  IJavaProject javaProject;

  private IClasspathEntry containerEntry;
  
  public MavenClasspathContainerPage() {
    super(Messages.MavenClasspathContainerPage_title);
  }

  // IClasspathContainerPageExtension

  public void initialize(IJavaProject javaProject, IClasspathEntry[] currentEntries) {
    this.javaProject = javaProject;
    // this.currentEntries = currentEntries;
  }

  // IClasspathContainerPage

  public IClasspathEntry getSelection() {
    return this.containerEntry;
  }

  public void setSelection(IClasspathEntry containerEntry) {
    this.containerEntry = containerEntry;
  }

  public void createControl(Composite parent) {
    setTitle(Messages.MavenClasspathContainerPage_control_title);
    setDescription(Messages.MavenClasspathContainerPage_control_desc);


    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    setControl(composite);

    Link link = new Link(composite, SWT.NONE);
    link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    link.setText(Messages.MavenClasspathContainerPage_link);
    link.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer) getContainer();
        // container.openPage(MavenProjectPreferencePage.ID, javaProject.getProject());
        
        PreferencesUtil.createPropertyDialogOn(getShell(), javaProject.getProject(), //
            IMavenConstants.PREFERENCE_PAGE_ID, new String[] {IMavenConstants.PREFERENCE_PAGE_ID}, null).open();
      }
    });
  }

  public boolean finish() {
    return true;
  }

}
