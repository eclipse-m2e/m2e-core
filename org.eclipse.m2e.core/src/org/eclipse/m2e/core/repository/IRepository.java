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

import java.io.File;

import org.apache.maven.wagon.authentication.AuthenticationInfo;

/**
 * Repository tracked by repository registry.
 *
 * @author igor
 */
public interface IRepository {

  /*
   * Element/attribute names in settings.xml are quite confusing.
   * 
   * "server" defines repository access credentials.
   * 
   * "mirror/id" references server/id, i.e. credentials used to
   * access the mirror. It does NOT identify the mirror.
   * 
   * "repository/id" references server/id but also used to override repository
   * definition. For example, repository with id=central defined in settings.xml
   * overrides definition of central hardcoded in maven code. 
   * 
   */

  /**
   * Repository access credentials. Can be null.
   */
  public AuthenticationInfo getAuthenticationInfo();

  /**
   * Repository URL 
   */
  public String getUrl();

  /**
   * For local repositories, returns basedir of repository contents.
   * 
   * Returns null for remote repositories;
   */
  public File getBasedir();

  /**
   * Repository id element as defined in settings.xml or pom.xml file.
   * 
   * Note that repository id is a reference to server element in settings.xml file,
   * it does not uniquely identify a repository.
   */
  public String getId();

  /**
   * Unique repository id. Generated based on combination of repository url and userId.
   * Can be used to store repository-related information on local filesystem.
   */
  public String getUid();

  /**
   * Indicates that repository id matches mirrorOf clause of a mirror. In other
   * words, all repository requests will be redirected to a mirror.
   * 
   * If null, repository is accessed directly.
   * 
   * TODO decide return value format.
   */
  public String getMirrorId();

  /**
   * For repository mirrors, returns value of mirrorOf element as defined in settings.xml.
   * 
   * Returns null for other repositories.
   */
  public String getMirrorOf();

  /**
   * Protocol part of repository url, i.e. "file", "http", etc.
   */
  public String getProtocol();

  public boolean isScope(int scope);

  /**
   * Human readable repository identifier 
   */
  public String toString();
}
