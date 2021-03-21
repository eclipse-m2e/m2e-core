/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others
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

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.archetype.metadata.RequiredProperty;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;

import org.eclipse.m2e.core.archetype.ArchetypeUtil;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.components.TextComboBoxCellEditor;


/**
 * Wizard page responsible for gathering information about the Maven2 artifact when an archetype is being used to create
 * a project (thus the class name pun).
 */
public class MavenProjectWizardArchetypeParametersPage extends AbstractMavenWizardPage {

  private static final Logger log = LoggerFactory.getLogger(MavenProjectWizardArchetypeParametersPage.class);

  public static final String DEFAULT_VERSION = "0.0.1-SNAPSHOT"; //$NON-NLS-1$

  @Deprecated
  public static final String _DEFAULT_PACKAGE = ""; //$NON-NLS-1$

  Table propertiesTable;

  TableViewer propertiesViewer;

  final public static String KEY_PROPERTY = "key"; //$NON-NLS-1$

  final public static int KEY_INDEX = 0;

  final public static String VALUE_PROPERTY = "value"; //$NON-NLS-1$

  final public static int VALUE_INDEX = 1;

  /** group id text field */
  protected Combo groupIdCombo;

  /** artifact id text field */
  protected Combo artifactIdCombo;

  /** version text field */
  protected Combo versionCombo;

  /** package text field */
  protected Combo packageCombo;

  protected Button removeButton;

  private boolean isUsed = true;

  protected Set<String> requiredProperties;

  protected Set<String> optionalProperties;

  protected Archetype archetype;

  protected boolean archetypeChanged = false;

  /** shows if the package has been customized by the user */
  protected boolean packageCustomized = false;

  /** Creates a new page. */
  public MavenProjectWizardArchetypeParametersPage(ProjectImportConfiguration projectImportConfiguration) {
    super("Maven2ProjectWizardArchifactPage", projectImportConfiguration); //$NON-NLS-1$

    setTitle(Messages.wizardProjectPageMaven2Title);
    setDescription(Messages.wizardProjectPageMaven2ArchetypeParametersDescription);
    setPageComplete(false);

    requiredProperties = new HashSet<>();
    optionalProperties = new HashSet<>();
  }

  /** Creates page controls. */
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    composite.setLayout(new GridLayout(3, false));

    createArtifactGroup(composite);
    createPropertiesGroup(composite);

    validate();

    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
    resolverConfigurationComponent.setModifyListener(e -> validate());

