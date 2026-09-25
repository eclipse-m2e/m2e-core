/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype, Inc. and others
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenArtifactIdentifier;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IMojoExecutionFacade;
import org.eclipse.m2e.core.project.IPluginFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * This configurator performs the following tasks:
 * <ul>
 * <li>Enable the PDE nature for this project to make PDE aware of this
 * project</li>
 * <li>Set the location of the "bundle-root" where PDE looks for the
 * manifest</li>
 * </ul>
 */
@SuppressWarnings("restriction")
public class PDEMavenBundlePluginConfigurator extends AbstractProjectConfigurator implements IJavaProjectConfigurator {

	private static final String FELIX_PARAM_MANIFESTLOCATION = "manifestLocation";
	private static final String FELIX_PARAM_SUPPORTINCREMENTALBUILD = "supportIncrementalBuild";
	private static final String FELIX_MANIFEST_GOAL = "manifest";
	private static final String BND_PARAM_MANIFESTLOCATION = "manifestPath";
	private static final List<String> BND_MANIFEST_GOALS = List.of("bnd-process", "bnd-process-tests", "jar",
			"test-jar");

	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		List<IMojoExecutionFacade> executions = getMojoExecutionFacades(request, monitor);
		boolean hasManifestExecution = false;
		for (IMojoExecutionFacade execution : executions) {
			IPluginFacade plugin = execution.getPlugin();
			if (isFelix(plugin)) {
				if (isFelixManifestGoal(execution)) {
					Boolean supportIncremental = execution.getMojoParameterValue(
							FELIX_PARAM_SUPPORTINCREMENTALBUILD, Boolean.class, monitor);
					if (supportIncremental == null || !supportIncremental.booleanValue()) {
						createWarningMarker(request, execution, SourceLocationHelper.CONFIGURATION,
								"Incremental updates are currently disabled, set supportIncrementalBuild=true to support automatic manifest updates for this project.");
					}
					hasManifestExecution = true;
				}
			} else if (isBND(plugin) && isBNDBundleGoal(execution)) {
				hasManifestExecution = true;
			}
		}
		if (!hasManifestExecution && !executions.isEmpty()) {
			IMojoExecutionFacade execution = executions.get(0);
			createWarningMarker(request, execution, "executions",
					"There is currently no execution that generates a manifest, consider adding an execution for one of the following goal: "
							+ (isFelix(execution.getPlugin()) ? FELIX_MANIFEST_GOAL : BND_MANIFEST_GOALS) + ".");
		}

