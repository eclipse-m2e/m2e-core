/*******************************************************************************
 * Copyright (c) 2012-2014 Rob Newton and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Rob Newton - initial warnings preference page
 *      Fred Bricon (Red Hat, Inc.) - use combos for problem severity 
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import static org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants.P_DUP_OF_PARENT_GROUPID_PB;
import static org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants.P_DUP_OF_PARENT_VERSION_PB;
import static org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants.P_OUT_OF_DATE_PROJECT_CONFIG_PB;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


public class WarningsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public static String[][] ERROR_SEVERITIES = new String[][] {
      new String[] {Messages.MavenWarningsPreferencePage_Ignore, ProblemSeverity.ignore.toString()},
      new String[] {Messages.MavenWarningsPreferencePage_Warning, ProblemSeverity.warning.toString()},
      new String[] {Messages.MavenWarningsPreferencePage_Error, ProblemSeverity.error.toString()}};

  private Composite parent;

  public WarningsPreferencePage() {
    super(GRID);
    setPreferenceStore(M2EUIPluginActivator.getDefault().getPreferenceStore());
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

    addField(getDefaultCombo(P_DUP_OF_PARENT_GROUPID_PB, //
        Messages.MavenWarningsPreferencePage_groupidDupParent, parent));

    addField(getDefaultCombo(P_DUP_OF_PARENT_VERSION_PB, //
        Messages.MavenWarningsPreferencePage_versionDupParent, parent));

    addField(getDefaultCombo(P_OUT_OF_DATE_PROJECT_CONFIG_PB, //
        Messages.MavenWarningsPreferencePage_OutOfDate_Project_Config, parent));
  }

  private FieldEditor getDefaultCombo(String key, String label, Composite parent) {
    return new ComboFieldEditor(key, label, ERROR_SEVERITIES, parent);
  }

}
