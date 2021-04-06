/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class JavaConfigurationTest extends AbstractMavenProjectTestCase {

  @Test
  public void testFileChangeUpdatesJDTSettings() throws CoreException, IOException, InterruptedException {
    ((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
    setAutoBuilding(true);
    File pomFileFS = new File(FileLocator.toFileURL(getClass().getResource("/projects/compilerSettings/pom.xml")).getFile());
    IProject project = importProject(pomFileFS.getAbsolutePath());
    waitForJobsToComplete();
    IJavaProject javaProject = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
    assertEquals("1.8", javaProject.getOption(JavaCore.COMPILER_SOURCE, false));
    IFile pomFileWS = project.getFile("pom.xml");
    byte[] bytes = new byte[(int) pomFileFS.length()];
    try (InputStream stream = pomFileWS.getContents()) {
      stream.read(bytes);
    }
    String contents = new String(bytes);
    contents = contents.replace("1.8", "11");
    pomFileWS.setContents(new ByteArrayInputStream(contents.getBytes()), true, false, null);
    waitForJobsToComplete();
    assertEquals("11", javaProject.getOption(JavaCore.COMPILER_SOURCE, false));
  }
}
