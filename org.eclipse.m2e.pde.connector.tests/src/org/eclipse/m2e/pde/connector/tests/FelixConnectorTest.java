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
import static org.eclipse.m2e.pde.connector.tests.TychoConnectorTest.copyHierarchyRootFiles;

import java.io.IOException;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class FelixConnectorTest extends AbstractMavenProjectTestCase {

	@Before
	public void setUpBeforeClass() throws IOException {
		copyHierarchyRootFiles("projects/felix");
	}

	@Test
	public void importFelixBundleProject() throws IOException, CoreException {
		IProject project = importFelixProject("bundle-project/pom.xml");
		assertErrorFreeProjectWithBuildersAndNatures(project, PLUGIN_NATURES,
				Set.of(JavaCore.BUILDER_ID, IMavenConstants.BUILDER_ID));
		assertPluginProjectExists(project, "m2e.pde.connector.tests.bundle-project");
	}

	@Test
	public void importFelixJarProject() throws IOException, CoreException {
		IProject project = importFelixProject("jar-project/pom.xml");
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		assertErrorFreeProjectWithBuildersAndNatures(project, PLUGIN_NATURES,
				Set.of(JavaCore.BUILDER_ID, IMavenConstants.BUILDER_ID));
		assertPluginProjectExists(project, "m2e.pde.connector.tests.jar-project");
	}

	private IProject importFelixProject(String name) throws IOException, CoreException {
		return importProjects("projects/felix", new String[] { name }, new ResolverConfiguration(), false, null)[0];
	}
}