/*******************************************************************************
 * Copyright (c) 2011, 2022 Sonatype, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.pde.connector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleProjectService;

public class PDEProjectHelper {

	@SuppressWarnings("restriction")
	private static final String PDE_PLUGIN_NATURE = org.eclipse.pde.internal.core.natures.PDE.PLUGIN_NATURE;

	private static boolean isListeningForPluginModelChanges = false;

	private static final List<IProject> PROJECTS_FOR_UPDATE_CLASSPATH = new ArrayList<>();

	private PDEProjectHelper() {
	}

	@SuppressWarnings("restriction")
	private static final org.eclipse.pde.internal.core.IPluginModelListener CLASSPATH_UPDATER = delta -> {
		synchronized (PROJECTS_FOR_UPDATE_CLASSPATH) {
			PROJECTS_FOR_UPDATE_CLASSPATH.removeIf(project -> {
				IPluginModelBase model = PluginRegistry.findModel(project);
				if (model != null) {
					UpdateClasspathWorkspaceJob job = new UpdateClasspathWorkspaceJob(project, model);
					job.schedule();
					return true;
				}
				return false;
			});
		}
	};

	private static class UpdateClasspathWorkspaceJob extends WorkspaceJob {
		private final IProject project;

		private final IPluginModelBase model;

		public UpdateClasspathWorkspaceJob(IProject project, IPluginModelBase model) {
			super("Updating classpath");
			this.project = project;
			this.model = model;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			setClasspath(project, model, monitor);
			return Status.OK_STATUS;
		}
	}

	@SuppressWarnings("restriction")
	public static IJavaProject configurePDEBundleProject(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
			throws CoreException {
		// see org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation

		if (!project.hasNature(PDE_PLUGIN_NATURE)) {
			org.eclipse.pde.internal.core.util.CoreUtility.addNatureToProject(project, PDE_PLUGIN_NATURE, null);
		}

		if (!project.hasNature(JavaCore.NATURE_ID)) {
			org.eclipse.pde.internal.core.util.CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, null);
		}

		// PDE can't handle default JDT classpath
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.setOutputLocation(getOutputLocation(project, mavenProject, monitor), monitor);

		// see org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob
		// PDE populates the model cache lazily from WorkspacePluginModelManager.visit()
		// ResourceChangeListenter
		// That means the model may be available or not at this point in the lifecycle.
		// If it is, update its classpath right away.
		// If not add the project to the list to be updated later based on model change
		// events.
		IPluginModelBase model = PluginRegistry.findModel(project);
		if (model != null) {
			setClasspath(project, model, monitor);
		} else {
			addProjectForUpdateClasspath(project);
		}
		return javaProject;
	}

	@SuppressWarnings("restriction")
	private static void addProjectForUpdateClasspath(IProject project) {
		synchronized (PROJECTS_FOR_UPDATE_CLASSPATH) {
			PROJECTS_FOR_UPDATE_CLASSPATH.add(project);
			if (!isListeningForPluginModelChanges) {
				org.eclipse.pde.internal.core.PDECore pdeCore = org.eclipse.pde.internal.core.PDECore.getDefault();
				pdeCore.getModelManager().addPluginModelListener(CLASSPATH_UPDATER);
				isListeningForPluginModelChanges = true;
			}
		}
	}

	private static IPath getOutputLocation(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
			throws CoreException {
		File outputDirectory = new File(mavenProject.getBuild().getOutputDirectory());
		outputDirectory.mkdirs();
		IPath relPath = MavenProjectUtils.getProjectRelativePath(project, outputDirectory.toString());
		IFolder folder = project.getFolder(relPath);
		folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		return folder.getFullPath();
	}

	public static void addPDENature(IProject project, IPath manifestPath, IProgressMonitor monitor)
			throws CoreException {
		AbstractProjectConfigurator.addNature(project, PDE_PLUGIN_NATURE, monitor);
		IProjectDescription description = project.getDescription();
		Stream<ICommand> builders = Arrays.stream(description.getBuildSpec())
				.filter(b -> !b.getBuilderName().startsWith("org.eclipse.pde"));
		description.setBuildSpec(builders.toArray(ICommand[]::new));
		project.setDescription(description, monitor);

		setManifestLocaton(project, manifestPath, monitor);
	}

	protected static void setManifestLocaton(IProject project, IPath manifestPath, IProgressMonitor monitor)
			throws CoreException {
		IBundleProjectService projectService = Activator.getBundleProjectService().get();
		if (manifestPath != null && manifestPath.segmentCount() > 1) {
			IPath metainfPath = manifestPath.removeLastSegments(1);
			project.getFile(metainfPath).refreshLocal(IResource.DEPTH_INFINITE, monitor);
			projectService.setBundleRoot(project, metainfPath);
		} else {
			// in case of configuration update, reset to the default value
			projectService.setBundleRoot(project, null);
		}
	}

	/**
	 * Returns bundle manifest as known to PDE project metadata. Returned file may
	 * not exist in the workspace or on the filesystem. Never returns null.
	 */
	public static IFile getBundleManifest(IProject project) {
		// PDE API is very inconvenient, lets use internal classes instead
		@SuppressWarnings("restriction")
		IContainer metainf = org.eclipse.pde.internal.core.project.PDEProject.getBundleRoot(project);
		if (metainf == null || metainf instanceof IProject) {
			metainf = project.getFolder("META-INF");
		} else {
			metainf = metainf.getFolder(new Path("META-INF"));
		}
		return metainf.getFile(new Path("MANIFEST.MF"));
	}

	private static void setClasspath(IProject project, IPluginModelBase model, IProgressMonitor monitor)
			throws CoreException {
		@SuppressWarnings("restriction")
		IClasspathEntry[] entries = org.eclipse.pde.internal.core.ClasspathComputer.getClasspath(project, model, null,
				true /* clear existing entries */, true);
		JavaCore.create(project).setRawClasspath(entries, null);
		// workaround PDE sloppy model management during the first multimodule project
		// import in eclipse session
		// 1. m2e creates all modules as simple workspace projects without JDT or PDE
		// natures
		// 2. first call to
		// org.eclipse.pde.internal.core.PluginModelManager.initializeTable() reads all
		// workspace
		// projects regardless of their natures (see
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319268)
		// 3. going through all projects one by one
		// 3.1. m2e enables JDE and PDE natures and adds PDE classpath container
		// 3.2. org.eclipse.pde.internal.core.PDEClasspathContainer.addProjectEntry
		// ignores all project's dependencies
		// that do not have JAVA nature. at this point PDE classpath is missing some/all
		// workspace dependencies.
		// 4. PDE does not re-resolve classpath when dependencies get JAVA nature
		// enabled

		// as a workaround, touch project bundle manifests to force PDE re-read the
		// model, re-resolve dependencies
		// and recalculate PDE classpath

		IFile manifest = PDEProjectHelper.getBundleManifest(project);
		if (manifest.isAccessible()) {
			manifest.touch(monitor);
		}
	}
}
