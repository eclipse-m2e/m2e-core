/*******************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.pde.connector.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.natures.PDE;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("restriction")
public class TychoConnectorTest extends AbstractMavenProjectTestCase {

	static final Set<String> PLUGIN_NATURES = Set.of(PDE.PLUGIN_NATURE, JavaCore.NATURE_ID, IMavenConstants.NATURE_ID);
	static final Set<String> PLUGIN_BUILDERS = Set.of(PDE.MANIFEST_BUILDER_ID, PDE.SCHEMA_BUILDER_ID,
			JavaCore.BUILDER_ID, IMavenConstants.BUILDER_ID);
	static final Set<String> PLUGIN_WITH_DS_BUILDERS = Set.of(PDE.MANIFEST_BUILDER_ID, PDE.SCHEMA_BUILDER_ID,
			"org.eclipse.pde.ds.core.builder", JavaCore.BUILDER_ID, IMavenConstants.BUILDER_ID);
	static final Set<String> FEATURE_NATURES = Set.of(PDE.FEATURE_NATURE, IMavenConstants.NATURE_ID);
	static final Set<String> FEATURE_BUILDERS = Set.of(PDE.FEATURE_BUILDER_ID, IMavenConstants.BUILDER_ID);

	// FIXME: requires the osgi.compatibility fragment.

	private IProject importTychoProject(String name) throws IOException, CoreException {
		return importProjects("projects/tycho", new String[] { name }, new ResolverConfiguration(), false, null)[0];
	}

	@Test
	public void importTychoPlugin() throws IOException, CoreException {
		IProject project = importTychoProject("pde.tycho.plugin/pom.xml");
		assertErrorFreeProjectWithBuildersAndNatures(project, PLUGIN_NATURES, PLUGIN_BUILDERS);
		assertPluginProjectExists(project, "pde.tycho.plugin");
		assertClasspathContainer(project);
	}

	@Test
	@Ignore("This test currently only fails on the Jenkins CI")
	public void importPomlessTychoPlugin() throws IOException, CoreException {
		// TODO why .polyglot.META-INF ? Actually this should work without and m2e
		// generates the file automatically!
		IProject project = importTychoProject("pde.tycho.pomless.plugin/.polyglot.META-INF");
		assertErrorFreeProjectWithBuildersAndNatures(project, PLUGIN_NATURES, PLUGIN_BUILDERS);
		assertPluginProjectExists(project, "pde.tycho.pomless.plugin");
		assertClasspathContainer(project);
	}

	@Test
	public void importTychoPluginWithDS() throws Exception {
		IProject project = importTychoProject("pde.tycho.plugin.with.ds/pom.xml");
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		
		assertErrorFreeProjectWithBuildersAndNatures(project, PLUGIN_NATURES, PLUGIN_WITH_DS_BUILDERS);
		assertPluginProjectExists(project, "pde.tycho.plugin.with.ds");
		IEclipsePreferences dsNode = new ProjectScope(project).getNode("org.eclipse.pde.ds.annotations");
		assertNotNull(dsNode);
		assertTrue(dsNode.getBoolean(org.eclipse.pde.ds.internal.annotations.Activator.PREF_ENABLED, false));
		assertEquals("V1_2", dsNode.get(org.eclipse.pde.ds.internal.annotations.Activator.PREF_SPEC_VERSION, ""));
		assertEquals("OSGI-INF", dsNode.get(org.eclipse.pde.ds.internal.annotations.Activator.PREF_PATH, ""));
	}

	@Test
	public void importTychoFeature() throws IOException, CoreException {
		IProject project = importTychoProject("pde.tycho.feature/pom.xml");
		assertErrorFreeProjectWithBuildersAndNatures(project, FEATURE_NATURES, FEATURE_BUILDERS);
	}

	@Test
	public void importPomlessTychoFeature() throws IOException, CoreException {
		IProject project = importTychoProject("pde.tycho.pomless.feature/.polyglot.feature.xml");
		assertNaturesAndBuilders(project, FEATURE_NATURES, FEATURE_BUILDERS);
		assertNoErrorsAndWarnings(project, w -> {
			try {
				return w.getResource().getName().equals(".polyglot.feature.xml")
						&& "Version is duplicate of parent version".equals(w.getAttribute(IMarker.MESSAGE));
			} catch (CoreException e) {
				throw new AssertionError(e);
			}
		});
	}

	static void copyHierarchyRootFiles(String rootPath) throws IOException {
		// Copy the files that are referenced from the later imported projects into the
		// test-workspace root
		Path wsRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
		Files.copy(Path.of(rootPath + "/pom.xml"), wsRoot.resolve("pom.xml"));
		FileUtils.copyDirectory(new File(rootPath + "/.mvn"), wsRoot.resolve(".mvn").toFile());
	}

	static void assertErrorFreeProjectWithBuildersAndNatures(IProject project, Set<String> expectedNatures,
			Set<String> expectedBuilders) throws CoreException {
		assertNaturesAndBuilders(project, expectedNatures, expectedBuilders);
		assertNoErrorsAndWarnings(project, w -> false);
	}

	private static void assertNaturesAndBuilders(IProject project, Set<String> expectedNatures,
			Set<String> expectedBuilders) throws CoreException {
		assertEquals(expectedNatures, Set.of(project.getDescription().getNatureIds()));
		var actualBuilders = Arrays.stream(project.getDescription().getBuildSpec()).map(s -> s.getBuilderName());
		assertEquals(expectedBuilders, actualBuilders.collect(Collectors.toSet()));
	}

	private static void assertNoErrorsAndWarnings(IProject project, Predicate<IMarker> ignoredWarnings)
			throws CoreException {
		WorkspaceHelpers.assertNoErrors(project);
		List<IMarker> markers = WorkspaceHelpers.findWarningMarkers(project);
		markers.removeIf(ignoredWarnings);
		Assert.assertEquals("Unexpected warning markers " + WorkspaceHelpers.toString(markers), 0, markers.size());
	}

	static void assertPluginProjectExists(IProject project, String expectedBSN) {
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull("No Plug-in exists with id: " + expectedBSN, model);
		assertEquals(expectedBSN, model.getPluginBase().getId());
	}

	void assertClasspathContainer(IProject project) throws CoreException {
		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] entries = javaProject.getRawClasspath();
		for (IClasspathEntry entry : entries) {
			if ("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER".equals(entry.getPath().toString())) {
				return;
			}
		}
		fail("M2EClasspath Container not found! ("
				+ Arrays.stream(entries).map(String::valueOf).collect(Collectors.joining(", ")));
	}
}
