/*******************************************************************************
 * Copyright (c) 2022 Konrad Windszus
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Konrad Windszus
 *******************************************************************************/
package org.eclipse.m2e.pde.connector;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.internal.AbstractJavaProjectConfigurator;
import org.eclipse.m2e.jdt.internal.ClasspathDescriptor;
import org.eclipse.m2e.jdt.internal.MavenClasspathHelpers;
import org.eclipse.pde.ds.internal.annotations.DSAnnotationVersion;
import org.eclipse.pde.internal.core.natures.PDE;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class TychoPackagingsConfigurator extends AbstractProjectConfigurator {

	private static final String TYCHO_GROUP_ID = "org.eclipse.tycho";
	private static final String TYCHO_DS_PLUGIN_ARTIFACT_ID = "tycho-ds-plugin";
	private static final String TYCHO_COMPILER_PLUGIN_ARTIFACT_ID = "tycho-compiler-plugin";
	private static final String GOAL_DECLARATIVE_SERVICES = "declarative-services";
	private static final String GOAL_COMPILE = "compile";
	private static final String GOAL_TEST_COMPILE = "testCompile";

	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		MavenProject mavenProject = request.mavenProject();
		IProject project = request.mavenProjectFacade().getProject();
		String packaging = mavenProject.getPackaging();
		if ("eclipse-plugin".equals(packaging) || "eclipse-test-plugin".equals(packaging)) {
			IJavaProject javaProject = PDEProjectHelper.configurePDEBundleProject(project, mavenProject, monitor);
			addProjectTestSourceFolders(javaProject, request, monitor);
			applyDsConfiguration(request, monitor);
		} else if ("eclipse-feature".equals(packaging)) {
			// see org.eclipse.pde.internal.ui.wizards.feature.AbstractCreateFeatureOperation
			if (!project.hasNature(PDE.FEATURE_NATURE)) {
				addNature(project, PDE.FEATURE_NATURE, monitor);
			}
		}
	}

	private void applyDsConfiguration(ProjectConfigurationRequest request, IProgressMonitor monitor)
			throws CoreException {
		List<MojoExecution> mojoExecutions = getTychoDsPluginMojoExecutions(request, monitor);
		if (mojoExecutions.isEmpty()) {
			return;
		}
		if (mojoExecutions.size() > 1) {
			String message = String.format(
					"Found more than one execution for plugin %s:%s and goal %s, only consider configuration of this one",
					TYCHO_GROUP_ID, TYCHO_DS_PLUGIN_ARTIFACT_ID, GOAL_DECLARATIVE_SERVICES);
			createWarningMarker(request, mojoExecutions.get(0), "executions", message);
		}
		// first mojo execution is relevant
		Xpp3Dom dom = mojoExecutions.get(0).getConfiguration();
		// apply PDE configuration for DS
		IEclipsePreferences prefs = new ProjectScope(request.mavenProjectFacade().getProject())
				.getNode(org.eclipse.pde.ds.internal.annotations.Activator.PLUGIN_ID);
		Xpp3Dom dsEnabled = dom.getChild("enabled");
		boolean isDsEnabled = false;
		if (dsEnabled != null && !dsEnabled.getValue().isEmpty()) {
			isDsEnabled = Boolean.valueOf(dsEnabled.getValue());
			prefs.putBoolean(org.eclipse.pde.ds.internal.annotations.Activator.PREF_ENABLED, isDsEnabled);
		}
		if (isDsEnabled) {
			Xpp3Dom dsVersion = dom.getChild("dsVersion");
			if (containsExplicitConfigurationValue(dsVersion)) {
				String versionValue = dsVersion.getValue();
				DSAnnotationVersion version = parseVersion(versionValue);
				if (version != null) {
					prefs.put(org.eclipse.pde.ds.internal.annotations.Activator.PREF_SPEC_VERSION, version.name());
				} else {
					String message = "Unsupported DS spec version " + versionValue + " found, using default instead";
					createWarningMarker(request, mojoExecutions.get(0), SourceLocationHelper.CONFIGURATION, message);
				}
			}
			Xpp3Dom path = dom.getChild("path");
			if (containsExplicitConfigurationValue(path)) {
				prefs.put(org.eclipse.pde.ds.internal.annotations.Activator.PREF_PATH, path.getValue());
			}
		}
	}

	private boolean containsExplicitConfigurationValue(Xpp3Dom config) {
		// check if value is a property name
		return config != null && !config.getValue().startsWith("${");
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

	private List<MojoExecution> getTychoDsPluginMojoExecutions(ProjectConfigurationRequest request,
			IProgressMonitor monitor) throws CoreException {
		return request.mavenProjectFacade().getMojoExecutions(TYCHO_GROUP_ID, TYCHO_DS_PLUGIN_ARTIFACT_ID, monitor,
				GOAL_DECLARATIVE_SERVICES);
	}

	private List<MojoExecution> getTychoCompilerPluginMojoExecutions(ProjectConfigurationRequest request,
			IProgressMonitor monitor, boolean isForTest) throws CoreException {
		return request.mavenProjectFacade().getMojoExecutions(TYCHO_GROUP_ID, TYCHO_COMPILER_PLUGIN_ARTIFACT_ID, monitor,
				isForTest ? GOAL_COMPILE : GOAL_TEST_COMPILE);
	}

	private void addProjectTestSourceFolders(IJavaProject javaProject, ProjectConfigurationRequest request,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor mon = SubMonitor.convert(monitor, 5);
		MavenProject mavenProject = request.mavenProject();
		IProject project = request.mavenProjectFacade().getProject();
		List<String> testSourcePaths = getTestSourcePaths(project);
		List<MojoExecution> mojoExecutions = getTychoCompilerPluginMojoExecutions(request, mon.newChild(1), true);
		if (mojoExecutions.isEmpty()) {
			return;
		}
		if (mojoExecutions.size() > 1) {
			String message = String.format(
					"Found more than one execution for plugin %s:%s and goal %s, only consider configuration of this one",
					TYCHO_GROUP_ID, TYCHO_COMPILER_PLUGIN_ARTIFACT_ID, GOAL_TEST_COMPILE);
			createWarningMarker(request, mojoExecutions.get(0), "executions", message);
		}
		
		IFolder testClasses = AbstractJavaProjectConfigurator.getFolder(project, mavenProject.getBuild().getTestOutputDirectory());
		IPath[] inclusionTest = new IPath[0];
		IPath[] exclusionTest = new IPath[0];
		String testSourceEncoding = null;
		//String testResourcesEncoding = null;
		try {
			testSourceEncoding = maven.getMojoParameterValue(mavenProject, mojoExecutions.get(0), "encoding", //$NON-NLS-1$
					String.class, mon.newChild(1));
		} catch (CoreException ex) {
			String message = "Could not evaluate \"encoding\" configuration, using default instead: " + ex.getMessage();
			createWarningMarker(request, mojoExecutions.get(0), SourceLocationHelper.CONFIGURATION, message);
		}
		try {
			inclusionTest = AbstractJavaProjectConfigurator.toPaths(maven.getMojoParameterValue(mavenProject,
					mojoExecutions.get(0), "includes", String[].class, mon.newChild(1))); //$NON-NLS-1$
		} catch (CoreException ex) {
			String message = "Could not evaluate \"includes\" configuration, using default instead: " + ex.getMessage();
			createWarningMarker(request, mojoExecutions.get(0), SourceLocationHelper.CONFIGURATION, message);
		}
		try {
			exclusionTest = AbstractJavaProjectConfigurator.toPaths(maven.getMojoParameterValue(mavenProject,
					mojoExecutions.get(0), "excludes", String[].class, mon.newChild(1))); //$NON-NLS-1$
		} catch (CoreException ex) {
			String message = "Could not evaluate \"excludes\" configuration, using default instead: " + ex.getMessage();
			createWarningMarker(request, mojoExecutions.get(0), SourceLocationHelper.CONFIGURATION, message);
		}

		IClasspathDescriptor classpath = new ClasspathDescriptor(javaProject);

		// If the project properties contain m2e.disableTestClasspathFlag=true, then the
		// test flag must not be set
		boolean addTestFlag = !MavenClasspathHelpers.hasTestFlagDisabled(mavenProject);
		AbstractJavaProjectConfigurator.addSourceDirs(classpath, request.mavenProjectFacade().getProject(),
				testSourcePaths, testClasses.getFullPath(), inclusionTest, exclusionTest,
				testSourceEncoding, mon.newChild(1), addTestFlag);
	}

	private List<String> getTestSourcePaths(IProject project) throws CoreException {
		return Arrays.stream(project.members())
			.filter(r -> r.getType() == IResource.FOLDER && (r.getName().startsWith("test") || r.getName().endsWith("test")))
			.map(IResource::getName)
			.collect(Collectors.toList());
	}

	private void createWarningMarker(ProjectConfigurationRequest request, MojoExecution execution, String attribute,
			String message) {
		PDEMavenBundlePluginConfigurator.createWarningMarker(projectManager, markerManager, request, execution,
				attribute, message);
	}
}
