/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - Work on Bug 350414
 *      Fred Bricon (Red Hat, Inc.) - Add global checksum policy prefs.
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;


public class MavenPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  //private static final Logger log = LoggerFactory.getLogger(MavenPreferencePage.class);

  private String originalChecksumPolicy;

  private String originalUpdatePolicy;

  public MavenPreferencePage() {
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

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_OFFLINE, Messages.preferencesOffline,
        getFieldEditorParent()));

    //    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_GLOBAL_UPDATE_NEVER,
    //        Messages.preferencesGlobalUpdateNever, getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DEBUG_OUTPUT, //
        Messages.preferencesDebugOutput, getFieldEditorParent()));

    // addField( new BooleanFieldEditor( MavenPreferenceConstants.P_UPDATE_SNAPSHOTS,
    //     Messages.getString( "preferences.updateSnapshots" ), //$NON-NLS-1$
    //     getFieldEditorParent() ) );

    BooleanFieldEditor downloadSourcesField = new BooleanFieldEditor(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, //
        Messages.preferencesDownloadSources, getFieldEditorParent());
    downloadSourcesField.getDescriptionControl(getFieldEditorParent())
        .setToolTipText(Messages.preferencesDownloadSourcesTooltip);
    addField(downloadSourcesField);

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, //
        Messages.preferencesDownloadJavadoc, getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_UPDATE_INDEXES, //
        Messages.MavenPreferencePage_download, //
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_UPDATE_PROJECTS, //
        Messages.MavenPreferencePage_update, //
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_AUTO_UPDATE_CONFIGURATION, //
        Messages.MavenPreferencePage_autoUpdateProjectConfiguration, //
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS, //
        Messages.MavenPreferencePage_hide, getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_QUERY_CENTRAL_TO_IDENTIFY_ARTIFACT, //
        Messages.MavenPreferencePage_queryCentralToIdentifyArtifacts, getFieldEditorParent()));

    String[][] checksumPolicies = new String[][] {new String[] {Messages.preferencesGlobalChecksumPolicy_default, null},
        new String[] {Messages.preferencesGlobalChecksumPolicy_ignore, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE},
        new String[] {Messages.preferencesGlobalChecksumPolicy_warn, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN},
        new String[] {Messages.preferencesGlobalChecksumPolicy_fail, ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL}};
    originalChecksumPolicy = getPreferenceStore().getString(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);

    FieldEditor checksumPolicy = new ComboFieldEditor(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY,
        Messages.preferencesGlobalChecksumPolicy, checksumPolicies, getFieldEditorParent());
    checksumPolicy.getLabelControl(getFieldEditorParent())
        .setToolTipText(Messages.preferencesGlobalChecksumPolicy_tooltip);
    addField(checksumPolicy);

    String[][] updatePolicies = new String[][] {
        new String[] {Messages.preferencesGlobalUpdatePolicy_default,
            MavenPreferenceConstants.GLOBAL_UPDATE_POLICY_DEFAULT},
        new String[] {Messages.preferencesGlobalUpdatePolicy_never, RepositoryPolicy.UPDATE_POLICY_NEVER},
        new String[] {Messages.preferencesGlobalUpdatePolicy_always, RepositoryPolicy.UPDATE_POLICY_ALWAYS},
        new String[] {Messages.preferencesGlobalUpdatePolicy_daily, RepositoryPolicy.UPDATE_POLICY_DAILY},
        new String[] {Messages.preferencesGlobalUpdatePolicy_hourly,
            RepositoryPolicy.UPDATE_POLICY_INTERVAL + ":" + TimeUnit.HOURS.toMinutes(1)}};
    originalUpdatePolicy = getPreferenceStore().getString(MavenPreferenceConstants.P_GLOBAL_UPDATE_POLICY);

    FieldEditor updatePolicy = new ComboFieldEditor(MavenPreferenceConstants.P_GLOBAL_UPDATE_POLICY,
        Messages.preferencesGlobalUpdatePolicy, updatePolicies, getFieldEditorParent());
    updatePolicy.getLabelControl(getFieldEditorParent())
        .setToolTipText(Messages.preferencesGlobalChecksumPolicy_tooltip);
    addField(updatePolicy);

    if(M2EUIPluginActivator.showExperimentalFeatures()) {
      BooleanFieldEditor nullSchedulingRule = new BooleanFieldEditor(
          MavenPreferenceConstants.P_BUILDER_USE_NULL_SCHEDULING_RULE, Messages.preferencesNullSchedulingRule,
          getFieldEditorParent());
      addField(nullSchedulingRule);
    }
  }

  @Override
  protected void performApply() {
    super.performApply();
    updateProjects();
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
    //Update projects if the checksum policy changed
    String newChecksumPolicy = getPreferenceStore().getString(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    String newUpdatePolicy = getPreferenceStore().getString(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    boolean updateRequired = !originalChecksumPolicy.equals(newChecksumPolicy)
        || getMinutes(originalUpdatePolicy) > getMinutes(newUpdatePolicy);
    if(updateRequired) {
      List<IMavenProjectFacade> facades = MavenPlugin.getMavenProjectRegistry().getProjects();
      if(facades != null && !facades.isEmpty()) {
        boolean proceed = MessageDialog.openQuestion(getShell(),
            Messages.MavenPreferencePage_updateProjectRequired_title,
            Messages.MavenPreferencePage_changingPreferencesRequiresProjectUpdate);
        if(proceed) {
          ArrayList<IProject> allProjects = new ArrayList<>(facades.size());
          for(IMavenProjectFacade facade : facades) {
            allProjects.add(facade.getProject());
          }
          new UpdateMavenProjectJob(allProjects, //
              MavenPlugin.getMavenConfiguration().isOffline(), true /*forceUpdateDependencies*/,
              false /*updateConfiguration*/, true /*rebuild*/, true /*refreshFromLocal*/).schedule();
        }
      }
    }
    originalChecksumPolicy = newChecksumPolicy;
    originalUpdatePolicy = newUpdatePolicy;
  }

  private static long getMinutes(String policy) {
    if(policy != null) {
      if(RepositoryPolicy.UPDATE_POLICY_NEVER.equals(policy) || "true".equals(policy)) {
        return Long.MAX_VALUE;
      }
      try {
        String s = policy.substring(RepositoryPolicy.UPDATE_POLICY_INTERVAL.length() + 1);
        return Long.parseLong(s);
      } catch(RuntimeException e) {
      }
    }
    return TimeUnit.HOURS.toMinutes(24);
  }
}
