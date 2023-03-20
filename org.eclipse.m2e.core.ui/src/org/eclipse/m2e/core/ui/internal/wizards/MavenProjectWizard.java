/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc.
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.INewWizard;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.core.ui.internal.archetype.MavenArchetype;


/**
 * Simple project wizard for creating a new Maven2 project.
 * <p>
 * The wizard provides the following functionality to the user:
 * <ul>
 * <li>Create the project in the workspace or at some external location.</li>
 * <li>Provide information about the Maven2 artifact to create.</li>
 * <li>Choose directories of the default Maven2 directory structure to create.</li>
 * <li>Choose a set of Maven2 dependencies for the project.</li>
 * </ul>
 * </p>
 * <p>
 * Once the wizard has finished, the following resources are created and configured:
 * <ul>
 * <li>A POM file containing the given artifact information and the chosen dependencies.</li>
 * <li>The chosen Maven2 directories.</li>
 * <li>The .classpath file is configured to hold appropriate entries for the Maven2 directories created as well as the
 * Java and Maven2 classpath containers.</li>
 * </ul>
 * </p>
 */
public class MavenProjectWizard extends AbstractMavenProjectWizard implements INewWizard {

  /** The wizard page for gathering general project information. */
  protected MavenProjectWizardLocationPage locationPage;

  /** The archetype selection page. */
  protected MavenProjectWizardArchetypePage archetypePage;

  /** The wizard page for gathering Maven2 project information. */
  protected MavenProjectWizardArtifactPage artifactPage;

  /** The wizard page for gathering archetype project information. */
  protected MavenProjectWizardArchetypeParametersPage parametersPage;

  protected Button simpleProject;

  /**
   * Default constructor. Sets the title and image of the wizard.
   */
  public MavenProjectWizard() {
    setWindowTitle(Messages.wizardProjectTitle);
    setDefaultPageImageDescriptor(MavenImages.WIZ_NEW_MAVEN_PROJECT);
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    locationPage = new MavenProjectWizardLocationPage(importConfiguration, //
        Messages.wizardProjectPageProjectTitle, Messages.wizardProjectPageProjectDescription, workingSets) { //

      @Override
      protected void createAdditionalControls(Composite container) {
        simpleProject = new Button(container, SWT.CHECK);
        simpleProject.setText(Messages.wizardProjectPageProjectSimpleProject);
        simpleProject.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
        simpleProject.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> validate()));

        Label label = new Label(container, SWT.NONE);
        GridData labelData = new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1);
        labelData.heightHint = 10;
        label.setLayoutData(labelData);
      }

      /** Skips the archetype selection page if the user chooses a simple project. */
      @Override
      public IWizardPage getNextPage() {
        return getPage(
            simpleProject.getSelection() ? "MavenProjectWizardArtifactPage" : "MavenProjectWizardArchetypePage"); //$NON-NLS-1$ //$NON-NLS-2$
      }
    };
    locationPage.setLocationPath(SelectionUtil.getSelectedLocation(selection));

    archetypePage = new MavenProjectWizardArchetypePage(importConfiguration);
    parametersPage = new MavenProjectWizardArchetypeParametersPage(importConfiguration);
    artifactPage = new MavenProjectWizardArtifactPage(importConfiguration);

    addPage(locationPage);
    addPage(archetypePage);
    addPage(parametersPage);
    addPage(artifactPage);
  }

  /** Adds the listeners after the page controls are created. */
  @Override
  public void createPageControls(Composite pageContainer) {
    super.createPageControls(pageContainer);

    simpleProject.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      boolean isSimpleproject = simpleProject.getSelection();
      archetypePage.setUsed(!isSimpleproject);
      parametersPage.setUsed(!isSimpleproject);
      artifactPage.setUsed(isSimpleproject);
      getContainer().updateButtons();
    }));

    archetypePage.addArchetypeSelectionListener(selectionchangedevent -> {
      parametersPage.setArchetype(archetypePage.getArchetype());
      getContainer().updateButtons();
    });

