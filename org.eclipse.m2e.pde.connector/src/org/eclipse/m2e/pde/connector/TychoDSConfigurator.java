/*******************************************************************************
 * Copyright (c) 2022 Konrad Windszus and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Konrad Windszus - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.connector;

import java.util.List;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.pde.ds.internal.annotations.DSAnnotationVersion;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

@SuppressWarnings("restriction")
public class TychoDSConfigurator extends AbstractProjectConfigurator {

	private static final ILog LOG = Platform.getLog(TychoDSConfigurator.class);

	private static final String TYCHO_GROUP_ID = "org.eclipse.tycho";
	private static final String TYCHO_DS_PLUGIN_ARTIFACT_ID = "tycho-ds-plugin";
	private static final String GOAL_DECLARATIVE_SERVICES = "declarative-services";

	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		List<MojoExecution> mojoExecutions = getTychoDsPluginMojoExecutions(request.mavenProjectFacade(), monitor);
		if (mojoExecutions.isEmpty()) {
			return;
		}
		MojoExecution mojoExecution = mojoExecutions.get(0); // first mojo execution is relevant
		if (mojoExecutions.size() > 1) {
			String message = String.format(
					"Found more than one execution for plugin %s:%s and goal %s, only consider configuration of this one",
					TYCHO_GROUP_ID, TYCHO_DS_PLUGIN_ARTIFACT_ID, GOAL_DECLARATIVE_SERVICES);
			createWarningMarker(request, mojoExecution, "executions", message);
		}
		// apply PDE configuration for DS
		MavenProject project = request.mavenProject();
		boolean isDsEnabled = maven.getMojoParameterValue(project, mojoExecution, "enabled", Boolean.class, monitor);
		if (isDsEnabled) {
			IEclipsePreferences prefs = new ProjectScope(request.mavenProjectFacade().getProject())
					.getNode(org.eclipse.pde.ds.internal.annotations.Activator.PLUGIN_ID);

			prefs.putBoolean(org.eclipse.pde.ds.internal.annotations.Activator.PREF_ENABLED, isDsEnabled);

			String dsVersion = maven.getMojoParameterValue(project, mojoExecution, "dsVersion", String.class, monitor);
			DSAnnotationVersion version = parseVersion(dsVersion);
			if (version != null) {
				prefs.put(org.eclipse.pde.ds.internal.annotations.Activator.PREF_SPEC_VERSION, version.name());
			} else {
				String message = "Unsupported DS spec version " + dsVersion + " found, using default instead";
				createWarningMarker(request, mojoExecution, SourceLocationHelper.CONFIGURATION, message);
			}
			String path = maven.getMojoParameterValue(project, mojoExecution, "path", String.class, monitor);
			prefs.put(org.eclipse.pde.ds.internal.annotations.Activator.PREF_PATH, path);
			try {
				prefs.flush();
			} catch (BackingStoreException e) {
				LOG.error("Failed to save PDE-DS preferences", e);
			}
		}
	}

	private DSAnnotationVersion parseVersion(String version) {
		if (version.startsWith("V")) {
			version = version.substring(1).replace('_', '.');
		}
		try {
			Version osgiVersion = Version.parseVersion(version);
			if (osgiVersion.getMajor() == 1 && osgiVersion.getMinor() == 1) {
				return DSAnnotationVersion.V1_1;
			} else if (osgiVersion.getMajor() == 1 && osgiVersion.getMinor() == 2) {
				return DSAnnotationVersion.V1_2;
			} else if (osgiVersion.getMajor() == 1 && osgiVersion.getMinor() == 3) {
				return DSAnnotationVersion.V1_3;
			}
		} catch (IllegalArgumentException e) { // assume no match
		}
		return null;
	}

	private List<MojoExecution> getTychoDsPluginMojoExecutions(IMavenProjectFacade projectFacade,
			IProgressMonitor monitor) throws CoreException {
		return projectFacade.getMojoExecutions(TYCHO_GROUP_ID, TYCHO_DS_PLUGIN_ARTIFACT_ID, monitor,
				GOAL_DECLARATIVE_SERVICES);
	}

	private void createWarningMarker(ProjectConfigurationRequest request, MojoExecution execution, String attribute,
			String message) {
		PDEMavenBundlePluginConfigurator.createWarningMarker(projectManager, markerManager, request, execution,
				attribute, message);
	}
}
