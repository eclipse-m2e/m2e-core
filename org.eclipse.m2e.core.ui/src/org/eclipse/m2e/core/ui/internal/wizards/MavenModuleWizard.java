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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElementWithText;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.textEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.progress.IProgressConstants;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.actions.OpenMavenConsoleAction;
import org.eclipse.m2e.core.ui.internal.archetype.MavenArchetype;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;


/**
 * A project wizard for creating a new Maven2 module project.
 */
public class MavenModuleWizard extends AbstractMavenProjectWizard implements INewWizard {
  private static final Logger LOG = LoggerFactory.getLogger(MavenModuleWizard.class);

  /** the parent page (#1) */
  protected MavenModuleWizardParentPage parentPage;

  /** The archetype selection page. */
  protected MavenProjectWizardArchetypePage archetypePage;

  /** The wizard page for gathering Maven2 project information. */
  protected MavenProjectWizardArtifactPage artifactPage;

  /** The wizard page for gathering archetype project information. */
  protected MavenProjectWizardArchetypeParametersPage parametersPage;

  private String moduleName;

  protected boolean isEditor = false;

  /** Default constructor. Sets the title and image of the wizard. */
  public MavenModuleWizard() {
    setWindowTitle(Messages.wizardModuleTitle);
    setDefaultPageImageDescriptor(MavenImages.WIZ_NEW_MODULE_PROJECT);
    setNeedsProgressMonitor(true);
  }

  public MavenModuleWizard(boolean isEditor) {
    this();
    this.isEditor = isEditor;
  }

  /** Creates the pages. */
  @Override
  public void addPages() {
    parentPage = new MavenModuleWizardParentPage(importConfiguration, workingSets);
    archetypePage = new MavenProjectWizardArchetypePage(importConfiguration);
    parametersPage = new MavenProjectWizardArchetypeParametersPage(importConfiguration);
    artifactPage = new MavenProjectWizardArtifactPage(importConfiguration);

    addPage(parentPage);
    addPage(archetypePage);
    addPage(parametersPage);
    addPage(artifactPage);
  }

  /** Adds the listeners after the page controls are created. */
  @Override
  public void createPageControls(Composite pageContainer) {
    artifactPage.setParentReadonly(true);
    artifactPage.setTitle(Messages.wizardModulePageArtifactTitle);
    archetypePage.setTitle(Messages.wizardModulePageArchetypeTitle);
    parametersPage.setTitle(Messages.wizardModulePageParametersTitle);

    super.createPageControls(pageContainer);

    parametersPage.setArtifactIdEnabled(false);

    parentPage.addArchetypeSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      boolean isArchetype = !parentPage.isSimpleProject();
      archetypePage.setUsed(isArchetype);
      parametersPage.setUsed(isArchetype);
    }));

    parentPage.addModuleNameListener(e -> {
      parametersPage.setProjectName(parentPage.getModuleName());
      artifactPage.setProjectName(parentPage.getModuleName());
    });

    parentPage.addParentProjectListener(e -> copyParentValues());

    archetypePage.addArchetypeSelectionListener(selectionchangedevent -> {
      parametersPage.setArchetype(archetypePage.getArchetype());
      getContainer().updateButtons();
    });

    if(selection != null && selection.size() > 0) {
      parentPage.setParent(selection.getFirstElement());
      copyParentValues();
    }
  }

  /** Copies the parent project parameters to the artifact page. */
  protected void copyParentValues() {
    Model model = parentPage.getParentModel();
    if(model != null) {
      String groupId = model.getGroupId();
      String artifactId = model.getArtifactId();
      String version = model.getVersion();

      if(groupId == null) {
        Parent parent = model.getParent();
        if(parent != null) {
          groupId = parent.getGroupId();
        }
      }
      if(version == null) {
        Parent parent = model.getParent();
        if(parent != null) {
          version = parent.getVersion();
        }
      }

      artifactPage.setParentProject(groupId, artifactId, version);
      parametersPage.setParentProject(groupId, artifactId, version);
    }
  }

  /** Performs the "finish" action. */
  @Override
  public boolean performFinish() {
    // First of all, we extract all the information from the wizard pages.
    // Note that this should not be done inside the operation we will run
    // since many of the wizard pages' methods can only be invoked from within
    // the SWT event dispatcher thread. However, the operation spawns a new
    // separate thread to perform the actual work, i.e. accessing SWT elements
    // from within that thread would lead to an exception.

    final String moduleName = parentPage.getModuleName();

    // Get the location where to create the project. For some reason, when using
    // the default workspace location for a project, we have to pass null
    // instead of the actual location.
    final IPath location = parentPage.getParentContainer().getLocation();

    final IFile parentPom = parentPage.getPom();

    Job job;

    if(parentPage.isSimpleProject()) {

      final Model model = artifactPage.getModel();
      if(model.getParent() != null) {
        Parent par = model.getParent();
        String relPath = location.makeRelativeTo(location.append(moduleName)).toOSString();
        if(!"..".equals(relPath)) { //$NON-NLS-1$
          par.setRelativePath(relPath);
        }

        //#335331 remove current model's version and groupId if equal to parent, to prevent showing a warning marker
        if(par.getGroupId() != null && par.getGroupId().equals(model.getGroupId())) {
          model.setGroupId(null);
        }
        if(par.getVersion() != null && par.getVersion().equals(model.getVersion())) {
          model.setVersion(null);
        }
      }

      final List<String> folders = artifactPage.getFolders();

      job = new AbstractCreateMavenProjectJob(NLS.bind(Messages.wizardProjectJobCreatingProject, moduleName)) {
        @Override
        protected List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException {
          setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
          String projectName = ProjectConfigurationManager.getProjectName(importConfiguration, model);
          IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

          // XXX respect parent's setting for separate projects for modules
          // XXX should run update sources on parent instead of creating new module project

          MavenPlugin.getProjectConfigurationManager().createSimpleProject(project, location.append(moduleName), model,
              folders, importConfiguration, new MavenProjectWorkspaceAssigner(workingSets), monitor);

          setModule(projectName);

          return Arrays.asList(project);
        }
      };

    } else {
      Model model = parametersPage.getModel();

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
          setModule(moduleName);

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
        if(result.isOK()) {
          if(!isEditor) {
            //add the <module> element to the parent pom
            try {
              performOnDOMDocument(new OperationTuple(parentPom, (Operation) document -> {
                Element root = document.getDocumentElement();
                Element modules = getChild(root, "modules"); //$NON-NLS-1$
                if(findChild(modules, "module", textEquals(moduleName)) == null) { //$NON-NLS-1$
                  format(createElementWithText(modules, "module", moduleName)); //$NON-NLS-1$
                }
              }));
            } catch(Exception e) {
              LOG.error("Cannot add module to parent POM", e); //$NON-NLS-1$
            }
          }

        } else {
          Display.getDefault().asyncExec(() -> MessageDialog.openError(getShell(), //
              NLS.bind(Messages.wizardProjectJobFailed, moduleName), //
              result.getMessage()));
        }
      }
    });
    job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
    job.schedule();

    if(isEditor) {
      try {
        job.join();
      } catch(InterruptedException ex) {
        // ignore
      }
    }

    return true;
  }

  void setModule(String moduleName) {
    this.moduleName = moduleName;
  }

  public String getModuleName() {
    return this.moduleName;
  }
}
