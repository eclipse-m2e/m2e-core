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

package org.eclipse.m2e.scm;

import java.io.File;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.project.MavenProjectInfo;


/**
 * @author Eugene Kuleshov
 */
public class MavenProjectScmInfo extends MavenProjectInfo {

  private final String folderUrl;

  private final String repositoryUrl;

  private final String revision;

  private final String branch;

  private String username;

  private String password;

  private File sslCertificate;

  private String sslCertificatePassphrase;

  public MavenProjectScmInfo(String label, Model model, MavenProjectInfo parent, //
      String revision, String folderUrl, String repositoryUrl) {
    this(label, model, parent, null, revision, folderUrl, repositoryUrl);
  }

  public MavenProjectScmInfo(String label, Model model, MavenProjectInfo parent, //
      String branch, String revision, String folderUrl, String repositoryUrl) {
    super(label, null, model, parent);
    this.revision = revision;
    this.folderUrl = folderUrl;
    this.repositoryUrl = repositoryUrl;
    this.branch = branch;
  }

  public String getBranch() {
    return this.branch;
  }

  public String getRevision() {
    return this.revision;
  }

  public String getFolderUrl() {
    return folderUrl;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof MavenProjectScmInfo info && folderUrl.equals(info.getFolderUrl());
  }

  @Override
  public int hashCode() {
    return folderUrl.hashCode();
  }

  @Override
  public String toString() {
    return getLabel() + " " + folderUrl; //$NON-NLS-1$
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setSSLCertificate(File certificate) {
    this.sslCertificate = certificate;
  }

  public File getSSLCertificate() {
    return sslCertificate;
  }

  public String getSSLCertificatePassphrase() {
    return sslCertificatePassphrase;
  }

  public void setSSLCertificatePassphrase(String passphrase) {
    this.sslCertificatePassphrase = passphrase;
  }
}
