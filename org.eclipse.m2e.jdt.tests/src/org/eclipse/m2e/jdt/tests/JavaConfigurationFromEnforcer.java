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

package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

public class JavaConfigurationFromEnforcer extends AbstractMavenProjectTestCase {
	private static final String JRE_CONTAINER_PREFIX = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/";

	@Test
	public void testEnforcerVersion() throws Exception {
		IProject project = importProject("projects/enforcerSettingsWithVersion/pom.xml");
		waitForJobsToComplete();
		IJavaProject jproject = JavaCore.create(project);
		assertEquals("1.8", jproject.getOption(JavaCore.COMPILER_SOURCE, false));
		assertEquals("1.8", jproject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, false));
		assertEquals(List.of("JavaSE-13"), getJREContainerVMType(jproject));
	}

	@Test
	public void testEnforcerVersionRange() throws Exception {
		IProject project = importProject("projects/enforcerSettingsWithVersionRange/pom.xml");
		waitForJobsToComplete();
		IJavaProject jproject = JavaCore.create(project);
		assertEquals("1.8", jproject.getOption(JavaCore.COMPILER_SOURCE, false));
		assertEquals("1.8", jproject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, false));
		assertEquals(List.of("JavaSE-11"), getJREContainerVMType(jproject));
	}

	private static List<String> getJREContainerVMType(IJavaProject jproject) throws JavaModelException {
		return Arrays.stream(jproject.getRawClasspath())
				.filter(cp -> cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER).map(IClasspathEntry::getPath)
				.map(IPath::toString).filter(p -> p.startsWith(JRE_CONTAINER_PREFIX))
				.map(p -> p.substring(JRE_CONTAINER_PREFIX.length())).toList();
	}
}
