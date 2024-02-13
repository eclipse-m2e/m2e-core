/*******************************************************************************
 * Copyright (c) 2024 Pascal Treilhes
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.jdt.internal.UnitTestSupport;
import org.eclipse.m2e.jdt.internal.launch.MavenRuntimeClasspathProvider;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@SuppressWarnings("restriction")
@RunWith(Parameterized.class)
public class UnitTestLaunchConfigConfigurationTest extends AbstractMavenProjectTestCase {

	private static final String REPLACED_SUREFIRE_POM_STRING = "<!-- surefireArgs: replacedByArgsSets -->";
	private static final String REPLACED_FAILSAFE_POM_STRING = "<!-- failsafeArgs: replacedByArgsSets -->";
	private static final String ROOT_PATH = "/projects/surefireFailsafeToTestLaunchSettings";

	private String testType;
	private ILaunchManager launchManager;

	public UnitTestLaunchConfigConfigurationTest(String testType) {
		super();
		this.testType = testType;
	}

	// Define the parameters to be used in the test
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				// { MavenRuntimeClasspathProvider.JDT_TESTNG_TEST }, // not by default yet
				{ MavenRuntimeClasspathProvider.JDT_JUNIT_TEST } });
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
		setAutoBuilding(true);

		// Get the launch manager
		launchManager = DebugPlugin.getDefault().getLaunchManager();

	}

	@Test
	public void test_configuration_must_be_updated_with_surefire_config()
			throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(testType);

		assumeNotNull(type);// if null unit test support for junit or testng is not installed

		File pomFile = getTestFile("argumentsAreSet/pom.xml");
		File surefireConf = getTestFile("argumentsAreSet/surefireArgsSet.xml");
		File failsafeConf = null;

		IProject project = importProject(pomFile.getAbsolutePath());

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTest");

		updateProject(project);
		waitForJobsToComplete();

		ILaunchConfiguration[] updatedConfigurations = launchManager.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		mergePomAndPluginConfigIntoProject(project, pomFile, surefireConf, failsafeConf);
		updateProject(project);
		waitForJobsToComplete();

		updatedConfigurations = launchManager.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		ILaunchConfiguration config = updatedConfigurations[0];

		// check argLine
		String argLine = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_VM_ARGUMENTS, "");
		assertTrue(argLine.contains("--argLineItem=surefireArgLineValue"));

		// check environmentVariables
		Map<String, String> envVars = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_ENVIRONMENT_VARIABLES,
				(Map<String, String>) null);

		assertNotNull(envVars);
		assertTrue(envVars.size() == 1);
		assertTrue(envVars.containsKey("surefireEnvironmentVariables1"));
		assertEquals("surefireEnvironmentVariables1Value", envVars.get("surefireEnvironmentVariables1"));

		// check systemPropertyVariables
		assertTrue(argLine.contains("-DsurefireProp1=surefireProp1Value"));
	}

	@Test
	public void test_configuration_must_be_updated_with_failsafe_config()
			throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(testType);

		assumeNotNull(type);// if null unit test support for junit or testng is not installed

		File pomFile = getTestFile("argumentsAreSet/pom.xml");
		File surefireConf = null;
		File failsafeConf = getTestFile("argumentsAreSet/failsafeArgsSet.xml");

		IProject project = importProject(pomFile.getAbsolutePath());
		// waitForJobsToComplete();

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTestIT");

		updateProject(project);
		waitForJobsToComplete();

		ILaunchConfiguration[] updatedConfigurations = launchManager.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		mergePomAndPluginConfigIntoProject(project, pomFile, surefireConf, failsafeConf);
		updateProject(project);

		updatedConfigurations = launchManager.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		ILaunchConfiguration config = updatedConfigurations[0];

		// check argLine
		String argLine = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_VM_ARGUMENTS, "");
		assertTrue(argLine.contains("--argLineItem=failsafeArgLineValue"));

		// check environmentVariables
		Map<String, String> envVars = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_ENVIRONMENT_VARIABLES,
				(Map<String, String>) null);

		assertNotNull(envVars);
		assertTrue(envVars.size() == 1);
		assertTrue(envVars.containsKey("failsafeEnvironmentVariables1"));
		assertEquals("failsafeEnvironmentVariables1Value", envVars.get("failsafeEnvironmentVariables1"));

		// check systemPropertyVariables
		assertTrue(argLine.contains("-DfailsafeProp1=failsafeProp1Value"));
	}

	@Test
	public void test_configuration_must_be_updated_with_surefire_config_when_created()
			throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(testType);

		assumeNotNull(type);// if null unit test support for junit or testng is not installed

		File pomFile = getTestFile("argumentsAreSet/pom.xml");
		File surefireConf = getTestFile("argumentsAreSet/surefireArgsSet.xml");
		File failsafeConf = null;

		IProject project = importProject(pomFile.getAbsolutePath());
		mergePomAndPluginConfigIntoProject(project, pomFile, surefireConf, failsafeConf);
		updateProject(project);
		waitForJobsToComplete();

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTest");

		ILaunchConfiguration[] updatedConfigurations = launchManager.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		ILaunchConfiguration config = updatedConfigurations[0];

		// check argLine
		String argLine = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_VM_ARGUMENTS, "");
		assertTrue(argLine.contains("--argLineItem=surefireArgLineValue"));

		// check environmentVariables
		Map<String, String> envVars = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_ENVIRONMENT_VARIABLES,
				(Map<String, String>) null);

		assertNotNull(envVars);
		assertTrue(envVars.size() == 1);
		assertTrue(envVars.containsKey("surefireEnvironmentVariables1"));
		assertEquals("surefireEnvironmentVariables1Value", envVars.get("surefireEnvironmentVariables1"));

		// check systemPropertyVariables
		assertTrue(argLine.contains("-DsurefireProp1=surefireProp1Value"));
	}

	@Test
	public void test_configuration_must_be_updated_with_failSafe_config_when_created()
			throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(testType);

		assumeNotNull(type);// if null unit test support for junit or testng is not installed

		File pomFile = getTestFile("argumentsAreSet/pom.xml");
		File surefireConf = null;
		File failsafeConf = getTestFile("argumentsAreSet/failsafeArgsSet.xml");

		IProject project = importProject(pomFile.getAbsolutePath());
		mergePomAndPluginConfigIntoProject(project, pomFile, surefireConf, failsafeConf);
		updateProject(project);
		waitForJobsToComplete();

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTestIT");

		ILaunchConfiguration[] updatedConfigurations = launchManager.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		ILaunchConfiguration config = updatedConfigurations[0];

		// check argLine
		String argLine = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_VM_ARGUMENTS, "");
		assertTrue(argLine.contains("--argLineItem=failsafeArgLineValue"));

		// check environmentVariables
		Map<String, String> envVars = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_ENVIRONMENT_VARIABLES,
				(Map<String, String>) null);

		assertNotNull(envVars);
		assertTrue(envVars.size() == 1);
		assertTrue(envVars.containsKey("failsafeEnvironmentVariables1"));
		assertEquals("failsafeEnvironmentVariables1Value", envVars.get("failsafeEnvironmentVariables1"));

		// check systemPropertyVariables
		assertTrue(argLine.contains("-DfailsafeProp1=failsafeProp1Value"));
	}

	@Test
	public void properties_plugin_must_be_executed_before_launch_configuration_update()
			throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(testType);

		assumeNotNull(type);// if null unit test support for junit or testng is not installed

		File pomFile = getTestFile("prerequisitesAreLoaded/pom.xml");

		IProject project = importProject(pomFile.getAbsolutePath());
		waitForJobsToComplete();

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTest");

		ILaunchConfiguration[] updatedConfigurations = launchManager.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		ILaunchConfiguration config = updatedConfigurations[0];

		// check argLine
		String argLine = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_VM_ARGUMENTS, "");
		assertTrue(argLine.contains("--argLineItem=somevalue"));// somevalue is from the properties file

	}

	private void updateProject(IProject project) throws CoreException, InterruptedException {
		MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
		waitForJobsToComplete();
	}

	// Create a default test
	private void createDefaultTest(IProject project, ILaunchConfigurationType type, String testClassName)
			throws CoreException {
		// create basic unit test
		ILaunchConfigurationWorkingCopy launchConfig = type.newInstance(project, "sampleTest");
		launchConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		launchConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, testClassName);
		launchConfig.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, project.getLocation().toString());
		launchConfig.doSave();
	}

	// Merge the pom and plugins configuration into the project
	private void mergePomAndPluginConfigIntoProject(IProject project, File pomTemplate, File surefireConf,
			File failsafeConf) throws IOException, CoreException {
		String pom = Utils.read(project, pomTemplate);
		IFile pomFileWS = project.getFile(pomTemplate.getName());
		String newContent = pom;

		if (surefireConf != null) {
			String plugin = Utils.read(project, surefireConf);
			newContent = newContent.replace(REPLACED_SUREFIRE_POM_STRING, plugin);
		}

		if (failsafeConf != null) {
			String plugin = Utils.read(project, failsafeConf);
			newContent = newContent.replace(REPLACED_FAILSAFE_POM_STRING, plugin);
		}

		pomFileWS.setContents(new ByteArrayInputStream(newContent.getBytes()), true, false, null);
	}

	private File getTestFile(String filename) throws IOException {
		return new File(FileLocator.toFileURL(getClass().getResource(ROOT_PATH + "/" + filename)).getFile());
	}
}
