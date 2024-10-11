/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Lars Vogel <Lars.Vogel@gmail.com> - Bug 344997, remove goal selection button
 *******************************************************************************/

package org.eclipse.m2e.ui.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenPropertyDialog;
import org.eclipse.m2e.internal.launch.LaunchingUtils;
import org.eclipse.m2e.internal.launch.Messages;


/**
 * Maven Launch dialog Main tab
 *
 * @author Dmitri Maximovich
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class MavenLaunchMainTab extends AbstractLaunchConfigurationTab implements MavenLaunchConstants {
  private static final Logger log = LoggerFactory.getLogger(MavenLaunchMainTab.class);

  public static final String ID_EXTERNAL_TOOLS_LAUNCH_GROUP = "org.eclipse.ui.externaltools.launchGroup"; //$NON-NLS-1$

  protected Text pomDirNameText;

  protected Text goalsText;

  protected Text goalsAutoBuildText;

  protected Text goalsManualBuildText;

  protected Text goalsCleanText;

  protected Text goalsAfterCleanText;

  protected Text profilesText;

  protected Table propsTable;

  private Button offlineButton;

  private Button updateSnapshotsButton;

  private Button debugOutputButton;

  private Button skipTestsButton;

  private Button nonRecursiveButton;

  private Button enableWorkspaceResolution;

  private Button removePropButton;

  private Button editPropButton;

  private Combo threadsCombo;

  private Combo colorOutputCombo;

  private MavenRuntimeSelector runtimeSelector;

  private Text userSettings;

  public MavenLaunchMainTab() {
  }

  public Image getImage() {
    return MavenImages.IMG_LAUNCH_MAIN;
  }

  /**
   * @wbp.parser.entryPoint
   */
  public void createControl(Composite parent) {
    Composite mainComposite = new Composite(parent, SWT.NONE);
    setControl(mainComposite);
    //PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, IAntUIHelpContextIds.ANT_MAIN_TAB);
    GridLayout layout = new GridLayout();
    layout.numColumns = 5;
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    mainComposite.setLayout(layout);
    mainComposite.setLayoutData(gridData);
    mainComposite.setFont(parent.getFont());

    class Listener implements ModifyListener, SelectionListener {
      public void modifyText(ModifyEvent e) {
        entriesChanged();
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        entriesChanged();
      }

      public void widgetSelected(SelectionEvent e) {
        entriesChanged();
      }
    }
    Listener modyfyingListener = new Listener();

    Label label = new Label(mainComposite, SWT.NONE);
    label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 5, 1));
    label.setText(Messages.launchPomGroup);

    this.pomDirNameText = new Text(mainComposite, SWT.BORDER);
    this.pomDirNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 5, 1));
    this.pomDirNameText.addModifyListener(modyfyingListener);

    final Composite pomDirButtonsComposite = new Composite(mainComposite, SWT.NONE);
    pomDirButtonsComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 5, 1));
    final GridLayout pomDirButtonsGridLayout = new GridLayout();
    pomDirButtonsGridLayout.marginWidth = 0;
    pomDirButtonsGridLayout.marginHeight = 0;
    pomDirButtonsGridLayout.numColumns = 3;
    pomDirButtonsComposite.setLayout(pomDirButtonsGridLayout);

    final Button browseWorkspaceButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseWorkspaceButton.setText(Messages.launchBrowseWorkspace);
    browseWorkspaceButton
        .addSelectionListener(new BrowseWorkspaceDirAction(pomDirNameText, Messages.launchChoosePomDir));

    final Button browseFilesystemButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseFilesystemButton.setText(Messages.launchBrowseFs);
    browseFilesystemButton.addSelectionListener(new BrowseDirAction(pomDirNameText));

    final Button browseVariablesButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseVariablesButton.setText(Messages.launchBrowseVariables);
    browseVariablesButton.addSelectionListener(new VariablesAction(pomDirNameText));

    // pom file

    // goals

    Label goalsLabel = new Label(mainComposite, SWT.NONE);
    GridData gd_goalsLabel = new GridData();
    gd_goalsLabel.horizontalAlignment = SWT.RIGHT;
    gd_goalsLabel.verticalIndent = 7;
    goalsLabel.setLayoutData(gd_goalsLabel);
    goalsLabel.setText(Messages.launchGoalsLabel);
    goalsText = new Text(mainComposite, SWT.BORDER);
    goalsText.setData("name", "goalsText"); //$NON-NLS-1$ //$NON-NLS-2$
    GridData gd_goalsText = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
    gd_goalsText.verticalIndent = 7;
    goalsText.setLayoutData(gd_goalsText);
    goalsText.addModifyListener(modyfyingListener);
    goalsText.addFocusListener(new GoalsFocusListener(goalsText));

    Label profilesLabel = new Label(mainComposite, SWT.NONE);
    profilesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    profilesLabel.setText(Messages.launchProfilesLabel);
    // profilesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    profilesText = new Text(mainComposite, SWT.BORDER);
    profilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
    profilesText.addModifyListener(modyfyingListener);

    Label lblUserSettings = new Label(mainComposite, SWT.NONE);
    lblUserSettings.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    lblUserSettings.setText(Messages.MavenLaunchMainTab_lblUserSettings_text);

    userSettings = new Text(mainComposite, SWT.BORDER);
    userSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
    userSettings.addModifyListener(modyfyingListener);

    final Composite userSettingsButtonsComposite = new Composite(mainComposite, SWT.NONE);
    userSettingsButtonsComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 5, 1));
    final GridLayout userSettingsButtonsGridLayout = new GridLayout();
    userSettingsButtonsGridLayout.marginWidth = 0;
    userSettingsButtonsGridLayout.marginHeight = 0;
    userSettingsButtonsGridLayout.numColumns = 3;
    userSettingsButtonsComposite.setLayout(userSettingsButtonsGridLayout);

    final Button userSettingsWorkspaceButton = new Button(userSettingsButtonsComposite, SWT.NONE);
    userSettingsWorkspaceButton.setText(Messages.launchBrowseWorkspace);
    userSettingsWorkspaceButton
        .addSelectionListener(new BrowseWorkspaceFileAction(userSettings, Messages.launchChooseSettingsFile));

    final Button userSettingsFilesystemButton = new Button(userSettingsButtonsComposite, SWT.NONE);
    userSettingsFilesystemButton.setText(Messages.launchBrowseFs);
    userSettingsFilesystemButton.addSelectionListener(new BrowseFileAction(userSettings, new String[] {"*.xml"}));

    final Button userSettingsVariablesButton = new Button(userSettingsButtonsComposite, SWT.NONE);
    userSettingsVariablesButton.setText(Messages.launchBrowseVariables);
    userSettingsVariablesButton.addSelectionListener(new VariablesAction(userSettings));

    new Label(mainComposite, SWT.NONE);
    offlineButton = new Button(mainComposite, SWT.CHECK);
    offlineButton.setToolTipText("-o");
    offlineButton.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_btnOffline);
    offlineButton.addSelectionListener(modyfyingListener);

    updateSnapshotsButton = new Button(mainComposite, SWT.CHECK);
    updateSnapshotsButton.setToolTipText("-U"); //$NON-NLS-1$
    updateSnapshotsButton.addSelectionListener(modyfyingListener);
    GridData gd_updateSnapshotsButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
    gd_updateSnapshotsButton.horizontalIndent = 10;
    updateSnapshotsButton.setLayoutData(gd_updateSnapshotsButton);
    updateSnapshotsButton.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_btnUpdateSnapshots);
    new Label(mainComposite, SWT.NONE);

    debugOutputButton = new Button(mainComposite, SWT.CHECK);
    debugOutputButton.setToolTipText("-X -e"); //$NON-NLS-1$
    debugOutputButton.addSelectionListener(modyfyingListener);
    debugOutputButton.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_btnDebugOutput);

    skipTestsButton = new Button(mainComposite, SWT.CHECK);
    skipTestsButton.setToolTipText("-Dmaven.test.skip=true"); //$NON-NLS-1$
    skipTestsButton.addSelectionListener(modyfyingListener);
    GridData gd_skipTestsButton = new GridData();
    gd_skipTestsButton.horizontalIndent = 10;
    skipTestsButton.setLayoutData(gd_skipTestsButton);
    skipTestsButton.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_btnSkipTests);

    nonRecursiveButton = new Button(mainComposite, SWT.CHECK);
    GridData gd_nonrecursiveButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_nonrecursiveButton.horizontalIndent = 10;
    nonRecursiveButton.setLayoutData(gd_nonrecursiveButton);
    nonRecursiveButton.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_btnNotRecursive);
    nonRecursiveButton.setToolTipText("-N"); //$NON-NLS-1$
    nonRecursiveButton.setData("name", "nonRecursiveButton"); //$NON-NLS-1$ //$NON-NLS-2$
    nonRecursiveButton.addSelectionListener(modyfyingListener);

    new Label(mainComposite, SWT.NONE);

    enableWorkspaceResolution = new Button(mainComposite, SWT.CHECK);
    enableWorkspaceResolution.addSelectionListener(modyfyingListener);
    enableWorkspaceResolution.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
    enableWorkspaceResolution.setData("name", "enableWorkspaceResolution"); //$NON-NLS-1$ //$NON-NLS-2$
    enableWorkspaceResolution.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_btnResolveWorkspace);

    {
      final int processors = Runtime.getRuntime().availableProcessors();
      new Label(mainComposite, SWT.NONE);
      Composite composite = new Composite(mainComposite, SWT.NONE);
      composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      composite.setLayout(gridLayout);

      Label threadsLabel = new Label(composite, SWT.NONE);
      threadsLabel.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_lblThreads);
      threadsLabel.setToolTipText("--threads"); //$NON-NLS-1$

      threadsCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY | SWT.SINGLE);
      for(int i = 1; i <= processors; i++ ) {
        threadsCombo.add(Integer.toString(i));
      }
      threadsCombo.setEnabled(processors > 1);
      threadsCombo.addSelectionListener(modyfyingListener);
    }

    {
      Composite composite = new Composite(mainComposite, SWT.NONE);
      composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      composite.setLayout(gridLayout);

      Label enableColorOutputLabel = new Label(composite, SWT.NONE);
      enableColorOutputLabel.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_lblEnableColorOutput);
      enableColorOutputLabel.setToolTipText("-Dstyle.color=auto|always|never"); //$NON-NLS-1$

      colorOutputCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY | SWT.SINGLE);
      // Warning: changing the order here would conflict with the constants in ColorOutput.
      // The current assumption is that the index in the combo is the same as the constant.
      colorOutputCombo.add(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_lblEnableColorOutput_Auto);
      colorOutputCombo.add(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_lblEnableColorOutput_Always);
      colorOutputCombo.add(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_lblEnableColorOutput_Never);
      colorOutputCombo.addSelectionListener(modyfyingListener);
    }

    TableViewer tableViewer = new TableViewer(mainComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
    tableViewer.addDoubleClickListener(event -> {
      TableItem[] selection = propsTable.getSelection();
      if(selection.length == 1) {
        editProperty(selection[0].getText(0), selection[0].getText(1));
      }
    });
    tableViewer.addSelectionChangedListener(event -> {
      TableItem[] items = propsTable.getSelection();
      if(items == null || items.length == 0) {
        editPropButton.setEnabled(false);
        removePropButton.setEnabled(false);
      } else if(items.length == 1) {
        editPropButton.setEnabled(true);
        removePropButton.setEnabled(true);
      } else {
        editPropButton.setEnabled(false);
        removePropButton.setEnabled(true);
      }
    });

    this.propsTable = tableViewer.getTable();

    TableLayout tableLayout = new TableLayout();
    ColumnWeightData weightData = new ColumnWeightData(20, true);
    tableLayout.addColumnData(weightData);
    weightData = new ColumnWeightData(80, true);
    tableLayout.addColumnData(weightData);
    this.propsTable.setLayout(tableLayout);

    //this.tProps.setItemCount(10);
    this.propsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 3));
    this.propsTable.setLinesVisible(true);
    this.propsTable.setHeaderVisible(true);

    final TableColumn propColumn = new TableColumn(this.propsTable, SWT.NONE, 0);
    propColumn.setText(Messages.launchPropName);

    final TableColumn valueColumn = new TableColumn(this.propsTable, SWT.NONE, 1);
    valueColumn.setText(Messages.launchPropValue);

    final Button addPropButton = new Button(mainComposite, SWT.NONE);
    addPropButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addPropButton.setText(Messages.launchPropAddButton);
    addPropButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> addProperty()));
    editPropButton = new Button(mainComposite, SWT.NONE);
    editPropButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editPropButton.setText(Messages.launchPropEditButton);
    editPropButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(propsTable.getSelectionCount() > 0) {
          TableItem[] selection = propsTable.getSelection();
          if(selection.length == 1) {
            editProperty(selection[0].getText(0), selection[0].getText(1));
          }
        }
      }
    });
    editPropButton.setEnabled(false);
    removePropButton = new Button(mainComposite, SWT.NONE);
    removePropButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    removePropButton.setText(Messages.launchPropRemoveButton);
    removePropButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      if(propsTable.getSelectionCount() > 0) {
        propsTable.remove(propsTable.getSelectionIndices());
        entriesChanged();
      }
    }));
    removePropButton.setEnabled(false);

    Label mavenRuntimeLabel = new Label(mainComposite, SWT.NONE);
    mavenRuntimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    mavenRuntimeLabel.setText(Messages.MavenLaunchMainTab_lblRuntime);

    runtimeSelector = new MavenRuntimeSelector(mainComposite);
    runtimeSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1));
    runtimeSelector.addSelectionChangedListener(event -> entriesChanged());
  }

  protected Shell getShell() {
    return super.getShell();
  }

  void addProperty() {
    MavenPropertyDialog dialog = getMavenPropertyDialog(
        org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_property_dialog_title, "", ""); //$NON-NLS-2$
    if(dialog.open() == IDialogConstants.OK_ID) {
      TableItem item = new TableItem(propsTable, SWT.NONE);
      item.setText(0, dialog.getName());
      item.setText(1, dialog.getValue());
      entriesChanged();
    }
  }

  void editProperty(String name, String value) {
    MavenPropertyDialog dialog = getMavenPropertyDialog(
        org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_property_dialog_edit_title, name, value);
    if(dialog.open() == IDialogConstants.OK_ID) {
      TableItem[] item = propsTable.getSelection();
      item[0].setText(0, dialog.getName());
      item[0].setText(1, dialog.getValue());
      entriesChanged();
    }
  }

  private MavenPropertyDialog getMavenPropertyDialog(String title, String initName, String initValue) {
    return new MavenPropertyDialog(getShell(), title, initName, initValue, null) {
      protected Control createDialogArea(Composite parent) {
        Composite comp = (Composite) super.createDialogArea(parent);

        Button variablesButton = new Button(comp, SWT.PUSH);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gd.horizontalSpan = 2;
        gd.widthHint = Math.max(convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH), //
            variablesButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        variablesButton.setLayoutData(gd);
        variablesButton.setFont(comp.getFont());
        variablesButton.setText(Messages.launchPropertyDialogBrowseVariables); //;

        variablesButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
          StringVariableSelectionDialog variablesDialog = new StringVariableSelectionDialog(getShell());
          if(variablesDialog.open() == IDialogConstants.OK_ID) {
            String variable = variablesDialog.getVariableExpression();
            if(variable != null) {
              valueText.insert(variable.trim());
            }
          }
        }));

        return comp;
      }
    };
  }

  public void initializeFrom(ILaunchConfiguration configuration) {
    String pomDirName = getAttribute(configuration, ATTR_POM_DIR, ""); //$NON-NLS-1$
    this.pomDirNameText.setText(pomDirName);

    this.goalsText.setText(getAttribute(configuration, ATTR_GOALS, "")); //$NON-NLS-1$

    this.profilesText.setText(getAttribute(configuration, ATTR_PROFILES, "")); //$NON-NLS-1$
    try {
      IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();

      this.offlineButton.setSelection(getAttribute(configuration, ATTR_OFFLINE, mavenConfiguration.isOffline()));
      this.debugOutputButton
          .setSelection(getAttribute(configuration, ATTR_DEBUG_OUTPUT, mavenConfiguration.isDebugOutput()));

      this.updateSnapshotsButton.setSelection(getAttribute(configuration, ATTR_UPDATE_SNAPSHOTS, false));
      this.skipTestsButton.setSelection(getAttribute(configuration, ATTR_SKIP_TESTS, false));
      this.nonRecursiveButton.setSelection(getAttribute(configuration, ATTR_NON_RECURSIVE, false));
      this.enableWorkspaceResolution.setSelection(getAttribute(configuration, ATTR_WORKSPACE_RESOLUTION, false));
      this.threadsCombo.select(getAttribute(configuration, ATTR_THREADS, 1) - 1);
      this.colorOutputCombo.select(getAttribute(configuration, ATTR_COLOR, MavenLaunchConstants.ATTR_COLOR_VALUE_AUTO));

      this.runtimeSelector.initializeFrom(configuration);

      this.userSettings.setText(getAttribute(configuration, ATTR_USER_SETTINGS, ""));
      this.userSettings.setMessage(nvl(mavenConfiguration.getUserSettingsFile(),
          SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath()));

      this.propsTable.removeAll();

      List<String> properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.emptyList());
      for(String property : properties) {
        int n = property.indexOf('=');
        String name = property;
        String value = ""; //$NON-NLS-1$
        if(n > -1) {
          name = property.substring(0, n);
          if(n > 1) {
            value = property.substring(n + 1);
          }
        }

        TableItem item = new TableItem(propsTable, SWT.NONE);
        item.setText(0, name);
        item.setText(1, value);
      }
    } catch(CoreException ex) {
      // XXX should we at least log something here?
    }
    setDirty(false);
  }

  protected static String nvl(String str, String nullValue) {
    return str != null ? str : nullValue;
  }

  private String getAttribute(ILaunchConfiguration configuration, String name, String defaultValue) {
    try {
      return configuration.getAttribute(name, defaultValue);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
      return defaultValue;
    }
  }

  private boolean getAttribute(ILaunchConfiguration configuration, String name, boolean defaultValue) {
    try {
      return configuration.getAttribute(name, defaultValue);
    } catch(CoreException ex) {
      return defaultValue;
    }
  }

  private int getAttribute(ILaunchConfiguration configuration, String name, int defaultValue) {
    try {
      return configuration.getAttribute(name, defaultValue);
    } catch(CoreException ex) {
      return defaultValue;
    }
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(ATTR_POM_DIR, this.pomDirNameText.getText());

    configuration.setAttribute(ATTR_GOALS, this.goalsText.getText());

    configuration.setAttribute(ATTR_PROFILES, this.profilesText.getText());

    configuration.setAttribute(ATTR_USER_SETTINGS, this.userSettings.getText());

    configuration.setAttribute(ATTR_OFFLINE, this.offlineButton.getSelection());
    configuration.setAttribute(ATTR_UPDATE_SNAPSHOTS, this.updateSnapshotsButton.getSelection());
    configuration.setAttribute(ATTR_SKIP_TESTS, this.skipTestsButton.getSelection());
    configuration.setAttribute(ATTR_NON_RECURSIVE, this.nonRecursiveButton.getSelection());
    configuration.setAttribute(ATTR_WORKSPACE_RESOLUTION, this.enableWorkspaceResolution.getSelection());
    configuration.setAttribute(ATTR_DEBUG_OUTPUT, this.debugOutputButton.getSelection());

    runtimeSelector.performApply(configuration);

    configuration.setAttribute(ATTR_THREADS, threadsCombo.getSelectionIndex() + 1);
    configuration.setAttribute(ATTR_COLOR, colorOutputCombo.getSelectionIndex());

    // store as String in "param=value" format
    List<String> properties = new ArrayList<>();
    for(TableItem item : this.propsTable.getItems()) {
      String p = item.getText(0);
      String v = item.getText(1);
      if(p != null && p.trim().length() > 0) {
        String prop = p.trim() + "=" + (v == null ? "" : v); //$NON-NLS-1$ //$NON-NLS-2$
        properties.add(prop);
      }
    }
    configuration.setAttribute(ATTR_PROPERTIES, properties);
  }

  public String getName() {
    return Messages.launchMainTabName;
  }

  public boolean isValid(ILaunchConfiguration launchConfig) {
    setErrorMessage(null);

    String pomFileName = this.pomDirNameText.getText();
    if(pomFileName == null || pomFileName.trim().length() == 0) {
      setErrorMessage(Messages.launchPomDirectoryEmpty);
      return false;
    }
    if(!isDirectoryExist(pomFileName)) {
      if(getErrorMessage() == null) {
        setErrorMessage(Messages.launchPomDirectoryDoesntExist);
      }
      return false;
    }
    return true;
  }

  protected boolean isDirectoryExist(String name) {
    if(name == null || name.trim().length() == 0) {
      return false;
    }
    try {
      String dirName = LaunchingUtils.substituteVar(name);
      if(dirName == null) {
        return false;
      }
      File pomDir = new File(dirName);
      if(!pomDir.exists()) {
        return false;
      }
      if(!pomDir.isDirectory()) {
        return false;
      }
    } catch(CoreException e) {
      setErrorMessage(Messages.launchErrorEvaluatingBaseDirectory);
      return false;
    }
    return true;
  }

  void entriesChanged() {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }

  private static final class GoalsFocusListener extends FocusAdapter {
    private final Text text;

    public GoalsFocusListener(Text text) {
      this.text = text;
    }

    public void focusGained(FocusEvent e) {
      super.focusGained(e);
      text.setData("focus"); //$NON-NLS-1$
    }
  }

  private class BrowseWorkspaceDirAction extends SelectionAdapter {

    private final Text target;

    private final String label;

    public BrowseWorkspaceDirAction(Text target, String label) {
      this.target = target;
      this.label = label;
    }

    public void widgetSelected(SelectionEvent e) {
      ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), //
          ResourcesPlugin.getWorkspace().getRoot(), false, label);
      dialog.showClosedProjects(false);

      int buttonId = dialog.open();
      if(buttonId == IDialogConstants.OK_ID) {
        Object[] resource = dialog.getResult();
        if(resource != null && resource.length > 0) {
          String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
              .generateVariableExpression("workspace_loc", ((IPath) resource[0]).toString()); //$NON-NLS-1$
          target.setText(fileLoc);
          entriesChanged();
        }
      }
    }
  }

  private class BrowseWorkspaceFileAction extends SelectionAdapter {

    private final Text target;

    private final String label;

    public BrowseWorkspaceFileAction(Text target, String label) {
      this.target = target;
      this.label = label;
    }

    public void widgetSelected(SelectionEvent e) {
      ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), //
          new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
      dialog.setTitle(label);
      dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());

      int buttonId = dialog.open();
      if(buttonId == IDialogConstants.OK_ID) {
        Object[] resource = dialog.getResult();
        if(resource != null && resource.length > 0) {
          String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
              .generateVariableExpression("workspace_loc", ((IResource) resource[0]).getFullPath().toString()); //$NON-NLS-1$
          target.setText(fileLoc);
          entriesChanged();
        }
      }
    }
  }

  private class BrowseDirAction extends SelectionAdapter {

    private final Text target;

    public BrowseDirAction(Text target) {
      this.target = target;
    }

    public void widgetSelected(SelectionEvent e) {
      DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
      dialog.setFilterPath(target.getText());
      String text = dialog.open();
      if(text != null) {
        target.setText(text);
        entriesChanged();
      }
    }
  }

  private class BrowseFileAction extends SelectionAdapter {

    private final Text target;

    private final String[] filter;

    public BrowseFileAction(Text target, String[] filter) {
      this.target = target;
      this.filter = filter;
    }

    public void widgetSelected(SelectionEvent e) {
      FileDialog dialog = new FileDialog(getShell(), SWT.NONE);
      dialog.setFilterPath(target.getText());
      dialog.setFilterExtensions(filter);
      String text = dialog.open();
      if(text != null) {
        target.setText(text);
        entriesChanged();
      }
    }
  }

  private class VariablesAction extends SelectionAdapter {

    private final Text target;

    public VariablesAction(Text target) {
      this.target = target;
    }

    public void widgetSelected(SelectionEvent e) {
      StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
      dialog.open();
      String variable = dialog.getVariableExpression();
      if(variable != null) {
        target.insert(variable);
      }
    }
  }
}
