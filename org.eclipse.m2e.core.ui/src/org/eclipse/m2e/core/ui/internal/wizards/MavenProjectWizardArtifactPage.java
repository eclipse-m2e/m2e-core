/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;


/**
 * Wizard page responsible for gathering information about the Maven2 artifact and the directories to create. This
 * wizard page gathers Maven2 specific information. The user must specify the necessary information about the Maven2
 * artifact and she can also decide which directories of the default Maven2 directory structure should be created. Input
 * validation is performed in order to make sure that all the provided information is valid before letting the wizard
 * continue.
 */
public class MavenProjectWizardArtifactPage extends AbstractMavenWizardPage {

  private static final ProjectFolder JAVA = new ProjectFolder("src/main/java", "target/classes"); //$NON-NLS-1$ //$NON-NLS-2$

  private static final ProjectFolder JAVA_TEST = new ProjectFolder("src/test/java", "target/test-classes"); //$NON-NLS-1$ //$NON-NLS-2$

  private static final ProjectFolder RESOURCES = new ProjectFolder("src/main/resources", "target/classes"); //$NON-NLS-1$ //$NON-NLS-2$

  private static final ProjectFolder RESOURCES_TEST = new ProjectFolder("src/test/resources", "target/test-classes"); //$NON-NLS-1$ //$NON-NLS-2$

  // private static final ProjectFolder FILTERS = new ProjectFolder("src/main/filters", null, false);

  // private static final ProjectFolder FILTERS_TEST = new ProjectFolder("src/test/filters", null, false);

  // private static final ProjectFolder ASSEMBLY = new ProjectFolder("src/main/assembly", null, false);

  // private static final ProjectFolder CONFIG = new ProjectFolder("src/main/config", null, false);

  private static final ProjectFolder WEBAPP = new ProjectFolder("src/main/webapp", null); //$NON-NLS-1$

  private static final ProjectFolder EAR = new ProjectFolder("src/main/application", null); //$NON-NLS-1$

  private static final ProjectFolder SITE = new ProjectFolder("src/site", null); //$NON-NLS-1$

  private static final ProjectFolder[] JAR_DIRS = {JAVA, JAVA_TEST, RESOURCES, RESOURCES_TEST};

  private static final ProjectFolder[] WAR_DIRS = {JAVA, JAVA_TEST, RESOURCES, RESOURCES_TEST, WEBAPP};

  private static final ProjectFolder[] EAR_DIRS = {EAR}; // MNGECLIPSE-688 add EAR Support

  private static final ProjectFolder[] POM_DIRS = {SITE};

  /** special directory sets, default is JAR_DIRS */
  private static final Map<String, ProjectFolder[]> directorySets = new HashMap<>();
  static {
    directorySets.put(MavenArtifactComponent.WAR, WAR_DIRS);
    directorySets.put(MavenArtifactComponent.POM, POM_DIRS);
    directorySets.put(MavenArtifactComponent.EAR, EAR_DIRS); // MNGECLIPSE-688 add EAR Support
  }

  /** parent property panel */
  protected MavenParentComponent parentComponent;

  /** artifact property panel */
  protected MavenArtifactComponent artifactComponent;

  /** the parent readonly state */
  private boolean readonlyParent = false;

  private boolean isUsed;

  /**
   * Sets the title and description of this wizard page and marks it as not being complete as user input is required for
   * continuing.
   *
   * @wbp.parser.constructor
   */
  public MavenProjectWizardArtifactPage(ProjectImportConfiguration projectImportConfiguration) {
    this("MavenProjectWizardArtifactPage", projectImportConfiguration); //$NON-NLS-1$
  }

  /**
   * Sets the title and description of this wizard page and marks it as not being complete as user input is required for
   * continuing.
   */
  protected MavenProjectWizardArtifactPage(String name, ProjectImportConfiguration projectImportConfiguration) {
    super(name, projectImportConfiguration);

    setTitle(Messages.wizardProjectPageMaven2Title);
    setDescription(Messages.wizardProjectPageMaven2Description);
    setPageComplete(false);
  }

  /**
   * {@inheritDoc} This wizard page contains a <code>MavenArtifactComponent</code> to gather information about the Maven
   * artifact and a <code>Maven2DirectoriesComponent</code> which allows to choose which directories of the default
   * Maven directory structure to create.
   */
  @Override
  public void createControl(Composite parent) {
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;

    Composite container = new Composite(parent, SWT.NULL);
    container.setLayout(layout);

    WidthGroup widthGroup = new WidthGroup();
    container.addControlListener(widthGroup);

    ModifyListener modifyingListener = e -> validate();

    artifactComponent = new MavenArtifactComponent(container, SWT.NONE);
    artifactComponent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    artifactComponent.setWidthGroup(widthGroup);
    artifactComponent.setModifyingListener(modifyingListener);
    artifactComponent.setArtifactIdEditable(!readonlyParent);

    parentComponent = new MavenParentComponent(container, readonlyParent ? SWT.READ_ONLY : SWT.NONE);
    parentComponent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    parentComponent.setWidthGroup(widthGroup);

    parentComponent.addModifyListener(modifyingListener);
    parentComponent.addBrowseButtonListener(SelectionListener.widgetSelectedAdapter(e -> {
      MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchParentDialog(getShell(),
          org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArtifactPage_searchDialog_title, null, null);

      if(dialog.open() == Window.OK) {
        IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
        if(indexedArtifactFile != null) {
          parentComponent.setValues(indexedArtifactFile.group, indexedArtifactFile.artifact,
              indexedArtifactFile.version);
        }
      }
    }));

    createAdvancedSettings(container, new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));
    resolverConfigurationComponent.setModifyListener(e -> validate());

