/*******************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.pde.connector.tests;

import static org.eclipse.m2e.pde.connector.tests.TychoConnectorTest.PLUGIN_NATURES;
import static org.eclipse.m2e.pde.connector.tests.TychoConnectorTest.assertErrorFreeProjectWithBuildersAndNatures;
import static org.eclipse.m2e.pde.connector.tests.TychoConnectorTest.assertPluginProjectExists;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

@SuppressWarnings("restriction")
public class BNDConnectorTest extends AbstractMavenProjectTestCase {

	@Test
	public void importBNDProject() throws Exception {
		IProject project = importProject("projects/bnd/pom.xml");
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

		assertErrorFreeProjectWithBuildersAndNatures(project, PLUGIN_NATURES,
				Set.of(JavaCore.BUILDER_ID, IMavenConstants.BUILDER_ID));
		assertPluginProjectExists(project, "m2e.pde.connector.tests.bnd");

		IPath expectedJavaxLibPath = Path.fromOSString("/bnd/target/m2e-bundleClassPath/slf4j-api-1.7.36.jar");
		List<IClasspathEntry> javaxLibEntries = Arrays.stream(JavaCore.create(project).getRawClasspath())
				.filter(e -> e.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
				.filter(e -> e.getPath().equals(expectedJavaxLibPath)).toList();
		assertEquals("Embedded javax.inject library classpath entry is missing", 1, javaxLibEntries.size());
	}

}
