/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests;

import java.util.List;

import org.eclipse.pde.core.target.ITargetLocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that the m2e PDE target correctly handles Maven BOM (Bill of Materials)
 * artifacts, including resolution of coordinates inherited from parent POMs.
 */
@RunWith(Parameterized.class)
public class MavenBomSupportTest extends AbstractMavenTargetTest {

	@Parameter(0)
	public Boolean includeSource;

	@Parameters(name = "includeSource={0}")
	public static List<Boolean> configurations() {
		return List.of(false, true);
	}

	@Test
	public void testBomInheritedGroupIdAndVersionResolvedCorrectly() throws Exception {
		// cucumber-bom:7.34.3 inherits both groupId (io.cucumber) and version (7.34.3)
		// from its parent POM - the raw model returns null for both fields.
		ITargetLocation target = resolveMavenTarget(String.format(
				"""
						<location includeDependencyDepth="none" includeDependencyScopes="compile" includeSource="%s" missingManifest="generate" type="Maven">
							<dependencies>
								<dependency>
									<groupId>io.cucumber</groupId>
									<artifactId>cucumber-bom</artifactId>
									<version>7.34.3</version>
									<type>pom</type>
								</dependency>
							</dependencies>
						</location>
						""",
				includeSource));
		assertStatusOk(target.getStatus());

		// A BOM has no regular <dependencies>, only <dependencyManagement>, so no
		// bundles are contributed.
		assertTargetBundles(target, List.of());

		// The generated feature must use the resolved coordinates, not the raw model
		// values which are null when inherited from the parent POM.
		List<ExpectedFeature> expectedFeatures = List.of(generatedFeature("io.cucumber.cucumber-bom.pom", "7.34.3",
				List.of()));
		assertTargetFeatures(target, includeSource ? withSourceFeatures(expectedFeatures) : expectedFeatures);
	}
}
