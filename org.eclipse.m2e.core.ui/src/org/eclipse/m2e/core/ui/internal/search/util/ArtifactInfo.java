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

package org.eclipse.m2e.core.ui.internal.search.util;

/**
 * Information about the artifact.
 *
 * @author Lukas Krecan
 */
public class ArtifactInfo {
  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String classfier;

  private final String type;

  public ArtifactInfo(String groupId, String artifactId, String version, String classfier, String type) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.classfier = classfier;
    this.type = type;
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

  public String getClassfier() {
    return classfier;
  }

  public String getType() {
    return type;
  }

  /**
   * Constructs a <code>String</code> with all attributes in name = value format.
   *
   * @return a <code>String</code> representation of this object.
   */
  @Override
  public String toString() {
    final String TAB = "    "; //$NON-NLS-1$

    String retValue = ""; //$NON-NLS-1$

    retValue = "ArtifactInfo ( " //$NON-NLS-1$
        + "groupId = " + this.groupId + TAB //$NON-NLS-1$
        + "artifactId = " + this.artifactId + TAB //$NON-NLS-1$
        + "version = " + this.version + TAB //$NON-NLS-1$
        + "classfier = " + this.classfier + TAB //$NON-NLS-1$
        + "type = " + this.type + TAB //$NON-NLS-1$
        + " )"; //$NON-NLS-1$

    return retValue;
  }
}
