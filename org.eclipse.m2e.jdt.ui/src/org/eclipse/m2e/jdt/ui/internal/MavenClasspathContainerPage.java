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

package org.eclipse.m2e.jdt.ui.internal;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;


/**
 * MavenClasspathContainerPage
 *
 * @author Eugene Kuleshov
 */
public class MavenClasspathContainerPage extends WizardPage
    implements IClasspathContainerPage, IClasspathContainerPageExtension {

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
    link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      // IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer) getContainer();
      // container.openPage(MavenProjectPreferencePage.ID, javaProject.getProject());

      @SuppressWarnings("restriction")
      String mavenPageId = org.eclipse.m2e.core.internal.IMavenConstants.PREFERENCE_PAGE_ID;
      PreferencesUtil.createPropertyDialogOn(getShell(), javaProject.getProject(), //
          mavenPageId, new String[] {mavenPageId}, null).open();
    }));
  }

  public boolean finish() {
    return true;
  }

}
