/*******************************************************************************
 * Copyright (c) 2014-2015 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.core.ui.internal.project;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.jobs.MavenJob;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.wizards.LifecycleMappingDiscoveryHelper;
import org.eclipse.m2e.core.ui.internal.wizards.MappingDiscoveryJob;

public class MavenProjectConfigurator implements ProjectConfigurator {

    public static final String UPDATE_MAVEN_CONFIGURATION_JOB_NAME = "Update Maven projects configuration";

    private static class CumulativeMappingDiscoveryJob extends MappingDiscoveryJob {
        private static CumulativeMappingDiscoveryJob INSTANCE;
        private final Set<IProject> toProcess;
        private boolean started;

        public synchronized static CumulativeMappingDiscoveryJob getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new CumulativeMappingDiscoveryJob();
            }
            return INSTANCE;
        }

        private CumulativeMappingDiscoveryJob() {
          super(Collections.emptyList(), true);
          this.toProcess = Collections.synchronizedSet(new HashSet<>());
        }

        @Override
        @SuppressWarnings("restriction")
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            try {
                // This makes job execution wait until the main Import job is completed
                // So the mapping discovery happens as a next step, after projects are imported in workspace
                getJobManager().join(org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob.class, monitor);
            } catch (InterruptedException ex) {
                throw new CoreException(Status.warning(ex.getMessage(), ex));
            }
            synchronized (this.toProcess) {
                this.started = true;
            }
            // Detect and resolve Lifecycle Mapping issues
            try {
                LifecycleMappingDiscoveryRequest discoveryRequest = LifecycleMappingDiscoveryHelper
                        .createLifecycleMappingDiscoveryRequest(toProcess, monitor);
                if (discoveryRequest.isMappingComplete()) {
                    return Status.OK_STATUS;
                }
                // Some errors were detected
                discoverProposals(discoveryRequest, monitor);
                Map<ILifecycleMappingRequirement, List<IMavenDiscoveryProposal>> proposals = discoveryRequest
                    .getAllProposals();
                if(proposals.isEmpty()) {
                  //if we can't propose anything to the user there is actually no point in open the dialog.
                  return Status.OK_STATUS;
                }
                openProposalWizard(toProcess, discoveryRequest);
            } finally {
                this.toProcess.clear();
                this.started = false;
            }
            return Status.OK_STATUS;
        }

        public void addProjects(Collection<IProject> projects) {
            synchronized (this.toProcess) {
                if (this.started) {
                    throw new IllegalStateException("Cannot add projects when processing is started");
                }
                if (projects != null) {
                    this.toProcess.addAll(projects);
                }
            }
        }

    }

    /**
     * This singleton job will loop running on the background to update
     * configuration of Maven projects as they're imported.
     *
     * @author mistria
     *
     */
    @SuppressWarnings("restriction")
    public static class UpdateMavenConfigurationJob extends MavenJob
        implements org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue {

        private static UpdateMavenConfigurationJob INSTANCE;
        private final Set<IProject> toProcess;

        public synchronized static UpdateMavenConfigurationJob getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new UpdateMavenConfigurationJob();
            }
            return INSTANCE;
        }

        private UpdateMavenConfigurationJob() {
            super(UPDATE_MAVEN_CONFIGURATION_JOB_NAME);
            this.toProcess = Collections.synchronizedSet(new HashSet<>());
            this.setUser(true);
        }

        /**
         * Rather than scheduling this job another time, requestors simply add
         * to ask for being processed here. The job lifecycle will take care of
         * processing it as best.
         *
         * @param project
         */
        public void addProjectToProcess(IProject project) {
            synchronized (this.toProcess) {
                toProcess.add(project);
            }
        }

        @Override
        public IStatus run(IProgressMonitor monitor) {
            Set<IProject> toProcessNow = new HashSet<>();
            while (!monitor.isCanceled()) {
                synchronized (this.toProcess) {
                    if (this.toProcess.isEmpty()) {
                        CumulativeMappingDiscoveryJob.getInstance().schedule();
                        return Status.OK_STATUS;
                    }
                    toProcessNow.addAll(this.toProcess);
                    this.toProcess.removeAll(toProcessNow);
                }
                if (!toProcessNow.isEmpty()) {
                    CumulativeMappingDiscoveryJob.getInstance().addProjects(toProcessNow);
                    ProjectConfigurationManager configurationManager = (ProjectConfigurationManager) MavenPlugin
                            .getProjectConfigurationManager();
                    MavenUpdateRequest request = new MavenUpdateRequest(toProcessNow, false, false);
                    configurationManager.updateProjectConfiguration(request, true,
                            false, false, monitor);
                }
            }
            return new Status(IStatus.CANCEL, M2EUIPluginActivator.PLUGIN_ID, "Cancelled by user");
        }

        @Override
        public boolean isEmpty() {
            return this.toProcess.isEmpty();
        }

    }

    @Override
    public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
      LocalProjectScanner scanner = new LocalProjectScanner(List.of(root.getAbsolutePath()), false,
                MavenPlugin.getMavenModelManager());
        try {
            scanner.run(monitor);
        } catch (Exception ex) {
          M2EUIPluginActivator.getDefault().getLog().log(Status.error(ex.getMessage(), ex));
          return null;
        }
        Queue<MavenProjectInfo> projects = new LinkedList<>();
        projects.addAll(scanner.getProjects());
        HashSet<File> res = new HashSet<>();
        while (!projects.isEmpty()) {
            MavenProjectInfo projectInfo = projects.poll();
            res.add(projectInfo.getPomFile().getParentFile());
            projects.addAll(projectInfo.getProjects());
        }
        return res;
    }

    @Override
    public void removeDirtyDirectories(Map<File, List<ProjectConfigurator>> proposals) {
        Set<File> toRemove = new HashSet<>();
        for (File directory : proposals.keySet()) {
            String path = directory.getAbsolutePath();
            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }
            int index = path.indexOf(File.separator + "target" + File.separator); //$NON-NLS-1$
            if (index >= 0) {
                File potentialPomFile = new File(path.substring(0, index), "pom.xml"); //$NON-NLS-1$
                if (potentialPomFile.isFile()) {
                    toRemove.add(directory);
                }
            }
        }
        for (File directory : toRemove) {
            proposals.remove(directory);
        }
    }

    @Override
    public boolean canConfigure(IProject project, Set<IPath> ignoredPaths, IProgressMonitor monitor) {
        return shouldBeAnEclipseProject(project, monitor);
    }

    @Override
    public void configure(final IProject project, Set<IPath> excludedDirectories, final IProgressMonitor monitor) {
        // copied from
        // org.eclipse.m2e.core.ui.internal.actions.EnableNatureAction

        final ResolverConfiguration configuration = new ResolverConfiguration(project);
        configuration.setResolveWorkspaceProjects(true);
        try {
            if (!project.hasNature(IMavenConstants.NATURE_ID)) {
                IProjectDescription description = project.getDescription();
                String[] prevNatures = description.getNatureIds();
                String[] newNatures = new String[prevNatures.length + 1];
                System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
                newNatures[0] = IMavenConstants.NATURE_ID;
                description.setNatureIds(newNatures);
                project.setDescription(description, monitor);
            }
            UpdateMavenConfigurationJob.getInstance().addProjectToProcess(project);
            if (UpdateMavenConfigurationJob.getInstance().getState() == Job.NONE) {
                UpdateMavenConfigurationJob.getInstance().schedule();
            }
        } catch (Exception ex) {
          M2EUIPluginActivator.getDefault().getLog().log(Status.error(ex.getMessage(), ex));
        }
    }

    @Override
    public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
        IFile pomFile = container.getFile(IPath.fromOSString(IMavenConstants.POM_FILE_NAME));
        return pomFile.exists();
    }

    @Override
    public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
        Set<IFolder> res = new HashSet<>();
        // TODO: get these values from pom/project config
        res.add(project.getFolder("src"));
        res.add(project.getFolder("target"));
        return res;
    }
}
