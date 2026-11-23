/*******************************************************************************
 * Copyright (c) 2022, 2026 Hannes Wellmann and others
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
import static org.junit.Assert.assertFalse;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.pde.connector.PDEProjectHelper;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.internal.core.natures.FeatureProject;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("restriction")
public class TychoConnectorTest extends AbstractMavenProjectTestCase {

	static final Set<String> PLUGIN_NATURES = Set.of(IBundleProjectDescription.PLUGIN_NATURE, JavaCore.NATURE_ID, IMavenConstants.NATURE_ID);
	static final Set<String> PLUGIN_BUILDERS = Set.of(PluginProject.MANIFEST_BUILDER_ID, PluginProject.SCHEMA_BUILDER_ID,
			JavaCore.BUILDER_ID, IMavenConstants.BUILDER_ID);
	static final Set<String> PLUGIN_WITH_DS_BUILDERS = Set.of(PluginProject.MANIFEST_BUILDER_ID, PluginProject.SCHEMA_BUILDER_ID,
			"org.eclipse.pde.ds.core.builder", JavaCore.BUILDER_ID, IMavenConstants.BUILDER_ID);
	static final Set<String> FEATURE_NATURES = Set.of(FeatureProject.NATURE, IMavenConstants.NATURE_ID);
	static final Set<String> FEATURE_BUILDERS = Set.of(FeatureProject.BUILDER_ID, IMavenConstants.BUILDER_ID);

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
	public void importTychoPluginWithDeferredClasspathUpdate() throws Exception {
		assertEquals("Workspace must be empty before initializing PDE", 0,
				ResourcesPlugin.getWorkspace().getRoot().getProjects().length);
		// Initialize before import, so PDE discovers the new model from the resource
		// delta after m2e's batched import, rather than scanning it during configuration.
		assertEquals(0, PluginRegistry.getWorkspaceModels().length);

		AtomicReference<Job> deferredUpdate = new AtomicReference<>();
		CountDownLatch completed = new CountDownLatch(1);
		JobChangeAdapter listener = new JobChangeAdapter() {
			@Override
			public void scheduled(IJobChangeEvent event) {
				Job job = event.getJob();
				// Observe the real deferred writer without depending on its display name
				// or the compiler-generated anonymous class number.
				if (job.getClass().getEnclosingClass() == PDEProjectHelper.class) {
					deferredUpdate.compareAndSet(null, job);
				}
			}

			@Override
			public void done(IJobChangeEvent event) {
				if (event.getJob() == deferredUpdate.get()) {
					completed.countDown();
				}
			}
		};
		Job.getJobManager().addJobChangeListener(listener);
		try {
			IProject project = importTychoProject("pde.tycho.plugin/pom.xml");
			assertTrue("Deferred PDE classpath update was not observed completing",
					completed.await(30, TimeUnit.SECONDS));
			Job job = deferredUpdate.get();
			assertNotNull(job);
			assertNotNull(job.getResult());
			assertTrue("Deferred PDE classpath update failed: " + job.getResult(), job.getResult().isOK());
			assertNotNull("Deferred update must have a scheduling rule", job.getRule());
			assertTrue("Deferred update must not run concurrently with m2e configuration",
					job.getRule().isConflicting(MavenPlugin.getProjectConfigurationManager().getRule()));

			waitForJobsToComplete(monitor);
			assertClasspathContainer(project);
			IClasspathEntry[] entries = JavaCore.create(project).getRawClasspath();
			assertTrue("PDE container missing after deferred update", Arrays.stream(entries)
					.anyMatch(e -> e.getEntryKind() == IClasspathEntry.CPE_CONTAINER
							&& e.getPath().toString().equals("org.eclipse.pde.core.requiredPlugins")));
			assertTrue("Plug-in source folder missing after deferred update", Arrays.stream(entries)
					.anyMatch(e -> e.getEntryKind() == IClasspathEntry.CPE_SOURCE
							&& e.getPath().equals(project.getFullPath().append("src"))));
			assertFalse("JDT's default project-root source folder must not remain", Arrays.stream(entries)
					.anyMatch(e -> e.getEntryKind() == IClasspathEntry.CPE_SOURCE
							&& e.getPath().equals(project.getFullPath())));
		} finally {
			Job.getJobManager().removeJobChangeListener(listener);
		}
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

		// Build project to run the DS annotation processor, which also updates the
		// project build commends
		CountDownLatch projectFileUpdated = new CountDownLatch(1);
		IPath projectFile = IPath.fromOSString("pde.tycho.plugin.with.ds/.project");
		IResourceChangeListener listener = event -> {
			if (event.getDelta().findMember(projectFile) != null) {
				projectFileUpdated.countDown();
			}
		};
		project.getWorkspace().addResourceChangeListener(listener);
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		for (int i = 0; !projectFileUpdated.await(200, TimeUnit.MILLISECONDS) && i < 5; i++) {
			// For an unknown reason the
			// org.eclipse.pde.ds.internal.annotations.AnnotationProcessor does not always
			// participate on the first build.
			System.out.println("importTychoPluginWithDS rebuild: " + i);
			project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		}
		project.getWorkspace().removeResourceChangeListener(listener);
		
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
		var actualBuilders = Arrays.stream(project.getDescription().getBuildSpec()).map(ICommand::getBuilderName);
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
