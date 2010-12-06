/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

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

  public IFile getWorkspaceArtifact(ArtifactKey key);

}