//    locationPage.addProjectNameListener(new ModifyListener() {
//      public void modifyText(ModifyEvent e) {
//        parametersPage.setProjectName(locationPage.getProjectName());
//        artifactPage.setProjectName(locationPage.getProjectName());
//      }
//    });
  }

  /** Returns the model. */
  public Model getModel() {
    if(simpleProject.getSelection()) {
      return artifactPage.getModel();
    }
    return parametersPage.getModel();
  }

  /**
   * To perform the actual project creation, an operation is created and run using this wizard as execution context.
   * That way, messages about the progress of the project creation are displayed inside the wizard.
   */
  @Override
  public boolean performFinish() {
    // First of all, we extract all the information from the wizard pages.
    // Note that this should not be done inside the operation we will run
    // since many of the wizard pages' methods can only be invoked from within
    // the SWT event dispatcher thread. However, the operation spawns a new
    // separate thread to perform the actual work, i.e. accessing SWT elements
    // from within that thread would lead to an exception.

//    final IProject project = locationPage.getProjectHandle();
//    final String projectName = locationPage.getProjectName();

    // Get the location where to create the project. For some reason, when using
    // the default workspace location for a project, we have to pass null
    // instead of the actual location.
    final Model model = getModel();
    final String projectName = ProjectConfigurationManager.getProjectName(importConfiguration, model);
    IStatus nameStatus = validateProjectName(importConfiguration, model);
    if(!nameStatus.isOK()) {
      MessageDialog.openError(getShell(), NLS.bind(Messages.wizardProjectJobFailed, projectName),
          nameStatus.getMessage());
      return false;
    }

    final IPath location = locationPage.isInWorkspace() ? null : locationPage.getLocationPath();
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    final IProject project = root.getProject(projectName);

    boolean pomExists = (locationPage.isInWorkspace() ? root.getLocation().append(project.getName()) : location)
        .append(IMavenConstants.POM_FILE_NAME).toFile().exists();
    if(pomExists) {
      MessageDialog.openError(getShell(), NLS.bind(Messages.wizardProjectJobFailed, projectName),
          Messages.wizardProjectErrorPomAlreadyExists);
      return false;
    }

    final AbstractCreateMavenProjectJob job;

    if(simpleProject.getSelection()) {
      final List<String> folders = artifactPage.getFolders();

      job = new AbstractCreateMavenProjectJob(NLS.bind(Messages.wizardProjectJobCreatingProject, projectName)) {
        @Override
        protected List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException {
          MavenPlugin.getProjectConfigurationManager().createSimpleProject(project, location, model, folders, //
              importConfiguration, new MavenProjectWorkspaceAssigner(workingSets), monitor);
          return Arrays.asList(project);
        }
      };

    } else {
      final Archetype archetype = archetypePage.getArchetype();

      final String groupId = model.getGroupId();
      final String artifactId = model.getArtifactId();
      final String version = model.getVersion();
      final String javaPackage = parametersPage.getJavaPackage();
      final Map<String, String> properties = parametersPage.getProperties();
      final boolean interactive = parametersPage.isInteractive();

      job = new AbstractCreateMavenProjectJob(NLS.bind(Messages.wizardProjectJobCreating, archetype.getArtifactId())) {
        @Override
        protected List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException {
          Collection<MavenProjectInfo> projects = M2EUIPluginActivator.getDefault().getArchetypePlugin().getGenerator()
              .createArchetypeProjects(location,
              new MavenArchetype(archetype), //
              groupId, artifactId, version, javaPackage, //
                  properties, interactive, monitor);
          return MavenPlugin.getProjectConfigurationManager()
              .importProjects(projects, importConfiguration, new MavenProjectWorkspaceAssigner(workingSets), monitor)
              .stream().filter(r -> r.getProject() != null && r.getProject().exists())
              .map(IMavenProjectImportResult::getProject).toList();
        }
      };
    }

    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        final IStatus result = event.getResult();
        if(!result.isOK()) {
          Display.getDefault().asyncExec(() -> MessageDialog.openError(getShell(), //
              NLS.bind(Messages.wizardProjectJobFailed, projectName), result.getMessage()));
        }

        MappingDiscoveryJob discoveryJob = new MappingDiscoveryJob(job.getCreatedProjects(), true);
        discoveryJob.schedule();

      }
    });

    job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
    job.schedule();

    return true;
  }


  static IStatus validateProjectName(ProjectImportConfiguration configuration, Model model) {
    String projectName = ProjectConfigurationManager.getProjectName(configuration, model);
    IWorkspace workspace = ResourcesPlugin.getWorkspace();

    // check if the project name is valid
    IStatus nameStatus = workspace.validateName(projectName, IResource.PROJECT);
    if(!nameStatus.isOK()) {
      return nameStatus;
    }
    // check if project already exists
    if(workspace.getRoot().getProject(projectName).exists()) {
      return Status.error(NLS.bind(org.eclipse.m2e.core.internal.Messages.importProjectExists, projectName));
    }
    return Status.OK_STATUS;
  }
}
