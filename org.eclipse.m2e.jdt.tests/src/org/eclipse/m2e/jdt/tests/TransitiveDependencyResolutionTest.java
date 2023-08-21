/*******************************************************************************
 * Copyright (c) 2023-2023 Hannes Wellmann and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

public class TransitiveDependencyResolutionTest extends AbstractMavenProjectTestCase {

	@Test
	public void resolutionOfOlderDependencyVersionsAsTransitiveDependency() throws Exception {
		IProject projectA = importProject("projects/transitiveDependencyResolution/project.a/pom.xml");
		IProject projectB = importProject("projects/transitiveDependencyResolution/project.b/pom.xml");
		// Project-A depends on junit-jupiter-api:5.7
		// Project-B depends on junit-jupiter-api:5.8 and Project-A
		// B also contains a class that references a method added to the Assertions
		// class in junit-jupiter-api 5.8.
		// The Assertions class itself already existed in 5.7

		projectA.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		projectB.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		waitForJobsToComplete();
		assertEquals("Project-A has errors", List.of(), findErrorMarkers(projectA));
		assertEquals("Project-B has errors", List.of(), findErrorMarkers(projectB));
	}

}
