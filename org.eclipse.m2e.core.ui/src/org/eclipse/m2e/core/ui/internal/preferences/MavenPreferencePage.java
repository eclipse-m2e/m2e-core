/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;


public class MavenPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  private static final Logger log = LoggerFactory.getLogger(MavenPreferencePage.class);
  
  public MavenPreferencePage() {
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

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_OFFLINE, Messages.preferencesOffline,
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_GLOBAL_UPDATE_NEVER,
        Messages.preferencesGlobalUpdateNever, getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DEBUG_OUTPUT, //
        Messages.preferencesDebugOutput,
        getFieldEditorParent()));

    // addField( new BooleanFieldEditor( MavenPreferenceConstants.P_UPDATE_SNAPSHOTS, 
    //     Messages.getString( "preferences.updateSnapshots" ), //$NON-NLS-1$
    //     getFieldEditorParent() ) );

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, //
        Messages.preferencesDownloadSources,
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, //
        Messages.preferencesDownloadJavadoc,
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_UPDATE_INDEXES, //
        Messages.MavenPreferencePage_download, //
        getFieldEditorParent()));
    
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_UPDATE_PROJECTS, //
        Messages.MavenPreferencePage_update, //
        getFieldEditorParent()));
    
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS, //
        Messages.MavenPreferencePage_hide, getFieldEditorParent()));
    
    GridData comboCompositeGridData = new GridData();
    comboCompositeGridData.verticalIndent = 25;
    comboCompositeGridData.horizontalSpan = 3;
    comboCompositeGridData.grabExcessHorizontalSpace = true;
    comboCompositeGridData.horizontalAlignment = GridData.FILL;

    Composite comboComposite = new Composite(getFieldEditorParent(), SWT.NONE);
    comboComposite.setLayoutData(comboCompositeGridData);
    comboComposite.setLayout(new GridLayout(2, false));

  }
}
