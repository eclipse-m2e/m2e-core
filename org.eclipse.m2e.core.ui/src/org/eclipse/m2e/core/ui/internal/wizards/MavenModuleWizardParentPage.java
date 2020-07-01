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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.WorkingSets;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.core.ui.internal.components.WorkingSetGroup;


/**
 * Maven2ModuleParentPage
 */
public class MavenModuleWizardParentPage extends AbstractMavenWizardPage {
  private static final Logger log = LoggerFactory.getLogger(MavenModuleWizardParentPage.class);

  /** the module name input field */
  private Combo moduleNameCombo;

  /** the parent project input field */
  protected Text parentProjectText;

  /** the "create simple project" checkbox */
  private Button simpleProject;

  /** the parent object entity */
  protected Object parentObject;

  /** the parent container */
  private IContainer parentContainer;

  /** the parent POM file */
  private IFile pom;

  /** the parent model */
  private Model parentModel;

  /** working set selector widget */
  private WorkingSetGroup workingSetGroup;

  private final List<IWorkingSet> workingSets;

  /** Creates a new page. */
  public MavenModuleWizardParentPage(ProjectImportConfiguration projectImportConfiguration,
      List<IWorkingSet> workingSets) {
    super("MavenModuleWizardParentPage", projectImportConfiguration);
    this.workingSets = workingSets;
    setTitle(Messages.wizardModulePageParentTitle);
    setDescription(Messages.wizardModulePageParentDescription);
    setPageComplete(false);
  }

  /** Creates the page controls. */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    container.setLayout(new GridLayout(3, false));

