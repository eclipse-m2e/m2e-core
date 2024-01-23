/*************************************************************************************
 * Copyright (c) 2012-2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc - Initial implementation.
 ************************************************************************************/
package org.eclipse.m2e.apt.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer.FactoryType;
import org.eclipse.jdt.apt.core.internal.util.FactoryPath;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.apt.MavenJdtAptPlugin;
import org.eclipse.m2e.apt.internal.Messages;
import org.eclipse.m2e.apt.preferences.AnnotationProcessingMode;
import org.eclipse.m2e.apt.preferences.IPreferencesManager;
import org.eclipse.osgi.util.NLS;
import org.junit.Test;

@SuppressWarnings("restriction")
public class M2eAptProjectconfiguratorTest extends AbstractM2eAptProjectConfiguratorTestCase {

	static {
		@SuppressWarnings("unused")
		org.eclipse.jdt.internal.apt.pluggable.core.Apt6Plugin apt;
		// Providing plug-in is required. Keep a reference to a class to not
		// accidentally remove it in future clean-ups.
	}

	@Test
	public void testMavenCompilerPluginSupport() throws Exception {
		// Note: this is the old default, in new plugin versions it is
		// "target/generated-test-sources/test-annotations"
		defaultTest("p1", COMPILER_OUTPUT_DIR, "target/generated-sources/test-annotations");
	}

	@Test
	public void testMavenCompilerPluginSupportWithTestClasspathDisabled() throws Exception {
		// Note: this is the old default, in new plugin versions it is
		// "target/generated-test-sources/test-annotations"
		defaultTest("p1_test_classpath_disabled", COMPILER_OUTPUT_DIR, "target/generated-sources/test-annotations",
				false);
	}

	@Test
	public void testMavenCompilerPluginDependencies() throws Exception {
		defaultTest("p2", "target/generated-sources/m2e-apt", "target/generated-test-sources/m2e-apt");
	}

	@Test
	public void testMavenProcessorPluginSupport() throws Exception {
		defaultTest("p3", PROCESSOR_OUTPUT_DIR, "target/generated-sources/apt-test");
	}

	@Test
	public void testDisabledAnnotationProcessing() throws Exception {
		testDisabledAnnotationProcessing("p4");// using <compilerArgument>-proc:none</compilerArgument>
		testDisabledAnnotationProcessing("p5");// using <proc>none</proc>
	}

	@Test
	public void testAnnotationProcessorArguments() throws Exception {
		Map<String, String> expectedOptions = new HashMap<>(2);
		expectedOptions.put("addGenerationDate", "true");
		expectedOptions.put("addGeneratedAnnotation", "true");
		testAnnotationProcessorArguments("p6", expectedOptions);
		testAnnotationProcessorArguments("p7", expectedOptions);
	}

	@Test
	public void testAnnotationProcessorArgumentsMap() throws Exception {
		Map<String, String> expectedOptions = new HashMap<>(2);
		expectedOptions.put("addGenerationDate", "true");
		// this option is false in <compilerArguments> but is overriden by
		// <compilerArgument>
		expectedOptions.put("addGeneratedAnnotation", "true");
		expectedOptions.put("flag", null);
		IProject p = testAnnotationProcessorArguments("argumentMap", expectedOptions);
		List<IMarker> errors = findErrorMarkers(p);
		assertEquals(1, errors.size());
		String expectedMsg = NLS.bind(Messages.ProjectUtils_error_invalid_option_name, "-foo");
		assertEquals(expectedMsg, errors.get(0).getAttribute(IMarker.MESSAGE));
	}

	@Test
	public void testNoAnnotationProcessor() throws Exception {
		IProject p = importProject("projects/p0/pom.xml");
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);

