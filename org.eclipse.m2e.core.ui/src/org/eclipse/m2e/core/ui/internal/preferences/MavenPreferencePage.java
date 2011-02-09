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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.Messages;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public class MavenPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  final MavenPlugin plugin;
  
  public MavenPreferencePage() {
    super(GRID);
    setPreferenceStore(M2EUIPluginActivator.getDefault().getPreferenceStore());

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

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_OFFLINE, Messages.getString("preferences.offline"), //$NON-NLS-1$
        getFieldEditorParent()));
    
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DEBUG_OUTPUT, //
        Messages.getString("preferences.debugOutput"), //$NON-NLS-1$
        getFieldEditorParent()));

    // addField( new BooleanFieldEditor( MavenPreferenceConstants.P_UPDATE_SNAPSHOTS, 
    //     Messages.getString( "preferences.updateSnapshots" ), //$NON-NLS-1$
    //     getFieldEditorParent() ) );

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, //
        Messages.getString("preferences.downloadSources"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, //
        Messages.getString("preferences.downloadJavadoc"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_UPDATE_INDEXES, //
        org.eclipse.m2e.core.ui.internal.Messages.MavenPreferencePage_download, //
        getFieldEditorParent()));
    
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_UPDATE_PROJECTS, //
        org.eclipse.m2e.core.ui.internal.Messages.MavenPreferencePage_update, //
        getFieldEditorParent()));
    
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS, //
        org.eclipse.m2e.core.ui.internal.Messages.MavenPreferencePage_hide, getFieldEditorParent()));
    
    GridData comboCompositeGridData = new GridData();
    comboCompositeGridData.verticalIndent = 25;
    comboCompositeGridData.horizontalSpan = 3;
    comboCompositeGridData.grabExcessHorizontalSpace = true;
    comboCompositeGridData.horizontalAlignment = GridData.FILL;

    Composite comboComposite = new Composite(getFieldEditorParent(), SWT.NONE);
    comboComposite.setLayoutData(comboCompositeGridData);
    comboComposite.setLayout(new GridLayout(2, false));

    // addSeparator();
  }

  private void addSeparator() {
    Label separator = new Label(getFieldEditorParent(), SWT.HORIZONTAL | SWT.SEPARATOR);
    // separator.setVisible(false);
    GridData separatorGridData = new GridData();
    separatorGridData.horizontalSpan = 4;
    separatorGridData.grabExcessHorizontalSpace = true;
    separatorGridData.horizontalAlignment = GridData.FILL;
    separatorGridData.verticalIndent = 10;
    separator.setLayoutData(separatorGridData);
  }

}
