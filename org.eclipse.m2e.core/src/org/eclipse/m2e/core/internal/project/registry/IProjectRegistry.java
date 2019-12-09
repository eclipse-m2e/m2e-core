/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * Registry of all known workspace maven projects.
 * 
 * @author igor
 */
public interface IProjectRegistry {

  public MavenProjectFacade getProjectFacade(IFile pom);

  public MavenProjectFacade getProjectFacade(String groupId, String artifactId, String version);

  public MavenProjectFacade[] getProjects();

  public Map<ArtifactKey, Collection<IFile>> getWorkspaceArtifacts(String groupId, String artifactId);

}
