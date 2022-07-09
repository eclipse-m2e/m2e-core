/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.junit.Assert;
import org.junit.Test;

public class ExtensionsTest extends AbstractMavenProjectTestCase {
	@Test
	public void testProjectExtensions() throws Exception {
		IProject project = createExisting("projectExtension", "resources/projects/projectExtension/", false);
		waitForJobsToComplete(monitor);
		IMavenProjectFacade facade = Adapters.adapt(project, IMavenProjectFacade.class);
		Assert.assertNotNull(facade);
		IComponentLookup projectLookup = facade.getComponentLookup();
		Collection<AbstractMavenLifecycleParticipant> participantList = projectLookup
				.lookupCollection(AbstractMavenLifecycleParticipant.class);
		assertTrue("Should not return a project scoped extension", participantList.isEmpty());
		Collection<AbstractMavenLifecycleParticipant> buildParticipants = facade.createExecutionContext()
				.execute((context, monitor) -> {
					assertNotNull("context has no project!", context.getSession().getCurrentProject());
					return context.getComponentLookup().lookupCollection(AbstractMavenLifecycleParticipant.class);
				}, monitor);
		assertTrue("the must be at laest one build participant!", buildParticipants.size() > 0);
		Collection<AbstractMavenLifecycleParticipant> participantListAfterCall = projectLookup
				.lookupCollection(AbstractMavenLifecycleParticipant.class);
		assertTrue("Should not return a project scoped extension", participantListAfterCall.isEmpty());
	}

	@Test
	public void testCoreExtension() throws Exception {
		IProject project = importPomlessProject("pomless", "bundle/pom.xml");
		waitForJobsToComplete(monitor);
		assertEquals("my.bundle", project.getName());
		assertNotNull(project.getNature("org.eclipse.m2e.core.maven2Nature"));
	}

	@Test
	public void testLoadSameExtensionFromMultipleLocations() throws Exception {
		IProject project1 = importPomlessProject("pomless", "bundle/pom.xml");
		waitForJobsToComplete(monitor);
		assertEquals("my.bundle", project1.getName());

		IProject project2 = importPomlessProject("pomless2", "bundle2/pom.xml");
		waitForJobsToComplete(monitor);
		assertEquals("my.bundle2", project2.getName());
	}

	@Test
	public void testReloadExtensionAfterDeletion() throws Exception {
		IProject project = importPomlessProject("pomless", "bundle/pom.xml");

		waitForJobsToComplete(monitor);
		assertEquals("my.bundle", project.getName());

		WorkspaceHelpers.cleanWorkspace();

		project = importPomlessProject("pomless", "bundle/pom.xml");
		waitForJobsToComplete(monitor);
		assertEquals("my.bundle", project.getName());
	}

	@Test
	public void testMavenConfigWithCoreExtension() throws Exception {
		IProject project = importPomlessProject("mavenConfig", "bundle/pom.xml");

		assertEquals("my.bundle3", project.getName());
		assertTrue(project.hasNature("org.eclipse.m2e.core.maven2Nature"));
		assertNoErrors(project);
	}

	@Test
	public void testCopyResourcesWithMVNFolder() throws Exception {
		IProject project = importProject("resources/projects/resourcesWithMVNFolder/pom.xml");
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		WorkspaceHelpers.assertNoErrors(project);
		IFile file = project.getFile("target/classes/file.txt");
		assertTrue(file.exists());
		assertEquals("foo-bar-content", new String(file.getContents().readAllBytes()));
	}

	private IProject importPomlessProject(String rootProject, String... poms) throws IOException, CoreException {
		IProject[] projects = importProjects("resources/projects/" + rootProject + "/", poms,
				new ResolverConfiguration(), false);
		assertEquals(1, projects.length);
		return projects[0];
	}
}
