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

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import org.eclipse.m2e.core.embedder.ArtifactKey;


public class MavenRequiredCapability extends RequiredCapability {

  private static final long serialVersionUID = 3254716937353332553L;

  private final String versionRange;

  private final String scope;

  private final boolean optional;

  private final boolean resolved;

  private MavenRequiredCapability(String namespace, String id, String versionRange, String scope, boolean optional,
      boolean resolved) {
    super(namespace, id);

    if(versionRange == null) {
      throw new NullPointerException();
    }

    this.versionRange = versionRange;
    this.scope = scope;
    this.optional = optional;
    this.resolved = resolved;
  }

  public static MavenRequiredCapability createResolvedMavenArtifact(ArtifactKey key, String scope, boolean optional) {
    return new MavenRequiredCapability(MavenCapability.NS_MAVEN_ARTIFACT, MavenCapability.getId(key), key.getVersion(),
        scope, optional, true);
  }

  public static MavenRequiredCapability createMavenArtifact(ArtifactKey key, String scope, boolean optional) {
    return new MavenRequiredCapability(MavenCapability.NS_MAVEN_ARTIFACT, MavenCapability.getId(key), key.getVersion(),
        scope, optional, false);
  }

  public static MavenRequiredCapability createMavenArtifactImport(ArtifactKey key) {
    return new MavenRequiredCapability(MavenCapability.NS_MAVEN_ARTIFACT_IMPORT, MavenCapability.getId(key),
        key.getVersion(), "import", false, false); //$NON-NLS-1$
  }

  public static MavenRequiredCapability createMavenParent(ArtifactKey key) {
    return new MavenRequiredCapability(MavenCapability.NS_MAVEN_PARENT, MavenCapability.getId(key), key.getVersion(),
        null, false, false);
  }

  public static MavenRequiredCapability createResolvedMavenParent(ArtifactKey key) {
    return new MavenRequiredCapability(MavenCapability.NS_MAVEN_PARENT, MavenCapability.getId(key), key.getVersion(),
        null, false, true);
  }

  @Override
  public boolean isPotentialMatch(Capability capability, boolean narrowMatch) {
    if(capability instanceof MavenCapability && getVersionlessKey().equals(capability.getVersionlessKey())) {
      String version = ((MavenCapability) capability).getVersion();

      // not interested in any version, but just in the resolved one
      if(resolved && narrowMatch) {
        return versionRange.equals(version);
      }

      try {
        // TODO may need to cache parsed version and versionRange for performance reasons
        VersionRange range = VersionRange.createFromVersionSpec(versionRange);
        return range.containsVersion(new DefaultArtifactVersion(version));
      } catch(InvalidVersionSpecificationException ex) {
        return true; // better safe than sorry
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getVersionlessKey().toString());
    if(scope != null) {
      sb.append(':').append(scope);
    }
    sb.append('/').append(versionRange);
    if(optional) {
      sb.append("(optional)"); //$NON-NLS-1$
    }
    return sb.toString();
  }

  @Override
  public int hashCode() {
    int hash = getVersionlessKey().hashCode();
    hash = hash * 17 + versionRange.hashCode();
    hash = hash * 17 + (scope != null ? scope.hashCode() : 0);
    hash = hash * 17 + (optional ? 1 : 0);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof MavenRequiredCapability)) {
      return false;
    }
    MavenRequiredCapability other = (MavenRequiredCapability) obj;
    return getVersionlessKey().equals(other.getVersionlessKey()) && versionRange.equals(other.versionRange)
        && eq(scope, other.scope) && optional == other.optional;
  }

}
