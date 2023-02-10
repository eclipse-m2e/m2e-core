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

package org.eclipse.m2e.core.ui.internal.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


public class UserInterfacePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  private Composite parent;

  public UserInterfacePreferencePage() {
    super(GRID);
    setPreferenceStore(M2EUIPluginActivator.getDefault().getPreferenceStore());
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  /*
   * Creates the field editors. Field editors are abstractions of the common GUI
   * blocks needed to manipulate various types of preferences. Each field editor
   * knows how to save and restore itself.
   */
  @Override
  public void createFieldEditors() {
    parent = getFieldEditorParent();
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DEFAULT_POM_EDITOR_PAGE, Messages.pomEditorDefaultPage,
        parent));
  }
}
