/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc.
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

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.conversion.IProjectConversionEnabler;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Wizard page used to specify basic POM parameters
 */
public class MavenPomWizardPage extends AbstractMavenWizardPage {
  private Text projectText;

  private final ISelection selection;

  private MavenArtifactComponent pomComponent;

  private IProjectConversionEnabler projectConversionEnabler;

  public MavenPomWizardPage(ISelection selection) {
    super("wizardPage"); //$NON-NLS-1$
    setTitle(Messages.MavenPomWizardPage_title);
    setDescription(Messages.MavenPomWizardPage_desc);
    this.selection = selection;
  }

  @Override
  public void createControl(Composite parent) {
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.makeColumnsEqualWidth = false;

    Composite container = new Composite(parent, SWT.NULL);
    container.setLayout(layout);

    ModifyListener modifyingListener = e -> dialogChanged();

    Label label = new Label(container, SWT.NULL);
    label.setText(Messages.MavenPomWizardPage_lblProject);

    projectText = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
    projectText.setEditable(false);
    projectText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    projectText.addModifyListener(modifyingListener);

    pomComponent = new MavenArtifactComponent(container, SWT.NONE);
    pomComponent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    pomComponent.setModifyingListener(modifyingListener);
    addFieldWithHistory("groupId", pomComponent.getGroupIdCombo()); //$NON-NLS-1$
    addFieldWithHistory("artifactId", pomComponent.getArtifactIdCombo()); //$NON-NLS-1$
    addFieldWithHistory("version", pomComponent.getVersionCombo()); //$NON-NLS-1$
    addFieldWithHistory("name", pomComponent.getNameCombo()); //$NON-NLS-1$

    initialize();
    dialogChanged();
    setControl(container);
  }

  /**
   * Tests if the current workbench selection is a suitable container to use.
   */
  private void initialize() {
    String packagingToUse = MavenArtifactComponent.DEFAULT_PACKAGING;
    List<String> availablePackagingTypes = Arrays.asList(MavenArtifactComponent.PACKAGING_OPTIONS);
    if(selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection ssel) {
      if(ssel.size() > 1) {
        return;
      }
      Object obj = ssel.getFirstElement();
      if(obj instanceof IResource resource) {
        IContainer container = obj instanceof IContainer c ? c : resource.getParent();
        projectText.setText(container.getFullPath().toString());
        pomComponent.setArtifactId(container.getName());
        pomComponent.setGroupId(container.getName());
        if(container instanceof IProject project) {
          IProjectConversionManager pcm = MavenPlugin.getProjectConversionManager();
          projectConversionEnabler = pcm.getConversionEnablerForProject(project);
          if(projectConversionEnabler != null) {
            availablePackagingTypes = projectConversionEnabler.getPackagingTypes(project);
            packagingToUse = availablePackagingTypes.get(0);
          }
        }
      }
    }

    pomComponent.setVersion(MavenArtifactComponent.DEFAULT_VERSION);
    pomComponent.setPackagingTypes(availablePackagingTypes);
    pomComponent.setPackaging(packagingToUse);
    pomComponent.setFocus();
  }

  /**
   * Uses the standard container selection dialog to choose the new value for the container field.
   */
  void handleBrowse() {
    ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(),
        false, Messages.MavenPomWizardPage_dialog_title);
    dialog.showClosedProjects(false);
    if(dialog.open() == Window.OK) {
      Object[] result = dialog.getResult();
      if(result.length == 1) {
        projectText.setText(((IPath) result[0]).toString());
      }
    }

//    IJavaModel javaModel = JavaCore.create();
//
//    IJavaProject[] projects;
//    try {
//      projects = javaModel.getJavaProjects();
//    } catch(JavaModelException e) {
//      log.error(e.getMessage(), e);
//      projects = new IJavaProject[0];
//    }
//
//    ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
//    ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
//    dialog.setTitle("Select Project");
//    dialog.setMessage("Choose project where POM will be created");
//    dialog.setElements(projects);
//
//    String projectName = getProject();
//    if(projectName != null && projectName.length() > 0) {
//      IJavaProject javaProject = javaModel.getJavaProject(projectName);
//      if(javaProject != null) {
//        dialog.setInitialSelections(new Object[] {javaProject});
//      }
//    }
//
//    if(dialog.open() == Window.OK) {
//      projectText.setText(((IJavaProject) dialog.getFirstResult()).getProject().getFullPath().toString());
//    }
  }

  /**
   * Ensures that both text fields are set.
   */
  void dialogChanged() {
    IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(IPath.fromOSString(getProject()));

    if(getProject().length() == 0) {
      updateStatus(Messages.MavenPomWizardPage_error_folder);
      return;
    }
    if(container == null || (container.getType() & IResource.FOLDER | IResource.PROJECT) == 0) {
      updateStatus(Messages.MavenPomWizardPage_error_folder2);
      return;
    }
    if(!container.isAccessible()) {
      updateStatus(Messages.MavenPomWizardPage_error_folder_write);
      return;
    }

    String message = validateGroupIdInput(pomComponent.getGroupId());
    if(message != null) {
      updateStatus(message);
      return;
    }

    message = validateArtifactIdInput(pomComponent.getArtifactId());
    if(message != null) {
      updateStatus(message);
      return;
    }

    if(pomComponent.getVersion().length() == 0) {
      updateStatus(Messages.MavenPomWizardPage_error_version);
      return;
    }

    if(pomComponent.getPackaging().length() == 0) {
      updateStatus(Messages.MavenPomWizardPage_error_pack);
      return;
    }

    if(container instanceof IProject project && projectConversionEnabler != null) {
      IStatus status = projectConversionEnabler.canBeConverted(project);
      if(status.getSeverity() == IStatus.ERROR) {
        updateStatus(status.getMessage());
        return;
      }
    }

    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getProject() {
    return projectText.getText();
  }

  public Model getModel() {
    return pomComponent.getModel();
  }

}
