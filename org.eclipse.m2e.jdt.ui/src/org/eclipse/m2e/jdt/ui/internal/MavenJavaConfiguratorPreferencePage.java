/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.ui.internal;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.m2e.jdt.JreSystemVersion;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * Maven Java configurator preference page
 */
public class MavenJavaConfiguratorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  // https://git.eclipse.org/c/jdt/eclipse.jdt.debug.git/tree/org.eclipse.jdt.debug.ui/plugin.xml#n2467
  private static final String INSTALLED_JRE_PAGE = "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage"; //$NON-NLS-1$

  private static final String EXECUTION_ENVIRONMENTS_PAGE = "org.eclipse.jdt.debug.ui.jreProfiles"; //$NON-NLS-1$

  public MavenJavaConfiguratorPreferencePage() {
    super(GRID);
    setPreferenceStore(MavenJdtUiPlugin.getDefault().getPreferenceStore());
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  protected Control createContents(Composite parent) {
    Composite result = (Composite) super.createContents(parent);
    createLink(result, Messages.MavenPreferencePage_executionEnvironmentJreLink, EXECUTION_ENVIRONMENTS_PAGE);
    createLink(result, Messages.MavenPreferencePage_workspaceDefaultJreLink, INSTALLED_JRE_PAGE);
    return result;
  }

  /*
   * Creates the field editors. Field editors are abstractions of the common GUI
   * blocks needed to manipulate various types of preferences. Each field editor
   * knows how to save and restore itself.
   */
  @Override
  public void createFieldEditors() {

    RadioGroupFieldEditor field = new RadioGroupFieldEditor(MavenJdtPlugin.PREFERENCES_JRE_SYSTEM_LIBRARY_VERSION,
        Messages.MavenPreferencePage_jreSystemLibraryVersion, 1,
        new String[][] {
            {Messages.MavenPreferencePage_useExecutionEnvironment,
                JreSystemVersion.EXECUTION_ENVIRONMENT_FROM_PLUGIN_CONFIG.name()},
            {Messages.MavenPreferencePage_useWorkspaceDefault, JreSystemVersion.WORKSPACE_DEFAULT.name()}},
        getFieldEditorParent(), true);
    addField(field);
  }

  private void createLink(final Composite parent, final String text, String target) {
    final PreferenceLinkArea link = new PreferenceLinkArea(parent, SWT.NONE, target, text,
        (IWorkbenchPreferenceContainer) getContainer(), null);
    link.getControl().setLayoutData(new GridData());
  }
}
