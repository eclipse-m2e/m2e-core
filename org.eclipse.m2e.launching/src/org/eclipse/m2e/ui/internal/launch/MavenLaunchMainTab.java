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

package org.eclipse.m2e.ui.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenGoalSelectionDialog;
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

  private MavenRuntimeSelector runtimeSelector;

  protected Text userSettings;

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
    browseWorkspaceButton.setText(Messages.launchBrowseWorkspace); //$NON-NLS-1$
    browseWorkspaceButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), //
            ResourcesPlugin.getWorkspace().getRoot(), false, Messages.launchChoosePomDir); //$NON-NLS-1$
        dialog.showClosedProjects(false);

        int buttonId = dialog.open();
        if(buttonId == IDialogConstants.OK_ID) {
          Object[] resource = dialog.getResult();
          if(resource != null && resource.length > 0) {
            String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
                .generateVariableExpression("workspace_loc", ((IPath) resource[0]).toString()); //$NON-NLS-1$
            pomDirNameText.setText(fileLoc);
            entriesChanged();
          }
        }
      }
    });

    final Button browseFilesystemButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseFilesystemButton.setText(Messages.launchBrowseFs); //$NON-NLS-1$
    browseFilesystemButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
        dialog.setFilterPath(pomDirNameText.getText());
        String text = dialog.open();
        if(text != null) {
          pomDirNameText.setText(text);
          entriesChanged();
        }
      }
    });

    final Button browseVariablesButton = new Button(pomDirButtonsComposite, SWT.NONE);
    browseVariablesButton.setText(Messages.launchBrowseVariables); //$NON-NLS-1$
    browseVariablesButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
        dialog.open();
        String variable = dialog.getVariableExpression();
        if(variable != null) {
          pomDirNameText.insert(variable);
        }
      }
    });

    // pom file

    // goals

    Label goalsLabel = new Label(mainComposite, SWT.NONE);
    GridData gd_goalsLabel = new GridData();
    gd_goalsLabel.verticalIndent = 7;
    goalsLabel.setLayoutData(gd_goalsLabel);
    goalsLabel.setText(Messages.launchGoalsLabel); //$NON-NLS-1$
    goalsText = new Text(mainComposite, SWT.BORDER);
    goalsText.setData("name", "goalsText"); //$NON-NLS-1$ //$NON-NLS-2$
    GridData gd_goalsText = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
    gd_goalsText.verticalIndent = 7;
    goalsText.setLayoutData(gd_goalsText);
    goalsText.addModifyListener(modyfyingListener);
    goalsText.addFocusListener(new GoalsFocusListener(goalsText));

    Button selectGoalsButton = new Button(mainComposite, SWT.NONE);
    GridData gd_selectGoalsButton = new GridData(SWT.FILL, SWT.CENTER, false, false);
    gd_selectGoalsButton.verticalIndent = 7;
    selectGoalsButton.setLayoutData(gd_selectGoalsButton);
    selectGoalsButton.setText(Messages.launchGoals); //$NON-NLS-1$
    selectGoalsButton.addSelectionListener(new GoalSelectionAdapter(goalsText));

    Label profilesLabel = new Label(mainComposite, SWT.NONE);
    profilesLabel.setText(Messages.launchProfilesLabel); //$NON-NLS-1$
    // profilesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    profilesText = new Text(mainComposite, SWT.BORDER);
    profilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
    profilesText.addModifyListener(modyfyingListener);

    Label lblUserSettings = new Label(mainComposite, SWT.NONE);
    lblUserSettings.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    lblUserSettings.setText(Messages.MavenLaunchMainTab_lblUserSettings_text);

    userSettings = new Text(mainComposite, SWT.BORDER);
    userSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    userSettings.addModifyListener(modyfyingListener);

    Button btnUserSettings = new Button(mainComposite, SWT.NONE);
    btnUserSettings.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell());
        String file = dialog.open();
        if(file != null) {
          userSettings.setText(file);
          entriesChanged();
        }
      }
    });
    btnUserSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnUserSettings.setText(Messages.MavenLaunchMainTab_btnUserSettings_text);
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
      threadsCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY | SWT.SINGLE);
      for(int i = 1; i <= processors; i++ ) {
        threadsCombo.add(Integer.toString(i));
      }
      threadsCombo.setEnabled(processors > 1);
      threadsCombo.addSelectionListener(modyfyingListener);

      Label threadsLabel = new Label(composite, SWT.NONE);
      threadsLabel.setText(org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_lblThreads);
      threadsLabel.setToolTipText("--threads"); //$NON-NLS-1$
    }
    new Label(mainComposite, SWT.NONE);
    new Label(mainComposite, SWT.NONE);

    TableViewer tableViewer = new TableViewer(mainComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
    tableViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        TableItem[] selection = propsTable.getSelection();
        if(selection.length == 1) {
          editProperty(selection[0].getText(0), selection[0].getText(1));
        }
      }
    });
    tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

      public void selectionChanged(SelectionChangedEvent event) {
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
      }

    });

    this.propsTable = tableViewer.getTable();
    //this.tProps.setItemCount(10);
    this.propsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 3));
    this.propsTable.setLinesVisible(true);
    this.propsTable.setHeaderVisible(true);

    final TableColumn propColumn = new TableColumn(this.propsTable, SWT.NONE, 0);
    propColumn.setWidth(120);
    propColumn.setText(Messages.launchPropName); //$NON-NLS-1$

    final TableColumn valueColumn = new TableColumn(this.propsTable, SWT.NONE, 1);
    valueColumn.setWidth(200);
    valueColumn.setText(Messages.launchPropValue); //$NON-NLS-1$

    final Button addPropButton = new Button(mainComposite, SWT.NONE);
    addPropButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addPropButton.setText(Messages.launchPropAddButton); //$NON-NLS-1$
    addPropButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        addProperty();
      }
    });
    editPropButton = new Button(mainComposite, SWT.NONE);
    editPropButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editPropButton.setText(Messages.launchPropEditButton); //$NON-NLS-1$
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
    removePropButton.setText(Messages.launchPropRemoveButton); //$NON-NLS-1$
    removePropButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(propsTable.getSelectionCount() > 0) {
          propsTable.remove(propsTable.getSelectionIndices());
          entriesChanged();
        }
      }
    });
    removePropButton.setEnabled(false);

    runtimeSelector = new MavenRuntimeSelector(mainComposite);
    runtimeSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 5, 1));
    runtimeSelector.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        entriesChanged();
      }
    });

    goalsText.setFocus();
  }

  protected Shell getShell() {
    return super.getShell();
  }

  void addProperty() {
    MavenPropertyDialog dialog = getMavenPropertyDialog(
        org.eclipse.m2e.internal.launch.Messages.MavenLaunchMainTab_property_dialog_title, "", ""); //$NON-NLS-2$ //$NON-NLS-3$
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
        variablesButton.setText(Messages.launchPropertyDialogBrowseVariables); //$NON-NLS-1$;

        variablesButton.addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent se) {
            StringVariableSelectionDialog variablesDialog = new StringVariableSelectionDialog(getShell());
            if(variablesDialog.open() == IDialogConstants.OK_ID) {
              String variable = variablesDialog.getVariableExpression();
              if(variable != null) {
                valueText.insert(variable.trim());
              }
            }
          }
        });

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

      this.userSettings.setText(getAttribute(configuration, ATTR_USER_SETTINGS, ""));
      this.userSettings.setMessage(nvl(mavenConfiguration.getUserSettingsFile(), ""));

      this.offlineButton.setSelection(getAttribute(configuration, ATTR_OFFLINE, mavenConfiguration.isOffline()));
      this.debugOutputButton.setSelection(getAttribute(configuration, ATTR_DEBUG_OUTPUT,
          mavenConfiguration.isDebugOutput()));

      this.updateSnapshotsButton.setSelection(getAttribute(configuration, ATTR_UPDATE_SNAPSHOTS, false));
      this.skipTestsButton.setSelection(getAttribute(configuration, ATTR_SKIP_TESTS, false));
      this.nonRecursiveButton.setSelection(getAttribute(configuration, ATTR_NON_RECURSIVE, false));
      this.enableWorkspaceResolution.setSelection(getAttribute(configuration, ATTR_WORKSPACE_RESOLUTION, false));
      this.threadsCombo.select(getAttribute(configuration, ATTR_THREADS, 1) - 1);

      this.runtimeSelector.initializeFrom(configuration);

      this.propsTable.removeAll();

      @SuppressWarnings("unchecked")
      List<String> properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
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

  private static String nvl(String str, String nullValue) {
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

    // store as String in "param=value" format
    List<String> properties = new ArrayList<String>();
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
    return Messages.launchMainTabName; //$NON-NLS-1$
  }

  public boolean isValid(ILaunchConfiguration launchConfig) {
    setErrorMessage(null);

    String pomFileName = this.pomDirNameText.getText();
    if(pomFileName == null || pomFileName.trim().length() == 0) {
      setErrorMessage(Messages.launchPomDirectoryEmpty);
      return false;
    }
    if(!isDirectoryExist(pomFileName)) {
      setErrorMessage(Messages.launchPomDirectoryDoesntExist);
      return false;
    }
    return true;
  }

  protected boolean isDirectoryExist(String name) {
    if(name == null || name.trim().length() == 0) {
      return false;
    }
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
    return true;
  }

  void entriesChanged() {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }

  private static final class GoalsFocusListener extends FocusAdapter {
    private Text text;

    public GoalsFocusListener(Text text) {
      this.text = text;
    }

    public void focusGained(FocusEvent e) {
      super.focusGained(e);
      text.setData("focus"); //$NON-NLS-1$
    }
  }

  private final class GoalSelectionAdapter extends SelectionAdapter {
    private Text text;

    public GoalSelectionAdapter(Text text) {
      this.text = text;
    }

    public void widgetSelected(SelectionEvent e) {
//        String fileName = Util.substituteVar(fPomDirName.getText());
//        if(!isDirectoryExist(fileName)) {
//          MessageDialog.openError(getShell(), Messages.getString("launch.errorPomMissing"),
//              Messages.getString("launch.errorSelectPom")); //$NON-NLS-1$ //$NON-NLS-2$
//          return;
//        }
      MavenGoalSelectionDialog dialog = new MavenGoalSelectionDialog(getShell());
      int rc = dialog.open();
      if(rc == IDialogConstants.OK_ID) {
        text.insert(""); // clear selected text //$NON-NLS-1$

        String txt = text.getText();
        int len = txt.length();
        int pos = text.getCaretPosition();

        StringBuffer sb = new StringBuffer();
        if((pos > 0 && txt.charAt(pos - 1) != ' ')) {
          sb.append(' ');
        }

        String sep = ""; //$NON-NLS-1$
        Object[] o = dialog.getResult();
        for(int i = 0; i < o.length; i++ ) {
          if(o[i] instanceof MavenGoalSelectionDialog.Entry) {
            if(dialog.isQualifiedName()) {
              sb.append(sep).append(((MavenGoalSelectionDialog.Entry) o[i]).getQualifiedName());
            } else {
              sb.append(sep).append(((MavenGoalSelectionDialog.Entry) o[i]).getName());
            }
          }
          sep = " "; //$NON-NLS-1$
        }

        if(pos < len && txt.charAt(pos) != ' ') {
          sb.append(' ');
        }

        text.insert(sb.toString());
        text.setFocus();
        entriesChanged();
      }
    }
  }

}
