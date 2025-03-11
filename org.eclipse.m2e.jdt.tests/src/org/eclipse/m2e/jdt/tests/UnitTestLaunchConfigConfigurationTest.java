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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
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
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@SuppressWarnings("restriction")
@RunWith(Parameterized.class)
public class UnitTestLaunchConfigConfigurationTest extends AbstractMavenProjectTestCase {

	private static final String REPLACED_SUREFIRE_POM_STRING = "<!-- surefireArgs: replacedByArgsSets -->";
	private static final String REPLACED_FAILSAFE_POM_STRING = "<!-- failsafeArgs: replacedByArgsSets -->";
	private static final String ROOT_PATH = "/projects/surefireFailsafeToTestLaunchSettings";
	private static ILaunchManager LAUNCH_MANAGER = DebugPlugin.getDefault().getLaunchManager();

	/*
	 * XML allows encoding set of control characters: space (U+0020), carriage
	 * return (U+000d), line feed (U+000a) and horizontal tab (U+0009).
	 * https://www.w3.org/TR/xml-entity-names/000.html
	 */
	private static final String SUREFIRE_ARGS_SET = """
			<configuration>
				<argLine>
					--argLineItem=surefireArgLineValue --undefinedArgLineItem=${undefinedProperty}
				</argLine>
				<systemPropertyVariables>
					<surefireProp1>surefireProp1Value</surefireProp1>
					<surefirePropWithSpaces>surefire Prop&#x20;With Spaces</surefirePropWithSpaces>
					<surefirePropWithTab>surefirePropWith&#x09;Tab</surefirePropWithTab>
					<surefirePropWithCR>has&#x0d;CR</surefirePropWithCR>
					<surefirePropWithLF>has&#x0a;LF</surefirePropWithLF>
					<surefireEmptyProp>${undefinedProperty}</surefireEmptyProp>
				</systemPropertyVariables>
				<environmentVariables>
					<surefireEnvironmentVariables1>surefireEnvironmentVariables1Value</surefireEnvironmentVariables1>
					<surefireEmptyEnvironmentVariables1>${undefinedProperty}</surefireEmptyEnvironmentVariables1>
				</environmentVariables>
			</configuration>
			""";
	private static final String FAILSAFE_ARGS_SET = """
			<configuration>
				<argLine>
					--argLineItem=failsafeArgLineValue --undefinedArgLineItem=${undefinedProperty}
				</argLine>
				<systemPropertyVariables>
					<failsafeProp1>failsafeProp1Value</failsafeProp1>
					<failsafeEmptyProp>${undefiniedProperty}</failsafeEmptyProp>
					<failsafePropWithSpaces>failsafe Prop&#x20;With Spaces</failsafePropWithSpaces>
					<failsafePropWithTab>failsafePropWith&#x09;Tab</failsafePropWithTab>
					<failsafePropWithCR>has&#x0d;CR</failsafePropWithCR>
					<failsafePropWithLF>has&#x0a;LF</failsafePropWithLF>
				</systemPropertyVariables>
				<environmentVariables>
					<failsafeEnvironmentVariables1>failsafeEnvironmentVariables1Value</failsafeEnvironmentVariables1>
					<failsafeEmptyEnvironmentVariables1>${undefinedProperty}</failsafeEmptyEnvironmentVariables1>
				</environmentVariables>
			</configuration>
			""";

	// Define the parameters to be used in the test
	@Parameters
	public static Collection<Object> data() {
		return List.of(MavenRuntimeClasspathProvider.JDT_TESTNG_TEST, MavenRuntimeClasspathProvider.JDT_JUNIT_TEST);
	}

