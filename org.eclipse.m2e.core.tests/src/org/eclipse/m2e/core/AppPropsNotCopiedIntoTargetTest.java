/*******************************************************************************
 * Copyright (c) 2023 Alex Boyko and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AppPropsNotCopiedIntoTargetTest extends AbstractMavenProjectTestCase {
	
	private File projectDirectory;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	    setAutoBuilding(true);
		projectDirectory = new File(Files.createTempDirectory("m2e-tests").toFile(), "demo");
		projectDirectory.mkdirs();
		copyDir(new File("resources/projects/demo"), projectDirectory);
	}

	@Override
	@After
	public void tearDown() throws Exception {
		FileUtils.deleteDirectory(this.projectDirectory.getParentFile());
		super.tearDown();
	}

	@Test
	public void test() throws Exception {
		IProject project = importProject(new File(projectDirectory, "pom.xml").toString());
		waitForJobsToComplete(monitor);

		IJavaProject jp = JavaCore.create(project);

		assertTrue("Build Automatically is NOT on!", isAutoBuilding());

		IPath rootPath = project.getWorkspace().getRoot().getLocation();
		
		// Project imported for the first time and built - everything is properly in the target folder
		assertTrue("'application.properties' is NOT in the output folder", rootPath.append(jp.getOutputLocation()).append("/application.properties").toFile()
				.exists());
		assertTrue("'DemoApplication.class' is NOT in the output folder", rootPath.append(jp.getOutputLocation())
				.append("/com/example/demo/DemoApplication.class").makeAbsolute().toFile().exists());

		IFile pomXml = project.getFile("pom.xml");
		String content = Files.readString(Path.of(pomXml.getLocationURI()));
		pomXml.setContents(new ByteArrayInputStream("Nothing".getBytes()), true, false, null);

		waitForJobsToComplete(monitor);
		Thread.sleep(5000);
		// Invalid pom file. JavaBuilder built java sources but MavenBuilder wqs unable to do anything hence no resources in the target folder except for compiled classes
		assertFalse("'application.properties' hasn't been removed from the output folder", rootPath.append(jp.getOutputLocation()).append("/application.properties").toFile()
				.exists());
		assertTrue("'DemoApplication.class' should be created in the output folder by JavaBuilder", rootPath.append(jp.getOutputLocation())
				.append("/com/example/demo/DemoApplication.class").toFile().exists());

		pomXml.setContents(new ByteArrayInputStream(content.getBytes()), true, false, null);
		waitForJobsToComplete(monitor);
		Thread.sleep(5000);
		// Valid pom file. Compiled classes and resources should reappear in the target folder. However, resources are missing which makes the test fail
		assertTrue("'DemoApplication.class' hasn't been created in the output folder", rootPath.append(jp.getOutputLocation())
				.append("/com/example/demo/DemoApplication.class").toFile().exists());
		assertTrue("'application.properties' hasn't been copied in the output folder", rootPath.append(jp.getOutputLocation()).append("/application.properties").toFile()
				.exists());		
	}
	
}