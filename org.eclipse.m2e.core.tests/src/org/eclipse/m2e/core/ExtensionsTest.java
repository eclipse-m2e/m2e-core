/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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

import java.util.Collection;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Assert;
import org.junit.Test;

public class ExtensionsTest extends AbstractMavenProjectTestCase {
	@Test
	public void testProjectExtensions() throws Exception {
		IProject project = createExisting("buildStartTime", "resources/projects/projectExtension/", false);
		waitForJobsToComplete(monitor);
		IMavenProjectFacade facade = Adapters.adapt(project, IMavenProjectFacade.class);
		Assert.assertNotNull(facade);
		Collection<AbstractMavenLifecycleParticipant> buildParticipants = facade.createExecutionContext()
				.execute((context, monitor) -> {
					assertNotNull("context has no project!", context.getSession().getCurrentProject());
					return context.getComponentLookup().lookupCollection(AbstractMavenLifecycleParticipant.class);
				}, monitor);
		assertTrue("the must be at laest one build participant!", buildParticipants.size() > 0);
	}

	@Test
	public void testCoreExtension() throws Exception {
		boolean skipSanityCheck = true; // TODO this should also work! But not part of this change
		IProject[] projects = importProjects("resources/projects/pomless/", new String[] { "bundle/pom.xml" },
				new ResolverConfiguration(), skipSanityCheck);
		waitForJobsToComplete(monitor);
		assertEquals(1, projects.length);
		IProject project = projects[0];
		assertEquals("my.bundle", project.getName());
		assertNotNull(project.getNature("org.eclipse.m2e.core.maven2Nature"));
	}
}