	@Parameter(0)
	public String testType;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
		setAutoBuilding(true);
	}

	@Test
	public void test_configuration_must_be_updated_with_surefire_config()
			throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = LAUNCH_MANAGER.getLaunchConfigurationType(testType);

		assumeTrue(testType + " support not available", type != null);

		File pomFile = getTestFile("argumentsAreSet/pom.xml");
		String surefireConf = SUREFIRE_ARGS_SET;
		String failsafeConf = null;

		IProject project = importProject(pomFile.getAbsolutePath());

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTest");

		updateProject(project);
		waitForJobsToComplete();

		ILaunchConfiguration[] updatedConfigurations = LAUNCH_MANAGER.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		mergePomAndPluginConfigIntoProject(project, pomFile, surefireConf, failsafeConf);
		updateProject(project);
		waitForJobsToComplete();

		updatedConfigurations = LAUNCH_MANAGER.getLaunchConfigurations(type);
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

		// check systemPropertyVariables with white space in values have values quoted
		assertThat(argLine, Matchers.containsString("-DsurefirePropWithSpaces=\"surefire Prop With Spaces\""));
		assertThat(argLine, Matchers.containsString("-DsurefirePropWithTab=\"surefirePropWith\tTab\""));
		assertThat(argLine, Matchers.containsString("-DsurefirePropWithCR=\"has\rCR\""));
		assertThat(argLine, Matchers.containsString("-DsurefirePropWithLF=\"has\nLF\""));

		// check systemPropertyVariables with null value aren't set
		assertTrue(!argLine.contains("-DsurefireEmptyProp="));

	}

	@Test
	public void test_configuration_must_be_updated_with_failsafe_config()
			throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = LAUNCH_MANAGER.getLaunchConfigurationType(testType);

		assumeTrue(testType + " support not available", type != null);

		File pomFile = getTestFile("argumentsAreSet/pom.xml");
		String surefireConf = null;
		String failsafeConf = FAILSAFE_ARGS_SET;

		IProject project = importProject(pomFile.getAbsolutePath());
		// waitForJobsToComplete();

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTestIT");

		updateProject(project);
		waitForJobsToComplete();

		ILaunchConfiguration[] updatedConfigurations = LAUNCH_MANAGER.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		mergePomAndPluginConfigIntoProject(project, pomFile, surefireConf, failsafeConf);
		updateProject(project);

		updatedConfigurations = LAUNCH_MANAGER.getLaunchConfigurations(type);
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

		// check systemPropertyVariables with null value aren't set
		assertTrue(!argLine.contains("-DfailsafeEmptyProp="));

		// check systemPropertyVariables with white space in values have values quoted
		assertThat(argLine, Matchers.containsString("-DfailsafePropWithSpaces=\"failsafe Prop With Spaces\""));
		assertThat(argLine, Matchers.containsString("-DfailsafePropWithTab=\"failsafePropWith\tTab\""));
		assertThat(argLine, Matchers.containsString("-DfailsafePropWithCR=\"has\rCR\""));
		assertThat(argLine, Matchers.containsString("-DfailsafePropWithLF=\"has\nLF\""));

	}

	@Test
	public void test_configuration_must_be_updated_with_surefire_config_when_created()
			throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = LAUNCH_MANAGER.getLaunchConfigurationType(testType);

		assumeTrue(testType + " support not available", type != null);

		File pomFile = getTestFile("argumentsAreSet/pom.xml");
		String surefireConf = SUREFIRE_ARGS_SET;
		String failsafeConf = null;

		IProject project = importProject(pomFile.getAbsolutePath());
		mergePomAndPluginConfigIntoProject(project, pomFile, surefireConf, failsafeConf);
		updateProject(project);
		waitForJobsToComplete();

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTest");

		ILaunchConfiguration[] updatedConfigurations = LAUNCH_MANAGER.getLaunchConfigurations(type);
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

		// check systemPropertyVariables with white space in values have values quoted
		assertThat(argLine, Matchers.containsString("-DsurefirePropWithSpaces=\"surefire Prop With Spaces\""));
		assertThat(argLine, Matchers.containsString("-DsurefirePropWithTab=\"surefirePropWith\tTab\""));
		assertThat(argLine, Matchers.containsString("-DsurefirePropWithCR=\"has\rCR\""));
		assertThat(argLine, Matchers.containsString("-DsurefirePropWithLF=\"has\nLF\""));

	}

	@Test
	public void test_configuration_must_be_updated_with_failSafe_config_when_created()
			throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = LAUNCH_MANAGER.getLaunchConfigurationType(testType);

		assumeTrue(testType + " support not available", type != null);

		File pomFile = getTestFile("argumentsAreSet/pom.xml");
		String surefireConf = null;
		String failsafeConf = FAILSAFE_ARGS_SET;

		IProject project = importProject(pomFile.getAbsolutePath());
		mergePomAndPluginConfigIntoProject(project, pomFile, surefireConf, failsafeConf);
		updateProject(project);
		waitForJobsToComplete();

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTestIT");

		ILaunchConfiguration[] updatedConfigurations = LAUNCH_MANAGER.getLaunchConfigurations(type);
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

		// check systemPropertyVariables with white space in values have values quoted
		assertThat(argLine, Matchers.containsString("-DfailsafePropWithSpaces=\"failsafe Prop With Spaces\""));
		assertThat(argLine, Matchers.containsString("-DfailsafePropWithTab=\"failsafePropWith\tTab\""));
		assertThat(argLine, Matchers.containsString("-DfailsafePropWithCR=\"has\rCR\""));
		assertThat(argLine, Matchers.containsString("-DfailsafePropWithLF=\"has\nLF\""));

	}

	@Test
	public void test_deferred_variable_are_resolved() throws CoreException, IOException, InterruptedException {
		// Get launch type
		ILaunchConfigurationType type = LAUNCH_MANAGER.getLaunchConfigurationType(testType);

		assumeTrue(testType + " support not available", type != null);

		File pomFile = getTestFile("deferredVariables/pom.xml");

		IProject project = importProject(pomFile.getAbsolutePath());

		// create basic unit test
		createDefaultTest(project, type, "test.SomeTest");

		updateProject(project);
		waitForJobsToComplete();

		ILaunchConfiguration[] updatedConfigurations = LAUNCH_MANAGER.getLaunchConfigurations(type);
		assertTrue(updatedConfigurations.length == 1);

		ILaunchConfiguration config = updatedConfigurations[0];
		String argLine = config.getAttribute(UnitTestSupport.LAUNCH_CONFIG_VM_ARGUMENTS, "");
		assertTrue(argLine.contains("-javaagent")); // resolved jacoco agent
		assertTrue(argLine.contains("@{titi.tata}")); // unresolved property is unchanged as in CLI
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
	private void mergePomAndPluginConfigIntoProject(IProject project, File pomTemplate, String surefireConfiguration,
			String failsafeConfiguration) throws IOException, CoreException {
		String pom = Utils.read(project, pomTemplate);
		IFile pomFileWS = project.getFile(pomTemplate.getName());
		String newContent = pom;

		if (surefireConfiguration != null) {
			newContent = newContent.replace(REPLACED_SUREFIRE_POM_STRING, surefireConfiguration);
		}

		if (failsafeConfiguration != null) {
			newContent = newContent.replace(REPLACED_FAILSAFE_POM_STRING, failsafeConfiguration);
		}

		pomFileWS.setContents(new ByteArrayInputStream(newContent.getBytes()), true, false, null);
	}

	private File getTestFile(String filename) throws IOException {
		return new File(FileLocator.toFileURL(getClass().getResource(ROOT_PATH + "/" + filename)).getFile());
	}
}
