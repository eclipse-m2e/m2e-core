/*******************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others
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

package org.eclipse.m2e.core.internal.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

public class RequiredJavaVersionFromEnforcerRuleTest extends AbstractMavenProjectTestCase {

	@Test
	public void testEnforcer_Version() throws Exception {
		IProject project = importProject("resources/projects/enforcerSettingsWithVersion/pom.xml");
		waitForJobsToComplete();
		assertRequiredJavaBuildVersion(project, "13.0.3");
	}

	@Test
	public void testEnforcer_VersionRange() throws Exception {
		IProject project = importProject("resources/projects/enforcerSettingsWithVersionRange/pom.xml");
		waitForJobsToComplete();
		assertRequiredJavaBuildVersion(project, "[11.0.10,16)");
	}

	@Test
	public void testEnforcer_NoVersionRange() throws Exception {
		IProject project = importProject("resources/projects/enforcerSettingsWithoutRequiredJavaVersion/pom.xml");
		waitForJobsToComplete();
		assertRequiredJavaBuildVersion(project, null);
	}

	private void assertRequiredJavaBuildVersion(IProject project, String expectedVersionRange) {
		String actualVersionRange = ResolverConfigurationIO.readResolverConfiguration(project).getRequiredJavaVersion();
		if (expectedVersionRange == null) {
			assertNull("Explicit required Java version set but expected the default", actualVersionRange);
		} else {
			assertNotNull("No explicit required Java version set but expected " + expectedVersionRange, actualVersionRange);
			assertEquals(expectedVersionRange, actualVersionRange);
		}
	}
}
