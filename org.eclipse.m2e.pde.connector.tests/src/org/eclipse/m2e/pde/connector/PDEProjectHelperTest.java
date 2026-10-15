/*******************************************************************************
 * Copyright (c) 2026 Hélios Gilles and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hélios Gilles - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.pde.connector;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.junit.Test;

@SuppressWarnings("restriction")
public class PDEProjectHelperTest extends AbstractMavenProjectTestCase {

	/**
	 * If the plug-in model of a project is not available yet when the project is
	 * configured, its PDE classpath is computed later, once the model is. This
	 * update must not remove the Maven classpath container.
	 */
	@Test
	public void testClasspathUpdateKeepsMavenClasspathContainer() throws Exception {
		IProject project = importProjects("projects/tycho", new String[] { "pde.tycho.plugin/pom.xml" },
				new ResolverConfiguration(), false, null)[0];
		IJavaProject javaProject = JavaCore.create(project);
		assertTrue("Maven classpath container not found after import: " + toString(javaProject),
				hasMavenClasspathContainer(javaProject));
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull(model);

		PDEProjectHelper.setClasspath(project, model, monitor);

		assertTrue("Maven classpath container not found after classpath update: " + toString(javaProject),
				hasMavenClasspathContainer(javaProject));
	}

	private static boolean hasMavenClasspathContainer(IJavaProject javaProject) throws Exception {
		return Arrays.stream(javaProject.getRawClasspath()).filter(e -> e.getEntryKind() == IClasspathEntry.CPE_CONTAINER)
				.anyMatch(e -> IClasspathManager.CONTAINER_ID.equals(e.getPath().segment(0)));
	}

	private static String toString(IJavaProject javaProject) throws Exception {
		return Arrays.toString(javaProject.getRawClasspath());
	}
}
