/*******************************************************************************
 * Copyright (c) 2022 Konrad Windszus
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Konrad Windszus
 *******************************************************************************/
package org.eclipse.m2e.core.ui.archetype;

import java.util.Collection;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.project.IArchetype;
import org.eclipse.m2e.core.project.MavenProjectInfo;


/**
 * Implementations of this OSGi service interface allow to create new Maven projects based on Maven archetypes.
 * 
 * @since 2.1.0 (package version 1.0.0)
 */
@ProviderType
public interface ArchetypeGenerator {

  /**
   * Creates a project structure using the given archetype in non-interactive mode.
   *
   * @return a list of created Maven projects.
   */
  default Collection<MavenProjectInfo> createArchetypeProjects(IPath location, IArchetype archetype, String groupId,
      String artifactId, String version, String javaPackage, Map<String, String> properties, IProgressMonitor monitor)
      throws CoreException
  {
    return createArchetypeProjects(location, archetype, groupId, artifactId, version, javaPackage, properties, false,
        monitor);
  }

  /**
   * Creates a project structure using the given archetype.
   *
   * @return a list of created Maven projects.
   */
  Collection<MavenProjectInfo> createArchetypeProjects(IPath location, IArchetype archetype, String groupId,
      String artifactId, String version, String javaPackage, Map<String, String> properties, boolean interactive,
      IProgressMonitor monitor) throws CoreException;

}
