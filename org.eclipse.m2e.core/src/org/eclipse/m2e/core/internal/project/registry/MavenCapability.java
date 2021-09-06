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

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * MavenCapability
 *
 * @author igor
 */
public class MavenCapability extends Capability {

  private static final long serialVersionUID = 8930981127331238566L;

  /**
   * Regular Maven dependency as defined in <dependency/> pom.xml element.
   */
  public static final String NS_MAVEN_ARTIFACT = "maven-artifact"; //$NON-NLS-1$

  /**
   * Import-scoped Maven dependency as defined in <dependencyManagement/> pom.xml element.
   */
  public static final String NS_MAVEN_ARTIFACT_IMPORT = "maven-artifact-import"; //$NON-NLS-1$

  /**
   * Maven parent dependency as defined in <parent/> pom.xml element.
   */
  public static final String NS_MAVEN_PARENT = "maven-parent"; //$NON-NLS-1$

  private final String version;

  private MavenCapability(String namespace, String id, String version) {
    super(namespace, id);
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return getVersionlessKey().toString() + "/" + version; //$NON-NLS-1$
  }

  @Override
  public int hashCode() {
    int hash = getVersionlessKey().hashCode();
    hash = hash * 17 + version.hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof MavenCapability)) {
      return false;
    }
    MavenCapability other = (MavenCapability) obj;
    return getVersionlessKey().equals(other.getVersionlessKey()) && version.equals(other.version);
  }

  public static MavenCapability createMavenArtifact(ArtifactKey key) {
    return new MavenCapability(NS_MAVEN_ARTIFACT, getId(key), key.getVersion());
  }

  public static MavenCapability createMavenArtifactImport(ArtifactKey key) {
    return new MavenCapability(NS_MAVEN_ARTIFACT_IMPORT, getId(key), key.getVersion());
  }

  public static MavenCapability createMavenParent(ArtifactKey key) {
    return new MavenCapability(NS_MAVEN_PARENT, getId(key), key.getVersion());
  }

  static String getId(ArtifactKey key) {
    StringBuilder sb = new StringBuilder();
    sb.append(key.getGroupId());
    sb.append(':').append(key.getArtifactId());
    if(key.getClassifier() != null) {
      sb.append(':').append(key.getClassifier());
    }
    return sb.toString();
  }

}
