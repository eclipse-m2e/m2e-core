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

import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.metadata.RequiredProperty;

import org.eclipse.m2e.core.project.IArchetype;


/**
 * Provides operations to manage the catalog.
 *
 * @since 2.1.0 (package version 1.0.0)
 */
@ProviderType
public interface ArchetypeManager {

  /**
   * Gets the required properties of an {@link IArchetype}.
   *
   * @param archetype the archetype possibly declaring required properties
   * @param remoteArchetypeRepository the remote archetype repository, can be null
   * @param monitor the progress monitor, can be null
   * @return the required properties of the archetypes, null if none is found
   * @throws CoreException if no archetype can be resolved
   */
  List<RequiredProperty> getRequiredProperties(IArchetype archetype, IProgressMonitor monitor) throws CoreException;

  /**
   * Updates the local catalog to either add or refresh the given archetype.
   * 
   * @param archetype the archetype to add or refresh in the local catalog
   * @throws CoreException
   */
  void updateLocalCatalog(Archetype archetype) throws CoreException;

  /**
   * Returns all archetypes catalogs from the active underlying factories configured in the workspace properties.
   * 
   * @return a map containing the catalog factory id as key and the actual archetype catalogs as value
   * @throws CoreException
   */
  Map<String, ArchetypeCatalog> getActiveArchetypeCatalogs() throws CoreException;

}
