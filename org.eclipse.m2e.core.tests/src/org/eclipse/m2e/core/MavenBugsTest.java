/*******************************************************************************
 * Copyright (c) 2016, 2022 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;


public class MavenBugsTest extends AbstractMavenProjectTestCase {

  @After
  public void clearWorkspace() throws Exception {
    for(IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      p.delete(true, null);
    }
  }

  @Test
  public void testMNG6530() throws Exception {
    File sourceDirectory = new File(
        FileLocator.toFileURL(getClass().getResource("/resources/projects/testMNG6530")).toURI());
    File tempDirectory = Files.createTempDirectory(getClass().getSimpleName()).toFile();
    try {
      FileUtils.copyDirectory(sourceDirectory, tempDirectory);
      List<MavenProjectInfo> toImport = new ArrayList<>(2);
      toImport.add(new MavenProjectInfo("", new File(tempDirectory, "pom.xml"), null, null));
      toImport.add(new MavenProjectInfo("", new File(tempDirectory, "child/pom.xml"), null, null));
      MavenPlugin.getProjectConfigurationManager().importProjects(toImport, new ProjectImportConfiguration(), null,
          new NullProgressMonitor());

      IProject parent = ResourcesPlugin.getWorkspace().getRoot().getProject("testMNG6530");
      IProject child = ResourcesPlugin.getWorkspace().getRoot().getProject("child");
      try {
        IMavenProjectFacade childFacade = MavenPlugin.getMavenProjectRegistry().getProject(child);
        MavenProject mavenProject = childFacade.getMavenProject(new NullProgressMonitor());
        assertEquals("bar", mavenProject.getProperties().get("foo"));

		IFile pomXml = parent.getFile("pom.xml");
		String content = Files.readString(Path.of(pomXml.getLocationURI())).replace("bar", "lol");
		pomXml.setContents(new ByteArrayInputStream(content.getBytes()), true, false, null);

        MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(child, monitor);
        waitForJobsToComplete();
        mavenProject = childFacade.getMavenProject(monitor);
        assertEquals("lol", mavenProject.getProperties().get("foo"));
      } finally {
        parent.delete(true, null);
        child.delete(true, null);
      }
    } finally {
      FileUtils.deleteDirectory(tempDirectory);
    }
  }

	@Test
	public void testMultiModuleProjectDirectoryChild() throws Exception {
		IProject project = createExisting("simple", "resources/projects/dotMvn/", false);
		waitForJobsToComplete(monitor);
		IMavenProjectFacade facade = MavenPluginActivator.getDefault().getMavenProjectManagerImpl().create(project.getFile("child/pom.xml"),
			true, monitor);
		Assert.assertNotNull(facade);
		File[] multiModuleDirectory = new File[] { null };
		facade.createExecutionContext().execute((context, monitor) -> multiModuleDirectory[0] = context.getExecutionRequest().getMultiModuleProjectDirectory(), null);
		assertEquals(project.getLocation().toFile(), multiModuleDirectory[0]);
  }

	@Test
	public void testBuildStartTime() throws Exception {
		IProject project = createExisting("buildStartTime", "resources/projects/buildStartTime/", false);
		waitForJobsToComplete(monitor);
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		waitForJobsToComplete(monitor);
		IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		assertEquals(Arrays.toString(markers), 0, markers.length);
	}

	@Test
	public void testAllProjects() throws Exception {
		IMaven maven = MavenPlugin.getMaven();
		File pomFile = new File("resources/projects/simplePomOK/pom.xml");
		IMavenExecutionContext context = maven.createExecutionContext();
		MavenExecutionResult result = context.execute((context1, monitor1) -> {
			ProjectBuildingRequest configuration = context.newProjectBuildingRequest();
			configuration.setResolveDependencies(true);
			return maven.readMavenProject(pomFile, configuration);
		}, monitor);
		assertFalse(result.hasExceptions());
		MavenProject project = result.getProject();
		result = context.execute(project, (context1, monitor) -> {
			MavenSession session = context1.getSession();
			assertNotNull("getProjects", session.getProjects());
			assertNotNull("getAllProjects", session.getAllProjects());
			return session.getResult();
		}, monitor);
	}
}
