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
 *      Georg Tsakumagos - Add tests for global- & user- settings and toolchains.
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.IBiConsumer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
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

	/**
	 * Tests rendering of maven cli args for <em>global settings (-gs,--global-settings)</em>.
	 * @throws Exception On errors.
	 */
	@Test
	public void testGlobalSettings() throws Exception {
		assertMavenLaunchFileSetting(IMavenConfiguration::setGlobalSettingsFile, "-gs", "./resources/settings/empty_settings/settings_empty.xml");
	}

	/**
	 * Tests rendering of maven cli args for <em>global settings (-gs,--global-settings)</em>
	 * if setting is overridden by direct parameterization in the goal input
	 * @throws Exception On errors.
	 */
	@Test
	public void testGlobalSettings_GoalOverride() throws Exception {
		assertMavenLaunchFileSettingGoalOverride(IMavenConfiguration::setGlobalSettingsFile, "-gs", "./resources/settings/empty_settings/settings_empty.xml");
	}
	
	/**
	 * Tests rendering of maven cli args for <em>global settings (-gs,--global-settings)</em> 
	 * if an invalid path was provided.
	 * @throws Exception On errors.
	 */
	@Test
	public void testGlobalSettings_Invalid() throws Exception {
		assertMavenLaunchFileSettingPathInvalid(IMavenConfiguration::setGlobalSettingsFile);
	}
	
	/**
	 * Tests rendering of maven cli args for <em>global toolchains (-gt,--global-toolchains)</em>.
	 * @throws Exception On errors.
	 */
	@Test
	public void testGlobalToolchains() throws Exception {
		assertMavenLaunchFileSetting(IMavenConfiguration::setGlobalToolchainsFile, "-gt", "./resources/settings/empty_settings/toolchains_empty.xml");
	}

	/**
	 * Tests rendering of maven cli args for <em>global toolchains (-gt,--global-toolchains)</em> 
	 * if setting is overridden by direct parameterization in the goal input
	 * @throws Exception On errors.
	 */
	@Test
	public void testGlobalToolchains_GoalOverride() throws Exception {
		assertMavenLaunchFileSettingGoalOverride(IMavenConfiguration::setGlobalToolchainsFile, "-gt", "./resources/settings/empty_settings/toolchains_empty.xml");
	}
	
	/**
	 * Tests rendering of maven cli args for <em>global toolchains (-gt,--global-toolchains)</em> if an invalid path was provided.
	 * @throws Exception On errors.
	 */
	@Test
	public void testGlobalToolchains_Invalid() throws Exception {
		assertMavenLaunchFileSettingPathInvalid(IMavenConfiguration::setGlobalToolchainsFile);
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

	/**
	 * Tests rendering of maven cli args for <em>global settings (-s,--settings)</em>
	 * @throws Exception On errors.
	 */
	@Test
	public void testUserSettings() throws Exception {
		assertMavenLaunchFileSetting(IMavenConfiguration::setUserSettingsFile, "-s", "./resources/settings/empty_settings/settings_empty.xml");
	}

	/**
	 * Tests rendering of maven cli args for <em>global settings (-s,--settings)</em>
	 * if setting is overridden by direct parameterization in the goal input
	 * @throws Exception On errors.
	 */
	@Test
	public void testUserSettings_GoalOverride() throws Exception {
		assertMavenLaunchFileSettingGoalOverride(IMavenConfiguration::setUserSettingsFile, "-s", "./resources/settings/empty_settings/settings_empty.xml");
	}

	/**
	 * Tests rendering of maven cli args for <em>global settings (-s,--settings)</em> 
	 * if an invalid path was provided.
	 * @throws Exception On errors.
	 */
	@Test
	public void testUserSettings_Invalid() throws Exception {
		assertMavenLaunchFileSettingPathInvalid(IMavenConfiguration::setUserSettingsFile);
	}

	
	/**
	 * Tests rendering of maven cli args for <em>global toolchains (-t,--toolchains)</em>.
	 * @throws Exception On errors.
	 */
	@Test
	public void testUserToolchains() throws Exception {
		assertMavenLaunchFileSetting(IMavenConfiguration::setUserToolchainsFile, "-t", "./resources/settings/empty_settings/toolchains_empty.xml");
	}

	/**
	 * Tests rendering of maven cli args for <em>global toolchains (-t,--toolchains)</em>.
	 * if setting is overridden by direct parameterization in the goal input
	 * @throws Exception On errors.
	 */
	@Test
	public void testUserToolchains_GoalOverride() throws Exception {
		assertMavenLaunchFileSettingGoalOverride(IMavenConfiguration::setUserToolchainsFile, "-t", "./resources/settings/empty_settings/toolchains_empty.xml");
	}

	/**
	 * Tests rendering of maven cli args for <em>global toolchains (-t,--toolchains)</em> 
	 * if an invalid path was provided.
	 * @throws Exception On errors.
	 */
	@Test
	public void testUserToolchains_Invalid() throws Exception {
		assertMavenLaunchFileSettingPathInvalid(IMavenConfiguration::setUserToolchainsFile);
	}
	
	
	/**
	 * assertion shortcut for launch configuration. 
	 * @param configSetter Setter for the configuration accepting the relativePath. Must not be <code>null</code>.
	 * @param key Key of the configuration.  Must not be <code>null</code>.
	 * @param relativePath Relative path for the file.  Must not be <code>null</code>.
	 * @throws Exception Usually only on missed assertions. 
	 */
	private void assertMavenLaunchConfig(IBiConsumer<IMavenConfiguration, String> configSetter, String goal, IBiConsumer<MavenLaunchDelegate, ILaunchConfigurationWorkingCopy> verifier, String relativePath)
			throws Exception {

		waitForJobsToComplete();
		IProject project = importProject("resources/projects/simplePomOK/pom.xml");
		String pomDir = "${workspace_loc:/" + project.getName() + "}";

		try (var mock = mockJavaRuntime()) {
		    IMavenConfiguration mavenConfig = MavenPlugin.getMavenConfiguration();

			try {
				configSetter.accept(mavenConfig, relativePath);
				ILaunchConfigurationWorkingCopy config = createMavenLaunchConfig(pomDir);

				try {
					Optional.ofNullable(goal).ifPresent((g) -> config.setAttribute(MavenLaunchConstants.ATTR_GOALS, g));
				
					// Prepare Mocks to capture VM configuration
					Launch launch = new Launch(config, "run", new MavenSourceLocator());
					NullProgressMonitor mockMonitor = Mockito.spy(new NullProgressMonitor());
					Mockito.doReturn(true).when(mockMonitor).isCanceled();
				
					// mock launch
					MavenLaunchDelegate launcher = new MavenLaunchDelegate();
					launcher.launch(config, "run", launch, mockMonitor);
				
					verifier.accept(launcher, config);
				} finally {
					Optional.ofNullable(goal).ifPresent((g) -> config.removeAttribute(MavenLaunchConstants.ATTR_GOALS));
				}
			} finally {
				// Reset property to avoid conflicts with other test cases.
				configSetter.accept(mavenConfig, null);
			}
		}
	}

	/**
	 * assertion shortcut for launch configuration. 
	 * @param configSetter Setter for the configuration accepting the relativePath. Must not be <code>null</code>.
	 * @param key Key of the configuration.  Must not be <code>null</code>.
	 * @param relativePath Relative path for the file.  Must not be <code>null</code>.
	 * @throws Exception Usually only on missed assertions. 
	 */
	private void assertMavenLaunchFileSetting(IBiConsumer<IMavenConfiguration, String> configSetter, String key, String relativePath)
			throws Exception {
		this.assertMavenLaunchConfig(configSetter, null, (launcher, config) -> {
			String programArguments = launcher.getProgramArguments(config);
			
			// prepare assert
			Matcher<String> allSettings = CoreMatchers.allOf(
					CoreMatchers.containsString(key),
					CoreMatchers.containsString(new File(relativePath).getAbsolutePath())
					);
			
			// assert
			MatcherAssert.assertThat(programArguments, allSettings);
		}, relativePath);
	}

	/**
	 * assertion shortcut for launch configuration. 
	 * @param configSetter Setter for the configuration accepting the relativePath. Must not be <code>null</code>.
	 * @param key Key of the configuration.  Must not be <code>null</code>.
	 * @param relativePath Relative path for the file.  Must not be <code>null</code>.
	 * @throws Exception Usually only on missed assertions. 
	 */
	private void assertMavenLaunchFileSettingGoalOverride(IBiConsumer<IMavenConfiguration, String> configSetter, String key, String relativePath)
			throws Exception {
		final String userDerivedPath = "./resources/settings/empty_settings/this_do_not_exists.xml";
		final String goalConfig = "clean " + key + " " + userDerivedPath;
		
		this.assertMavenLaunchConfig(configSetter, goalConfig, (launcher, config) -> {
			String programArguments = launcher.getProgramArguments(config);
			
			// prepare assert
			Matcher<String> allSettings = CoreMatchers.allOf(
					CoreMatchers.containsString(key),
					CoreMatchers.containsString(userDerivedPath),
					CoreMatchers.not(CoreMatchers.containsString(relativePath))
					);
			
			// assert
			MatcherAssert.assertThat(programArguments, allSettings);
		}, relativePath);
	}
	
	
	/**
	 * assertion shortcut for launch configuration if an invalid path was provided.
	 * @param configSetter Setter for the configuration accepting the relativePath. Must not be <code>null</code>.
	 * @param key Key of the configuration.  Must not be <code>null</code>.
	 * @throws Exception Usually only on missed assertions. 
	 */
	private void assertMavenLaunchFileSettingPathInvalid(IBiConsumer<IMavenConfiguration, String> configSetter) throws Exception {
		final String path = "./resources/settings/empty_settings/this_do_not_exists.xml";
		try {
			this.assertMavenLaunchConfig(configSetter, null, (launcher, config) -> {}, path);
		} catch(IllegalArgumentException expected) {
			MatcherAssert.assertThat(expected.getMessage(), CoreMatchers.containsString(path));				
		}
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
