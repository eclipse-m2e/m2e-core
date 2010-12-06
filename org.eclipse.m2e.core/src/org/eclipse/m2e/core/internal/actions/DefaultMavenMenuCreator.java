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

package org.eclipse.m2e.core.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.AbstractMavenMenuCreator;
import org.eclipse.m2e.core.actions.AddDependencyAction;
import org.eclipse.m2e.core.actions.AddPluginAction;
import org.eclipse.m2e.core.actions.ChangeNatureAction;
import org.eclipse.m2e.core.actions.DisableNatureAction;
import org.eclipse.m2e.core.actions.EnableNatureAction;
import org.eclipse.m2e.core.actions.ModuleProjectWizardAction;
import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.actions.OpenUrlAction;
import org.eclipse.m2e.core.actions.RefreshMavenModelsAction;
import org.eclipse.m2e.core.actions.SelectionUtil;
import org.eclipse.m2e.core.actions.UpdateConfigurationAction;
import org.eclipse.m2e.core.core.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;


/**
 * Default Maven menu creator
 * 
 * @author Eugene Kuleshov
 */
public class DefaultMavenMenuCreator extends AbstractMavenMenuCreator {

  public void createMenu(IMenuManager mgr) {
    int selectionType = SelectionUtil.getSelectionType(selection);
    if(selectionType == SelectionUtil.UNSUPPORTED) {
      return;
    }

    if(selection.size() == 1 && selectionType == SelectionUtil.POM_FILE) {
      mgr.appendToGroup(NEW, getAction(new AddDependencyAction(), //
          AddDependencyAction.ID, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_dependency));
      mgr.appendToGroup(NEW, getAction(new AddPluginAction(), AddPluginAction.ID, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_plugin));
      mgr.appendToGroup(NEW, getAction(new ModuleProjectWizardAction(), //
          ModuleProjectWizardAction.ID, Messages.getString("action.moduleProjectWizardAction"))); //$NON-NLS-1$

      mgr.prependToGroup(OPEN, new Separator());
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_PROJECT), //
          OpenUrlAction.ID_PROJECT, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_project_page));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_ISSUES), //
          OpenUrlAction.ID_ISSUES, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_issues));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_SCM), //
          OpenUrlAction.ID_SCM, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_scm));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_CI), // 
          OpenUrlAction.ID_CI, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_ci));
    }

    if(selectionType == SelectionUtil.PROJECT_WITHOUT_NATURE) {
      mgr.appendToGroup(NATURE, getAction(new EnableNatureAction(), //
          EnableNatureAction.ID, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_enable_dm));
    }

    if(selectionType == SelectionUtil.PROJECT_WITH_NATURE) {
      if(selection.size() == 1) {
        mgr.appendToGroup(NEW, getAction(new AddDependencyAction(), AddDependencyAction.ID, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_dependency));
        mgr.appendToGroup(NEW, getAction(new AddPluginAction(), AddPluginAction.ID, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_plugin));
        mgr.appendToGroup(NEW, getAction(new ModuleProjectWizardAction(), //
            ModuleProjectWizardAction.ID, Messages.getString("action.moduleProjectWizardAction"))); //$NON-NLS-1$
        mgr.prependToGroup(UPDATE, new Separator());
      }
     

      mgr.appendToGroup(UPDATE, getAction(new RefreshMavenModelsAction(), RefreshMavenModelsAction.ID,
          org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_update_deps, "icons/update_dependencies.gif")); //$NON-NLS-2$
      mgr.appendToGroup(UPDATE, getAction(new RefreshMavenModelsAction(true), RefreshMavenModelsAction.ID_SNAPSHOTS,
          org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_update_snapshots));
      mgr.appendToGroup(UPDATE, getAction(new UpdateConfigurationAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()), //
          UpdateConfigurationAction.ID, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_update_config, "icons/update_source_folders.gif")); //$NON-NLS-2$

      mgr.prependToGroup(OPEN, new Separator());
      mgr.appendToGroup(OPEN, getAction(new OpenPomAction(), OpenPomAction.ID, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_open_pom));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_PROJECT), //
          OpenUrlAction.ID_PROJECT, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_project));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_ISSUES), OpenUrlAction.ID_ISSUES,
          org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_issues));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_SCM), OpenUrlAction.ID_SCM,
          org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_scm));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_CI), OpenUrlAction.ID_CI,
          org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_ci));

      boolean enableWorkspaceResolution = true;
      if(selection.size() == 1) {
        IProject project = SelectionUtil.getType(selection.getFirstElement(), IProject.class);
        if(project != null) {
          MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
          IMavenProjectFacade projectFacade = projectManager.create(project, new NullProgressMonitor());
          if(projectFacade != null) {
            ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
            enableWorkspaceResolution = !configuration.shouldResolveWorkspaceProjects();
          }
        }
      }

      mgr.prependToGroup(NATURE, new Separator());
      if(enableWorkspaceResolution) {
        mgr.appendToGroup(NATURE, getAction(new ChangeNatureAction(ChangeNatureAction.ENABLE_WORKSPACE),
            ChangeNatureAction.ID_ENABLE_WORKSPACE, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_enable_workspace));
      } else {
        mgr.appendToGroup(NATURE, getAction(new ChangeNatureAction(ChangeNatureAction.DISABLE_WORKSPACE),
            ChangeNatureAction.ID_DISABLE_WORKSPACE, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_disable_workspace));
      }

      mgr.appendToGroup(NATURE, getAction(new DisableNatureAction(), //
          DisableNatureAction.ID, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_disable_management));
    }
    
    if(selectionType == SelectionUtil.WORKING_SET) {
      mgr.appendToGroup(UPDATE, getAction(new RefreshMavenModelsAction(), RefreshMavenModelsAction.ID,
          org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_update_deps, "icons/update_dependencies.gif")); //$NON-NLS-2$
      mgr.appendToGroup(UPDATE, getAction(new RefreshMavenModelsAction(true), RefreshMavenModelsAction.ID_SNAPSHOTS,
          org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_update_snapshots));
      mgr.appendToGroup(UPDATE, getAction(new UpdateConfigurationAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()), //
          UpdateConfigurationAction.ID, org.eclipse.m2e.core.internal.Messages.DefaultMavenMenuCreator_action_update_config, "icons/update_source_folders.gif")); //$NON-NLS-2$
    }
  }

}