		IMavenProjectFacade facade = request.mavenProjectFacade();
		IPath metainfPath = getMetainfPath(facade, executions, monitor);
		PDEProjectHelper.addPDENature(facade.getProject(), metainfPath, monitor);
	}

	private void createWarningMarker(ProjectConfigurationRequest request, IMojoExecutionFacade execution,
			String attribute, String message) {
		createWarningMarker(projectManager, markerManager, request, execution.getKey(), attribute, message);
	}

	static void createWarningMarker(IMavenProjectRegistry projectManager, IMavenMarkerManager markerManager,
			ProjectConfigurationRequest request, MojoExecutionKey executionKey, String attribute, String message) {
		Plugin plugin = request.mavenProject().getPlugin(executionKey.groupId() + ":" + executionKey.artifactId());
		SourceLocation location = SourceLocationHelper.findLocation(plugin, attribute);

		String[] gav = location.getResourceId().split(":");
		IMavenProjectFacade facade = projectManager.getMavenProject(gav[0], gav[1], gav[2]);
		if (facade == null) {
			// attribute specifying project (probably parent) is not in the workspace.
			// The following code returns the location of the project's parent-element.
			location = SourceLocationHelper.findLocation(request.mavenProject(), executionKey);
			facade = request.mavenProjectFacade();
		}
		MavenProblemInfo problem = new MavenProblemInfo(message, IMarker.SEVERITY_WARNING, location);
		markerManager.addErrorMarker(facade.getPom(), IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, problem);
	}

	private boolean isFelixManifestGoal(IMojoExecutionFacade execution) {
		return isFelixManifestGoal(execution.getKey().goal());
	}

	private boolean isFelixManifestGoal(String goal) {
		return FELIX_MANIFEST_GOAL.equals(goal);
	}

	private boolean isBNDBundleGoal(IMojoExecutionFacade execution) {
		return isBNDBundleGoal(execution.getKey().goal());
	}

	private boolean isBNDBundleGoal(String goal) {
		return BND_MANIFEST_GOALS.contains(goal);
	}

	@Override
	public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
			throws CoreException { // nothing to do
	}

	@Override
	public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
			IProgressMonitor monitor) throws CoreException { // nothing to do
	}

	private IPath getMetainfPath(IMavenProjectFacade facade, List<IMojoExecutionFacade> executions,
			IProgressMonitor monitor) throws CoreException {
		// TODO: warn on multiple executions and prefer the one without classifier (i.e.
		// the main artifact or the one for the bnd-process/jar goal??
		for (IMojoExecutionFacade execution : executions) {
			IPluginFacade plugin = execution.getPlugin();
			String manifestParameter = isBND(plugin) ? BND_PARAM_MANIFESTLOCATION : FELIX_PARAM_MANIFESTLOCATION;
			File location = execution.getMojoParameterValue(manifestParameter, File.class, monitor);
			if (location != null) {
				return facade.getProjectRelativePath(location.getAbsolutePath());
			}
		}
		return null;
	}

	private boolean isBND(IPluginFacade plugin) {
		return plugin != null && "bnd-maven-plugin".equals(plugin.getArtifactId());
	}

	private boolean isBND(Plugin plugin) {
		return plugin != null && "bnd-maven-plugin".equals(plugin.getArtifactId());
	}

	private boolean isFelix(IPluginFacade plugin) {
		return plugin != null && "org.apache.felix".equals(plugin.getGroupId())
				&& "maven-bundle-plugin".equals(plugin.getArtifactId());
	}

	private boolean isFelix(Plugin plugin) {
		return plugin != null && "org.apache.felix".equals(plugin.getGroupId())
				&& "maven-bundle-plugin".equals(plugin.getArtifactId());
	}

	@Override
	public boolean hasConfigurationChanged(IMavenProjectFacade newFacade,
			ILifecycleMappingConfiguration oldProjectConfiguration, MojoExecutionKey key, IProgressMonitor monitor) {
		return false;
	}

	@Override
	public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
			IPluginExecutionMetadata executionMetadata) {
		Plugin plugin = execution.getPlugin();
		if ((isFelix(plugin) && isFelixManifestGoal(execution.getGoal()))
				|| (isBND(plugin) && isBNDBundleGoal(execution.getGoal()))) {
			// Run .classpath synchronization on each incremental build in order to consider
			// potential changes on the Bundle-ClassPath and the resources recognized by the
			// '-includeResource' instruction that are caused by previous mojo executions.
			return new ClasspathSynchronizer(execution, true, true);
		}
		return null;
	}

	private static final class ClasspathSynchronizer extends MojoExecutionBuildParticipant {

		public ClasspathSynchronizer(MojoExecution execution, boolean runOnIncremental, boolean runOnConfiguration) {
			super(execution, runOnIncremental, runOnConfiguration);
		}

		@Override
		public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
			if (!appliesToBuildKind(kind)) {
				return null;
			}
			Set<IProject> buildProjects = super.build(kind, monitor);

			// TODO: is deriving the .classpath from the MANIFEST.MF and build.properties
			// something useful for PDE?

			IProject project = getMavenProjectFacade().getProject();
			Path manifest = Path.of(PDEProject.getManifest(project).getLocationURI());
			if (!Files.isRegularFile(manifest)) {
				return buildProjects;
			}
			getBuildContext().refresh(manifest.toFile());

			List<ManifestElement> bundleClassPath = getBundleClassPathEntries(manifest);

			IJavaProject javaProject = JavaCore.create(project);
			Set<IPath> bundleClasspathJars = getBundleClassPathJars(bundleClassPath, javaProject, monitor);

			if (!bundleClasspathJars.isEmpty()) {

				List<IClasspathEntry> rawClasspath = new ArrayList<>(Arrays.asList(javaProject.getRawClasspath()));

				Set<IPath> existingLibraryPaths = new HashSet<>();
				int sizeBefore = rawClasspath.size();
				rawClasspath.removeIf(entry -> {
					if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
						existingLibraryPaths.add(entry.getPath());
						return !bundleClasspathJars.contains(entry.getPath()) && isPomDerived(entry);
					}
					return false;
				});

				boolean changed = sizeBefore != rawClasspath.size();
				for (IPath jarPath : bundleClasspathJars) {
					if (!existingLibraryPaths.contains(jarPath)) {
						rawClasspath.add(createLibraryEntry(jarPath, monitor));
						changed = true;
					}
				}
				if (changed) {
					javaProject.setRawClasspath(rawClasspath.toArray(IClasspathEntry[]::new), monitor);
				}
			}
			return buildProjects;
		}

		private List<ManifestElement> getBundleClassPathEntries(Path manifest) throws IOException, BundleException {
			try (var input = Files.newInputStream(manifest)) {
				Map<String, String> headers = ManifestElement.parseBundleManifest(input, null);
				String bcpEntries = headers.get(Constants.BUNDLE_CLASSPATH);
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, bcpEntries);
				return elements != null ? List.of(elements) : List.of();
			}
		}

		private Set<IPath> getBundleClassPathJars(List<ManifestElement> bundleClassPath, IJavaProject javaProject,
				IProgressMonitor monitor) throws IOException, CoreException {

			IProject project = javaProject.getProject();
			IContainer bundleRootContainer = PDEProject.getBundleRoot(project);
			Path bundleRoot = Path.of(bundleRootContainer.getLocationURI());

			Path bcpContainer = bundleRoot;
			IPath bcpContainerPath = bundleRootContainer.getFullPath();
			if (javaProject.getOutputLocation().equals(bundleRootContainer.getFullPath())) {
				// The project's PDE Bundle-Root is the build output folder. Because JDT does
				// not permit to reference Jars in the classpath located within the output
				// folder, the relevant files are copied to a co-located folder.
				Path buildDirectory = Path.of(getMavenProjectFacade().getMavenProject().getBuild().getDirectory());
				bcpContainer = buildDirectory.resolve("m2e-bundleClassPath");
				Path relativeBCPContainerPath = Path.of(project.getLocationURI()).relativize(bcpContainer);
				bcpContainerPath = project.getFolder(relativeBCPContainerPath.toString()).getFullPath();
			}

			IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
			Set<IPath> bundleClasspathJars = new LinkedHashSet<>();
			for (ManifestElement entry : bundleClassPath) {
				String path = entry.getValueComponents()[0];
				Path jar = bundleRoot.resolve(path);
				if (path.endsWith(".jar") && Files.exists(jar)) {
					Path referencedJar = bcpContainer.resolve(path);
					IPath jarPath = bcpContainerPath.append(path);
					if (!Files.exists(referencedJar)) {
						Files.createDirectories(referencedJar.getParent());
						Files.copy(jar, referencedJar);
						wsRoot.getFile(jarPath).refreshLocal(IResource.DEPTH_ZERO, monitor);
					}
					bundleClasspathJars.add(jarPath);
				}
			}
			return bundleClasspathJars;
		}

		private static boolean isPomDerived(IClasspathEntry entry) {
			return Arrays.stream(entry.getExtraAttributes())
					.anyMatch(a -> a.getName().equals(IClasspathManager.POMDERIVED_ATTRIBUTE)
							&& Boolean.parseBoolean(a.getValue()));
		}

		private static IClasspathEntry createLibraryEntry(IPath libPath, IProgressMonitor monitor) {
			IClasspathAttribute[] attributes = new IClasspathAttribute[] {
					JavaCore.newClasspathAttribute(IClasspathManager.POMDERIVED_ATTRIBUTE, Boolean.toString(true)) };
			IFile libFile = ResourcesPlugin.getWorkspace().getRoot().getFile(libPath);
			Collection<ArtifactKey> artifacts = MavenArtifactIdentifier.identify(libFile.getLocation().toFile());
			IPath sourcePath = artifacts.stream().map(a -> MavenArtifactIdentifier.resolveSourceLocation(a, monitor))
					.filter(Objects::nonNull).map(Path::toString).map(IPath::fromOSString).findFirst().orElse(null);
			return JavaCore.newLibraryEntry(libPath, sourcePath, null, null, attributes, true);
		}
	}

}
