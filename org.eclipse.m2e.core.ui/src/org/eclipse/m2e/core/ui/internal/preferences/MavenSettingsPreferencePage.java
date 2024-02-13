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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
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

  Text globalSettingsText;

  Text userSettingsText;

  Text localRepositoryText;

  boolean dirty = false;

  private Link globalSettingsLink;

  private Link userSettingsLink;

  private Link userToolchainsLink;

  private Text userToolchainsText;

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
    composite.setLayout(new GridLayout(2, false));

    globalSettingsLink = createLink(composite, Messages.MavenSettingsPreferencePage_globalSettingslink2,
        Messages.MavenSettingsPreferencePage_globalSettingslink_tooltip, this::getGlobalSettings, null);
    globalSettingsText = createFileSelectionWidgets(composite, mavenConfiguration.getGlobalSettingsFile(), null);

    userSettingsLink = createLink(composite, Messages.MavenSettingsPreferencePage_userSettingslink2,
        Messages.MavenSettingsPreferencePage_userSettingslink_tooltip, this::getUserSettings,
        SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE);
    userSettingsText = createFileSelectionWidgets(composite, mavenConfiguration.getUserSettingsFile(),
        SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE);

    userToolchainsLink = createLink(composite, Messages.MavenSettingsPreferencePage_userToolchainslink2,
        Messages.MavenSettingsPreferencePage_userToolchainslink_tooltip, this::getUserToolchains,
        MavenCli.DEFAULT_USER_TOOLCHAINS_FILE);
    userToolchainsText = createFileSelectionWidgets(composite, mavenConfiguration.getUserToolchainsFile(),
        MavenCli.DEFAULT_USER_TOOLCHAINS_FILE);

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

    checkSettings();
    updateLocalRepository();

    return composite;
  }

  private Link createLink(Composite composite, String text, String tooltip, Supplier<String> selectedFile,
      File defaultFile) {
    Link link = new Link(composite, SWT.NONE);
    link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    link.setText(text);
    link.setToolTipText(tooltip);
    link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      File file = Optional.ofNullable(selectedFile.get()).map(File::new).orElse(defaultFile);
      if(file != null) {
        openEditor(file);
      }
    }));
    return link;
  }

  private Text createFileSelectionWidgets(Composite composite, String selectedFile, File defaultFile) {
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
    browseButton.setLayoutData(new GridData(SWT.FILL, SWT.RIGHT, false, false, 1, 1));
    browseButton.setText(Messages.MavenSettingsPreferencePage_settingsBrowseButton_text);
    browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> browseSettingsAction(fileText)));
    return fileText;
  }

  private void updateLink(Link link, String path, File defaultFile, String activeText, String inactiveText) {
    File file = path != null ? new File(path) : defaultFile;
    boolean active = file != null && file.canRead();
    link.setText(active ? activeText : inactiveText);
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
    updateLink(globalSettingsLink, globalSettings, null, Messages.MavenSettingsPreferencePage_globalSettingslink2,
        Messages.MavenSettingsPreferencePage_globalSettingslink1);

    String userSettings = getUserSettings();
    updateLink(userSettingsLink, userSettings, SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE,
        Messages.MavenSettingsPreferencePage_userSettingslink2, Messages.MavenSettingsPreferencePage_userSettingslink1);

    String userToolchains = getUserToolchains();
    updateLink(userToolchainsLink, userToolchains, MavenCli.DEFAULT_USER_TOOLCHAINS_FILE,
        Messages.MavenSettingsPreferencePage_userToolchainslink2,
        Messages.MavenSettingsPreferencePage_userToolchainslink1);

    setMessage(null);
    checkSettings(globalSettings, Messages.MavenSettingsPreferencePage_error_globalSettingsMissing,
        l -> maven.validateSettings(l).stream().map(SettingsProblem::getMessage),
        Messages.MavenSettingsPreferencePage_error_globalSettingsParse);
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

    IEditorDescriptor desc = workbench.getEditorRegistry().getDefaultEditor("settings.xml"); //$NON-NLS-1$

    IEditorInput input = new FileStoreEditorInput(EFS.getLocalFileSystem().fromLocalFile(file));
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
