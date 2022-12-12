/*******************************************************************************
 * Copyright (c) 2022, 2023 Hannes Wellmann and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Hannes Wellmann - initial API and implementation
 *      Konrad Windszus - Add tests for required java runtime version implied by enforcer rule 
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@SuppressWarnings("restriction")
public class MavenLaunchDelegateTest extends AbstractMavenProjectTestCase {

	// If there is a dedicated locations for m2e.launching tests one day, move it
	// there.

	private static final String DEFAULT_VM = "defaultVM";
	private static final List<String> AVAILABLE_VM_VERSIONS = List.of("17.0.4", "11.0.7", "13.0.5", "11.0.1", "1.8.0");

	@Test
	public void testGetBestMatchingVM_majorOnly() throws InvalidVersionSpecificationException {
		try (var mock = mockJavaRuntime()) {
			assertEquals("11.0.7", MavenLaunchDelegate.getBestMatchingVM("11").getId());
		}
	}

	@Test
	public void testGetBestMatchingVM_rangeWithOnlyMajorLowerBound() throws InvalidVersionSpecificationException {
		try (var mock = mockJavaRuntime()) {
			assertEquals("11.0.7", MavenLaunchDelegate.getBestMatchingVM("[11,)").getId());
		}
	}

	@Test
	public void testGetBestMatchingVM_9versionRange() throws InvalidVersionSpecificationException {
		try (var mock = mockJavaRuntime()) {
			assertEquals("17.0.4", MavenLaunchDelegate.getBestMatchingVM("[11,18)").getId());
		}
	}

	@Test
	public void testGetBestMatchingVM_1XversionRange() throws InvalidVersionSpecificationException {
		try (var mock = mockJavaRuntime()) {
			assertEquals("1.8.0", MavenLaunchDelegate.getBestMatchingVM("[1.8,9)").getId());
		}
	}

	@Test
	public void testRequiredJavaVersionFromEnforcerRule_Version() throws Exception {
		IProject project = importProject("resources/projects/enforcerSettingsWithVersion/pom.xml");
		assertRequiredJavaBuildVersion(project, "13.0.3", "13.0.5");
	}

	@Test
	public void testRequiredJavaVersionFromEnforcerRule_VersionRange() throws Exception {
		IProject project = importProject("resources/projects/enforcerSettingsWithVersionRange/pom.xml");
		assertRequiredJavaBuildVersion(project, "[11.0.6,13)", "11.0.7");
	}

	@Test
	public void testRequiredJavaVersionFromEnforcerRule_NoVersionRange() throws Exception {
		IProject project = importProject("resources/projects/enforcerSettingsWithoutRequiredJavaVersion/pom.xml");
		assertRequiredJavaBuildVersion(project, null, DEFAULT_VM);
	}

	private void assertRequiredJavaBuildVersion(IProject project, String expectedVersionRange, String expectedVMVersion)
			throws Exception {

		waitForJobsToComplete();

		File pomFile = project.getLocation().toFile();

		assertEquals(expectedVersionRange, MavenLaunchDelegate.readEnforcedJavaVersion(pomFile, monitor));

		String pomDir = "${workspace_loc:/" + project.getName() + "}";

		try (var mock = mockJavaRuntime()) {
			ILaunchConfigurationWorkingCopy config = createMavenLaunchConfig(pomDir);
			assertEquals(expectedVMVersion, new MavenLaunchDelegate().getVMInstall(config).getId());

			ILaunchConfigurationWorkingCopy config2 = createMavenLaunchConfig(pomDir);
			config2.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
					"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-17/");
			assertEquals(DEFAULT_VM, new MavenLaunchDelegate().getVMInstall(config2).getId());
			// When a JRE_CONTAINER_PATH is set, getVMInstall hands over to
			// JavaRuntime.computeVMInstall(), which is mocked in this test to always return
			// the defaultVM.
		}
	}

	private static MockedStatic<JavaRuntime> mockJavaRuntime() {
		IVMInstall defaultVM = Mockito.mock(IVMInstall.class);
		Mockito.when(defaultVM.getId()).thenReturn(DEFAULT_VM);

		IVMInstallType standardVMType = Mockito.mock(StandardVMType.class, Mockito.CALLS_REAL_METHODS);
		IVMInstall[] installs = AVAILABLE_VM_VERSIONS.stream().map(version -> {
			AbstractVMInstall vm = Mockito.mock(AbstractVMInstall.class, Mockito.CALLS_REAL_METHODS);
			when(vm.getId()).thenReturn(version);
			when(vm.getJavaVersion()).thenReturn(version);
			when(vm.getVMInstallType()).thenReturn(standardVMType);
			when(vm.getName()).thenReturn("JDK " + version);
			return vm;
		}).toArray(IVMInstall[]::new);
		Mockito.doReturn(installs).when(standardVMType).getVMInstalls();

		MockedStatic<JavaRuntime> javaRuntimeMock = Mockito.mockStatic(JavaRuntime.class, Mockito.CALLS_REAL_METHODS);
		javaRuntimeMock.when(() -> JavaRuntime.getVMInstallTypes()).thenReturn(new IVMInstallType[] { standardVMType });
		javaRuntimeMock.when(() -> JavaRuntime.computeVMInstall(Mockito.any())).thenReturn(defaultVM);
		return javaRuntimeMock;
	}

	private static ILaunchConfigurationWorkingCopy createMavenLaunchConfig(String pomDir) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		String name = launchManager.generateLaunchConfigurationName("RequiredJavaVersionFromEnforcerRuleTest");
		ILaunchConfigurationWorkingCopy config = launchManager
				.getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID).newInstance(null, name);
		config.setAttribute(ILaunchManager.ATTR_PRIVATE, true);
		config.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, pomDir);
		return config;
	}

}
