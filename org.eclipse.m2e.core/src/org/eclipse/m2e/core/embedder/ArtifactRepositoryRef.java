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

package org.eclipse.m2e.core.embedder;

import java.io.Serializable;
import java.util.Objects;

import org.apache.maven.artifact.repository.ArtifactRepository;


public class ArtifactRepositoryRef implements Serializable {

  private static final long serialVersionUID = 8859289246547259912L;

  private final String id;

  private final String url;

  private final String username;

  public ArtifactRepositoryRef(ArtifactRepository repository) {
    this.id = repository.getId();
    this.url = repository.getUrl();
    this.username = repository.getAuthentication() != null ? repository.getAuthentication().getUsername() : null;
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, url, username);
  }

  @Override
  public boolean equals(Object o) {
    if(o == this) {
      return true;
    }
    return o instanceof ArtifactRepositoryRef other && //
        Objects.equals(id, other.id) && Objects.equals(url, other.url) && Objects.equals(username, other.username);
  }
}
