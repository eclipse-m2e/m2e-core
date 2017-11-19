/*******************************************************************************
 * Copyright (c) 2011-2016 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.binaryproject.internal.sourcelookup;

import static org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedSourceLookup.getClasspath;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.launching.sourcelookup.advanced.IWorkspaceProjectDescriber;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;
import org.eclipse.m2e.binaryproject.internal.BinaryProjectPlugin;

public class BinaryProjectDescriber implements IWorkspaceProjectDescriber {

  static File getBinaryLocation(IProject project) throws CoreException {
    if (!project.isOpen()) {
      return null;
    }
    final String binaryLocation = project.getPersistentProperty(BinaryProjectPlugin.QNAME_JAR);
    if (binaryLocation == null) {
      return null;
    }
    return new File(binaryLocation);
  }

  @Override
  public void describeProject(IJavaProject project, IJavaProjectSourceDescription description) throws CoreException {
    final File binaryLocation = getBinaryLocation(project.getProject());
    if (binaryLocation == null) {
      return;
    }

    Map<File, IPackageFragmentRoot> classpath = getClasspath(project);
    IPackageFragmentRoot binary = classpath.remove(binaryLocation);

    if (binary == null) {
      return; // this is a bug somewhere in my code
    }

    description.addDependencies(classpath);
    description.addLocation(binaryLocation);
    description.addSourceContainerFactory(() -> new PackageFragmentRootSourceContainer(binary));
  }

}
