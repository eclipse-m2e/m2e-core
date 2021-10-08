/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;

public class RunTest extends AbstractMavenProjectTestCase {

	@Test
	public void testRunTwice() throws Exception {
		IProject project = importProject(FileLocator.toFileURL(getClass().getResource("/projects/basicProjectWithDep/pom.xml")).getPath());
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationWorkingCopy launchConfig = launchManager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION).newInstance(project, "launch");
		launchConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		launchConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "testMvn.TestClass");
		launchConfig.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, project.getLocation().toString());
		IJobFunction assertSuccessfulRun = monitor -> {
			try {
				ILaunch launch = launchConfig.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
				while (!launch.isTerminated()) {
					Thread.sleep(300);
				}
				IProcess process = launch.getProcesses()[0];
				String errorOutput = process.getStreamsProxy().getErrorStreamMonitor().getContents();
				assertEquals("", errorOutput);
				assertEquals(0, process.getExitValue());
				assertEquals("ok", process.getStreamsProxy().getOutputStreamMonitor().getContents());
				return Status.OK_STATUS;
			} catch (Exception e) {
				return new Status(IStatus.ERROR, "org.eclipse.m2e.jdt.tests", e.getMessage(), e);
			}
		};
		assertSuccessfulRun.run(null);
		// Re-run, but from a different thread
		Job launch1 = Job.create("launch1", assertSuccessfulRun);
		launch1.schedule();
		launch1.join();
		assertTrue(launch1.getResult().toString(), launch1.getResult().isOK());
	}
}
