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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue;
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
            super(Collections.<IProject>emptyList());
            this.toProcess = Collections.synchronizedSet(new HashSet<IProject>());
        }

        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            try {
                // This makes job execution wait until the main Import job is completed
                // So the mapping discovery happens as a next step, after projects are imported in workspace
                getJobManager().join(SmartImportJob.class, monitor);
            } catch (InterruptedException ex) {
                throw new CoreException(new Status(IStatus.WARNING,
                    M2EUIPluginActivator.PLUGIN_ID, ex.getMessage(), ex));
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
    public static class UpdateMavenConfigurationJob extends Job implements IBackgroundProcessingQueue {

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
            this.toProcess = Collections.synchronizedSet(new HashSet<IProject>());
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
                    } else {
                        toProcessNow.addAll(this.toProcess);
                        this.toProcess.removeAll(toProcessNow);
                    }
                }
                if (!toProcessNow.isEmpty()) {
                    CumulativeMappingDiscoveryJob.getInstance().addProjects(toProcessNow);
                    ProjectConfigurationManager configurationManager = (ProjectConfigurationManager) MavenPlugin
                            .getProjectConfigurationManager();
                    MavenUpdateRequest request = new MavenUpdateRequest(
                            toProcessNow.toArray(new IProject[toProcessNow.size()]), false, false);
                    configurationManager.updateProjectConfiguration(request, true,
                            false, false, monitor);
                }
            }
            return new Status(IStatus.CANCEL, M2EUIPluginActivator.PLUGIN_ID,
                    "Cancelled by user");
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
          M2EUIPluginActivator.getDefault().getLog()
              .log(new Status(IStatus.ERROR, M2EUIPluginActivator.PLUGIN_ID, ex.getMessage(), ex));
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

    // TODO Uncomment @Override when this method API is exposed in ProjectConfigurator
    // @Override
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

    // TODO this method is going to be removed from API.
    // When done, also remove this method implementation.
    public IWizard getConfigurationWizard() {
        // no need for a wizard, will just set up the m2e nature
        return null;
    }

    @Override
    public void configure(final IProject project, Set<IPath> excludedDirectories, final IProgressMonitor monitor) {
        // copied from
        // org.eclipse.m2e.core.ui.internal.actions.EnableNatureAction

        final ResolverConfiguration configuration = new ResolverConfiguration();
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
          M2EUIPluginActivator.getDefault().getLog()
              .log(new Status(IStatus.ERROR, M2EUIPluginActivator.PLUGIN_ID, ex.getMessage(), ex));
        }
    }

    @Override
    public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
        IFile pomFile = container.getFile(new Path(IMavenConstants.POM_FILE_NAME));
        return pomFile.exists();
        // debated on m2e-dev:
        // https://dev.eclipse.org/mhonarc/lists/m2e-dev/msg01852.html
        // if (!pomFile.exists()) {
        // return false;
        // }
        // try {
        // Model pomModel =
        // MavenPlugin.getMavenModelManager().readMavenModel(pomFile);
        // return !pomModel.getPackaging().equals("pom"); // TODO find symbol
        // for "pom"
        // } catch (CoreException ex) {
        // Activator.log(IStatus.ERROR, "Could not parse pom file " +
        // pomFile.getLocation(), ex);
        // return false;
        // }
    }

    // Prepare for compatibility with M7
    // @Override
    public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
        Set<IFolder> res = new HashSet<>();
        // TODO: get these values from pom/project config
        res.add(project.getFolder("src"));
        res.add(project.getFolder("target"));
        return res;
    }

    // Prepare for compatibility with M7
    // @Override
    public Set<IFolder> getDirectoriesToIgnore(IProject project, IProgressMonitor monitor) {
        return getFoldersToIgnore(project, monitor);
    }

}
