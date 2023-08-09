/*******************************************************************************
 * Copyright (c) 2011-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.binaryproject.internal.sourcelookup;

import static org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedSourceLookup.getClasspath;
import static org.eclipse.m2e.binaryproject.internal.sourcelookup.BinaryProjectDescriber.getBinaryLocation;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.sourcelookup.internal.launch.MavenSourceContainerResolver;

// useful to lookup shaded binary projects like wagon-http-2.10-shaded.jar
public class BinaryProjectContainerResolver extends MavenSourceContainerResolver {

  @Override
  protected ISourceContainer resolveSourceContainer(ArtifactKey artifact, IProgressMonitor monitor) {
    String groupId = artifact.groupId();
    String artifactId = artifact.artifactId();
    String version = artifact.version();

    IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();

    IMavenProjectFacade mavenProject = projectRegistry.getMavenProject(groupId, artifactId, version);
    if (mavenProject == null) {
      return null;
    }

    IProject project = mavenProject.getProject();

    try {
      final File binaryLocation = getBinaryLocation(project);
      if (binaryLocation == null) {
        return null;
      }

      IJavaProject javaProject = JavaCore.create(project);

      Map<File, IPackageFragmentRoot> classpath = getClasspath(javaProject);
      IPackageFragmentRoot binary = classpath.remove(binaryLocation);

      if (binary == null) {
        return null; // this is a bug somewhere in my code
      }

      return new PackageFragmentRootSourceContainer(binary);
    } catch (CoreException e) {
      // ignore, maybe log
    }

    return null;
  }
}
