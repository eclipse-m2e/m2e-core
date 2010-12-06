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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.Messages;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


public class ProblemReportingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  final MavenPlugin plugin;
  private Composite parent;
  
  public ProblemReportingPreferencePage() {
    super(GRID);
    setPreferenceStore(MavenPlugin.getDefault().getPreferenceStore());

    plugin = MavenPlugin.getDefault();
  }

  public void init(IWorkbench workbench) {
  }

  /*
   * Creates the field editors. Field editors are abstractions of the common GUI
   * blocks needed to manipulate various types of preferences. Each field editor
   * knows how to save and restore itself.
   */
  public void createFieldEditors() {
    parent = getFieldEditorParent();
    
    addField(new StringFieldEditor(MavenPreferenceConstants.P_JIRA_USERNAME, Messages.getString("jira.username"), parent)); //$NON-NLS-1$
    
    StringFieldEditor passwordEditor = new StringFieldEditor(MavenPreferenceConstants.P_JIRA_PASSWORD, Messages.getString("jira.password"), parent); //$NON-NLS-1$
    
    addField(passwordEditor);
    Text passwordField = passwordEditor.getTextControl(parent);
    passwordField.setEchoChar('*');
  }
}
