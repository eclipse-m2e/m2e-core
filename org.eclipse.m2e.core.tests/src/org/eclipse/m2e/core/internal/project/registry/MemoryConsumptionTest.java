/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.FileUtils;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MemoryConsumptionTest extends AbstractMavenProjectTestCase {

  @Test
  public void testImportLongBuildChain() throws Exception {
    int[] maxMavenProjectInstancesInContext = new int[] {0};
    MavenPluginActivator.getDefault().getMavenProjectManagerImpl().addContextProjectListener = context -> {
      Map<MavenProject, Object> allMavenProjects = new IdentityHashMap<>();
      context.values().forEach(mavenProject -> {
        while(mavenProject != null) {
          allMavenProjects.put(mavenProject, new Object());
          mavenProject = mavenProject.getParent();
        }
      });
      maxMavenProjectInstancesInContext[0] = Math.max(maxMavenProjectInstancesInContext[0], allMavenProjects.size());
    };
    File tempDirectory = Files.createTempDirectory(getClass().getSimpleName()).toFile();
    int nbProjects = 50;
    Set<File> poms = buildLinearHierarchy(nbProjects, tempDirectory);
    try {
      List<MavenProjectInfo> toImport = poms.stream().map(pom -> new MavenProjectInfo("", pom, null, null))
          .collect(Collectors.toList());
      MavenPlugin.getProjectConfigurationManager().importProjects(toImport, new ProjectImportConfiguration(), null,
          new NullProgressMonitor());
      waitForJobsToComplete(monitor);
      for(IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
        if(p.hasNature(IMavenConstants.NATURE_ID)) {
          poms.remove(p.getFile("pom.xml").getLocation().toFile());
        }
      }
      Assert.assertEquals("Some poms were not imported as project", Collections.emptySet(), poms);
      Assert.assertEquals(nbProjects, maxMavenProjectInstancesInContext[0]);
    } finally {
      MavenPluginActivator.getDefault().getMavenProjectManagerImpl().addContextProjectListener = null;
      FileUtils.deleteDirectory(tempDirectory);
    }
  }

  private Set<File> buildLinearHierarchy(int depth, File tempDirectory) throws FileNotFoundException {
    Set<File> poms = new HashSet<>(depth, 1.f);
    for(int i = 0; i < depth; i++ ) {
      File projectDir = new File(tempDirectory, "p" + i);
      projectDir.mkdirs();
      File pom = new File(projectDir, "pom.xml");
      poms.add(pom);
      try (PrintStream content = new PrintStream(pom);) {
        content.println("<project>");
        content.println("  <modelVersion>4.0.0</modelVersion>");
        content.println("  <groupId>org.eclipse.m2e.core.tests.hierarchy</groupId>");
        content.println("  <artifactId>pNUMBER</artifactId>".replace("NUMBER", Integer.toString(i)));
        content.println("  <version>1</version>");
        content.println("  <packaging>pom</packaging>");
        if(i > 1) {
          content.println("  <parent>");
          content.println("    <groupId>org.eclipse.m2e.core.tests.hierarchy</groupId>");
          content.println("    <artifactId>pNUMBER</artifactId>".replace("NUMBER", Integer.toString(i - 1)));
          content.println("    <version>1</version>");
          content.println("    <relativePath>../pNUMBER</relativePath>".replace("NUMBER", Integer.toString(i - 1)));
          content.println("  </parent>");
        }
        content.println("</project>");
      }
    }
    return poms;
  }
}
