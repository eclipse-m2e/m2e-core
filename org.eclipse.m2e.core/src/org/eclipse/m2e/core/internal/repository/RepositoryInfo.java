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

package org.eclipse.m2e.core.internal.repository;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


public class RepositoryInfo implements IRepository {

  private final String id;

  private final String repositoryUrl;

  private final File basedir;

  private final int scope;

  private final AuthenticationInfo authInfo;

  private String uid;

  private String mirrorId;

  private String mirrorOf;

  private final Set<IPath> projects = new HashSet<>();

  public RepositoryInfo(String id, String repositoryUrl, int scope, AuthenticationInfo authInfo) {
    this(id, repositoryUrl, getBasedir(repositoryUrl), scope, authInfo);
  }

  public RepositoryInfo(String id, String repositoryUrl, File basedir, int scope, AuthenticationInfo authInfo) {
    this.id = id;
    this.repositoryUrl = repositoryUrl;
    this.scope = scope;
    this.authInfo = authInfo;
    this.basedir = basedir;
  }

  @Override
  public AuthenticationInfo getAuthenticationInfo() {
    return authInfo;
  }

  @Override
  public String getUrl() {
    return repositoryUrl;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getMirrorId() {
    return mirrorId;
  }

  @Override
  public String getMirrorOf() {
    return mirrorOf;
  }

  public void setMirrorOf(String mirrorOf) {
    this.mirrorOf = mirrorOf;
  }

  public void setMirrorId(String mirrorId) {
    this.mirrorId = mirrorId;
  }

  public Set<IPath> getProjects() {
    return projects;
  }

  public void addProject(IPath project) {
    if(isScope(IRepositoryRegistry.SCOPE_PROJECT)) {
      projects.add(project);
    }
  }

  public void removeProject(IPath project) {
    projects.remove(project);
  }

  @Override
  public String getUid() {
    if(uid == null) {
      uid = getUid(id, repositoryUrl, authInfo != null ? authInfo.getUserName() : null);
    }

    return uid;
  }

  public static String getUid(String id, String repositoryUrl, String username) {
    StringBuilder sb = new StringBuilder();
    if(id != null) {
      sb.append(id);
    }
    sb.append('|').append(repositoryUrl);
    if(username != null) {
      sb.append('|').append(username);
    }
    String uid;
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
      digest.update(sb.toString().getBytes());
      byte messageDigest[] = digest.digest();
      StringBuilder hexString = new StringBuilder();
      for(byte element : messageDigest) {
        String hex = Integer.toHexString(0xFF & element);
        if(hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      uid = hexString.toString();
    } catch(NoSuchAlgorithmException ex) {
      //this shouldn't happen with MD5
      uid = sb.toString();
      uid = uid.replace(':', '_').replace('/', '_').replace('|', '_');
    }
    return uid;
  }

  @Override
  public String getProtocol() {
    return getProtocol(repositoryUrl);
  }

  // copy&paste from MavenArtifactRepository#protocol
  public static String getProtocol(String repositoryUrl) {
    final int pos = repositoryUrl.indexOf(":"); //$NON-NLS-1$

    if(pos == -1) {
      return "file"; //$NON-NLS-1$
    }
    return repositoryUrl.substring(0, pos).trim();
  }

  public static File getBasedir(String repositoryUrl) {
    if("file".equalsIgnoreCase(getProtocol(repositoryUrl))) { //$NON-NLS-1$
      // dirty trick!
      MavenArtifactRepository trick = new MavenArtifactRepository();
      trick.setUrl(repositoryUrl);
      return new File(trick.getBasedir());
    }
    return null;
  }

  @Override
  public File getBasedir() {
    return basedir;
  }

  @Override
  public boolean isScope(int scope) {
    return (this.scope & scope) != 0;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if(id != null) {
      sb.append(id).append('|');
    }
    sb.append(repositoryUrl);
    return sb.toString();
  }
}
