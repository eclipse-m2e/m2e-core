/*******************************************************************************
 * Copyright (c) 2014-2015 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.importer.internal;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.LifecycleMappingDiscoveryHelper;
import org.eclipse.m2e.core.ui.internal.wizards.MappingDiscoveryJob;
import org.eclipse.ui.internal.wizards.datatransfer.EasymportJob;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

public class MavenProjectConfigurator implements ProjectConfigurator {

    private static class CumulativeMappingDiscoveryJob extends MappingDiscoveryJob {
        private static CumulativeMappingDiscoveryJob INSTANCE;
        private Set<IProject> toProcess;
        private boolean started;

        public synchronized static CumulativeMappingDiscoveryJob getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new CumulativeMappingDiscoveryJob();
            }
            return INSTANCE;
        }

        private CumulativeMappingDiscoveryJob() {
            super(null);
            this.toProcess = Collections.synchronizedSet(new HashSet<IProject>());
        }
        
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            try {
                // This makes job execution wait until the main Import job is completed
                // So the mapping discovery happens as a next step, after projects are imported in workspace
                getJobManager().join(EasymportJob.class, monitor);
            } catch (InterruptedException ex) {
                throw new CoreException(new Status(IStatus.WARNING,
                        Activator.getDefault().getBundle().getSymbolicName(), ex.getMessage(), ex));
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
    private static class UpdateMavenConfigurationJob extends Job {

        private static UpdateMavenConfigurationJob INSTANCE;
        private Set<IProject> toProcess;

        public synchronized static UpdateMavenConfigurationJob getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new UpdateMavenConfigurationJob();
            }
            return INSTANCE;
        }

        private UpdateMavenConfigurationJob() {
            super("Update Maven projects configuration");
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
        protected IStatus run(IProgressMonitor monitor) {
            Set<IProject> toProcessNow = new HashSet<IProject>();
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
                    Map<String, IStatus> updateStatus = configurationManager.updateProjectConfiguration(request, true,
                            false, false, monitor);
                }
            }
            return new Status(IStatus.CANCEL, Activator.getDefault().getBundle().getSymbolicName(),
                    "Cancelled by user");
        }

    }

    // TODO Uncomment @Override when following API got merged.
    // this is commented in order to check it in build before API is available
    // and avoid
    // build failure because inteface doesn't declare the method (yet).
    // @Override
    public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
        LocalProjectScanner scanner = new LocalProjectScanner(
                ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), root.getAbsolutePath(), false,
                MavenPlugin.getMavenModelManager());
        try {
            scanner.run(monitor);
        } catch (Exception ex) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex));
            return null;
        }
        Queue<MavenProjectInfo> projects = new LinkedList<MavenProjectInfo>();
        projects.addAll(scanner.getProjects());
        HashSet<File> res = new HashSet<File>();
        while (!projects.isEmpty()) {
            MavenProjectInfo projectInfo = projects.poll();
            res.add(projectInfo.getPomFile().getParentFile());
            projects.addAll(projectInfo.getProjects());
        }
        return res;
    }

    @Override
    public boolean canConfigure(IProject project, Set<IPath> ignoredPaths, IProgressMonitor monitor) {
        return shouldBeAnEclipseProject(project, monitor);
    }

    @Override
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
        final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
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
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex));
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

    @Override
    public Set<IFolder> getDirectoriesToIgnore(IProject project, IProgressMonitor monitor) {
        Set<IFolder> res = new HashSet<IFolder>();
        // TODO: get these values from pom/project config
        res.add(project.getFolder("src"));
        res.add(project.getFolder("target"));
        return res;
    }

}
