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

import org.eclipse.osgi.util.NLS;

import org.apache.maven.artifact.Artifact;


public class ArtifactKey implements Serializable {
  private static final long serialVersionUID = -8984509272834024387L;

  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String classifier;

  /**
   * Note that this constructor uses Artifact.getBaseVersion
   */
  public ArtifactKey(Artifact a) {
    this(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), null);
  }

  public ArtifactKey(org.eclipse.aether.artifact.Artifact a) {
    this(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), null);
  }

  public ArtifactKey(String groupId, String artifactId, String version, String classifier) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.classifier = classifier;
  }

  @Override
  public boolean equals(Object o) {
    if(this == o)
      return true;
    if(o instanceof ArtifactKey) {
      ArtifactKey other = (ArtifactKey) o;
      return equals(groupId, other.groupId) && equals(artifactId, other.artifactId) && equals(version, other.version)
          && equals(classifier, other.classifier);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + (groupId != null ? groupId.hashCode() : 0);
    hash = hash * 31 + (artifactId != null ? artifactId.hashCode() : 0);
    hash = hash * 31 + (version != null ? version.hashCode() : 0);
    hash = hash * 31 + (classifier != null ? classifier.hashCode() : 0);
    return hash;
  }

  private static boolean equals(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  // XXX this method does not belong here, it compares versions, while ArtifactKey uses baseVersions in many cases
  public static boolean equals(Artifact a1, Artifact a2) {
    if(a1 == null) {
      return a2 == null;
    }
    if(a2 == null) {
      return false;
    }
    return equals(a1.getGroupId(), a2.getGroupId()) && equals(a1.getArtifactId(), a2.getArtifactId())
        && equals(a1.getVersion(), a2.getVersion()) && equals(a1.getClassifier(), a2.getClassifier());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(groupId).append(':').append(artifactId).append(':').append(version);
    if(classifier != null) {
      sb.append(':').append(classifier);
    }
    return sb.toString();
  }

  public static ArtifactKey fromPortableString(String str) {
    int p, c;

    p = 0;
    c = nextColonIndex(str, p);
    String groupId = substring(str, p, c);

    p = c + 1;
    c = nextColonIndex(str, p);
    String artifactId = substring(str, p, c);

    p = c + 1;
    c = nextColonIndex(str, p);
    String version = substring(str, p, c);

    p = c + 1;
    c = nextColonIndex(str, p);
    String classifier = substring(str, p, c);

    return new ArtifactKey(groupId, artifactId, version, classifier);
  }

  private static String substring(String str, int start, int end) {
    String substring = str.substring(start, end);
    return "".equals(substring) ? null : substring; //$NON-NLS-1$
  }

  private static int nextColonIndex(String str, int pos) {
    int idx = str.indexOf(':', pos);
    if(idx < 0)
      throw new IllegalArgumentException(NLS.bind("Invalid portable string: {0}", str));
    return idx;
  }

  public String toPortableString() {
    StringBuilder sb = new StringBuilder();
    if(groupId != null)
      sb.append(groupId);
    sb.append(':');
    if(artifactId != null)
      sb.append(artifactId);
    sb.append(':');
    if(version != null)
      sb.append(version);
    sb.append(':');
    if(classifier != null)
      sb.append(classifier);
    sb.append(':');
    return sb.toString();
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getClassifier() {
    return classifier;
  }

}
