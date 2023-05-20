/*******************************************************************************
 * Copyright (c) 2011, 2022 Sonatype, Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.connector;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.natures.PDE;

public class PDEProjectHelper {

	@SuppressWarnings("restriction")
	private static final String PDE_PLUGIN_NATURE = org.eclipse.pde.internal.core.natures.PDE.PLUGIN_NATURE;

	private static AtomicBoolean isListeningForPluginModelChanges = new AtomicBoolean(false);

	private static final Set<IProject> PROJECTS_FOR_UPDATE_CLASSPATH = ConcurrentHashMap.newKeySet();

	private PDEProjectHelper() {
	}

	@SuppressWarnings("restriction")
	private static final org.eclipse.pde.internal.core.IPluginModelListener CLASSPATH_UPDATER = delta -> {
		PROJECTS_FOR_UPDATE_CLASSPATH.removeIf(project -> {
			IPluginModelBase model = PluginRegistry.findModel(project);
			if (model != null) {
				new WorkspaceJob("Updating classpath") {
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						setClasspath(project, model, monitor);
						return Status.OK_STATUS;
					}
				}.schedule();
				return true;
			}
			return false;
		});
	};

	static void configurePDEBundleProject(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
			throws CoreException {
		// see org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation

		if (!project.hasNature(PDE_PLUGIN_NATURE)) {
			AbstractProjectConfigurator.addNature(project, PDE_PLUGIN_NATURE, null);
		}

		if (!project.hasNature(JavaCore.NATURE_ID)) {
			AbstractProjectConfigurator.addNature(project, JavaCore.NATURE_ID, null);
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
	}

	@SuppressWarnings("restriction")
	static void configurePDEFeatureProject(IMavenProjectFacade projectFacade, IProgressMonitor monitor)
			throws CoreException {
		IProject project = projectFacade.getProject();
		if (project != null) {
			// see
			// org.eclipse.pde.internal.ui.wizards.feature.AbstractCreateFeatureOperation
			if (!project.hasNature(PDE.FEATURE_NATURE)) {
				AbstractProjectConfigurator.addNature(project, PDE.FEATURE_NATURE, monitor);
			}
		}
	}

	@SuppressWarnings("restriction")
	private static void addProjectForUpdateClasspath(IProject project) {
		PROJECTS_FOR_UPDATE_CLASSPATH.add(project);
		if (isListeningForPluginModelChanges.compareAndSet(false, true)) {
			org.eclipse.pde.internal.core.PDECore pdeCore = org.eclipse.pde.internal.core.PDECore.getDefault();
			pdeCore.getModelManager().addPluginModelListener(CLASSPATH_UPDATER);
		}
	}

	private static IPath getOutputLocation(IProject project, MavenProject mavenProject, IProgressMonitor m)
			throws CoreException {
		SubMonitor monitor = SubMonitor.convert(m, 2);
		IPath outputFolderPath = getDefaultLibraryOutputFolder(project, monitor.split(1));
		if (outputFolderPath == null) {
			File outputDirectory = new File(mavenProject.getBuild().getOutputDirectory());
			outputDirectory.mkdirs();
			outputFolderPath = MavenProjectUtils.getProjectRelativePath(project, outputDirectory.toString());
		}
		IFolder folder = project.getFolder(outputFolderPath);
		folder.refreshLocal(IResource.DEPTH_INFINITE, monitor.split(1));
		return folder.getFullPath();
	}

	@SuppressWarnings("restriction")
	private static IPath getDefaultLibraryOutputFolder(IProject project, IProgressMonitor monitor)
			throws CoreException {
		IFile buildProperties = org.eclipse.pde.internal.core.project.PDEProject.getBuildProperties(project);
		if (buildProperties.exists()) {
			IBuildModel model = new org.eclipse.pde.internal.core.build.WorkspaceBuildModel(buildProperties);
			model.load();
			monitor.done();
			IBuildEntry entry = model.getBuild().getEntry("output." + ".");
			if (entry != null) {
				return org.eclipse.core.runtime.Path.forPosix(entry.getTokens()[0]);
			}
		}
		return null;
	}

	static void addPDENature(IProject project, IPath manifestPath, IProgressMonitor monitor) throws CoreException {
		AbstractProjectConfigurator.addNature(project, PDE_PLUGIN_NATURE, monitor);
		IProjectDescription description = project.getDescription();
		Stream<ICommand> builders = Arrays.stream(description.getBuildSpec())
				.filter(b -> !b.getBuilderName().startsWith("org.eclipse.pde."));
		description.setBuildSpec(builders.toArray(ICommand[]::new));
		project.setDescription(description, monitor);

		setManifestLocaton(project, manifestPath, monitor);
	}

	@SuppressWarnings("restriction")
	private static void setManifestLocaton(IProject project, IPath manifestPath, IProgressMonitor monitor)
			throws CoreException {
		IContainer bundleRoot = null; // in case of configuration update, reset to the default value
		if (manifestPath != null) {
			IFile manifest;
			if (manifestPath.toFile().toPath().endsWith("META-INF")) {
				manifest = project.getFolder(manifestPath).getFile("MANIFEST.MF");
			} else if (manifestPath.toFile().toPath().endsWith(Path.of("META-INF", "MANIFEST.MF"))) {
				manifest = project.getFile(manifestPath);
			} else {
				return;
			}
			manifest.refreshLocal(IResource.DEPTH_ZERO, monitor);
			bundleRoot = manifest.getParent().getParent();
		}
		org.eclipse.pde.internal.core.project.PDEProject.setBundleRoot(project, bundleRoot);
	}

	/**
	 * Returns bundle manifest as known to PDE project metadata. Returned file may
	 * not exist in the workspace or on the filesystem. Never returns null.
	 */
	@SuppressWarnings("restriction")
	private static IFile getBundleManifest(IProject project) {
		// PDE API is very inconvenient, lets use internal classes instead
		IContainer metaInf = org.eclipse.pde.internal.core.project.PDEProject.getBundleRoot(project);
		return (metaInf == null || metaInf instanceof IProject ? project : metaInf)
				.getFile(org.eclipse.pde.internal.core.ICoreConstants.MANIFEST_PATH);
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

		IFile manifest = getBundleManifest(project);
		if (manifest.isAccessible()) {
			manifest.touch(monitor);
		}
	}
}