    simpleProject = new Button(container, SWT.CHECK);
    simpleProject.setText(Messages.wizardProjectPageProjectSimpleProject);
    simpleProject.setData("name", "simpleProjectButton"); //$NON-NLS-1$ //$NON-NLS-2$
    simpleProject.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
    simpleProject.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> validate()));

    Label nameLabel = new Label(container, SWT.NONE);
    GridData gd_nameLabel = new GridData();
    gd_nameLabel.verticalIndent = 10;
    nameLabel.setLayoutData(gd_nameLabel);
    nameLabel.setText(Messages.wizardModulePageParentModuleName);

    moduleNameCombo = new Combo(container, SWT.BORDER);
    GridData gd_moduleNameCombo = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
    gd_moduleNameCombo.verticalIndent = 10;
    moduleNameCombo.setLayoutData(gd_moduleNameCombo);
    moduleNameCombo.addModifyListener(e -> validate());
    addFieldWithHistory("moduleName", moduleNameCombo); //$NON-NLS-1$

    Label parentLabel = new Label(container, SWT.NONE);
    parentLabel.setText(Messages.wizardModulePageParentParentProject);

    parentProjectText = new Text(container, SWT.BORDER);
    parentProjectText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
    parentProjectText.setEditable(false);

    Button browseButton = new Button(container, SWT.NONE);
    browseButton.setText(Messages.wizardModulePageParentBrowse);
    browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      MavenProjectSelectionDialog dialog = new MavenProjectSelectionDialog(getShell());
      if(dialog.open() == Window.OK) {
        setParent(dialog.getFirstResult());
        validate();
      }
    }));

    this.workingSetGroup = new WorkingSetGroup(container, workingSets, getShell());

    createAdvancedSettings(container, new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

    initialize();

    setControl(container);
  }

  /** Initializes the GUI components and validates the page. */
  private void initialize() {
    loadParent();
    validate();
  }

  /** Validates the data entered. */
  void validate() {
    String moduleName = moduleNameCombo.getText().trim();
    if(moduleName.length() == 0) {
      setErrorMessage(null);
      setMessage(Messages.wizardModulePageParentValidatorModuleName);
      setPageComplete(false);
      return;
    }

    // check whether the project name is valid
    IStatus nameStatus = ResourcesPlugin.getWorkspace().validateName(moduleName, IResource.PROJECT);
    if(!nameStatus.isOK()) {
      setErrorMessage(nameStatus.getMessage());
      setPageComplete(false);
      return;
    }

    // check if the given folder already exists
    if(parentContainer != null && parentContainer.exists(new Path(moduleName))) {
      setErrorMessage(Messages.wizardModulePageParentValidatorNameExists);
      setPageComplete(false);
      return;
    }

    if(pom == null) {
      setErrorMessage(null);
      setMessage(Messages.wizardModulePageParentValidatorParentProject);
      setPageComplete(false);
      return;
    }
    if(!validateParent()) {
      return;
    }
    setErrorMessage(null);
    setMessage(null);
    setPageComplete(true);
  }

  /** Assigns a parent object. */
  public void setParent(Object parent) {
    parentObject = parent;
    loadParent();
  }

  /** Loads the data from the parent object. */
  protected void loadParent() {
    if(parentObject == null) {
      return;
    }

    int type = SelectionUtil.getElementType(parentObject);

    if(SelectionUtil.POM_FILE == type) {
      pom = SelectionUtil.getType(parentObject, IFile.class);
    } else if(SelectionUtil.PROJECT_WITH_NATURE == type) {
      IProject project = SelectionUtil.getType(parentObject, IProject.class);
      pom = project.getFile(IMavenConstants.POM_FILE_NAME);

      workingSetGroup.selectWorkingSets(WorkingSets.getAssignedWorkingSets(project));
    } else if(parentObject instanceof IContainer) {
      pom = ((IContainer) parentObject).getFile(new Path(IMavenConstants.POM_FILE_NAME));
    }

    if(pom != null && pom.exists()) {
      parentObject = pom;
      parentContainer = pom.getParent();

      try (InputStream pomStream = pom.getContents()) {
        parentModel = MavenPlugin.getMavenModelManager().readMavenModel(pomStream);
        validateParent();
        parentProjectText.setText(parentModel.getArtifactId());
      } catch(CoreException | IOException e) {
        log.error("Error loading POM: " + e.getMessage(), e); //$NON-NLS-1$
      }
    }
  }

  private boolean validateParent() {
    if(parentModel != null) {
      if(!"pom".equals(parentModel.getPackaging())) { //$NON-NLS-1$
        setMessage(null);
        setErrorMessage(org.eclipse.m2e.core.ui.internal.Messages.MavenModuleWizardParentPage_error);
        setPageComplete(false);
        return false;
      }
    }
    return true;
  }

  /** Returns "true" if the user chose not to use archetypes. */
  public boolean isSimpleProject() {
    return simpleProject.getSelection();
  }

  /** Skips the archetype selection page if the user chooses a simple project. */
  public IWizardPage getNextPage() {
    return getWizard()
        .getPage(isSimpleProject() ? "MavenProjectWizardArtifactPage" : "MavenProjectWizardArchetypePage");
  }

  /** Returns the module name. */
  public String getModuleName() {
    return moduleNameCombo.getText();
  }

  /** Returns the parent model. */
  public Model getParentModel() {
    return parentModel;
  }

  /** Returns the parent POM file handle. */
  public IFile getPom() {
    return pom;
  }

  /** Returns the parent container. */
  public IContainer getParentContainer() {
    return parentContainer;
  }

  /** Offers a listener hookup to the pages watching the module name field. */
  public void addModuleNameListener(ModifyListener modifyListener) {
    moduleNameCombo.addModifyListener(modifyListener);
  }

  /** Unhooks the listener watching the module name field. */
  public void removesModuleNameListener(ModifyListener modifyListener) {
    moduleNameCombo.removeModifyListener(modifyListener);
  }

  /** Offers a listener hookup to the pages watching the parent name field. */
  public void addParentProjectListener(ModifyListener modifyListener) {
    parentProjectText.addModifyListener(modifyListener);
  }

  /** Unhooks the listener watching the parent name field. */
  public void removesParentProjectListener(ModifyListener modifyListener) {
    parentProjectText.removeModifyListener(modifyListener);
  }

  /** Offers a listener hookup to the pages watching the archetype switch. */
  public void addArchetypeSelectionListener(SelectionListener selectionListener) {
    simpleProject.addSelectionListener(selectionListener);
  }

  /** Removes the listener watching the project name field. */
  public void removeArchetypeSelectionListener(SelectionListener selectionListener) {
    simpleProject.removeSelectionListener(selectionListener);
  }

  /** Cleans up. */
  public void dispose() {
    super.dispose();
    workingSetGroup.dispose();
  }
}
