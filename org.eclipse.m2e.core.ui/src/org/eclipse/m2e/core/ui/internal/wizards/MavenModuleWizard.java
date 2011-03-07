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

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
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
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.actions.OpenMavenConsoleAction;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.progress.IProgressConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.*;

/**
 * A project wizard for creating a new Maven2 module project.
 */
public class MavenModuleWizard extends AbstractMavenProjectWizard implements INewWizard {
  private static final Logger LOG = LoggerFactory.getLogger(MavenModuleWizard.class);

  /** The name of the default wizard page image. */
  // protected static final String DEFAULT_PAGE_IMAGE_NAME = "icons/new_m2_project_wizard.gif";

  /** The default wizard page image. */
  // protected static final ImageDescriptor DEFAULT_PAGE_IMAGE = MavenPlugin.getImageDescriptor(DEFAULT_PAGE_IMAGE_NAME);

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
    setDefaultPageImageDescriptor(MavenImages.WIZ_NEW_PROJECT);
    setNeedsProgressMonitor(true);
  }

  public MavenModuleWizard(boolean isEditor) {
    this();
    this.isEditor = isEditor;
  }

  /** Creates the pages. */
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
  public void createPageControls(Composite pageContainer) {
    artifactPage.setParentReadonly(true);
    artifactPage.setTitle(Messages.wizardModulePageArtifactTitle);
    archetypePage.setTitle(Messages.wizardModulePageArchetypeTitle);
    parametersPage.setTitle(Messages.wizardModulePageParametersTitle);

    super.createPageControls(pageContainer);

    parametersPage.setArtifactIdEnabled(false);

    parentPage.addArchetypeSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        archetypePage.setUsed(!parentPage.isSimpleProject());
      }
    });

    parentPage.addModuleNameListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        parametersPage.setProjectName(parentPage.getModuleName());
        artifactPage.setProjectName(parentPage.getModuleName());
      }
    });

    parentPage.addParentProjectListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        copyParentValues();
      }
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

    final MavenPlugin plugin = MavenPlugin.getDefault();

    if(parentPage.isSimpleProject()) {

      final Model model = artifactPage.getModel();
      //#335331 remove current model's version and groupId if equal to parent, to prevent showing a warning marker 
      if (model.getParent() != null) {
        Parent par = model.getParent();
        if (par.getGroupId() != null && par.getGroupId().equals(model.getGroupId())) {
          model.setGroupId(null);
        }
        if (par.getVersion() != null && par.getVersion().equals(model.getVersion())) {
          model.setVersion(null);
        }
      }

      final String[] folders = artifactPage.getFolders();

      job = new AbstactCreateMavenProjectJob(NLS.bind(Messages.wizardProjectJobCreatingProject, moduleName), workingSets) {
        @Override
        protected List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException {
          setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
          String projectName = importConfiguration.getProjectName(model);
          IProject project = importConfiguration.getProject(ResourcesPlugin.getWorkspace().getRoot(), model);

          // XXX respect parent's setting for separate projects for modules
          // XXX should run update sources on parent instead of creating new module project

          plugin.getProjectConfigurationManager().createSimpleProject(project, location.append(moduleName), model,
              folders, importConfiguration, monitor);

          setModule(projectName);

          return Arrays.asList(project);
        }
      };

    } else {
      Model model = parametersPage.getModel();
      
      final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(moduleName);
      final Archetype archetype = archetypePage.getArchetype();
      
      final String groupId = model.getGroupId();
      final String artifactId = model.getArtifactId();
      final String version = model.getVersion();
      final String javaPackage = parametersPage.getJavaPackage();
      final Properties properties = parametersPage.getProperties();

      job = new AbstactCreateMavenProjectJob(NLS.bind(Messages.wizardProjectJobCreating, archetype.getArtifactId()),
          workingSets) {
        @Override
        protected List<IProject> doCreateMavenProjects(IProgressMonitor monitor) throws CoreException {
          MavenPlugin plugin = MavenPlugin.getDefault();
          plugin.getProjectConfigurationManager().createArchetypeProject(project, location, archetype, //
              groupId, artifactId, version, javaPackage, properties, importConfiguration, monitor);

          setModule(moduleName);

          return Arrays.asList(project);
        }
      };
    }
    job.addJobChangeListener(new JobChangeAdapter() {
      public void done(IJobChangeEvent event) {
        final IStatus result = event.getResult();
        if(result.isOK()) {
          if(!isEditor) {
            //add the <module> element to the parent pom
            try {
              performOnDOMDocument(new OperationTuple(parentPom, new Operation() {
                public void process(Document document) {
                  Element root = document.getDocumentElement();
                  Element modules = getChild(root, "modules");
                  if (findChild(modules, "module", textEquals(moduleName)) == null) {
                    format(createElementWithText(modules, "module", moduleName));
                  }
                }
              }));
            } catch(Exception e) {
              LOG.error("Cannot add module to parent POM", e);
            }
          }

        } else {
          Display.getDefault().asyncExec(new Runnable() {
            public void run() {
              MessageDialog.openError(getShell(), //
                  NLS.bind(Messages.wizardProjectJobFailed, moduleName), //
                  result.getMessage());
            }
          });
        }
      }
    });
    job.setRule(plugin.getProjectConfigurationManager().getRule());
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
