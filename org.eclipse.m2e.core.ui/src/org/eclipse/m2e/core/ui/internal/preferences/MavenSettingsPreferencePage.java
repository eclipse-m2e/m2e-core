/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype, Inc.
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
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
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

import org.apache.maven.building.Problem;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.IMavenToolbox;
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

  private Text globalSettingsText;

  private Text globalToolchainsText;

  private Text userSettingsText;

  private Text userToolchainsText;

  Text localRepositoryText;

  boolean dirty = false;


  public MavenSettingsPreferencePage() {
    setTitle(Messages.MavenSettingsPreferencePage_title);

    this.mavenConfiguration = MavenPlugin.getMavenConfiguration();
    this.maven = MavenPlugin.getMaven();
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if(visible) {
      updateLocalRepository();
    }
  }

  protected void updateSettings(boolean updateMavenDependencies) {
    //Force reevaluation of local repository, in case the settings were modified externally
    updateLocalRepository();

    String userSettings = getUserSettings();
    String userToolchains = getUserToolchains();
    String globalSettings = getGlobalSettings();
    String globalToolchains = getGlobalToolchains();

    if(Objects.equals(globalSettings, mavenConfiguration.getGlobalSettingsFile())
        && Objects.equals(userSettings, mavenConfiguration.getUserSettingsFile())
        && Objects.equals(userToolchains, mavenConfiguration.getUserToolchainsFile())) {
      return; // current preferences  not changed 
    }

    Boolean[] updateProjects = new Boolean[1];
    updateProjects[0] = updateMavenDependencies;
    if(updateMavenDependencies) {
      List<IMavenProjectFacade> projects = MavenPlugin.getMavenProjectRegistry().getProjects();
      if(projects != null && !projects.isEmpty()) {
        updateProjects[0] = MessageDialog.openQuestion(getShell(),
            Messages.MavenPreferencePage_updateProjectRequired_title,
            Messages.MavenProjectPreferencePage_dialog_message);
      }
    }

    Job.create(Messages.MavenSettingsPreferencePage_job_updating, monitor -> {
      try {
        // this clears cached settings.xml instance
        mavenConfiguration.setGlobalSettingsFile(globalSettings);
        mavenConfiguration.setUserSettingsFile(userSettings);
        mavenConfiguration.setUserToolchainsFile(userToolchains);

        if(Boolean.TRUE.equals(updateProjects[0])) {
          List<IMavenProjectFacade> projects = MavenPlugin.getMavenProjectRegistry().getProjects();
          if(projects != null && !projects.isEmpty()) {
            MavenPlugin.getMaven().reloadSettings();

            List<IProject> allProjects = new ArrayList<>();
            SubMonitor subMonitor = SubMonitor.convert(monitor, projects.size());
            for(IMavenProjectFacade project : projects) {
              subMonitor.split(1).beginTask(
                  NLS.bind(Messages.MavenSettingsPreferencePage_task_updating, project.getProject().getName()), 1);
              allProjects.add(project.getProject());
            }
            MavenPlugin.getMavenProjectRegistry()
                .refresh(new MavenUpdateRequest(allProjects, mavenConfiguration.isOffline(), true));
            subMonitor.done();
          }
        }
        return Status.OK_STATUS;
      } catch(CoreException e) {
        log.error(e.getMessage(), e);
        return e.getStatus();
      }
    }).schedule();
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

  @Override
  protected Control createContents(Composite parent) {

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gl_composite = new GridLayout(4, false);
    gl_composite.horizontalSpacing = 10;
    composite.setLayout(gl_composite);

    createHeading(composite, Messages.MavenSettingsPreferencePage_globalPreferences, false);

    this.globalSettingsText = this.createFileSelectionWidgets(composite,
        Messages.MavenSettingsPreferencePage_globalSettingsLabel, this.mavenConfiguration.getGlobalSettingsFile(),
        new File("$M2_HOME/conf/settings.xml"));

    this.globalToolchainsText = this.createFileSelectionWidgets(composite,
        Messages.MavenSettingsPreferencePage_globalToolchainsLabel, mavenConfiguration.getGlobalToolchainsFile(),
        new File("$M2_HOME/conf/toolchains.xml"));

    createHeading(composite, Messages.MavenSettingsPreferencePage_UserPreferences, true);

    this.userSettingsText = this.createFileSelectionWidgets(composite,
        Messages.MavenSettingsPreferencePage_userSettingsLabel, this.mavenConfiguration.getUserSettingsFile(),
        SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE);

    this.userToolchainsText = this.createFileSelectionWidgets(composite,
        Messages.MavenSettingsPreferencePage_userToolchainsLabel, this.mavenConfiguration.getUserToolchainsFile(),
        MavenCli.DEFAULT_USER_TOOLCHAINS_FILE);

    createHeading(composite, Messages.MavenSettingsPreferencePage_mergedSettings, true);

    Label lblCaption = new Label(composite, SWT.NONE);
    lblCaption.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    lblCaption.setText(Messages.MavenSettingsPreferencePage_localRepoLabel);

    localRepositoryText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
    localRepositoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    localRepositoryText.setData("name", "localRepositoryText"); //$NON-NLS-1$ //$NON-NLS-2$
    localRepositoryText.setEditable(false);

    Button updateSettings = new Button(composite, SWT.NONE);
    updateSettings.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    updateSettings.setText(Messages.MavenSettingsPreferencePage_btnUpdate_text);
    updateSettings.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateSettings(true)));

    checkSettings();
    updateLocalRepository();

    return composite;
  }

  /**
   * @param composite
   * @param mavenSettingsPreferencePage_globalPreferences
   */
  private void createHeading(Composite composite, String caption, boolean sep) {
    if(sep) {
      Label lblSpace = new Label(composite, SWT.NONE);
      GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1);
      gridData.heightHint = 20;
      lblSpace.setLayoutData(gridData);
    }

    Label lblCaption = new Label(composite, SWT.NONE);
    lblCaption.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
    lblCaption.setText(caption);

    Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.SHADOW_NONE);
    GridData gd_separator = new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1);
    gd_separator.heightHint = 10;
    separator.setLayoutData(gd_separator);
  }



  private Text createFileSelectionWidgets(Composite composite, String label, String selectedFile, File defaultFile) {
    Label lbSetting = new Label(composite, SWT.NONE);
    lbSetting.setLayoutData(new GridData(SWT.LEFT, SWT.LEFT, false, false, 1, 1));
    lbSetting.setText(label);

    Text fileText = new Text(composite, SWT.BORDER);
    fileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    if(defaultFile != null) {
      fileText.setMessage(defaultFile.getAbsolutePath());
    }
    if(selectedFile != null) {
      fileText.setText(selectedFile);
    }
    fileText.addModifyListener(modifyevent -> {
      updateLocalRepository();
      checkSettings();
    });

    Button browseButton = new Button(composite, SWT.NONE);
    browseButton.setLayoutData(new GridData(SWT.LEFT, SWT.LEFT, false, false, 1, 1));
    browseButton.setText(Messages.MavenSettingsPreferencePage_btnBrowse_text);
    browseButton.setToolTipText(Messages.MavenSettingsPreferencePage_btnBrowse_tooltip);
    browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> browseSettingsAction(fileText)));

    Button editButton = new Button(composite, SWT.NONE);
    editButton.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, false, false, 1, 1));
    editButton.setText(Messages.MavenSettingsPreferencePage_btnEdit_text);
    editButton.setToolTipText(Messages.MavenSettingsPreferencePage_btnEdit_tooltip);
    editButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      final String path = fileText.getText();
      final File file = (path.isBlank()) ? defaultFile : new File(path);
      if(file != null) {
        openEditor(file);
      }
    }));
    return fileText;
  }

  protected void updateLocalRepository() {
    String globalSettings = getGlobalSettings();
    String userSettings = getUserSettings();
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
    String globalToolchains = getGlobalToolchains();
    String userSettings = getUserSettings();
    String userToolchains = getUserToolchains();

    setMessage(null);
    checkSettings(globalSettings, Messages.MavenSettingsPreferencePage_error_globalSettingsMissing,
        l -> maven.validateSettings(l).stream().map(SettingsProblem::getMessage),
        Messages.MavenSettingsPreferencePage_error_globalSettingsParse);
    checkSettings(globalToolchains, Messages.MavenSettingsPreferencePage_error_globalToolchainsMissing,
        l -> IMavenToolbox.of(maven).validateToolchains(l).stream().map(Problem::getMessage),
        Messages.MavenSettingsPreferencePage_error_globalToolchainsParse);

    checkSettings(userSettings, Messages.MavenSettingsPreferencePage_error_userSettingsMissing,
        l -> maven.validateSettings(l).stream().map(SettingsProblem::getMessage),
        Messages.MavenSettingsPreferencePage_error_userSettingsParse);
    checkSettings(userToolchains, Messages.MavenSettingsPreferencePage_error_userToolchainsMissing,
        l -> IMavenToolbox.of(maven).validateToolchains(l).stream().map(Problem::getMessage),
        Messages.MavenSettingsPreferencePage_error_userToolchainsParse);
  }

  private void checkSettings(String location, String errorMissing, Function<String, Stream<String>> validator,
      String errorParse) {
    if(location != null) {
      String newMessage = !new File(location).canRead() //
          ? errorMissing
          : validator.apply(location).findFirst().map(msg -> NLS.bind(errorParse, msg)).orElse(null);
      if(newMessage != null) {
        String prefix = getMessage() != null ? getMessage() + " and " : "";
        setMessage(prefix + newMessage, IMessageProvider.WARNING);
      }
    }
  }

  void openEditor(File file) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    IWorkbenchPage page = window.getActivePage();
    IEditorDescriptor desc = workbench.getEditorRegistry().getDefaultEditor(file.getName()); //$NON-NLS-1$
    IEditorInput input = new FileStoreEditorInput(EFS.getLocalFileSystem().fromLocalFile(file.getAbsoluteFile()));
    try {
      IEditorPart editor = IDE.openEditor(page, input, desc.getId());
      if(editor == null) {
        //external editor was opened
        return;
      }
      editor.addPropertyListener((source, propId) -> {
        if(!editor.isDirty()) {
          log.info("Refreshing settings {}", file); //$NON-NLS-1$
        }
      });
    } catch(PartInitException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private String getUserSettings() {
    return getSettings(userSettingsText);
  }

  private String getUserToolchains() {
    return getSettings(userToolchainsText);
  }

  private String getGlobalSettings() {
    return getSettings(globalSettingsText);
  }

  private String getGlobalToolchains() {
    return getSettings(globalToolchainsText);
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
    if(file != null && !file.isBlank()) {
      settings.setText(file.strip());
      updateLocalRepository();
      checkSettings();
    }
  }
}