    addFieldWithHistory("groupId", artifactComponent.getGroupIdCombo()); //$NON-NLS-1$
    addFieldWithHistory("artifactId", artifactComponent.getArtifactIdCombo()); //$NON-NLS-1$
    addFieldWithHistory("version", artifactComponent.getVersionCombo()); //$NON-NLS-1$
    addFieldWithHistory("name", artifactComponent.getNameCombo()); //$NON-NLS-1$

    addFieldWithHistory("groupId", parentComponent.getGroupIdCombo()); //$NON-NLS-1$
    addFieldWithHistory("artifactId", parentComponent.getArtifactIdCombo()); //$NON-NLS-1$
    addFieldWithHistory("version", parentComponent.getVersionCombo()); //$NON-NLS-1$

    container.layout();

    validate();

    setControl(container);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    artifactComponent.getGroupIdCombo().setFocus();
  }

  /**
   * Returns the Maven2 model containing the artifact information provided by the user.
   *
   * @return The Maven2 model containing the provided artifact information. Is never <code>null</code>.
   */
  public Model getModel() {
    Model model = artifactComponent.getModel();

    parentComponent.updateModel(model);

    return model;
  }

  /** Returns the list of directories for the currently selected packaging. */
  private ProjectFolder[] getProjectFolders() {
    ProjectFolder[] folders = directorySets.get(artifactComponent.getPackaging());
    return folders == null ? JAR_DIRS : folders;
  }

  /**
   * Returns the directories of the default Maven2 directory structure selected by the user. These directories should be
   * created along with the new project.
   *
   * @return The Maven2 directories selected by the user. Neither the array nor any of its elements is <code>null</code>
   *         .
   */
  public String[] getFolders() {
    ProjectFolder[] mavenDirectories = getProjectFolders();

    String[] directories = new String[mavenDirectories.length];
    for(int i = 0; i < directories.length; i++ ) {
      directories[i] = mavenDirectories[i].getPath();
    }

    return directories;
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
   * <li>The user must have provided a group ID.</li>
   * <li>The user must have provided an artifact ID.</li>
   * <li>The user must have provided a version for the artifact.</li>
   * <li>The user must have provided the packaging type for the artifact.</li>
   * </ul>
   * </p>
   *
   * @see org.eclipse.jface.dialogs.DialogPage#setMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setErrorMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setPageComplete(boolean)
   */
  void validate() {
    String error = validateInput();
    setErrorMessage(error);
    setPageComplete(error == null);
  }

  private String validateInput() {
    String error = validateGroupIdInput(artifactComponent.getGroupId().trim());
    if(error != null) {
      return error;
    }

    error = validateArtifactIdInput(artifactComponent.getArtifactId().trim());
    if(error != null) {
      return error;
    }

    if(artifactComponent.getVersion().trim().length() == 0) {
      return Messages.wizardProjectPageMaven2ValidatorVersion;
    }

    if(artifactComponent.getPackaging().trim().length() == 0) {
      return Messages.wizardProjectPageMaven2ValidatorPackaging;
    }

    // if the parent project is specified, all three fields must be present
    if(!parentComponent.validate()) {
      return Messages.wizardProjectPageMaven2ValidatorParent;
    }

    // validate project name
    IStatus nameStatus = getImportConfiguration().validateProjectName(artifactComponent.getModel());
    if(!nameStatus.isOK()) {
      return nameStatus.getMessage();
    }

    return null;
  }

  /**
   * Updates the properties when a project name is set on the first page of the wizard.
   */
  public void setProjectName(String projectName) {
    if(artifactComponent.getArtifactId().equals(artifactComponent.getGroupId())) {
      artifactComponent.setGroupId(projectName);
    }
    artifactComponent.setArtifactId(projectName);
    validate();
  }

  /** Sets the readonly parent flag. */
  public void setParentReadonly(boolean b) {
    readonlyParent = b;
  }

  /**
   * Updates the properties when a project name is set on the first page of the wizard.
   */
  public void setParentProject(String groupId, String artifactId, String version) {
    parentComponent.setValues(groupId, artifactId, version);
    artifactComponent.setGroupId(groupId);
    artifactComponent.setVersion(version);
    validate();
  }

  public void setUsed(boolean isUsed) {
    this.isUsed = isUsed;
  }

  @Override
  public boolean isPageComplete() {
    return !isUsed || super.isPageComplete();
  }

  /**
   * A placeholder representing a directory in the project structure.
   */
  final static class ProjectFolder {

    /** Folder path */
    private String path = null;

    /** Output path */
    private String outputPath = null;

    ProjectFolder(String path, String outputPath) {
      this.path = path;
      this.outputPath = outputPath;
    }

    /**
     * Returns folder path
     */
    String getPath() {
      return path;
    }

    /**
     * Returns folder output path
     */
    String getOutputPath() {
      return outputPath;
    }

    /**
     * Returns true for source folder
     */
    boolean isSourceEntry() {
      return this.getOutputPath() != null;
    }

  }

}
