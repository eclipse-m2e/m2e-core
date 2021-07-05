/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.) - Initial implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.TextConsole;
import org.junit.Test;

public class ConsoleTest extends AbstractMavenProjectTestCase {

  @Test
  public void testConsoleHasOutput() throws Exception {
	  var launchManager = DebugPlugin.getDefault().getLaunchManager();
	  var configName = launchManager.generateLaunchConfigurationName("testConsole");
	  var wc = launchManager.getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID).newInstance(null, configName);
	  var pomFile = new File(FileLocator.toFileURL(getClass().getResource("/resources/projects/simplePomOK/pom.xml")).getPath());
	  wc.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, pomFile.getParent());
	  wc.setAttribute(MavenLaunchConstants.ATTR_GOALS, "verify");
	  wc.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, pomFile.getParent());
	  var consoleManager = ConsolePlugin.getDefault().getConsoleManager();
	  var consolesBefore = Arrays.stream(consoleManager.getConsoles()).collect(Collectors.toSet());
	  ILaunch launch = wc.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
	  while (!launch.isTerminated()) {
		  Thread.sleep(200);
	  }
	  Set<IConsole> consolesAfter = new HashSet<>();
	  consolesAfter.addAll(List.of(consoleManager.getConsoles()));
	  consolesAfter.removeAll(consolesBefore);
	  assertEquals("console not found", 1, consolesAfter.size());
	  var mavenConsole = consolesAfter.iterator().next();
	  System.out.println("Console Text: " + ((TextConsole)mavenConsole).getDocument().get());
	  assertTrue("missing output in console", ((TextConsole)mavenConsole).getDocument().get().contains("BUILD SUCCESS"));
  }

}
