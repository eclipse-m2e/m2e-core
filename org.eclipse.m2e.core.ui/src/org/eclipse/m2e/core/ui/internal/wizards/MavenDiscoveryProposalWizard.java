/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation (in o.e.m.c.u.i.w.MavenImportWizard)
 *      Red Hat, Inc. - refactored lifecycle mapping discovery
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.eclipse.m2e.core.ui.internal.editing.LifecycleMappingOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider;


/**
 * Lifecycle Mapping remediation proposal Wizard
 *
 * @author Eugene Kuleshov
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class MavenDiscoveryProposalWizard extends Wizard implements IImportWizard {

  private static final Logger LOG = LoggerFactory.getLogger(MavenDiscoveryProposalWizard.class);

  private LifecycleMappingPage lifecycleMappingPage;

  private boolean initialized = false;

  private final LifecycleMappingDiscoveryRequest mappingDiscoveryRequest;

  private final Collection<IProject> projects;

  private IMavenDiscoveryUI pageFactory;

  public MavenDiscoveryProposalWizard(Collection<IProject> projects,
      LifecycleMappingDiscoveryRequest mappingDiscoveryRequest) {
    this.projects = projects;
    this.mappingDiscoveryRequest = mappingDiscoveryRequest;
    setNeedsProgressMonitor(false);
    setWindowTitle(Messages.MavenDiscoveryProposalWizard_title);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.pageFactory = M2EUIPluginActivator.getDefault().getImportWizardPageFactory();
    initialized = true;
  }

  @Override
  public void addPages() {
    if(!initialized) {
      init(null, null);
    }
    lifecycleMappingPage = new LifecycleMappingPage();
    addPage(lifecycleMappingPage);
  }

  @Override
  public boolean performFinish() {
    if(lifecycleMappingPage != null && !lifecycleMappingPage.isMappingComplete()) {
      return false;
    }

    final List<IMavenDiscoveryProposal> proposals = getMavenDiscoveryProposals();

    boolean doIgnore = !lifecycleMappingPage.getIgnore().isEmpty() || !lifecycleMappingPage.getIgnoreParent().isEmpty()
        || !lifecycleMappingPage.getIgnoreWorkspace().isEmpty();
    IMavenDiscoveryUI discovery = getPageFactory();
    if(discovery != null && !proposals.isEmpty()) {
      Set<String> projectsToConfigure = new HashSet<>();
      for(IProject project : projects) {
        projectsToConfigure.add(project.getName());
      }
      doIgnore = discovery.implement(proposals, null, getContainer(), projectsToConfigure);
    }

    if(doIgnore) {
      final IRunnableWithProgress ignoreJob = new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) {
          List<IProject> changed = new LinkedList<>();
          for(ILifecycleMappingLabelProvider prov : lifecycleMappingPage.getIgnore()) {
            ILifecycleMappingRequirement req = prov.getKey();
            if(req instanceof MojoExecutionMappingRequirement mojo) {
              changed.addAll(getProject(prov.getProjects()));
              ignore(mojo.getExecution(), prov.getProjects());
            }
          }

          for(ILifecycleMappingLabelProvider prov : lifecycleMappingPage.getIgnoreParent()) {
            ILifecycleMappingRequirement req = prov.getKey();
            if(req instanceof MojoExecutionMappingRequirement mojo) {
              changed.addAll(getProject(prov.getProjects()));
              ignoreAtDefinition(mojo.getExecution(), prov.getProjects());
            }
          }

          for(ILifecycleMappingLabelProvider prov : lifecycleMappingPage.getIgnoreWorkspace()) {
            ILifecycleMappingRequirement req = prov.getKey();
            if(req instanceof MojoExecutionMappingRequirement mojo) {
              changed.addAll(getProject(prov.getProjects()));
              ignoreWorkspace(mojo.getExecution());
            }
          }

          new UpdateMavenProjectJob(changed).schedule();
        }

        private Collection<IProject> getProject(Collection<MavenProject> projects) {
          List<IProject> workspaceProjects = new LinkedList<>();
          for(MavenProject project : projects) {
            IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(project.getGroupId(),
                project.getArtifactId(), project.getVersion());
            if(facade != null) {
              workspaceProjects.add(facade.getProject());
            }
          }
          return workspaceProjects;
        }

        private void ignore(MojoExecutionKey key, Collection<MavenProject> projects) {
          String pluginGroupId = key.groupId();
          String pluginArtifactId = key.artifactId();
          String pluginVersion = key.version();
          String[] goals = new String[] {key.goal()};
          for(MavenProject project : projects) {
            IFile pomFile = M2EUtils.getPomFile(project);
            try {
              PomEdits.performOnDOMDocument(new OperationTuple(pomFile, new LifecycleMappingOperation(pluginGroupId,
                  pluginArtifactId, pluginVersion, PluginExecutionAction.ignore, goals)));
            } catch(IOException ex) {
              LOG.error(ex.getMessage(), ex);
            } catch(CoreException ex) {
              LOG.error(ex.getMessage(), ex);
            }
          }
        }

        private void ignoreAtDefinition(MojoExecutionKey key, Collection<MavenProject> projects) {
          ignore(key, M2EUtils.getDefiningProjects(key, projects));
        }

        private void ignoreWorkspace(MojoExecutionKey key) {
          LifecycleMappingMetadataSource mapping = LifecycleMappingFactory.getWorkspaceMetadata(true);
          LifecycleMappingFactory.addLifecyclePluginExecution(mapping, key.groupId(), key.artifactId(),
              key.version(), List.of(key.goal()), PluginExecutionAction.ignore);
          LifecycleMappingFactory.writeWorkspaceMetadata(mapping);
        }
      };

      Job job = new WorkspaceJob("Apply Lifecycle Mapping Changes") {
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) {
          try {
            ignoreJob.run(monitor);
          } catch(InvocationTargetException e) {
            return AbstractCreateMavenProjectsOperation.toStatus(e);
          } catch(InterruptedException e) {
            return Status.CANCEL_STATUS;
          }
          return Status.OK_STATUS;
        }
      };
      job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
      job.schedule();
    }

    return true;
  }

  private IMavenDiscoveryUI getPageFactory() {
    return pageFactory;
  }

  @Override
  public boolean canFinish() {
    return true;
  }

  private List<IMavenDiscoveryProposal> getMavenDiscoveryProposals() {
    if(lifecycleMappingPage == null) {
      return Collections.emptyList();
    }
    return lifecycleMappingPage.getSelectedDiscoveryProposals();
  }

  /**
   * @return mapping configuration or null
   */
  public LifecycleMappingDiscoveryRequest getLifecycleMappingDiscoveryRequest() {
    return mappingDiscoveryRequest;
  }

}
