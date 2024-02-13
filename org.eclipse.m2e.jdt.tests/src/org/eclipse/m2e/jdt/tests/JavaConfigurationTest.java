/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.internal.ClasspathDescriptor;
import org.eclipse.m2e.jdt.internal.MavenClasspathHelpers;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class JavaConfigurationTest extends AbstractMavenProjectTestCase {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
		setAutoBuilding(true);
	}

	@Test
	public void testFileChangeUpdatesJDTSettings() throws CoreException, IOException, InterruptedException {
		IJavaProject project = importResourceProject("/projects/compilerSettings/pom.xml");

		// set external annotation path to JRE and Maven classpaths
		var classpath = new ClasspathDescriptor(project);
		var containerPath = classpath.getEntryDescriptors().stream().filter(e -> JavaRuntime.JRE_CONTAINER.equals(e.getPath().segment(0)))
			.findFirst().get().getPath();
		classpath.addEntry(MavenClasspathHelpers.newContainerEntry(containerPath, JavaCore.newClasspathAttribute(
			IClasspathAttribute.EXTERNAL_ANNOTATION_PATH, "/jre_external_anno_path")));
		classpath.addEntry(MavenClasspathHelpers.getDefaultContainerEntry(
			JavaCore.newClasspathAttribute(IClasspathAttribute.EXTERNAL_ANNOTATION_PATH, "/maven_external_anno_path")));
		project.setRawClasspath(classpath.getEntries(), project.getOutputLocation(), monitor);

		assertEquals("1.8", project.getOption(JavaCore.COMPILER_SOURCE, false));
		IFile pomFileWS = project.getProject().getFile("pom.xml");
		String pomContent = Files.readString(Path.of(pomFileWS.getLocationURI()));
		pomContent = pomContent.replace("1.8", "11");
		pomFileWS.setContents(new ByteArrayInputStream(pomContent.getBytes()), true, false, null);
		waitForJobsToComplete();
		assertEquals("11", project.getOption(JavaCore.COMPILER_SOURCE, false));

		// ensure external annotation paths are still present after update
		classpath = new ClasspathDescriptor(project);
		var jreCpe = classpath.getEntryDescriptors().stream().filter(e -> JavaRuntime.JRE_CONTAINER.equals(e.getPath().segment(0)))
			.findFirst().get();
		assertEquals("/jre_external_anno_path", jreCpe.getClasspathAttributes().get(IClasspathAttribute.EXTERNAL_ANNOTATION_PATH));
		var mavenCpePath = IPath.fromOSString(IClasspathManager.CONTAINER_ID);
		var mavenCpe = classpath.getEntryDescriptors().stream().filter(e -> mavenCpePath.equals(e.getPath())).findFirst().get();
		assertEquals("/maven_external_anno_path", mavenCpe.getClasspathAttributes().get(IClasspathAttribute.EXTERNAL_ANNOTATION_PATH));
	}

	@Test
	public void testComplianceVsReleaseSettings() throws CoreException, IOException, InterruptedException {
		IJavaProject project = importResourceProject("/projects/compilerReleaseSettings/pom.xml");
		assertEquals("1.8", project.getOption(JavaCore.COMPILER_SOURCE, false));
		assertEquals("1.8", project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, false));
		assertEquals("1.8", project.getOption(JavaCore.COMPILER_COMPLIANCE, false));
		assertEquals(JavaCore.ENABLED, project.getOption(JavaCore.COMPILER_RELEASE, false));
		IFile pomFileWS = project.getProject().getFile("pom.xml");
		String pomContent = Files.readString(Path.of(pomFileWS.getLocationURI()));
		pomContent = pomContent.replace(">8<", ">11<");
		pomFileWS.setContents(new ByteArrayInputStream(pomContent.getBytes()), true, false, null);
		waitForJobsToComplete();
		assertEquals("11", project.getOption(JavaCore.COMPILER_SOURCE, false));
		assertEquals("11", project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, false));
		assertEquals("11", project.getOption(JavaCore.COMPILER_COMPLIANCE, false));
		assertEquals(JavaCore.ENABLED, project.getOption(JavaCore.COMPILER_RELEASE, false));
	}

	@Test
	public void testJDTWarnings() throws CoreException, IOException, InterruptedException {
		IJavaProject project = importResourceProject("/projects/compilerWarnings/pom.xml");
		IFile file = project.getProject().getFile("src/main/java/A.java");
		project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		waitForJobsToComplete();
		IMarker[] findMarkers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
		assertArrayEquals(new IMarker[0], findMarkers);
	}

	@Test
	public void testSkipAllTest() throws CoreException, IOException, InterruptedException {
		IJavaProject project = importResourceProject("/projects/skipAllTest/pom.xml");
		assertTrue(Arrays.stream(project.getRawClasspath()).noneMatch(IClasspathEntry::isTest));
	}

	@Test
	public void testSkipTestCompilation() throws CoreException, IOException, InterruptedException {
		IJavaProject project = importResourceProject("/projects/skipTestCompilation/pom.xml");
		assertEquals(0, classpathEntriesCount(project, TEST_SOURCES));
		assertEquals(1, classpathEntriesCount(project, TEST_RESOURCES));
	}

	@Test
	public void testSkipTestResources() throws CoreException, IOException, InterruptedException {
		IJavaProject project = importResourceProject("/projects/skipTestResources/pom.xml");
		assertEquals(1, classpathEntriesCount(project, TEST_SOURCES));
		assertEquals(0, classpathEntriesCount(project, TEST_RESOURCES));
	}

	@Test
	public void testSkipOnlyOneOfMultipleExecutions() throws CoreException, IOException, InterruptedException {
		IJavaProject project = importResourceProject("/projects/skipOnlyOneOfMultipleExecutions/pom.xml");
		assertEquals(1, classpathEntriesCount(project, TEST_SOURCES));
		assertEquals(1, classpathEntriesCount(project, TEST_RESOURCES));
	}

	@Test
	public void testSkipNone() throws CoreException, IOException, InterruptedException {
		IJavaProject project = importResourceProject("/projects/skipNone/pom.xml");
		assertEquals(1, classpathEntriesCount(project, TEST_SOURCES));
		assertEquals(1, classpathEntriesCount(project, TEST_RESOURCES));
	}

	@Test
	public void testComplianceVsEnablePreviewSettings() throws CoreException, IOException, InterruptedException {
		IJavaProject project = importResourceProject("/projects/compilerEnablePreviewSettings/pom.xml");
		assertEquals("11", project.getOption(JavaCore.COMPILER_COMPLIANCE, false));
		assertEquals(JavaCore.ENABLED, project.getOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, false));
		assertEquals(JavaCore.IGNORE, project.getOption(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, false));
	}

	@Test
	public void testAddSourceResource() throws CoreException, IOException, InterruptedException {
		File baseDir = new File(FileLocator
				.toFileURL(JavaConfigurationTest.class.getResource("/projects/add-source-resource/submoduleA/pom.xml"))
				.getFile()).getParentFile().getParentFile();
		waitForJobsToComplete();
		IProject project = importProjects(baseDir.getAbsolutePath(), new String[] { "submoduleA/pom.xml" },
				new ResolverConfiguration())[0];
		waitForJobsToComplete();
		IJavaProject javaProject = JavaCore.create(project);

		List<String> srcEntryPaths = Arrays.stream(javaProject.getRawClasspath())
				.filter(cp -> IClasspathEntry.CPE_SOURCE == cp.getEntryKind()).filter(cp -> !cp.isTest())
				.map(IClasspathEntry::getPath).map(IPath::toString).toList();
		assertEquals(Set.of("/submoduleA/src/main/java", "/submoduleA/src/main/resources", //
				"/submoduleA/.._parent_src_main_java", "/submoduleA/.._parent_src_main_resources"),
				Set.copyOf(srcEntryPaths));
		List<String> testEntryPaths = Arrays.stream(javaProject.getRawClasspath())
				.filter(cp -> IClasspathEntry.CPE_SOURCE == cp.getEntryKind()).filter(cp -> cp.isTest())
				.map(IClasspathEntry::getPath).map(IPath::toString).toList();
		assertEquals(Set.of("/submoduleA/src/test/java", "/submoduleA/src/test/resources", //
				"/submoduleA/.._parent_src_test_java", "/submoduleA/.._parent_src_test_resources"),
				Set.copyOf(testEntryPaths));
	}

	// --- utility methods ---

	private static final Predicate<IClasspathEntry> TEST_SOURCES = cp -> cp.isTest()
			&& cp.getPath().toString().contains("test/java");
	private static final Predicate<IClasspathEntry> TEST_RESOURCES = cp -> cp.isTest()
			&& cp.getPath().toString().contains("test/resources");

	private long classpathEntriesCount(IJavaProject project, Predicate<IClasspathEntry> testSources)
			throws JavaModelException {
		return Arrays.stream(project.getRawClasspath()).filter(testSources).count();
	}

	private IJavaProject importResourceProject(String name) throws IOException, InterruptedException, CoreException {
		File pomFile = new File(FileLocator.toFileURL(JavaConfigurationTest.class.getResource(name)).getFile());
		waitForJobsToComplete();
		IProject project = importProject(pomFile.getAbsolutePath());
		waitForJobsToComplete();
		return JavaCore.create(project);
	}
}