		assertFalse("Annotation processing is enabled for " + p, AptConfig.isEnabled(javaProject));
		String expectedOutputFolder = COMPILER_OUTPUT_DIR;
		IFolder annotationsFolder = p.getFolder(expectedOutputFolder);
		assertFalse(annotationsFolder + " was generated", annotationsFolder.exists());
	}

	@Test
	public void testRuntimePluginDependency() throws Exception {

		IProject p = importProject("projects/autovalue/pom.xml");
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);

		assertTrue("Annotation processing is disabled for " + p, AptConfig.isEnabled(javaProject));
		String expectedOutputFolder = COMPILER_OUTPUT_DIR;
		IFolder annotationsFolder = p.getFolder(expectedOutputFolder);
		assertTrue(annotationsFolder + " was not generated", annotationsFolder.exists());

		FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
		assertEquals(2, factoryPath.getEnabledContainers().size());
		assertFactoryContainerContains(factoryPath, "auto-value:1.6.2");

		IFile generatedFile = p.getFile(expectedOutputFolder + "/foo/bar/AutoValue_Dummy.java");

		assertTrue(generatedFile + " was not generated", generatedFile.exists());
		assertNoErrors(p);
	}

	@Test
	public void testDisableAnnotationProcessingFromWorkspace() throws Exception {
		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getPreferencesManager();
		try {
			preferencesManager.setAnnotationProcessorMode(null, AnnotationProcessingMode.disabled);
			IProject p = importProject("projects/p1/pom.xml");
			waitForJobsToComplete();
			IJavaProject javaProject = JavaCore.create(p);
			assertFalse("JDT APT support was enabled", AptConfig.isEnabled(javaProject));

			IFolder annotationsFolder = p.getFolder(COMPILER_OUTPUT_DIR);
			assertFalse(annotationsFolder + " was generated", annotationsFolder.exists());

		} finally {
			preferencesManager.setAnnotationProcessorMode(null, AnnotationProcessingMode.jdt_apt);
		}
	}

	@Test
	public void testDisableAnnotationProcessingFromProject() throws Exception {
		IProject p = importProject("projects/p1/pom.xml");
		waitForJobsToComplete();
		IJavaProject javaProject = JavaCore.create(p);
		assertTrue("JDT APT support was not enabled", AptConfig.isEnabled(javaProject));

		// Manually disable APT support
		AptConfig.setEnabled(javaProject, false);

		// Disable m2e-apt on the project
		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getPreferencesManager();
		preferencesManager.setAnnotationProcessorMode(p, AnnotationProcessingMode.disabled);

		// Update Maven Configuration
		updateProject(p);

		// Check APT support is still disabled
		assertFalse("JDT APT support was enabled", AptConfig.isEnabled(javaProject));

	}

	@Test
	public void testDisableProcessDuringReconcileFromWorkspace() throws Exception {
		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getPreferencesManager();

		preferencesManager.setAnnotationProcessDuringReconcile(null, false);
		IProject p = importProject("projects/p1/pom.xml");
		waitForJobsToComplete();
		IJavaProject javaProject = JavaCore.create(p);
		assertFalse("JDT APT Processing on Edit was enabled", AptConfig.shouldProcessDuringReconcile(javaProject));

	}

	@Test
	public void testDisableProcessDuringReconcileFromProject() throws Exception {
		IProject p = importProject("projects/p1/pom.xml");
		waitForJobsToComplete();
		IJavaProject javaProject = JavaCore.create(p);
		assertTrue("JDT APT Processing on Edit was not enabled", AptConfig.shouldProcessDuringReconcile(javaProject));

		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getPreferencesManager();
		preferencesManager.setAnnotationProcessDuringReconcile(p, false);

		// Update Maven Configuration
		updateProject(p);

		// Check APT process on edit is still disabled
		assertFalse("JDT APT Processing on Edit was enabled", AptConfig.shouldProcessDuringReconcile(javaProject));
	}

	@Test
	public void testMavenPropertyProcessDuringReconcileSupport() throws Exception {
		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getPreferencesManager();
		preferencesManager.setAnnotationProcessDuringReconcile(null, true);
		IProject p = importProject("projects/p10/pom.xml");
		waitForJobsToComplete();
		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);
		assertFalse(AptConfig.shouldProcessDuringReconcile(javaProject));

		preferencesManager.setAnnotationProcessDuringReconcile(p, true);
		updateProject(p);

		// Check Eclipse Project settings override pom property
		assertTrue("JDT APT Processing on Edit disabled for " + p, AptConfig.shouldProcessDuringReconcile(javaProject));
	}

	@Test
	public void testPluginExecutionDelegation() throws Exception {
		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getPreferencesManager();
		try {
			preferencesManager.setAnnotationProcessorMode(null, AnnotationProcessingMode.maven_execution);
			IProject p = importProject("projects/p3/pom.xml");
			waitForJobsToComplete();

			IJavaProject javaProject = JavaCore.create(p);
			assertFalse("JDT APT support was enabled", AptConfig.isEnabled(javaProject));

			IFolder annotationsFolder = p.getFolder(PROCESSOR_OUTPUT_DIR);
			assertTrue(annotationsFolder + " was not generated", annotationsFolder.exists());

			IFolder testAnnotationsFolder = p.getFolder("target/generated-sources/apt-test");
			assertTrue(testAnnotationsFolder + " was not generated", testAnnotationsFolder.exists());

		} finally {
			preferencesManager.setAnnotationProcessorMode(null, AnnotationProcessingMode.jdt_apt);
		}
	}

	private void testDisabledAnnotationProcessing(String projectName) throws Exception {
		IProject p = importProject("projects/" + projectName + "/pom.xml");
		waitForJobsToComplete();
		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);
		assertFalse(AptConfig.isEnabled(javaProject));
	}

	private IProject testAnnotationProcessorArguments(String projectName, Map<String, String> expectedOptions)
			throws Exception {
		IProject p = importProject("projects/" + projectName + "/pom.xml");
		waitForJobsToComplete();
		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);
		assertTrue("Annotation processing is disabled for " + projectName, AptConfig.isEnabled(javaProject));
		Map<String, String> options = AptConfig.getRawProcessorOptions(javaProject);
		for (Map.Entry<String, String> option : expectedOptions.entrySet()) {
			assertEquals("Unexpected value for " + option.getKey(), option.getValue(), options.get(option.getKey()));
			if (option.getValue() == null) {
				assertTrue(option.getKey() + " is missing ", options.containsKey(option.getKey()));
			}
		}
		return p;
	}

	@Test
	public void testMavenPropertySupport1() throws Exception {
		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getPreferencesManager();
		preferencesManager.setAnnotationProcessorMode(null, AnnotationProcessingMode.disabled);
		// Check pom property overrides Workspace settings
		defaultTest("p8", PROCESSOR_OUTPUT_DIR, null);
	}

	@Test
	public void testMavenPropertySupport2() throws Exception {
		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getPreferencesManager();
		preferencesManager.setAnnotationProcessorMode(null, AnnotationProcessingMode.jdt_apt);
		IProject p = importProject("projects/p9/pom.xml");
		waitForJobsToComplete();
		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);
		assertFalse(AptConfig.isEnabled(javaProject));

		preferencesManager.setAnnotationProcessorMode(p, AnnotationProcessingMode.jdt_apt);
		updateProject(p);

		// Check Eclipse Project settings override pom property
		assertTrue("Annotation processing is disabled for " + p, AptConfig.isEnabled(javaProject));
		IFolder annotationsFolder = p.getFolder(PROCESSOR_OUTPUT_DIR);
		assertTrue(annotationsFolder + " was not generated", annotationsFolder.exists());
	}

	@Test
	public void testCompilerArgs() throws Exception {
		Map<String, String> expectedOptions = new HashMap<>(3);
		// this option is false in <compilerArguments>, overriden by <compilerArgument>
		// and <compilerArgs>
		expectedOptions.put("addGenerationDate", "true");
		expectedOptions.put("addGeneratedAnnotation", "true");
		expectedOptions.put("compilerArg", null);
		expectedOptions.put("foo", "bar");
		testAnnotationProcessorArguments("compilerArgs", expectedOptions);
	}

	@Test
	public void testNullCompilerArgs() throws Exception {
		Map<String, String> expectedOptions = new HashMap<>(2);
		expectedOptions.put("addGeneratedAnnotation", "true");
		expectedOptions.put("compilerArg", null);
		testAnnotationProcessorArguments("nullCompilerArgs", expectedOptions);
	}

	@Test
	public void testAnnotationProcessorsPaths() throws Exception {
		IProject p = importProject("projects/p11/pom.xml");
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);

		assertTrue("Annotation processing is disabled for " + p, AptConfig.isEnabled(javaProject));
		IFile generatedFile = p.getFile("target/generated-sources/annotations/foo/bar/Dummy_.java");
		assertTrue(generatedFile + " was not generated", generatedFile.exists());
		assertNoErrors(p);

		FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
		assertEquals(9, factoryPath.getEnabledContainers().size());
		assertFactoryContainerContains(factoryPath, "hibernate-jpamodelgen:" + JPA_MODELGEN_VERSION);
		assertFactoryContainerContains(factoryPath, "jboss-logging:3.4.3.Final");
	}

	@Test
	public void testDeleteStaleClasspathEntries() throws Exception {
		String expectedOutputFolder = PROCESSOR_OUTPUT_DIR;
		IProject p = importProject("projects/p12/pom.xml");
		waitForJobsToComplete();

		IFile generatedFile = p.getFile(expectedOutputFolder + "/foo/bar/Dummy_.java");
		assertTrue(generatedFile + " was not generated", generatedFile.exists());

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);
		assertClasspathEntry(javaProject, PROCESSOR_OUTPUT_DIR, true);
		assertClasspathEntry(javaProject, COMPILER_OUTPUT_DIR, false);

		updateProject(p, "new-pom.xml");
		waitForJobsToComplete();

		assertNoErrors(p);

		expectedOutputFolder = COMPILER_OUTPUT_DIR;
		generatedFile = p.getFile(expectedOutputFolder + "/foo/bar/Dummy_.java");
		assertTrue(generatedFile + " was not generated", generatedFile.exists());

		javaProject = JavaCore.create(p);
		assertClasspathEntry(javaProject, PROCESSOR_OUTPUT_DIR, false);
		assertClasspathEntry(javaProject, COMPILER_OUTPUT_DIR, true);
	}

	@Test
	public void testNonJarDependency() throws Exception {
		IProject p = importProject("projects/nonjar_plugin_deps/pom.xml");
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);

		FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
		assertTrue(factoryPath.getEnabledContainers().size() > 2);
		assertFactoryContainerContains(factoryPath, "hibernate-jpamodelgen:" + JPA_MODELGEN_VERSION);
		assertFactoryContainerContains(factoryPath, "maven-plugin-api:2.0.9");
		assertFactoryContainerContains(factoryPath, "hibernate-jpa-2.0-api:1.0.0.Final");

		IFile generatedFile = p.getFile(COMPILER_OUTPUT_DIR + "/foo/bar/Dummy_.java");
		assertTrue(generatedFile + " was not generated", generatedFile.exists());
		assertNoErrors(p);
	}

	@Test
	public void testJDTCompilerPluginSupport() throws Exception {
		IProject p = importProject("projects/p13/pom.xml");
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);

		assertTrue("Annotation processing is disabled for " + p, AptConfig.isEnabled(javaProject));

		FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
		assertFactoryContainerContains(factoryPath, "hibernate-jpamodelgen:" + JPA_MODELGEN_VERSION);

		// project won't actually compile unless
		// https://github.com/jbosstools/m2e-jdt-compiler is available
	}

	@Test
	public void testJavacWithErrorproneCompilerPluginSupport() throws Exception {
		IProject p = importProject("projects/p14/pom.xml");
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);

		assertTrue("Annotation processing is disabled for " + p, AptConfig.isEnabled(javaProject));

		FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
		assertFactoryContainerContains(factoryPath, "hibernate-jpamodelgen:" + JPA_MODELGEN_VERSION);
	}

	@Test
	public void testAnnotationPluginsDisabled() throws Exception {
		IProject p = importProject("projects/p13/pom.xml");
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);

		FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
		for (FactoryContainer fc : factoryPath.getEnabledContainers().keySet()) {
			if (FactoryType.PLUGIN.equals(fc.getType())) {
				fail(fc.getId() + " should not be enabled");
			}
		}
	}

	@Test
	public void testDependencyManagement() throws Exception {
		IProject p = importProject("projects/p15/pom.xml");
		waitForJobsToComplete();
		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);
		assertNoErrors(p);
	}
}
