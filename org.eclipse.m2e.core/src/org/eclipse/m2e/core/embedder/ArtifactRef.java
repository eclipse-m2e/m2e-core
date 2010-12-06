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
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * @author Igor Fedorenko
 */
public class ArtifactRef implements Serializable {
  private static final long serialVersionUID = -7560496230862532267L;
  
  private final ArtifactKey artifactKey;
  private final String scope;

  public ArtifactRef(Artifact artifact) {
    this.artifactKey = new ArtifactKey(artifact);
    this.scope = artifact.getScope();
  }

  public ArtifactKey getArtifactKey() {
    return artifactKey;
  }
  
  public String getGroupId() {
    return artifactKey.getGroupId();
  }

  public String getArtifactId() {
    return artifactKey.getArtifactId();
  }

  public String getVersion() {
    return artifactKey.getVersion();
  }

  public String getClassifier() {
    return artifactKey.getClassifier();
  }

  public String getScope() {
    return scope;
  }
  
  public static Set<ArtifactKey> toArtifactKey(Set<ArtifactRef> refs) {
    LinkedHashSet<ArtifactKey> keys = new LinkedHashSet<ArtifactKey>(refs.size());
    for (ArtifactRef ref : refs) {
      keys.add(ref.getArtifactKey());
    }
    return keys;
  }

  public static Set<ArtifactRef> fromArtifact(Set<Artifact> artifacts) {
    LinkedHashSet<ArtifactRef> refs = new LinkedHashSet<ArtifactRef>(artifacts.size());
    for (Artifact artifact : artifacts) {
      refs.add(new ArtifactRef(artifact));
    }
    return refs;
  }
}