    setControl(composite);

  }

  private void createArtifactGroup(Composite parent) {
//    Composite artifactGroup = new Composite(parent, SWT.NONE);
//    GridData gd_artifactGroup = new GridData( SWT.FILL, SWT.FILL, true, false );
//    artifactGroup.setLayoutData(gd_artifactGroup);
//    artifactGroup.setLayout(new GridLayout(2, false));

    Label groupIdlabel = new Label(parent, SWT.NONE);
    groupIdlabel.setText(Messages.artifactComponentGroupId);

    groupIdCombo = new Combo(parent, SWT.BORDER);
    groupIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    addFieldWithHistory("groupId", groupIdCombo); //$NON-NLS-1$
    groupIdCombo.setData("name", "groupId"); //$NON-NLS-1$ //$NON-NLS-2$
    groupIdCombo.addModifyListener(e -> {
      updateJavaPackage();
      validate();
    });

    Label artifactIdLabel = new Label(parent, SWT.NONE);
    artifactIdLabel.setText(Messages.artifactComponentArtifactId);

    artifactIdCombo = new Combo(parent, SWT.BORDER);
    artifactIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    addFieldWithHistory("artifactId", artifactIdCombo); //$NON-NLS-1$
    artifactIdCombo.setData("name", "artifactId"); //$NON-NLS-1$ //$NON-NLS-2$
    artifactIdCombo.addModifyListener(e -> {
      updateJavaPackage();
      validate();
    });

    Label versionLabel = new Label(parent, SWT.NONE);
    versionLabel.setText(Messages.artifactComponentVersion);

    versionCombo = new Combo(parent, SWT.BORDER);
    GridData gd_versionCombo = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_versionCombo.widthHint = 150;
    versionCombo.setLayoutData(gd_versionCombo);
    versionCombo.setText(DEFAULT_VERSION);
    addFieldWithHistory("version", versionCombo); //$NON-NLS-1$
    versionCombo.addModifyListener(e -> validate());

    Label packageLabel = new Label(parent, SWT.NONE);
    packageLabel.setText(Messages.artifactComponentPackage);

    packageCombo = new Combo(parent, SWT.BORDER);
    packageCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    packageCombo.setData("name", "package"); //$NON-NLS-1$ //$NON-NLS-2$
    addFieldWithHistory("package", packageCombo); //$NON-NLS-1$
    packageCombo.addModifyListener(e -> {
      if(!packageCustomized && !packageCombo.getText().equals(getDefaultJavaPackage())) {
        packageCustomized = true;
      }
      validate();
    });
  }

  private void createPropertiesGroup(Composite composite) {
    Label propertiesLabel = new Label(composite, SWT.NONE);
    propertiesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    propertiesLabel
        .setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypeParametersPage_lblProps);

    propertiesViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
    propertiesTable = propertiesViewer.getTable();
    propertiesTable.setLinesVisible(true);
    propertiesTable.setHeaderVisible(true);
    propertiesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));

    TableColumn propertiesTableNameColumn = new TableColumn(propertiesTable, SWT.NONE);
    propertiesTableNameColumn.setWidth(130);
    propertiesTableNameColumn
        .setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypeParametersPage_columnName);

    TableColumn propertiesTableValueColumn = new TableColumn(propertiesTable, SWT.NONE);
    propertiesTableValueColumn.setWidth(230);
    propertiesTableValueColumn
        .setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypeParametersPage_columnValue);

    propertiesViewer.setColumnProperties(new String[] {KEY_PROPERTY, VALUE_PROPERTY});

    propertiesViewer.setCellEditors(new CellEditor[] {new TextCellEditor(propertiesTable, SWT.NONE),
        new TextCellEditor(propertiesTable, SWT.NONE)});
    propertiesViewer.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }

      public void modify(Object element, String property, Object value) {
        if(element instanceof TableItem) {
          ((TableItem) element).setText(getTextIndex(property), String.valueOf(value));
          validate();
        }
      }

      public Object getValue(Object element, String property) {
        if(element instanceof TableItem) {
          return ((TableItem) element).getText(getTextIndex(property));
        }
        return null;
      }
    });

    Button addButton = new Button(composite, SWT.NONE);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addButton.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypeParametersPage_btnAdd);
    addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      TableItem item = addTableItem("?", "?"); //$NON-NLS-1$ //$NON-NLS-2$
      propertiesTable.setFocus();
      propertiesViewer.editElement(item, KEY_INDEX);
      propertiesViewer.setSelection(new StructuredSelection(item.getData()));
    }));

    removeButton = new Button(composite, SWT.NONE);
    removeButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    removeButton.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypeParametersPage_btnRemove);
    removeButton.setEnabled(propertiesTable.getSelectionCount() > 0);
    removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      propertiesTable.remove(propertiesTable.getSelectionIndices());
      removeButton.setEnabled(propertiesTable.getSelectionCount() > 0);
      validate();
    }));

    propertiesTable.addSelectionListener(
        SelectionListener.widgetSelectedAdapter(e -> removeButton.setEnabled(propertiesTable.getSelectionCount() > 0)
      ));
  }

  /**
   * Validates the contents of this wizard page.
   * <p>
   * Feedback about the validation is given to the user by displaying error messages or informative messages on the
   * wizard page. Depending on the provided user input, the wizard page is marked as being complete or not.
   * <p>
   * If some error or missing input is detected in the user input, an error message or informative message,
   * respectively, is displayed to the user. If the user input is complete and correct, the wizard page is marked as
   * begin complete to allow the wizard to proceed. To that end, the following conditions must be met:
   * <ul>
   * <li>The user must have provided a valid group ID.</li>
   * <li>The user must have provided a valid artifact ID.</li>
   * <li>The user must have provided a version for the artifact.</li>
   * </ul>
   * </p>
   * 
   * @see org.eclipse.jface.dialogs.DialogPage#setMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setErrorMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setPageComplete(boolean)
   */
  void validate() {
    if(isVisible()) {
      String error = validateInput();
      setErrorMessage(error);
      setPageComplete(error == null);
    }
  }

  private String validateInput() {
    String error = validateGroupIdInput(groupIdCombo.getText().trim());
    if(error != null) {
      return error;
    }

    error = validateArtifactIdInput(artifactIdCombo.getText().trim());
    if(error != null) {
      return error;
    }

    String versionValue = versionCombo.getText().trim();
    if(versionValue.length() == 0) {
      return Messages.wizardProjectPageMaven2ValidatorVersion;
    }
    //TODO: check validity of version?

    String packageName = packageCombo.getText();
    if(packageName.trim().length() != 0) {
      if(!Pattern.matches("[A-Za-z_$][A-Za-z_$\\d]*(?:\\.[A-Za-z_$][A-Za-z_$\\d]*)*", packageName)) { //$NON-NLS-1$
        return org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypeParametersPage_error_package;
      }
    }

    // validate project name
    IStatus nameStatus = getImportConfiguration().validateProjectName(getModel());
    if(!nameStatus.isOK()) {
      return NLS.bind(Messages.wizardProjectPageMaven2ValidatorProjectNameInvalid, nameStatus.getMessage());
    }

    if(requiredProperties.size() > 0) {
      Properties properties = getProperties();
      for(String key : requiredProperties) {
        String value = properties.getProperty(key);
        if(value == null || value.length() == 0) {
          return NLS.bind(Messages.wizardProjectPageMaven2ValidatorRequiredProperty, key);
        }
      }
    }

    return null;
  }

  /** Ends the wizard flow chain. */
  public IWizardPage getNextPage() {
    return null;
  }

  public void setArchetype(Archetype archetype) {
    if(archetype == null) {
      propertiesTable.removeAll();
      archetypeChanged = false;
    } else if(!ArchetypeUtil.areEqual(archetype, this.archetype)) {
      this.archetype = archetype;
      propertiesTable.removeAll();
      requiredProperties.clear();
      optionalProperties.clear();
      archetypeChanged = true;

      Properties properties = archetype.getProperties();
      if(properties != null) {
        for(Entry<Object, Object> entry : properties.entrySet()) {
          Map.Entry<?, ?> e = entry;
          String key = (String) e.getKey();
          addTableItem(key, (String) e.getValue());
          optionalProperties.add(key);
        }
      }
    }
  }

  void loadArchetypeDescriptor() {

    try {
      RequiredPropertiesLoader propertiesLoader = new RequiredPropertiesLoader(archetype);
      getContainer().run(true, true, propertiesLoader);

      List<?> properties = propertiesLoader.getProperties();
      if(properties != null) {
        for(Object o : properties) {
          if(o instanceof RequiredProperty) {
            RequiredProperty rp = (RequiredProperty) o;
            requiredProperties.add(rp.getKey());
            addTableItem(rp.getKey(), rp.getDefaultValue());
          }
        }
      }

    } catch(InterruptedException ex) {
      // ignore
    } catch(InvocationTargetException ex) {
      String msg = NLS.bind(
          org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypeParametersPage_error_download,
          getName(archetype));
      log.error(msg, ex);
      setErrorMessage(msg + "\n" + ex.toString()); //$NON-NLS-1$
    }
  }

  static String getName(Archetype archetype) {
    final String groupId = archetype.getGroupId();
    final String artifactId = archetype.getArtifactId();
    final String version = archetype.getVersion();
    return groupId + ":" + artifactId + ":" + version; //$NON-NLS-1$ //$NON-NLS-2$
  }

  private static class RequiredPropertiesLoader implements IRunnableWithProgress {

    private Archetype archetype;

    private List<?> properties;

    RequiredPropertiesLoader(Archetype archetype) {
      this.archetype = archetype;
    }

    List<?> getProperties() {
      return properties;
    }

    public void run(IProgressMonitor monitor) {
      String archetypeName = getName(archetype);
      monitor.beginTask(NLS.bind(
          org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypeParametersPage_task, archetypeName),
          IProgressMonitor.UNKNOWN);

      try {

        ArchetypeManager archetypeManager = MavenPluginActivator.getDefault().getArchetypeManager();

        ArtifactRepository remoteArchetypeRepository = archetypeManager.getArchetypeRepository(archetype);

        properties = archetypeManager.getRequiredProperties(archetype, remoteArchetypeRepository, monitor);

      } catch(UnknownArchetype e) {
        log.error(NLS.bind("Error downloading archetype {0}", archetypeName), e); //$NON-NLS-1$
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      } finally {
        monitor.done();
      }
    }
  }

  /**
   * @param key
   * @param value
   */
  TableItem addTableItem(String key, String value) {
    TableItem item = new TableItem(propertiesTable, SWT.NONE);
    item.setData(item);
    item.setText(KEY_INDEX, key);
    item.setText(VALUE_INDEX, value == null ? "" : value); //$NON-NLS-1$
    return item;
  }

  /**
   * Updates the properties when a project name is set on the first page of the wizard.
   */
  public void setProjectName(String projectName) {
    if(artifactIdCombo.getText().equals(groupIdCombo.getText())) {
      groupIdCombo.setText(projectName);
    }
    artifactIdCombo.setText(projectName);
    packageCombo.setText("org." + projectName.replace('-', '.')); //$NON-NLS-1$
    validate();
  }

  /**
   * Updates the properties when a project name is set on the first page of the wizard.
   */
  public void setParentProject(String groupId, String artifactId, String version) {
    groupIdCombo.setText(groupId);
    versionCombo.setText(version);
    validate();
  }

  /** Enables or disables the artifact id text field. */
  public void setArtifactIdEnabled(boolean b) {
    artifactIdCombo.setEnabled(b);
  }

  /** Returns the package name. */
  public String getJavaPackage() {
    if(packageCombo.getText().length() > 0) {
      return packageCombo.getText();
    }
    return getDefaultJavaPackage();
  }

  /** Updates the package name if the related fields changed. */
  protected void updateJavaPackage() {
    if(packageCustomized) {
      return;
    }

    String defaultPackageName = getDefaultJavaPackage();
    packageCombo.setText(defaultPackageName);
  }

  /** Returns the default package name. */
  protected String getDefaultJavaPackage() {
    return MavenProjectWizardArchetypeParametersPage.getDefaultJavaPackage(groupIdCombo.getText().trim(),
        artifactIdCombo.getText().trim());
  }

  /** Creates the Model object. */
  public Model getModel() {
    Model model = new Model();

    model.setModelVersion("4.0.0"); //$NON-NLS-1$
    model.setGroupId(groupIdCombo.getText());
    model.setArtifactId(artifactIdCombo.getText());
    model.setVersion(versionCombo.getText());

    return model;
  }

  public void setUsed(boolean isUsed) {
    this.isUsed = isUsed;
  }

  public boolean isPageComplete() {
    return !isUsed || super.isPageComplete();
  }

  /** Loads the group value when the page is displayed. */
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    boolean shouldValidate = false;

    if(visible) {

      if(archetypeChanged && archetype != null) {
        archetypeChanged = false;
        loadArchetypeDescriptor();
        shouldValidate = true;
      }

      if(groupIdCombo.getText().length() == 0 && groupIdCombo.getItemCount() > 0) {
        groupIdCombo.setText(groupIdCombo.getItem(0));
        packageCombo.setText(getDefaultJavaPackage());
        packageCustomized = false;
      }

      if(shouldValidate) {
        validate();
      }

      updatePropertyEditors();
    }
  }

  public Properties getProperties() {
    if(propertiesViewer.isCellEditorActive()) {
      propertiesTable.setFocus();
    }
    Properties properties = new Properties();
    for(int i = 0; i < propertiesTable.getItemCount(); i++ ) {
      TableItem item = propertiesTable.getItem(i);
      properties.put(item.getText(KEY_INDEX), item.getText(VALUE_INDEX));
    }
    return properties;
  }

  public int getTextIndex(String property) {
    return KEY_PROPERTY.equals(property) ? KEY_INDEX : VALUE_INDEX;
  }

  public void updatePropertyEditors() {
    CellEditor[] ce = propertiesViewer.getCellEditors();

    int n = requiredProperties.size() + optionalProperties.size();
    if(n == 0) {
      if(ce[KEY_INDEX] instanceof TextComboBoxCellEditor) {
        // if there was a combo editor previously defined, and the current
        // archetype has no properties, replace it with a plain text editor
        ce[KEY_INDEX].dispose();
        ce[KEY_INDEX] = new TextCellEditor(propertiesTable, SWT.FLAT);
      }
    } else {
      TextComboBoxCellEditor comboEditor = null;
      // if there was a plain text editor previously defined, and the current
      // archetype has properties, replace it with a combo editor
      if(ce[KEY_INDEX] instanceof TextComboBoxCellEditor) {
        comboEditor = (TextComboBoxCellEditor) ce[KEY_INDEX];
      } else {
        ce[KEY_INDEX].dispose();
        comboEditor = new TextComboBoxCellEditor(propertiesTable, SWT.FLAT);
        ce[KEY_INDEX] = comboEditor;
      }

      // populate the property name selection
      List<String> propertyKeys = new ArrayList<>(n);
      propertyKeys.addAll(requiredProperties);
      propertyKeys.addAll(optionalProperties);
      comboEditor.setItems(propertyKeys.toArray(new String[n]));
    }
  }

  public static String getDefaultJavaPackage(String groupId, String artifactId) {
    StringBuilder sb = new StringBuilder(groupId);

    if(sb.length() > 0 && artifactId.length() > 0) {
      sb.append('.');
    }

    sb.append(artifactId);

    boolean isFirst = true;
    StringBuilder pkg = new StringBuilder();
    for(int i = 0; i < sb.length(); i++ ) {
      char c = sb.charAt(i);
      if(c == '-') {
        pkg.append('_');
        isFirst = false;
      } else {
        if(isFirst) {
          if(Character.isJavaIdentifierStart(c)) {
            pkg.append(c);
            isFirst = false;
          }
        } else {
          if(c == '.') {
            pkg.append('.');
            isFirst = true;
          } else if(Character.isJavaIdentifierPart(c)) {
            pkg.append(c);
          }
        }
      }
    }

    return pkg.toString();
  }

  private boolean isVisible() {
    return getControl() != null && getControl().isVisible();
  }
}
