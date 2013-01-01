/*******************************************************************************
 * Copyright (c) 2012 Rob Newton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Rob Newton - initial warnings preference page
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


public class WarningsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

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
    String text;

    text = NLS.bind(Messages.MavenWarningsPreferencePage_groupidDupParent,
        org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_groupid);
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DISABLE_GROUPID_DUP_OF_PARENT_WARNING, text, parent));

    text = NLS.bind(Messages.MavenWarningsPreferencePage_versionDupParent,
        org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_version);
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DISABLE_VERSION_DUP_OF_PARENT_WARNING, text, parent));
  }
}
