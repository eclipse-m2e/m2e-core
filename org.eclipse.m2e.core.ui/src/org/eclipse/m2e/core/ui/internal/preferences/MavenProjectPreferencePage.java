/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceInitializer;
import org.eclipse.m2e.core.internal.project.ResolverConfigurationIO;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.project.MavenUpdateConfigurationChangeListener;


/**
 * Maven project preference page
 *
 * @author Eugene Kuleshov
 */
public class MavenProjectPreferencePage extends PropertyPage {
  private static final Logger log = LoggerFactory.getLogger(MavenProjectPreferencePage.class);

  private Button resolveWorspaceProjectsButton;

  private Button autoUpdateConfigurationButton;

//  private Button includeModulesButton;

  private Text selectedProfilesText;

  public MavenProjectPreferencePage() {
    setTitle(Messages.MavenProjectPreferencePage_title);
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(GridData.FILL));

    Label profilesLabel = new Label(composite, SWT.NONE);
    profilesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    profilesLabel.setText(Messages.MavenProjectPreferencePage_lblProfiles);

    selectedProfilesText = new Text(composite, SWT.BORDER);
    selectedProfilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    resolveWorspaceProjectsButton = new Button(composite, SWT.CHECK);
    resolveWorspaceProjectsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    resolveWorspaceProjectsButton.setText(Messages.MavenProjectPreferencePage_btnResolve);

    autoUpdateConfigurationButton = new Button(composite, SWT.CHECK);
    autoUpdateConfigurationButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    autoUpdateConfigurationButton.setText(Messages.MavenPreferencePage_autoUpdateProjectConfiguration);
    if(MavenUpdateConfigurationChangeListener.isAutoConfigurationUpdateDisabled()) {
      autoUpdateConfigurationButton.setEnabled(false);
      String text = autoUpdateConfigurationButton.getText() + " (disabled in workspace preferences)";
      autoUpdateConfigurationButton.setText(text);
    }

//    includeModulesButton = new Button(composite, SWT.CHECK);
//    GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
//    gd.verticalIndent = 15;
//    includeModulesButton.setLayoutData(gd);
//    includeModulesButton.setText("Include &Modules");
//
//    Text includeModulesText = new Text(composite, SWT.WRAP | SWT.READ_ONLY | SWT.MULTI);
//    includeModulesText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
//    GridData gd_includeModulesText = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
//    gd_includeModulesText.horizontalIndent = 15;
//    gd_includeModulesText.verticalIndent = 0;
//    gd_includeModulesText.widthHint = 300;
//    gd_includeModulesText.heightHint = 120;
//    includeModulesText.setLayoutData(gd_includeModulesText);
//    includeModulesText.setBackground(composite.getBackground());
//    includeModulesText.setText("When enabled, dependencies from all nested modules "
//        + "are added to the \"Maven Dependencies\" container and "
//        + "source folders from nested modules are added to the current "
//        + "project build path (use \"Update Sources\" action)");

    init(getResolverConfiguration());

    boolean isAutoUpdate = ResolverConfigurationIO.isAutomaticallyUpdateConfiguration(getProject());
    autoUpdateConfigurationButton.setSelection(isAutoUpdate);

    return composite;
  }

  @Override
  protected void performDefaults() {
    init(new ResolverConfiguration());
    autoUpdateConfigurationButton.setSelection(MavenPreferenceInitializer.P_AUTO_UPDATE_CONFIGURATION_DEFAULT);
  }

  private void init(ResolverConfiguration configuration) {
    resolveWorspaceProjectsButton.setSelection(configuration.isResolveWorkspaceProjects());
//    includeModulesButton.setSelection(configuration.shouldIncludeModules());
    selectedProfilesText.setText(configuration.getSelectedProfiles());
  }

  @Override
  public boolean performOk() {
    final IProject project = getProject();
    try {
      if(!project.isAccessible() || !project.hasNature(IMavenConstants.NATURE_ID)) {
        return true;
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
      return false;
    }

    final ResolverConfiguration configuration = new ResolverConfiguration(getResolverConfiguration());
    if(configuration.getSelectedProfiles().equals(selectedProfilesText.getText()) &&
//        configuration.shouldIncludeModules()==includeModulesButton.getSelection() &&
        configuration.isResolveWorkspaceProjects() == resolveWorspaceProjectsButton.getSelection()
        && ResolverConfigurationIO.isAutomaticallyUpdateConfiguration(project) == autoUpdateConfigurationButton
            .getSelection()) {
      return true;
    }

    configuration.setResolveWorkspaceProjects(resolveWorspaceProjectsButton.getSelection());
    ResolverConfigurationIO.setAutomaticallyUpdateConfiguration(project, autoUpdateConfigurationButton.getSelection());
//    configuration.setIncludeModules(includeModulesButton.getSelection());
    configuration.setSelectedProfiles(selectedProfilesText.getText());

    IProjectConfigurationManager projectManager = MavenPlugin.getProjectConfigurationManager();
    boolean isSet = projectManager.setResolverConfiguration(getProject(), configuration);
    if(isSet) {

      boolean res = MessageDialog.openQuestion(getShell(), Messages.MavenProjectPreferencePage_dialog_title, //
          Messages.MavenProjectPreferencePage_dialog_message);
      if(res) {
        WorkspaceJob job = new WorkspaceJob(NLS.bind(Messages.MavenProjectPreferencePage_job, project.getName())) {
          @Override
          public IStatus runInWorkspace(IProgressMonitor monitor) {
            try {
              MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
            } catch(CoreException ex) {
              return ex.getStatus();
            }
            return Status.OK_STATUS;
          }
        };
        job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
        job.schedule();
      }

    }

    return isSet;
  }

  private ResolverConfiguration getResolverConfiguration() {
    IProjectConfigurationManager projectManager = MavenPlugin.getProjectConfigurationManager();
    return (ResolverConfiguration) projectManager.getProjectConfiguration(getProject());
  }

  private IProject getProject() {
    return getElement().getAdapter(IProject.class);
  }

}
