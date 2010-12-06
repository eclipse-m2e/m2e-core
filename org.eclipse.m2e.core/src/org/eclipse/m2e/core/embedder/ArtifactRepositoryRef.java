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

package org.eclipse.m2e.core.embedder;

import java.io.Serializable;

import org.apache.maven.artifact.repository.ArtifactRepository;


public class ArtifactRepositoryRef implements Serializable {

  private static final long serialVersionUID = 8859289246547259912L;

  private final String id;

  private final String url;

  private final String username;

  public ArtifactRepositoryRef(ArtifactRepository repository) {
    this.id = repository.getId();
    this.url = repository.getUrl();
    this.username = repository.getAuthentication() != null? repository.getAuthentication().getUsername(): null;
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

  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + (id != null ? id.hashCode() : 0);
    hash = hash * 31 + (url != null ? url.hashCode() : 0);
    hash = hash * 31 + (username != null ? username.hashCode() : 0);
    return hash;
  }

  public boolean equals(Object o) {
    if(o == this) {
      return true;
    }
    if(!(o instanceof ArtifactRepositoryRef)) {
      return false;
    }
    ArtifactRepositoryRef other = (ArtifactRepositoryRef) o;
    return eq(id, other.id) && eq(url, other.url) && eq(username, other.username);
  }

  private static <T> boolean eq(T a, T b) {
    return a != null? a.equals(b): b == null;
  }
}
