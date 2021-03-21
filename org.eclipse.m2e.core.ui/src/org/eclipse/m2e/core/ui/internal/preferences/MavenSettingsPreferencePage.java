/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;

import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.index.IndexManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Maven installations preference page
 * 
 * @author Eugene Kuleshov
 */
public class MavenSettingsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  private static final Logger log = LoggerFactory.getLogger(MavenSettingsPreferencePage.class);

  final IMavenConfiguration mavenConfiguration;

  final IMaven maven;

  Text globalSettingsText;

  Text userSettingsText;

  Text localRepositoryText;

  boolean dirty = false;

  private Link globalSettingsLink;

  private Link userSettingsLink;

  public MavenSettingsPreferencePage() {
    setTitle(Messages.MavenSettingsPreferencePage_title);

    this.mavenConfiguration = MavenPlugin.getMavenConfiguration();
    this.maven = MavenPlugin.getMaven();
  }

  public void init(IWorkbench workbench) {
  }

  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if(visible) {
      updateLocalRepository();
    }
  }

  protected void updateSettings(final boolean updateMavenDependencies) {
    //Force reevaluation of local repository, in case the settings were modified externally
    updateLocalRepository();

    final String userSettings = getUserSettings();
    final String globalSettings = getGlobalSettings();

    String currentGlobalSettings = mavenConfiguration.getGlobalSettingsFile();
    String currentUserSettings = mavenConfiguration.getUserSettingsFile();

    if(Objects.equals(globalSettings, currentGlobalSettings) && Objects.equals(currentUserSettings, userSettings)) {
      return;
    }

    final Boolean[] updateProjects = new Boolean[1];
    updateProjects[0] = updateMavenDependencies;
    if(updateMavenDependencies) {
      IMavenProjectFacade[] projects = MavenPlugin.getMavenProjectRegistry().getProjects();
      if(projects != null && projects.length > 0) {
        updateProjects[0] = MessageDialog.openQuestion(getShell(),
            Messages.MavenPreferencePage_updateProjectRequired_title,
            Messages.MavenProjectPreferencePage_dialog_message);
      }
    }

    new Job(Messages.MavenSettingsPreferencePage_job_updating) {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          final File localRepositoryDir = new File(maven.getLocalRepository().getBasedir());

          // this clears cached settings.xml instance
          mavenConfiguration.setGlobalSettingsFile(globalSettings);
          mavenConfiguration.setUserSettingsFile(userSettings);

          File newRepositoryDir = new File(maven.getLocalRepository().getBasedir());
          if(!newRepositoryDir.equals(localRepositoryDir)) {
            IndexManager indexManager = MavenPlugin.getIndexManager();
            indexManager.getWorkspaceIndex().updateIndex(true, monitor);
          }
          if(updateProjects[0]) {
            IMavenProjectFacade[] projects = MavenPlugin.getMavenProjectRegistry().getProjects();
            ArrayList<IProject> allProjects = new ArrayList<>();
            if(projects != null && projects.length > 0) {
              MavenPlugin.getMaven().reloadSettings();

              SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, projects.length);
              for(IMavenProjectFacade project : projects) {
                subMonitor
                    .beginTask(NLS.bind(Messages.MavenSettingsPreferencePage_task_updating, project.getProject()
                        .getName()), 1);
                allProjects.add(project.getProject());
              }
              MavenPlugin.getMavenProjectRegistry().refresh(
                  new MavenUpdateRequest(allProjects.toArray(new IProject[] {}), mavenConfiguration.isOffline(), true));
              subMonitor.done();
            }
          }
          return Status.OK_STATUS;
        } catch(CoreException e) {
          log.error(e.getMessage(), e);
          return e.getStatus();
        }
      }
    }.schedule();
  }

  @Override
  protected void performDefaults() {
    globalSettingsText.setText("");
    userSettingsText.setText("");
    checkSettings();
    updateLocalRepository();
    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    updateSettings(true);
    return true;
  }

  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));

    globalSettingsLink = new Link(composite, SWT.NONE);
    globalSettingsLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    globalSettingsLink.setText(Messages.MavenSettingsPreferencePage_globalSettingslink2);
    globalSettingsLink.setToolTipText(Messages.MavenSettingsPreferencePage_globalSettingslink_tooltip);
    globalSettingsLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
        String globalSettings = getGlobalSettings();
        if(globalSettings != null) {
          openEditor(globalSettings);
        }
      }
      ));

    globalSettingsText = new Text(composite, SWT.BORDER);
    globalSettingsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    Button globalSettingsBrowseButton = new Button(composite, SWT.NONE);
    globalSettingsBrowseButton.setText(Messages.MavenSettingsPreferencePage_globalSettingsBrowseButton_text);
    globalSettingsBrowseButton
        .addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> browseSettingsAction(globalSettingsText)));

    userSettingsLink = new Link(composite, SWT.NONE);
    userSettingsLink.setText(Messages.MavenSettingsPreferencePage_userSettingslink2);
    userSettingsLink.setToolTipText(Messages.MavenSettingsPreferencePage_userSettingslink_tooltip);
    userSettingsLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    userSettingsLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      String userSettings = getUserSettings();
      if(userSettings == null) {
        userSettings = SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath();
      }
      openEditor(userSettings);
    }));
    userSettingsText = new Text(composite, SWT.BORDER);
    userSettingsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    userSettingsText.setMessage(SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());

    Button userSettingsBrowseButton = new Button(composite, SWT.NONE);
    userSettingsBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.RIGHT, false, false, 1, 1));
    userSettingsBrowseButton.setText(Messages.MavenSettingsPreferencePage_userSettingsBrowseButton_text);
    userSettingsBrowseButton
        .addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> browseSettingsAction(userSettingsText)));

    Button updateSettings = new Button(composite, SWT.NONE);
    updateSettings.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    updateSettings.setText(Messages.MavenSettingsPreferencePage_btnUpdate);
    updateSettings.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateSettings(true)));
    Label localRepositoryLabel = new Label(composite, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd.verticalIndent = 25;
    localRepositoryLabel.setLayoutData(gd);
    localRepositoryLabel.setText(Messages.MavenSettingsPreferencePage_lblLocal);

    localRepositoryText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
    localRepositoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    localRepositoryText.setData("name", "localRepositoryText"); //$NON-NLS-1$ //$NON-NLS-2$
    localRepositoryText.setEditable(false);
    Button reindexButton = new Button(composite, SWT.NONE);
    reindexButton.setLayoutData(new GridData(SWT.FILL, SWT.RIGHT, false, false, 1, 1));
    reindexButton.setText(Messages.preferencesReindexButton);
    reindexButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      new WorkspaceJob(Messages.MavenSettingsPreferencePage_job_indexing) {
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
          IndexManager indexManager = MavenPlugin.getIndexManager();
          indexManager.getWorkspaceIndex().updateIndex(true, monitor);
          return Status.OK_STATUS;
        }
      }.schedule();
    }));

    ModifyListener settingsModifyListener = modifyevent -> {
      updateLocalRepository();
      checkSettings();
    };
    userSettingsText.addModifyListener(settingsModifyListener);
    globalSettingsText.addModifyListener(settingsModifyListener);

    String globalSettings = mavenConfiguration.getGlobalSettingsFile();
    if(globalSettings != null) {
      globalSettingsText.setText(globalSettings);
    }
    String userSettings = mavenConfiguration.getUserSettingsFile();
    if(userSettings != null) {
      userSettingsText.setText(userSettings);
    }
    checkSettings();
    updateLocalRepository();

    return composite;
  }

  private void updateUserSettingsLink(String userSettings) {
    File userSettingsFile = SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE;
    if(userSettings != null) {
      userSettingsFile = new File(userSettings);
    }
    boolean active = userSettingsFile.canRead();

    String text = Messages.MavenSettingsPreferencePage_userSettingslink1;
    if(active) {
      text = Messages.MavenSettingsPreferencePage_userSettingslink2;
    }
    userSettingsLink.setText(text);
  }

  private void updateGlobalSettingsLink(String globalSettings) {
    boolean active = globalSettings != null && new File(globalSettings).canRead();
    String text = Messages.MavenSettingsPreferencePage_globalSettingslink1;
    if(active) {
      text = Messages.MavenSettingsPreferencePage_globalSettingslink2;
    }
    globalSettingsLink.setText(text);
  }

  protected void updateLocalRepository() {
    final String globalSettings = getGlobalSettings();
    final String userSettings = getUserSettings();
    try {
      Settings settings = maven.buildSettings(globalSettings, userSettings);
      String localRepository = settings.getLocalRepository();
      if(localRepository == null) {
        localRepository = RepositorySystem.defaultUserLocalRepository.getAbsolutePath();
      }
      if(!localRepositoryText.isDisposed()) {
        localRepositoryText.setText(localRepository == null ? "" : localRepository); //$NON-NLS-1$
      }
    } catch(CoreException e) {
      setMessage(e.getMessage(), IMessageProvider.ERROR);
    }
  }

  protected void checkSettings() {
    setErrorMessage(null);
    setMessage(null);

    // NB: enable/disable links regardless of validation errors

    String globalSettings = getGlobalSettings();
    updateGlobalSettingsLink(globalSettings);

    String userSettings = getUserSettings();
    updateUserSettingsLink(userSettings);

    if(globalSettings != null
        && !checkSettings(globalSettings, Messages.MavenSettingsPreferencePage_error_globalSettingsMissing,
            Messages.MavenSettingsPreferencePage_error_globalSettingsParse)) {
      return;
    }

    if(userSettings != null
        && !checkSettings(userSettings, Messages.MavenSettingsPreferencePage_error_userSettingsMissing,
            Messages.MavenSettingsPreferencePage_error_userSettingsParse)) {
      return;
    }
  }

  private boolean checkSettings(String location, String errorMissing, String errorParse) {
    if(!new File(location).canRead()) {
      setMessage(errorMissing, IMessageProvider.WARNING);
      return false;
    }
    List<SettingsProblem> result = maven.validateSettings(location);
    if(result.size() > 0) {
      setMessage(NLS.bind(errorParse, result.get(0).getMessage()), IMessageProvider.WARNING);
      return false;
    }
    return true;
  }

  void openEditor(final String fileName) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    IWorkbenchPage page = window.getActivePage();

    IEditorDescriptor desc = workbench.getEditorRegistry().getDefaultEditor("settings.xml"); //$NON-NLS-1$

    IEditorInput input = new FileStoreEditorInput(EFS.getLocalFileSystem().fromLocalFile(new File(fileName)));
    try {
      final IEditorPart editor = IDE.openEditor(page, input, desc.getId());
      if(editor == null) {
        //external editor was opened
        return;
      }
      editor.addPropertyListener((source, propId) -> {
        if(!editor.isDirty()) {
          log.info("Refreshing settings " + fileName); //$NON-NLS-1$
        }
      });
    } catch(PartInitException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  String getUserSettings() {
    return getSettings(userSettingsText);
  }

  String getGlobalSettings() {
    return getSettings(globalSettingsText);
  }

  private String getSettings(Text settings) {
    String location = settings.getText().trim();
    return location.length() > 0 ? location : null;
  }

  protected void browseSettingsAction(Text settings) {
    FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
    if(settings.getText().trim().length() > 0) {
      dialog.setFileName(settings.getText());
    }
    String file = dialog.open();
    if(file != null) {
      file = file.trim();
      if(file.length() > 0) {
        settings.setText(file);
        updateLocalRepository();
        checkSettings();
      }
    }
  }
}
