/*******************************************************************************
 * Copyright (c) 2012, 2019 Rob Newton and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Rob Newton - initial warnings preference page
 *      Fred Bricon (Red Hat, Inc.) - use combos for problem severity
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import static org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants.P_DUP_OF_PARENT_GROUPID_PB;
import static org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants.P_DUP_OF_PARENT_VERSION_PB;
import static org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants.P_NOT_COVERED_MOJO_EXECUTION_PB;
import static org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants.P_OUT_OF_DATE_PROJECT_CONFIG_PB;
import static org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants.P_OVERRIDING_MANAGED_VERSION_PB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;


public class WarningsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public static final String[][] ERROR_SEVERITIES = new String[][] {
      new String[] {Messages.MavenWarningsPreferencePage_Ignore, ProblemSeverity.ignore.toString()},
      new String[] {Messages.MavenWarningsPreferencePage_Warning, ProblemSeverity.warning.toString()},
      new String[] {Messages.MavenWarningsPreferencePage_Error, ProblemSeverity.error.toString()}};

  private Composite parent;

  private final static List<String> SENSIBLE_PREFERENCES = Arrays.asList(P_DUP_OF_PARENT_GROUPID_PB,
      P_DUP_OF_PARENT_VERSION_PB, P_NOT_COVERED_MOJO_EXECUTION_PB, P_OUT_OF_DATE_PROJECT_CONFIG_PB,
      P_OVERRIDING_MANAGED_VERSION_PB);

  private final Map<String, String> originalValues = new HashMap<>();

  public WarningsPreferencePage() {
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

    addField(getDefaultCombo(P_DUP_OF_PARENT_GROUPID_PB, //
        Messages.MavenWarningsPreferencePage_groupidDupParent, parent));

    addField(getDefaultCombo(P_DUP_OF_PARENT_VERSION_PB, //
        Messages.MavenWarningsPreferencePage_versionDupParent, parent));

    addField(getDefaultCombo(P_OUT_OF_DATE_PROJECT_CONFIG_PB, //
        Messages.MavenWarningsPreferencePage_OutOfDate_Project_Config, parent));

    addField(getDefaultCombo(P_NOT_COVERED_MOJO_EXECUTION_PB, //
        Messages.MavenWarningsPreferencePage_notCoveredMojoExecution, parent));

    addField(getDefaultCombo(P_OVERRIDING_MANAGED_VERSION_PB, //
        Messages.MavenWarningsPreferencePage_overridingManagedPreferences, parent));

    initOriginalValues();

  }

  private void initOriginalValues() {
    originalValues.clear();
    for(String pref : SENSIBLE_PREFERENCES) {
      originalValues.put(pref, getPreferenceStore().getString(pref));
    }
  }

  private FieldEditor getDefaultCombo(String key, String label, Composite parent) {
    return new ComboFieldEditor(key, label, ERROR_SEVERITIES, parent);
  }

  @Override
  public boolean performOk() {
    boolean result = super.performOk();
    if(result) {
      updateProjects();
    }
    return result;
  }

  private void updateProjects() {
    //Update projects if problem severities changed
    if(isDirty()) {
      IMavenProjectFacade[] facades = MavenPlugin.getMavenProjectRegistry().getProjects();
      if(facades != null && facades.length > 0) {
        boolean proceed = MessageDialog.openQuestion(getShell(),
            Messages.MavenPreferencePage_updateProjectRequired_title,
            Messages.MavenWarningsPreferencePage_changingProblemSeveritiesRequiresProjectUpdate);
        if(proceed) {
          ArrayList<IProject> allProjects = new ArrayList<>(facades.length);
          for(IMavenProjectFacade facade : facades) {
            allProjects.add(facade.getProject());
          }
          new UpdateMavenProjectJob(
              allProjects.toArray(new IProject[allProjects.size()]), //
              MavenPlugin.getMavenConfiguration().isOffline(), true /*forceUpdateDependencies*/,
              false /*updateConfiguration*/, true /*rebuild*/, true /*refreshFromLocal*/).schedule();
          initOriginalValues();
        }
      }
    }
  }

  private boolean isDirty() {
    for(Entry<String, String> original : originalValues.entrySet()) {
      if(!Objects.equals(original.getValue(), getPreferenceStore().getString(original.getKey()))) {
        return true;
      }
    }
    return false;
  }
}
