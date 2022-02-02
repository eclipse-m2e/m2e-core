/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.pde.connector;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;

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
		List<MojoExecution> executions = getMojoExecutions(request, monitor);
		boolean hasManifestExecution = false;
		for (MojoExecution execution : executions) {
			Plugin plugin = execution.getPlugin();
			if (isFelix(plugin)) {
				if (isFelixManifestGoal(execution)) {
					IMaven maven = MavenPlugin.getMaven();
					Boolean supportIncremental = maven.getMojoParameterValue(request.getMavenProject(), execution,
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
			MojoExecution execution = executions.get(0);
			createWarningMarker(request, execution, "executions",
					"There is currently no execution that generates a manifest, consider adding an execution for one of the following goal: "
							+ (isFelix(execution.getPlugin()) ? FELIX_MANIFEST_GOAL : BND_MANIFEST_GOALS) + ".");
		}

		IMavenProjectFacade facade = request.getMavenProjectFacade();
		IPath metainfPath = getMetainfPath(facade, executions, monitor);
		PDEProjectHelper.addPDENature(facade.getProject(), metainfPath, monitor);
	}

	private void createWarningMarker(ProjectConfigurationRequest request, MojoExecution execution, String attribute,
			String message) {
		SourceLocation location = SourceLocationHelper.findLocation(execution.getPlugin(), attribute);

		String[] gav = location.getResourceId().split(":");
		IMavenProjectFacade facade = projectManager.getMavenProject(gav[0], gav[1], gav[2]);
		if (facade == null) {
			// attribute specifying project (probably parent) is not in the workspace.
			// The following code returns the location of the project's parent-element.
			location = SourceLocationHelper.findLocation(request.getMavenProject(), new MojoExecutionKey(execution));
			facade = request.getMavenProjectFacade();
		}
		MavenProblemInfo problem = new MavenProblemInfo(message, IMarker.SEVERITY_WARNING, location);
		markerManager.addErrorMarker(facade.getPom(), IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, problem);
	}

	private boolean isFelixManifestGoal(MojoExecution execution) {
		return FELIX_MANIFEST_GOAL.equals(execution.getGoal());
	}

	private boolean isBNDBundleGoal(MojoExecution execution) {
		return BND_MANIFEST_GOALS.contains(execution.getGoal());
	}

	@Override
	public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
			throws CoreException { // nothing to do
	}

	@Override
	public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
			IProgressMonitor monitor) throws CoreException { // nothing to do
	}

	private IPath getMetainfPath(IMavenProjectFacade facade, List<MojoExecution> executions, IProgressMonitor monitor)
			throws CoreException {
		IMaven maven = MavenPlugin.getMaven();
		for (MojoExecution execution : executions) {
			Plugin plugin = execution.getPlugin();
			MavenProject project = facade.getMavenProject(monitor);
			String manifestParameter = isBND(plugin) ? BND_PARAM_MANIFESTLOCATION : FELIX_PARAM_MANIFESTLOCATION;
			File location = maven.getMojoParameterValue(project, execution, manifestParameter, File.class, monitor);
			if (location != null) {
				return facade.getProjectRelativePath(location.getAbsolutePath());
			}
		}
		return null;
	}

	private boolean isBND(Plugin plugin) {
		return plugin != null && "bnd-maven-plugin".equals(plugin.getArtifactId());
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
		if ((isFelix(execution.getPlugin()) && isFelixManifestGoal(execution))
				|| (isBND(execution.getPlugin()) && isBNDBundleGoal(execution))) {
			return new MojoExecutionBuildParticipant(execution, true, true);
		}
		return super.getBuildParticipant(projectFacade, execution, executionMetadata);
	}
}
