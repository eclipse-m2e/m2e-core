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

package org.eclipse.m2e.core.repository;

import java.util.List;

import org.eclipse.m2e.core.embedder.ArtifactRepositoryRef;

/**
 * Registry of repositories accessible by m2e.
 * 
 * The registry automatically tracks the following repositories
 * 
 * <dl>
 * <li>Maven local repository
 * <li>Workspace repository
 * <li>Mirrors defined in settings.xml
 * <li>Repositories and pluginRepositories defined in active profiles in
 *     settings.xml
 * <li>Repositories and pluginRepositories defined in pom.xml files of
 *     workspace Maven projects. 
 * </dl>
 * 
 * @author igor
 */
public interface IRepositoryRegistry {

  /**
   * 
   */
  public static final int SCOPE_UNKNOWN = 1;

  /**
   * Maven local repositories.
   */
  public static final int SCOPE_LOCAL = 1 << 1;

  /**
   * Eclipse workspace repository
   */
  public static final int SCOPE_WORKSPACE = 1 << 2;

  /**
   * Repositories defined in settings.xml file.
   */
  public static final int SCOPE_SETTINGS = 1 << 3;

  /**
   * Repositories defined in pom.xml files of workspace Maven projects
   */
  public static final int SCOPE_PROJECT = 1 << 4;


  public List<IRepository> getRepositories(int scope);

  public IRepository getWorkspaceRepository();

  public IRepository getLocalRepository();

  public IRepository getRepository(ArtifactRepositoryRef repositoryRef);
}
