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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer;
import org.eclipse.jdt.apt.core.internal.util.FactoryPath;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.apt.MavenJdtAptPlugin;
import org.eclipse.m2e.apt.preferences.AnnotationProcessingMode;
import org.eclipse.m2e.apt.preferences.IPreferencesManager;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IProjectCreationListener;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Before;

@SuppressWarnings("restriction")
abstract class AbstractM2eAptProjectConfiguratorTestCase extends AbstractMavenProjectTestCase {
	static final String COMPILER_OUTPUT_DIR = "target/generated-sources/annotations";
	static final String PROCESSOR_OUTPUT_DIR = "target/generated-sources/apt";
	static final String JPA_MODELGEN_VERSION = "5.6.4.Final";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getPreferencesManager();
		preferencesManager.setAnnotationProcessorMode(null, AnnotationProcessingMode.jdt_apt);
		setAutoBuilding(true);
	}

	protected boolean contains(Collection<FactoryContainer> containers, String id) {
		Stream<FactoryContainer> stream = containers.stream();
		return stream.filter(fc -> id.equals(fc.getId())).findAny().isPresent();
	}

	protected void defaultTest(String projectName, String expectedOutputFolder, String expectedTestOutputFolder)
			throws Exception {
		defaultTest(projectName, expectedOutputFolder, expectedTestOutputFolder, true);
	}

	protected void defaultTest(String projectName, String expectedOutputFolder, String expectedTestOutputFolder,
			boolean expectTestAttribute) throws Exception {

		IProject p = importProject("projects/" + projectName + "/pom.xml");
		waitForJobsToComplete();

		p.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);

		assertTrue("Annotation processing is disabled for " + p, AptConfig.isEnabled(javaProject));
		IFolder annotationsFolder = p.getFolder(expectedOutputFolder);
		assertTrue(annotationsFolder + " was not generated", annotationsFolder.exists());

		if (expectedTestOutputFolder != null && expectTestAttribute) {
			IFolder testAnnotationsFolder = p.getFolder(expectedTestOutputFolder);
			assertTrue(testAnnotationsFolder + " was not generated", testAnnotationsFolder.exists());
		}

		FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
		assertFactoryContainerContains(factoryPath, "hibernate-jpamodelgen:" + JPA_MODELGEN_VERSION);
		assertFactoryContainerContains(factoryPath, "hibernate-jpa-2.0-api:1.0.0.Final");

		IFile generatedFile = p.getFile(expectedOutputFolder + "/foo/bar/Dummy_.java");
		assertTrue(generatedFile + " was not generated", generatedFile.exists());

		if (expectedTestOutputFolder != null) {
			IFile generatedTestFile = p.getFile((expectTestAttribute ? expectedTestOutputFolder : expectedOutputFolder)
					+ "/foo/bar/TestDummy_.java");
			assertTrue(generatedTestFile + " was not generated", generatedTestFile.exists());

			boolean testAttributeFound = false;
			entryLoop: for (IClasspathEntry entry : javaProject.getRawClasspath()) {
				for (IClasspathAttribute attribute : entry.getExtraAttributes()) {
					if ("test".equals(attribute.getName()) && "true".equals(attribute.getValue())) {
						testAttributeFound = true;
						break entryLoop;
					}
				}
			}
			if (expectTestAttribute) {
				assertTrue("test attribute not found, but expected", testAttributeFound);
			} else {
				assertFalse("test attribute found, but not expected", testAttributeFound);
			}
		}

		assertNoErrors(p);
	}

	protected void updateProject(IProject project) throws Exception {
		updateProject(project, null);
	}

	protected void updateProject(IProject project, String newPomName) throws Exception {

		if (newPomName != null) {
			copyContent(project, newPomName, "pom.xml");
		}

		IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
		IProjectConfiguration configuration = new ResolverConfiguration();
		configurationManager.enableMavenNature(project, configuration, monitor);
		configurationManager.updateProjectConfiguration(project, monitor);
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		waitForJobsToComplete();
	}

	protected void assertClasspathEntry(IJavaProject jp, String path, boolean present) throws Exception {
		IPath expectedPath = IPath.fromOSString(path);
		for (IClasspathEntry cpe : jp.getRawClasspath()) {
			if (expectedPath.equals(cpe.getPath())) {
				if (present) {
					return;
				}
				throw new AssertionFailedException("Unexpected " + path + " was found in the Classpath");
			}
		}

	}

	@Override
	protected IProject[] importProjects(String basedir, String[] pomNames, ResolverConfiguration configuration,
			boolean skipSanityCheck, IProjectCreationListener listener) throws IOException, CoreException {
		// Avoid project import freezing because of
		// https://github.com/eclipse-mirrors/org.eclipse.jdt.core/commit/298e47435e19b1217e5593f3c29652720857f6e1#diff-35069a9c6c893e0c29b0b97b820cff43e828fda949767f7b00a64a4b869aacafR504-R512
		try {
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, monitor);
		} catch (OperationCanceledException | InterruptedException e) {
			// ignore
		}

		return super.importProjects(basedir, pomNames, configuration, skipSanityCheck, listener);
	}

	protected void assertFactoryContainerContains(FactoryPath factoryPath, String artifactIdVersion) {
		String[] av = artifactIdVersion.split(":");
		String targetArtifact = av[0] + "-" + av[1] + ".jar";
		Supplier<Stream<String>> ids = () -> factoryPath.getEnabledContainers().keySet().stream().map(fc -> fc.getId());
		boolean found = ids.get().filter(id -> id.endsWith(targetArtifact)).findFirst().isPresent();

		if (!found) {
			fail(targetArtifact + " is missing from " + ids.get().collect(Collectors.joining(",")));
		}

	}

}
