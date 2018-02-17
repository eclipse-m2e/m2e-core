/*************************************************************************************
 * Copyright (c) 2012-2018 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.maven.apt.tests;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer;
import org.eclipse.jdt.apt.core.internal.util.FactoryPath;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.jboss.tools.maven.apt.MavenJdtAptPlugin;
import org.jboss.tools.maven.apt.preferences.AnnotationProcessingMode;
import org.jboss.tools.maven.apt.preferences.IPreferencesManager;

@SuppressWarnings("restriction")
abstract class AbstractM2eAptProjectConfiguratorTestCase extends AbstractMavenProjectTestCase {
	static final String COMPILER_OUTPUT_DIR = "target/generated-sources/annotations";
	static final String PROCESSOR_OUTPUT_DIR = "target/generated-sources/apt";
	
	public void setUp() throws Exception {
		super.setUp();
		IPreferencesManager preferencesManager = MavenJdtAptPlugin.getDefault().getPreferencesManager();
		preferencesManager.setAnnotationProcessorMode(null, AnnotationProcessingMode.jdt_apt);
		setAutoBuilding(true);
	}


	protected List<FactoryContainer> getFactoryContainers(IJavaProject project) {
		FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(project);
		String excludedId = "org.eclipse.jst.ws.annotations.core";
        Stream<FactoryContainer> stream = factoryPath.getEnabledContainers().keySet().stream();
        return stream.filter(fc -> !excludedId.equals(fc.getId()))
        		.collect(Collectors.toList());
	}
	
	protected boolean contains(Collection<FactoryContainer> containers, String id) {
        Stream<FactoryContainer> stream = containers.stream();
        return stream.filter(fc -> id.equals(fc.getId())).findAny().isPresent();
	}
	
	protected void defaultTest(String projectName, String expectedOutputFolder, String expectedTestOutputFolder) throws Exception {
		try {
			AptConfig.class.getMethod("setGenTestSrcDir", IJavaProject.class, String.class);
		} catch (NoSuchMethodException | SecurityException ex) {
			expectedTestOutputFolder = null;
		}

		IProject p = importProject("projects/"+projectName+"/pom.xml");
		waitForJobsToComplete();

		p.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);

		assertTrue("Annotation processing is disabled for "+p, AptConfig.isEnabled(javaProject));
        IFolder annotationsFolder = p.getFolder(expectedOutputFolder);
        assertTrue(annotationsFolder  + " was not generated", annotationsFolder.exists());

        if (expectedTestOutputFolder != null) {
			IFolder testAnnotationsFolder = p.getFolder(expectedTestOutputFolder);
			assertTrue(testAnnotationsFolder + " was not generated", testAnnotationsFolder.exists());
		}

        FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
        Iterator<FactoryContainer> ite = factoryPath.getEnabledContainers().keySet().iterator();
        FactoryContainer jpaModelGen = ite.next();
        assertEquals(FactoryContainer.FactoryType.VARJAR, jpaModelGen.getType());
        assertEquals("M2_REPO/org/hibernate/hibernate-jpamodelgen/1.1.1.Final/hibernate-jpamodelgen-1.1.1.Final.jar", jpaModelGen.getId());

        FactoryContainer jpaApi = ite.next();
        assertEquals(FactoryContainer.FactoryType.VARJAR, jpaApi.getType());
        assertEquals("M2_REPO/org/hibernate/javax/persistence/hibernate-jpa-2.0-api/1.0.0.Final/hibernate-jpa-2.0-api-1.0.0.Final.jar", jpaApi.getId());

        IFile generatedFile = p.getFile(expectedOutputFolder + "/foo/bar/Dummy_.java");
		assertTrue(generatedFile + " was not generated", generatedFile.exists());

		if(expectedTestOutputFolder != null) {
			IFile generatedTestFile = p.getFile(expectedTestOutputFolder + "/foo/bar/TestDummy_.java");
			assertTrue(generatedTestFile + " was not generated", generatedTestFile.exists());
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
	    ResolverConfiguration configuration = new ResolverConfiguration();
	    configurationManager.enableMavenNature(project, configuration, monitor);
	    configurationManager.updateProjectConfiguration(project, monitor);
	    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
	    waitForJobsToComplete();
	  }

	protected void assertClasspathEntry(IJavaProject jp, String path, boolean present) throws Exception {
		IPath expectedPath = new Path(path);
		for (IClasspathEntry cpe : jp.getRawClasspath()) {
			if (expectedPath.equals(cpe.getPath())) {
				if (present) {
					return;
				} 
				throw new AssertionFailedException("Unexpected "+path+ " was found in the Classpath");
			}
		}
	}
}
